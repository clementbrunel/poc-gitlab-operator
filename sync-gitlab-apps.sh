#!/bin/bash
# Usage:
#   ./sync-gitlab-apps.sh <gitlab-groups> [branch] [--dry-run]
# Examples:
#   ./sync-gitlab-apps.sh "team/backend,team/frontend" develop
#   ./sync-gitlab-apps.sh "team/*" main --dry-run

set -euo pipefail

# Force UTF-8
export LANG=C.UTF-8
export LC_ALL=C.UTF-8

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
YAML_FILE="$SCRIPT_DIR/src/main/resources/applications.yaml"
DEFAULT_YAML="$SCRIPT_DIR/src/main/resources/applications.default.yaml"

ANONYMOUS_MODE="${ANONYMOUS_MODE:-false}"
DRY_RUN=false

# Requirements
if ! command -v curl &>/dev/null; then
  echo "❌ Error: curl not found"
  exit 1
fi
if ! command -v jq &>/dev/null; then
  echo "❌ Error: jq not found (required)"
  echo "Install it: sudo apt install jq"
  exit 1
fi

# ---- Helpers ----
log_url() {
  if [ "$ANONYMOUS_MODE" = "true" ]; then
    echo "https://gitlab.example.com"
  else
    echo "$1"
  fi
}

log_group() {
  if [ "$ANONYMOUS_MODE" = "true" ]; then
    echo "GROUP_ANONYMIZED"
  else
    echo "$1"
  fi
}

