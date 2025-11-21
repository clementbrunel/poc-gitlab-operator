#!/bin/bash

# Script to build and start GitLab Deployment Manager

set -e

# Force UTF-8 encoding
export LANG=C.UTF-8
export LC_ALL=C.UTF-8

echo "========================================"
echo "GitLab Deployment Manager - BUILD & RUN"
echo "========================================"
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Error: Maven is not installed"
    echo "Please install Maven first"
    exit 1
fi

MAVEN_VERSION=$(mvn -version | head -n 1)
echo "✓ $MAVEN_VERSION"
echo ""

# Load environment variables from .env if it exists
if [ -f .env ]; then
    echo "✓ Loading variables from .env"
    set -a
    source .env
    set +a
else
    echo "⚠️  .env file not found - GitLab sync will be skipped"
fi
echo ""

# Sync GitLab applications if GITLAB_GROUPS is set
if [ -n "$GITLAB_GROUPS" ]; then
    echo "🔄 Synchronizing applications from GitLab..."
    echo "================================================"
    if [ -x ./sync-gitlab-apps.sh ]; then
        ./sync-gitlab-apps.sh "$GITLAB_GROUPS" "${GITLAB_SYNC_BRANCH:-develop}"
    else
        echo "⚠️  sync-gitlab-apps.sh not found or not executable"
    fi
    echo "================================================"
    echo ""
fi

# Stop the application if it's running
if [ -f app.pid ]; then
    echo "🛑 Stopping running application..."
    ./stop.sh 2>/dev/null || true
    echo ""
fi

# Build the application
echo "🔨 Building application with Maven..."
echo "================================================"
mvn clean package -DskipTests
echo "================================================"
echo ""

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "✓ Build successful"
    echo ""

    # Start the application
    ./start.sh
else
    echo "❌ Build failed"
    exit 1
fi
