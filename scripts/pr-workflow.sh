#!/bin/bash

# Usage: ./scripts/pr-workflow.sh [options] [branch-name]
#
# Helper script to turn local commits into a GitHub Pull Request.

set -e

BASE_BRANCH="master"
TARGET_BRANCH=""
QUICK_MODE=false
IS_DRAFT=false
WATCH_CHECKS=false
OPEN_WEB=false

print_usage() {
  cat <<EOF
Usage: $0 [options] [branch-name]

Turn local commits into a GitHub Pull Request.

Options:
  -q, --quick         Quick mode: push, create PR, switch back to $BASE_BRANCH, and delete local branch
  -d, --draft         Create the pull request as a draft
  -w, --watch         Watch GitHub Actions checks after creating/updating the PR
      --web           Open the PR in your default web browser
  -b, --base <branch> Base branch to target (default: master)
  -h, --help          Show this help message

Examples:
  $0 feat/new-feature           # Create/use branch, push, open PR, stay on branch
  $0                            # Use current branch (if not on master) or prompt for name
  $0 --quick fix/typo           # Quick mode: PR and immediately switch back to master
  $0 -w                         # Push, open PR, and watch CI/AI review checks live
EOF
}

# Parse options
while [ $# -gt 0 ]; do
  case "$1" in
    -q|--quick)
      QUICK_MODE=true
      shift
      ;;
    -d|--draft)
      IS_DRAFT=true
      shift
      ;;
    -w|--watch)
      WATCH_CHECKS=true
      shift
      ;;
    --web)
      OPEN_WEB=true
      shift
      ;;
    -b|--base)
      if [ -z "$2" ]; then
        echo "Error: --base requires a branch name argument."
        exit 1
      fi
      BASE_BRANCH="$2"
      shift 2
      ;;
    -h|--help)
      print_usage
      exit 0
      ;;
    -*)
      echo "Error: Unknown option '$1'."
      print_usage
      exit 1
      ;;
    *)
      if [ -z "$TARGET_BRANCH" ]; then
        TARGET_BRANCH="$1"
      else
        echo "Error: Unexpected argument '$1'."
        print_usage
        exit 1
      fi
      shift
      ;;
  esac
done

# Ensure we're in the repository root
REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || true)
if [ -z "$REPO_ROOT" ]; then
  echo "Error: Not inside a git repository."
  exit 1
fi
cd "$REPO_ROOT"

# Ensure GitHub CLI is available
if ! command -v gh >/dev/null 2>&1; then
  echo "Error: GitHub CLI ('gh') is required but not installed or not in PATH."
  echo "See https://cli.github.com/ for installation instructions."
  exit 1
fi

CURRENT_BRANCH=$(git branch --show-current)
if [ -z "$CURRENT_BRANCH" ]; then
  echo "Error: You are in a detached HEAD state. Please checkout a branch first."
  exit 1
fi

# Determine target branch if not specified
if [ -z "$TARGET_BRANCH" ]; then
  if [ "$CURRENT_BRANCH" != "$BASE_BRANCH" ]; then
    TARGET_BRANCH="$CURRENT_BRANCH"
    echo "Using current branch: '$TARGET_BRANCH'"
  else
    if [ -t 0 ]; then
      read -r -p "You are on $BASE_BRANCH. Enter a branch name for your PR: " TARGET_BRANCH || true
    else
      read -r TARGET_BRANCH 2>/dev/null || true
    fi
    if [ -z "$TARGET_BRANCH" ]; then
      echo "Error: You are on $BASE_BRANCH. Please specify a branch name: $0 <branch-name>"
      exit 1
    fi
  fi
fi

# Branch handling: 3 cases
if [ "$CURRENT_BRANCH" = "$TARGET_BRANCH" ]; then
  echo "Already on branch '$TARGET_BRANCH'."
