#!/bin/bash

# Usage: ./pr-workflow.sh <new-branch-name>

set -e

if [ $# -lt 1 ]; then
  echo "Usage: $0 <new-branch-name>"
  exit 1
fi

NEW_BRANCH="$1"
BASE_BRANCH="master"

# Fetch latest changes from origin
git fetch origin

# Check if there are commits ahead of origin/master
AHEAD_COUNT=$(git rev-list --count HEAD..origin/$BASE_BRANCH)
BEHIND_COUNT=$(git rev-list --count origin/$BASE_BRANCH..HEAD)

if [ "$BEHIND_COUNT" -eq 0 ]; then
  echo "No new local commits to push. Nothing to PR."
  exit 0
fi

if [ "$AHEAD_COUNT" -ne 0 ]; then
  echo "Warning: Your local branch is behind origin/$BASE_BRANCH by $AHEAD_COUNT commits."
  echo "It's recommended to pull or rebase before creating a PR."
  read -p "Continue anyway? (y/N) " CONT
  if [[ ! "$CONT" =~ ^[Yy]$ ]]; then
    exit 1
  fi
fi

# Create and checkout the new branch from current HEAD
git checkout -b "$NEW_BRANCH"

# Push new branch to origin
git push -u origin "$NEW_BRANCH"

# Create a PR to master, auto-fill the title and body
gh pr create --base "$BASE_BRANCH" --fill

# Go back to the base branch
git checkout "$BASE_BRANCH"

# Delete the new branch
git branch -d $NEW_BRANCH

echo "Pull request created! To merge go to GitHub.com"