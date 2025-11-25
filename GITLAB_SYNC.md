# GitLab Applications Auto-Sync

This document explains how to automatically synchronize the list of applications from GitLab groups.

## Overview

Instead of manually maintaining the `applications.yaml` file, you can configure the application to automatically fetch projects from GitLab groups at startup.

## How It Works

1. **At startup**, if `GITLAB_GROUPS` is configured, the `sync-gitlab-apps.sh` script runs
2. The script **fetches projects** from the specified GitLab groups using the API
3. It **generates** the `applications.yaml` file with all found projects
4. If sync fails or is not configured, the application uses **`applications.default.yaml`** as fallback

## Configuration

### Option 1: Automatic Sync (Recommended)

Add these variables to your `.env` file:

```bash
# GitLab connection (required)
GITLAB_URL=https://gitlab.company.com
GITLAB_TOKEN=glpat-your-token

# Auto-sync configuration
GITLAB_GROUPS=team/backend,team/frontend
GITLAB_SYNC_BRANCH=develop
```

The application will automatically sync at each startup.

### Option 2: Manual Sync

Run the sync script manually:

```bash
# Load environment variables
source .env

# Sync from specific groups
./sync-gitlab-apps.sh "team/backend,team/frontend"

# Sync from all projects in a group
./sync-gitlab-apps.sh "my-organization"

# Sync with custom branch
./sync-gitlab-apps.sh "team/*" main
```

### Option 3: Manual Configuration

Edit `src/main/resources/applications.yaml` manually (traditional method).

## GITLAB_GROUPS Format

The `GITLAB_GROUPS` variable accepts a comma-separated list of GitLab group paths:

### Examples

```bash
# Single group
GITLAB_GROUPS=team/backend

# Multiple groups
GITLAB_GROUPS=team/backend,team/frontend,infra/tools

# Organization-level group
GITLAB_GROUPS=my-company

# Nested groups
GITLAB_GROUPS=company/engineering/backend,company/engineering/frontend
```

## How to Find Group Paths

1. Go to your GitLab instance
2. Navigate to **Groups**
3. Select your group
4. The URL shows the group path: `https://gitlab.com/<group-path>`
5. Use this path in `GITLAB_GROUPS`

**Example:**
- URL: `https://gitlab.company.com/engineering/backend`
- Group path: `engineering/backend`

## Sync Script Behavior

### Success Scenario

```
Fetching projects from group: team/backend
  ✓ Found 5 project(s)
Fetching projects from group: team/frontend
  ✓ Found 3 project(s)

✓ Successfully synchronized 8 application(s)
  File: src/main/resources/applications.yaml
```

### Fallback Scenarios

**Case 1: GitLab not configured**
```
⚠️  Warning: GITLAB_URL or GITLAB_TOKEN not set
   Skipping GitLab synchronization
   Using default applications.yaml
```
→ Uses `applications.default.yaml`

**Case 2: No projects found**
```
❌ No projects found
   Keeping existing applications.yaml
```
→ Keeps current `applications.yaml` or uses default

**Case 3: API error**
```
⚠️  Warning: Failed to fetch projects from group team/backend
```
→ Continues with other groups

## Generated File Format

The sync script generates a YAML file with this structure:

```yaml
# Auto-generated file - DO NOT EDIT MANUALLY
# Generated on: 2025-11-21 10:30:45
# GitLab URL: https://gitlab.company.com
# Groups: team/backend,team/frontend

applications:
  - name: "Backend API"
    description: "Main REST API"
    gitlabProjectId: 123
    gitlabProjectPath: "team/backend/api"
    branch: "develop"
    enabled: true
    freezable: true

  - name: "Frontend Web"
    description: "Web application"
    gitlabProjectId: 456
    gitlabProjectPath: "team/frontend/web"
    branch: "develop"
    enabled: true
    freezable: true
```

## Customization After Sync

If you need to customize applications after sync:

### Option 1: Disable auto-sync for specific adjustments

1. Remove or comment `GITLAB_GROUPS` from `.env`
2. Manually edit `applications.yaml`
3. The file won't be overwritten on next startup

### Option 2: Mix auto-sync with manual entries

1. Run sync script to generate base file
2. Manually add custom applications
3. Disable auto-sync to prevent overwriting

### Option 3: Use manual sync only when needed

```bash
# Sync once, then configure manually
./sync-gitlab-apps.sh "team/*"
# Edit applications.yaml as needed
# Don't set GITLAB_GROUPS in .env
```

## Sync Schedule

