#!/bin/bash

# Script to sync GitLab applications
# This script fetches projects from specified GitLab groups and generates applications.yaml

set -e

# Force UTF-8 encoding
export LANG=C.UTF-8
export LC_ALL=C.UTF-8

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
YAML_FILE="$SCRIPT_DIR/src/main/resources/applications.yaml"
DEFAULT_YAML="$SCRIPT_DIR/src/main/resources/applications.default.yaml"

# Anonymous mode configuration
ANONYMOUS_MODE="${ANONYMOUS_MODE:-false}"

# Logging functions
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

echo "=========================================="
echo "GitLab Applications Synchronization"
if [ "$ANONYMOUS_MODE" = "true" ]; then
    echo "(ANONYMOUS MODE ENABLED)"
fi
echo "=========================================="

# Check if required tools are installed
if ! command -v curl &> /dev/null; then
    echo "❌ Error: curl is not installed"
    exit 1
fi

if ! command -v jq &> /dev/null; then
    echo "❌ Error: jq is not installed"
    echo "Install it with: sudo apt install jq"
    exit 1
fi

echo "✓ Required tools: curl, jq"

# Check if GITLAB_URL and GITLAB_TOKEN are set
if [ -z "$GITLAB_URL" ] || [ -z "$GITLAB_TOKEN" ]; then
    echo "⚠️  Warning: GITLAB_URL or GITLAB_TOKEN not set"
    echo "   Skipping GitLab synchronization"

    # Use default file if applications.yaml doesn't exist
    if [ ! -f "$YAML_FILE" ] && [ -f "$DEFAULT_YAML" ]; then
        echo "   Using default applications.yaml"
        cp "$DEFAULT_YAML" "$YAML_FILE"
    fi

    exit 0
fi

# Parse command line arguments
GITLAB_GROUPS="${1:-}"
BRANCH="${2:-develop}"

if [ -z "$GITLAB_GROUPS" ]; then
    echo "Usage: $0 <gitlab-groups> [branch]"
    echo ""
    echo "Examples:"
    echo "  $0 'team/backend,team/frontend'"
    echo "  $0 'team/*' develop"
    echo "  $0 'my-group' main"
    echo ""
    echo "⚠️  No GitLab groups specified, using existing applications.yaml"
    exit 0
fi

echo "GitLab URL: $(log_url "$GITLAB_URL")"
echo "Branch: $BRANCH"
echo "Groups: $(log_group "$GITLAB_GROUPS")"
echo ""

# Create temp file
echo "Creating temporary file..."
TEMP_FILE=$(mktemp)
echo "  → Temp file: $TEMP_FILE"
trap "rm -f $TEMP_FILE" EXIT

# Start YAML file
echo "Writing YAML header..."
cat > "$TEMP_FILE" <<EOF
# Auto-generated file - DO NOT EDIT MANUALLY
# Generated on: $(date)
# GitLab URL: $GITLAB_URL
# Groups: $GITLAB_GROUPS

applications:
EOF

echo "  → YAML header written successfully"

# Split groups by comma
echo "Splitting groups by comma..."
echo "  → GITLAB_GROUPS value length: ${#GITLAB_GROUPS}"
echo "  → About to parse groups..."
IFS=',' read -ra GROUPS <<< "$GITLAB_GROUPS" || {
    echo "  ❌ ERROR: Failed to split GITLAB_GROUPS"
    echo "  → This might be a bash version issue or empty variable"
    exit 1
}
echo "  → Found ${#GROUPS[@]} group(s) to process"

PROJECT_COUNT=0

