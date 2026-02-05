#!/bin/bash
# bin/release-branch-create.sh
#
# Creates a release branch, updates the version, and pushes it.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

show_help() {
  echo "Usage: $0 [OPTIONS] <VERSION>"
  echo ""
  echo "Creates a release branch release-v<VERSION>,"
  echo "sets the version in pom.xml files, and commits/pushes the change."
  echo ""
  echo "Options:"
  echo "  -h, --help    Show this help message"
  echo "  --push        Push the branch to origin (default if GITHUB_ACTIONS=true)"
  echo "  --no-push     Do not push the branch"
}

VERSION=""
PUSH=false
[ "$GITHUB_ACTIONS" = "true" ] && PUSH=true

while [[ $# -gt 0 ]]; do
  case $1 in
  -h | --help)
    show_help
    exit 0
    ;;
  --push)
    PUSH=true
    shift
    ;;
  --no-push)
    PUSH=false
    shift
    ;;
  -*)
    show_help
    error "Unknown parameter $1"
    ;;
  *)
    if [ -z "$VERSION" ]; then
      VERSION=$1
      shift
    else
      show_help
      error "Too many arguments: $1"
    fi
    ;;
  esac
done

if [ -z "$VERSION" ]; then
    error "No version provided."
fi

BRANCH="release-v$VERSION"

log "Creating release branch $BRANCH..."
if [ "$GITHUB_ACTIONS" = "true" ]; then
  git config user.name "github-actions[bot]"
  git config user.email "github-actions[bot]@users.noreply.github.com"
fi

git checkout -b "$BRANCH"

log "Updating Maven versions to $VERSION..."
"$SCRIPT_DIR/version-set.sh" "$VERSION"

log "Committing version change..."
git add "**/pom.xml" "pom.xml" VERSION
git commit -m "chore: release version $VERSION"

if [ "$PUSH" = true ]; then
  log "Pushing release branch..."
  git push origin "$BRANCH" --force
else
  warn "Push disabled. Skipping push of branch $BRANCH."
fi