The sync only happens **at application startup**. To re-sync:

1. Restart the application: `./stop.sh && ./start.sh`
2. Or run sync script manually: `./sync-gitlab-apps.sh "group/path"`

**Note:** There is no automatic periodic sync while the application is running.

## Requirements

The sync script requires:

- `curl` - For API calls
- `jq` - For JSON parsing

Install on Ubuntu/Debian:
```bash
sudo apt install curl jq
```

Install on RHEL/CentOS:
```bash
sudo dnf install curl jq
```

## GitLab Token Permissions

The GitLab token must have these permissions:

- ✓ `read_api` - Read GitLab API
- ✓ `read_repository` - Read project details

Create a token at: `https://your-gitlab.com/-/profile/personal_access_tokens`

## Troubleshooting

### Sync script not running

Check:
```bash
# Script is executable?
ls -l sync-gitlab-apps.sh

# Make it executable
chmod +x sync-gitlab-apps.sh
```

### No projects found

Possible causes:
1. **Wrong group path** - Verify the group path in GitLab URL
2. **Token permissions** - Token must have `read_api` access
3. **Group access** - Token owner must have access to the group
4. **Empty group** - The group doesn't contain any projects

### API errors

Check:
```bash
# Test GitLab API manually
curl -H "PRIVATE-TOKEN: $GITLAB_TOKEN" \
  "$GITLAB_URL/api/v4/groups/your-group/projects"
```

If this fails, check:
- GitLab URL is correct
- Token is valid
- Network connectivity

### Wrong branch

By default, sync uses `develop` branch. To change:

```bash
# In .env
GITLAB_SYNC_BRANCH=main

# Or when running manually
./sync-gitlab-apps.sh "team/backend" main
```

## Integration with CI/CD

You can sync during deployment:

```yaml
# .gitlab-ci.yml example
deploy:
  script:
    - export GITLAB_GROUPS="team/backend,team/frontend"
    - ./sync-gitlab-apps.sh "$GITLAB_GROUPS"
    - ./start.sh
```

## Security Notes

- ✅ **Token is never stored** in generated files
- ✅ **applications.yaml** is safe to commit (no secrets)
- ❌ **Never commit `.env`** (contains token)
- ✅ Use **read-only tokens** (read_api, read_repository only)

## Fallback Strategy

The application uses a three-tier fallback:

1. **Auto-generated** - If GITLAB_GROUPS is set and sync succeeds
2. **Existing file** - If sync fails, keeps current applications.yaml
3. **Default file** - If no file exists, uses applications.default.yaml

This ensures the application always has applications configured, even if GitLab is unavailable.

## Anonymous Mode for Debugging

When troubleshooting sync issues, you can enable anonymous mode to hide sensitive information from logs.

### Enable Anonymous Mode

**Option 1: Environment variable**
```bash
export ANONYMOUS_MODE=true
./sync-gitlab-apps.sh "your-groups" develop 2>&1 | tee output.log
```

**Option 2: Inline**
```bash
ANONYMOUS_MODE=true ./sync-gitlab-apps.sh "your-groups" develop 2>&1 | tee output.log
```

**Option 3: In .env file**
```bash
ANONYMOUS_MODE=true
```

### What Gets Anonymized

When `ANONYMOUS_MODE=true`:

| Information | Normal Mode | Anonymous Mode |
|-------------|-------------|----------------|
| GitLab URL | `https://gitlab.company.com` | `https://gitlab.example.com` |
| Group names | `team/backend` | `GROUP_ANONYMIZED` |
| API URLs | Full URL with group path | Generic URL |
| Branch name | `develop` | `develop` (visible) |
| Error messages | Full details | Full details (visible) |
| Project count | Number of projects | Number of projects (visible) |

### Example Output Comparison

**Normal Mode:**
```
GitLab URL: https://gitlab.company.com
Branch: develop
Groups: team/backend,team/frontend
Fetching projects from group: team/backend
  → URL: https://gitlab.company.com/api/v4/groups/team%2Fbackend/projects
```

**Anonymous Mode:**
```
GitLab Applications Synchronization
(ANONYMOUS MODE ENABLED)
==========================================
GitLab URL: https://gitlab.example.com
Branch: develop
Groups: GROUP_ANONYMIZED
Fetching projects from group: GROUP_ANONYMIZED
  → URL: https://gitlab.example.com/api/v4/groups/GROUP_ENCODED/projects
```

This allows you to safely share logs for troubleshooting without exposing your GitLab infrastructure details.
