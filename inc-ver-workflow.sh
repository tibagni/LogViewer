#!/bin/bash

# Usage: ./inc-ver-workflow.sh <new-version>
# Example: ./inc-ver-workflow.sh 2.8

set -e

if [ $# -lt 1 ]; then
  echo "Usage: $0 <new-version>"
  exit 1
fi

NEW_VERSION="$1"
PROPERTIES_FILE="src/main/resources/properties/app.properties"

# Ensure we're in the repository root
REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null)
if [ -z "$REPO_ROOT" ]; then
  echo "Error: Not inside a git repository."
  exit 1
fi
cd "$REPO_ROOT"

# Check for uncommitted changes
if ! git diff-index --quiet HEAD --; then
  echo "Error: Working directory has uncommitted changes. Please commit or stash them first."
  exit 1
fi

# Read current version
if [ ! -f "$PROPERTIES_FILE" ]; then
  echo "Error: Cannot find $PROPERTIES_FILE"
  exit 1
fi

CURRENT_VERSION=$(grep -E "^version=" "$PROPERTIES_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
if [ -z "$CURRENT_VERSION" ]; then
  echo "Error: Could not read current version from $PROPERTIES_FILE"
  exit 1
fi

echo "Current version: $CURRENT_VERSION"
echo "New version:     $NEW_VERSION"

# Validate that the new version is strictly greater than the current version
IS_GREATER=$(python3 -c '
import sys

def parse(v):
    return [int(x) if x.isdigit() else x for x in v.split(".")]

cur, new = sys.argv[1], sys.argv[2]
try:
    print("true" if parse(new) > parse(cur) else "false")
except Exception:
    print("false")
' "$CURRENT_VERSION" "$NEW_VERSION")

if [ "$IS_GREATER" != "true" ]; then
  echo "Error: New version ($NEW_VERSION) must be strictly greater than current version ($CURRENT_VERSION)."
  exit 1
fi

# Check if tag already exists
if git rev-parse -q --verify "refs/tags/$NEW_VERSION" >/dev/null; then
  echo "Error: Tag '$NEW_VERSION' already exists."
  exit 1
fi

# Update properties file
sed -i -E "s/^version=.*/version=$NEW_VERSION/" "$PROPERTIES_FILE"

# Commit and tag
git add "$PROPERTIES_FILE"
git commit -m "Increment version for release $NEW_VERSION"
git tag "$NEW_VERSION"

echo "Successfully committed and tagged version $NEW_VERSION locally."

# Attempt to push both master and the new tag
echo "Attempting to push master and tag $NEW_VERSION to origin..."
if git push origin master "$NEW_VERSION"; then
  echo ""
  echo "============================================================"
  echo "Successfully pushed master and tag $NEW_VERSION to GitHub!"
  echo "Trigger deployment here:"
  echo "  https://github.com/tibagni/LogViewer/actions/workflows/deploy.yml"
  echo "============================================================"
else
  echo ""
  echo "============================================================"
  echo "Warning: Could not push to origin automatically."
  echo "The version bump commit and tag '$NEW_VERSION' were created locally."
  echo "When ready, push them manually by running:"
  echo "  git push origin master $NEW_VERSION"
  echo "============================================================"
fi
