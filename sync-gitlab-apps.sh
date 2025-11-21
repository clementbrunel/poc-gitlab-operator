#!/bin/bash

# Script to sync GitLab applications
# This script fetches projects from specified GitLab groups and generates applications.yaml

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
YAML_FILE="$SCRIPT_DIR/src/main/resources/applications.yaml"
DEFAULT_YAML="$SCRIPT_DIR/src/main/resources/applications.default.yaml"

echo "=========================================="
echo "GitLab Applications Synchronization"
echo "=========================================="

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

echo "GitLab URL: $GITLAB_URL"
echo "Branch: $BRANCH"
echo "Groups: $GITLAB_GROUPS"
echo ""

# Create temp file
TEMP_FILE=$(mktemp)
trap "rm -f $TEMP_FILE" EXIT

# Start YAML file
cat > "$TEMP_FILE" <<EOF
# Auto-generated file - DO NOT EDIT MANUALLY
# Generated on: $(date)
# GitLab URL: $GITLAB_URL
# Groups: $GITLAB_GROUPS

applications:
EOF

# Split groups by comma
IFS=',' read -ra GROUPS <<< "$GITLAB_GROUPS"

PROJECT_COUNT=0

for GROUP_PATH in "${GROUPS[@]}"; do
    GROUP_PATH=$(echo "$GROUP_PATH" | xargs) # trim whitespace

    echo "Fetching projects from group: $GROUP_PATH"

    # URL-encode the group path
    ENCODED_GROUP=$(echo -n "$GROUP_PATH" | jq -sRr @uri)

    # Fetch projects from GitLab API
    RESPONSE=$(curl -s --fail \
        -H "PRIVATE-TOKEN: $GITLAB_TOKEN" \
        "$GITLAB_URL/api/v4/groups/$ENCODED_GROUP/projects?per_page=100" 2>&1)

    if [ $? -ne 0 ]; then
        echo "⚠️  Warning: Failed to fetch projects from group $GROUP_PATH"
        continue
    fi

    # Parse JSON and create YAML entries
    echo "$RESPONSE" | jq -r '.[] |
        "  - name: \"" + .name + "\"" + "\n" +
        "    description: \"" + (.description // "No description") + "\"" + "\n" +
        "    gitlabProjectId: " + (.id | tostring) + "\n" +
        "    gitlabProjectPath: \"" + .path_with_namespace + "\"" + "\n" +
        "    branch: \"'"$BRANCH"'\"" + "\n" +
        "    enabled: true" + "\n" +
        "    freezable: true" + "\n"
    ' >> "$TEMP_FILE" 2>/dev/null

    if [ $? -eq 0 ]; then
        PROJECTS_IN_GROUP=$(echo "$RESPONSE" | jq '. | length')
        PROJECT_COUNT=$((PROJECT_COUNT + PROJECTS_IN_GROUP))
        echo "  ✓ Found $PROJECTS_IN_GROUP project(s)"
    else
        echo "  ⚠️  Failed to parse projects from group $GROUP_PATH"
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