elif git show-ref --verify --quiet "refs/heads/$TARGET_BRANCH"; then
  echo "⚠️  Branch '$TARGET_BRANCH' already exists locally."
  echo "Your current commit is on '$CURRENT_BRANCH'."
  echo ""
  echo "Options:"
  echo "  [1] Reset '$TARGET_BRANCH' to current HEAD (includes your new commit)"
  echo "  [2] Abort and choose a different branch name"
  CHOICE="2"
  if [ -t 0 ]; then
    read -r -p "Select option [1/2] (default: 2): " CHOICE || true
  else
    read -r CHOICE 2>/dev/null || CHOICE="2"
  fi
  if [ "$CHOICE" = "1" ]; then
    git checkout -B "$TARGET_BRANCH"
    echo "Reset and switched to '$TARGET_BRANCH' at current HEAD."
  else
    echo "Aborted."
    exit 1
  fi
else
  git checkout -b "$TARGET_BRANCH"
  echo "Created and checked out new branch '$TARGET_BRANCH'."
fi

# Fetch latest changes for the base branch
git fetch origin "$BASE_BRANCH" 2>/dev/null || true

# Check commit relationship with origin/$BASE_BRANCH
BEHIND_COUNT=$(git rev-list --count HEAD..origin/$BASE_BRANCH 2>/dev/null || echo 0)
AHEAD_COUNT=$(git rev-list --count origin/$BASE_BRANCH..HEAD 2>/dev/null || echo 0)

if [ "$AHEAD_COUNT" -eq 0 ]; then
  echo "No commits ahead of origin/$BASE_BRANCH. Nothing to PR."
  exit 0
fi

if [ "$BEHIND_COUNT" -gt 0 ]; then
  echo "Warning: Your branch is behind origin/$BASE_BRANCH by $BEHIND_COUNT commits."
  echo "It's recommended to pull or rebase before creating a PR."
  CONT="N"
  if [ -t 0 ]; then
    read -r -p "Continue anyway? (y/N) " CONT || true
  else
    read -r CONT 2>/dev/null || CONT="N"
  fi
  if [[ ! "$CONT" =~ ^[Yy]$ ]]; then
    echo "Aborted."
    exit 1
  fi
fi

# Push branch to origin
echo "Pushing branch '$TARGET_BRANCH' to origin..."
git push -u origin "$TARGET_BRANCH"

# Check if a PR is already open for this branch
EXISTING_PR_URL=$(gh pr list --head "$TARGET_BRANCH" --base "$BASE_BRANCH" --state open --json url --jq '.[0].url' 2>/dev/null || true)

if [ -n "$EXISTING_PR_URL" ]; then
  echo ""
  echo "Pull request already exists for '$TARGET_BRANCH':"
  echo "  $EXISTING_PR_URL"
  echo "Pushed latest commits to origin/$TARGET_BRANCH."
  PR_URL="$EXISTING_PR_URL"
else
  echo "Creating pull request targeting '$BASE_BRANCH'..."
  CREATE_ARGS=("pr" "create" "--base" "$BASE_BRANCH" "--head" "$TARGET_BRANCH" "--fill")
  if [ "$IS_DRAFT" = true ]; then
    CREATE_ARGS+=("--draft")
  fi
  PR_URL=$(gh "${CREATE_ARGS[@]}")
  echo ""
  echo "Pull request created successfully:"
  echo "  $PR_URL"
fi

# Open in browser if requested
if [ "$OPEN_WEB" = true ]; then
  gh pr view "$PR_URL" --web
fi

# Watch checks if requested
if [ "$WATCH_CHECKS" = true ]; then
  echo ""
  echo "Waiting for GitHub Actions checks to start..."
  for i in {1..8}; do
    STATUS=$(gh pr checks "$PR_URL" 2>&1 || true)
    if [[ ! "$STATUS" =~ "no checks reported" ]]; then
      break
    fi
    sleep 2
  done
  echo "Watching GitHub Actions checks..."
  gh pr checks "$PR_URL" --watch || true
fi

# Cleanup (Quick mode vs Default mode)
if [ "$QUICK_MODE" = true ]; then
  echo ""
  echo "Quick mode enabled: cleaning up local branch..."
  git checkout "$BASE_BRANCH"
  if [ "$TARGET_BRANCH" != "$BASE_BRANCH" ]; then
    git branch -D "$TARGET_BRANCH"
  fi
  echo "Switched back to '$BASE_BRANCH' and deleted local branch '$TARGET_BRANCH'."
else
  echo ""
  echo "You are currently on branch '$TARGET_BRANCH'."
  echo "You can make further changes and push, or run with --quick next time for automatic cleanup."
fi