usage() {
  cat <<EOF
Usage: $0 <gitlab-groups> [branch] [--dry-run]

<gitlab-groups> : comma-separated groups or patterns (e.g. team/backend,team/*)
[branch]        : optional, default "develop"
--dry-run       : do everything except overwrite applications.yaml
Environment variables required:
  GITLAB_URL    : e.g. https://gitlab.example.com
  GITLAB_TOKEN  : personal access token with API read access

EOF
  exit 1
}

# ---- Parse args ----
if [ $# -lt 1 ]; then
  usage
fi

GITLAB_GROUPS_RAW="$1"
BRANCH="${2:-develop}"
if [ "${3:-}" = "--dry-run" ] || [ "${2:-}" = "--dry-run" ] || [ "${1:-}" = "--dry-run" ]; then
  DRY_RUN=true
  # If --dry-run was given in place of branch, shift branch to default
  if [ $# -ge 2 ] && [ "$2" = "--dry-run" ]; then
    BRANCH="${3:-develop}"
  fi
fi

# ---- Validate env ----
if [ -z "${GITLAB_URL:-}" ] || [ -z "${GITLAB_TOKEN:-}" ]; then
  echo "⚠️  Warning: GITLAB_URL or GITLAB_TOKEN not set. Skipping GitLab synchronization."
  if [ ! -f "$YAML_FILE" ] && [ -f "$DEFAULT_YAML" ]; then
    echo "   Using default applications.yaml"
    cp "$DEFAULT_YAML" "$YAML_FILE"
  fi
  exit 0
fi

echo "=========================================="
echo "GitLab Applications Synchronization"
[ "$ANONYMOUS_MODE" = "true" ] && echo "(ANONYMOUS MODE ENABLED)"
echo "=========================================="
echo "GitLab URL: $(log_url "$GITLAB_URL")"
echo "Branch: $BRANCH"
echo "Groups (raw): $(log_group "$GITLAB_GROUPS_RAW")"
echo "Dry-run: $DRY_RUN"
echo "=========================================="

# ---- Utilities for API calls ----
# perform a curl and return response body and headers separately
# args: url
curl_api() {
  local url="$1"
  # We'll capture headers to a temp file and body to stdout
  local headers
  headers="$(mktemp)"
  # curl writes body to stdout and headers to $headers
  if ! curl -sS --fail --max-time 30 --connect-timeout 10 -H "PRIVATE-TOKEN: $GITLAB_TOKEN" -D "$headers" "$url"; then
    local ec=$?
    echo "CURL_ERROR:$ec" > "$headers"
    cat "$headers"
    rm -f "$headers"
    return 22  # indicate curl failed (we'll handle upstream)
  fi
  # print headers path marker then print body (so caller can separate)
  printf '%s\n' "----HEADERS-START----"
  sed -n '1,200p' "$headers"
  printf '%s\n' "----HEADERS-END----"
  rm -f "$headers"
  return 0
}

# Helper to iterate paginated endpoints - yields all JSON arrays concatenated
# usage: fetch_all_pages "$base_url" -> prints concatenated JSON arrays elements (newline separated JSON elements)
fetch_all_pages() {
  local base_url="$1"
  local page=1
  local per_page=100
  local collected=false

  # We'll fetch pages until we get an empty array / no next page
  while true; do
    local url="${base_url}&per_page=${per_page}&page=${page}"
    # Use curl and capture body to variable (we'll ignore headers for page control because GitLab sometimes returns x-next-page)
    local tmp_headers tmp_body
    tmp_headers="$(mktemp)"
    tmp_body="$(mktemp)"

    if ! curl -sS --fail --max-time 30 --connect-timeout 10 -D "$tmp_headers" -H "PRIVATE-TOKEN: $GITLAB_TOKEN" "$url" -o "$tmp_body"; then
      # print error with context
      echo "CURL_FAILED_PAGE:$page" >&2
      cat "$tmp_body" >&2 || true
      rm -f "$tmp_headers" "$tmp_body"
      return 22
    fi

    # Ensure valid JSON array
    if ! jq -e . "$tmp_body" >/dev/null 2>&1; then
      echo "INVALID_JSON_PAGE:$page" >&2
      head -c 400 "$tmp_body" >&2
      rm -f "$tmp_headers" "$tmp_body"
      return 23
    fi

    # Emit elements one per line as JSON
    if jq -c '.[]' "$tmp_body" 2>/dev/null | sed -n '1,100000p'; then
      cat "$tmp_body" | jq -c '.[]' || true
      collected=true
    fi

    # Check X-Next-Page header
    local next_page
    next_page="$(awk 'BEGIN{IGNORECASE=1} /^x-next-page:/ {gsub(/\r/,"",$2); print $2}' "$tmp_headers" | tr -d '\r')"
    # Some setups might use Link header; fallback to checking if returned number of items < per_page
    local count
    count=$(jq '. | length' "$tmp_body" 2>/dev/null || echo 0)

    rm -f "$tmp_headers" "$tmp_body"

    if [ -n "$next_page" ] && [ "$next_page" != "0" ]; then
      page="$next_page"
      continue
    fi

    if [ "$count" -lt "$per_page" ]; then
      # last page
      break
    fi

    # otherwise increment page
    page=$((page + 1))
  done

  # if nothing collected, return success but nothing printed
  return 0
}

# ---- Process groups list (support comma-separated and wildcards) ----
IFS=',' read -r -a GROUP_PATTERNS <<< "$GITLAB_GROUPS_RAW"

# We'll build an array of resolved group IDs/paths (path_with_namespace)
declare -a RESOLVED_GROUP_PATHS=()

for rawpat in "${GROUP_PATTERNS[@]}"; do
  # trim
  pat="$(echo "$rawpat" | xargs)"
  [ -z "$pat" ] && continue

  # If pattern contains wildcard characters '*', '?' or '[' then we treat it as glob
  if [[ "$pat" == *"*"* || "$pat" == *"?"* || "$pat" == *"["* ]]; then
    echo "Resolving pattern: $(log_group "$pat")"
    # Use the portion before first slash as a search term if exists, else whole pat without wildcards for better matches
    search_term="${pat%%/*}"
    # remove wildcard chars for search fallback
    search_term="${search_term//\*/}"
    search_term="${search_term//\?/}"
    search_term="${search_term//\[}"
    search_term="${search_term//\]}"
    if [ -z "$search_term" ]; then
      search_term=""
    fi

    # Fetch groups pages with search param (if search_term empty we still fetch with no search)
    base_groups_url="${GITLAB_URL}/api/v4/groups?per_page=100"
    if [ -n "$search_term" ]; then
      base_groups_url="${GITLAB_URL}/api/v4/groups?search=$(printf '%s' "$search_term" | jq -sRr @uri)"
    else
      base_groups_url="${GITLAB_URL}/api/v4/groups"
    fi

    # Iterate all groups and filter by shell glob against path_with_namespace
    if ! group_items="$(fetch_all_pages "${base_groups_url}")"; then
      echo "  ⚠️  Warning: failed to fetch groups for pattern $pat, skipping." >&2
      continue
    fi

    # group_items are newline-separated compact JSON objects; iterate and match
    while IFS= read -r group_json; do
      if [ -z "$group_json" ]; then continue; fi
      gp_path="$(echo "$group_json" | jq -r '.path_with_namespace')"
      # Match shell-glob
      if [[ "$gp_path" == $pat ]]; then
        RESOLVED_GROUP_PATHS+=("$gp_path")
        echo "  → Matched group: $(log_group "$gp_path")"
      fi
    done <<< "$group_items"

  else
    # exact group path provided
    RESOLVED_GROUP_PATHS+=("$pat")
  fi
done

# Remove duplicates
mapfile -t RESOLVED_GROUP_PATHS < <(printf '%s\n' "${RESOLVED_GROUP_PATHS[@]}" | awk '!seen[$0]++')

if [ ${#RESOLVED_GROUP_PATHS[@]} -eq 0 ]; then
  echo "No groups resolved from input patterns."
  exit 0
fi

echo "Groups to process (${#RESOLVED_GROUP_PATHS[@]}):"
for g in "${RESOLVED_GROUP_PATHS[@]}"; do
  echo " - $(log_group "$g")"
done

# ---- Create temporary YAML file ----
TEMP_FILE="$(mktemp)"
trap 'rm -f "$TEMP_FILE"' EXIT

cat > "$TEMP_FILE" <<EOF
# Auto-generated file - DO NOT EDIT MANUALLY
# Generated on: $(date -u +"%Y-%m-%dT%H:%M:%SZ")
# GitLab URL: $(log_url "$GITLAB_URL")
# Groups: $GITLAB_GROUPS_RAW

applications:
EOF

PROJECT_COUNT=0

# ---- For each group, fetch projects (with pagination) ----
for GROUP_PATH in "${RESOLVED_GROUP_PATHS[@]}"; do
  echo ""
  echo "Fetching projects for group: $(log_group "$GROUP_PATH")"

  ENCODED_GROUP="$(printf '%s' "$GROUP_PATH" | jq -sRr @uri)"
  base_projects_url="${GITLAB_URL}/api/v4/groups/${ENCODED_GROUP}/projects?include_subgroups=true&simple=true"

  # fetch all project elements (one JSON object per line)
  if ! projects_json="$(fetch_all_pages "$base_projects_url")"; then
    echo "  ⚠️  Warning: failed to fetch projects for group $(log_group "$GROUP_PATH"), skipping."
    continue
  fi

  # count
  count="$(echo "$projects_json" | sed '/^\s*$/d' | wc -l || true)"
  if [ -z "$count" ] || [ "$count" -eq 0 ]; then
    echo "  ℹ️  No projects found in group $(log_group "$GROUP_PATH")"
    continue
  fi

  echo "  → Found $count project(s). Parsing..."

  # For each project JSON object, append a YAML entry using jq to escape strings safely via @json
  # We'll reformat project line-by-line
  while IFS= read -r pj; do
    [ -z "$pj" ] && continue
    # Build YAML block using jq (JSON strings are valid YAML scalars)
    entry="$(echo "$pj" | jq -r --arg branch "$BRANCH" '
      "  - name: " + (.name | @json) +
      "\n    description: " + ((.description // "No description") | @json) +
      "\n    gitlabProjectId: " + (.id|tostring) +
      "\n    gitlabProjectPath: " + (.path_with_namespace | @json) +
      "\n    branch: " + ($branch | @json) +
      "\n    enabled: true" +
      "\n    freezable: true"
    ')"

    if [ -n "$entry" ]; then
      printf '%s\n' "$entry" >> "$TEMP_FILE"
      PROJECT_COUNT=$((PROJECT_COUNT + 1))
    fi
  done <<< "$projects_json"

  echo "  ✓ Parsed projects for group $(log_group "$GROUP_PATH")"
done

echo ""
if [ "$PROJECT_COUNT" -eq 0 ]; then
  echo "❌ No projects found across all groups"
  echo "   Keeping existing applications.yaml (or using default if missing)"
  if [ ! -f "$YAML_FILE" ] && [ -f "$DEFAULT_YAML" ]; then
    echo "   Using default applications.yaml"
    cp "$DEFAULT_YAML" "$YAML_FILE"
  fi
  exit 0
fi

# ---- Install the file (or dry-run show) ----
if [ "$DRY_RUN" = "true" ]; then
  echo ""
  echo "----- DRY RUN: generated YAML preview -----"
  sed -n '1,400p' "$TEMP_FILE" || true
  echo "----- End preview (DRY RUN) -----"
else
  mkdir -p "$(dirname "$YAML_FILE")"
  mv "$TEMP_FILE" "$YAML_FILE"
  # clear trap for temp file (we moved it)
  trap - EXIT
  echo ""
  echo "✓ Successfully synchronized $PROJECT_COUNT application(s)"
  echo "  File: $YAML_FILE"
fi

echo "=========================================="