echo ""
for GROUP_PATH in "${GROUPS[@]}"; do
    GROUP_PATH=$(echo "$GROUP_PATH" | xargs) # trim whitespace

    echo "Fetching projects from group: $(log_group "$GROUP_PATH")"

    # URL-encode the group path
    ENCODED_GROUP=$(echo -n "$GROUP_PATH" | jq -sRr @uri)

    echo "  → Calling GitLab API..."
    if [ "$ANONYMOUS_MODE" = "true" ]; then
        echo "  → URL: https://gitlab.example.com/api/v4/groups/GROUP_ENCODED/projects"
    else
        echo "  → URL: $GITLAB_URL/api/v4/groups/$ENCODED_GROUP/projects"
    fi

    # Fetch projects from GitLab API with timeout
    RESPONSE=$(curl -s --fail --max-time 30 --connect-timeout 10 \
        -H "PRIVATE-TOKEN: $GITLAB_TOKEN" \
        "$GITLAB_URL/api/v4/groups/$ENCODED_GROUP/projects?per_page=100" 2>&1)

    CURL_EXIT_CODE=$?

    if [ $CURL_EXIT_CODE -ne 0 ]; then
        echo "  ⚠️  Warning: Failed to fetch projects from group $(log_group "$GROUP_PATH")"
        echo "  → curl exit code: $CURL_EXIT_CODE"
        if [ $CURL_EXIT_CODE -eq 22 ]; then
            echo "  → HTTP error (group not found or no access)"
        elif [ $CURL_EXIT_CODE -eq 28 ]; then
            echo "  → Timeout error"
        elif [ $CURL_EXIT_CODE -eq 6 ]; then
            echo "  → Could not resolve host"
        fi
        continue
    fi

    echo "  → API response received"

    # Check if response is valid JSON
    if ! echo "$RESPONSE" | jq empty 2>/dev/null; then
        echo "  ⚠️  Invalid JSON response from GitLab API"
        echo "  → Response preview: ${RESPONSE:0:200}"
        continue
    fi

    # Count projects first
    PROJECTS_IN_GROUP=$(echo "$RESPONSE" | jq '. | length' 2>/dev/null)

    if [ -z "$PROJECTS_IN_GROUP" ] || [ "$PROJECTS_IN_GROUP" = "null" ]; then
        echo "  ⚠️  Failed to parse project count (response might not be an array)"
        echo "  → Response preview: ${RESPONSE:0:200}"
        continue
    fi

    if [ "$PROJECTS_IN_GROUP" -eq 0 ]; then
        echo "  ℹ️  No projects found in group $(log_group "$GROUP_PATH")"
        continue
    fi

    echo "  → Parsing $PROJECTS_IN_GROUP project(s)..."

    # Parse JSON and create YAML entries
    PARSE_OUTPUT=$(echo "$RESPONSE" | jq -r '.[] |
        "  - name: \"" + .name + "\"" + "\n" +
        "    description: \"" + (.description // "No description") + "\"" + "\n" +
        "    gitlabProjectId: " + (.id | tostring) + "\n" +
        "    gitlabProjectPath: \"" + .path_with_namespace + "\"" + "\n" +
        "    branch: \"'"$BRANCH"'\"" + "\n" +
        "    enabled: true" + "\n" +
        "    freezable: true" + "\n"
    ' 2>&1)

    if [ $? -eq 0 ]; then
        echo "$PARSE_OUTPUT" >> "$TEMP_FILE"
        PROJECT_COUNT=$((PROJECT_COUNT + PROJECTS_IN_GROUP))
        echo "  ✓ Successfully parsed $PROJECTS_IN_GROUP project(s)"
    else
        echo "  ⚠️  Failed to parse projects from group $(log_group "$GROUP_PATH")"
        echo "  → jq error: $PARSE_OUTPUT"
    fi
done

echo ""

if [ $PROJECT_COUNT -eq 0 ]; then
    echo "❌ No projects found"
    echo "   Keeping existing applications.yaml"

    # Use default file if applications.yaml doesn't exist
    if [ ! -f "$YAML_FILE" ] && [ -f "$DEFAULT_YAML" ]; then
        echo "   Using default applications.yaml"
        cp "$DEFAULT_YAML" "$YAML_FILE"
    fi

    exit 0
fi

# Move temp file to final location
mv "$TEMP_FILE" "$YAML_FILE"

echo "✓ Successfully synchronized $PROJECT_COUNT application(s)"
echo "  File: $YAML_FILE"
echo "=========================================="
