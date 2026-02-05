#!/bin/bash
# bin/release-post.sh
#
# Bumps to next snapshot on main branch.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

show_help() {
  echo "Usage: $0 [OPTIONS] <NEXT_SNAPSHOT>"
  echo ""
  echo "Bumps the version to <NEXT_SNAPSHOT> on the main branch."
  echo ""
  echo "Options:"
  echo "  -h, --help    Show this help message"
  echo "  --push        Push changes to main (default if GITHUB_ACTIONS=true)"
  echo "  --no-push     Do not push changes"
}

NEXT_SNAPSHOT=""
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
    if [ -z "$NEXT_SNAPSHOT" ]; then
      NEXT_SNAPSHOT=$1
      shift
    else
      show_help
      error "Too many arguments: $1"
    fi
    ;;
  esac
done

if [ -z "$NEXT_SNAPSHOT" ]; then
    error "No next snapshot version provided."
fi

log "Bumping to next snapshot $NEXT_SNAPSHOT on main..."
git checkout main

if [ "$GITHUB_ACTIONS" = "true" ]; then
  git config user.name "github-actions[bot]"
  git config user.email "github-actions[bot]@users.noreply.github.com"
fi

"$SCRIPT_DIR/version-set-snapshot.sh" "$NEXT_SNAPSHOT"
git add "**/pom.xml" "pom.xml" VERSION
git commit -m "chore: Bump snapshot version to $NEXT_SNAPSHOT" || echo "Nothing to commit"

if [ "$PUSH" = true ]; then
  log "Pushing origin main..."
  git push origin main
else
  warn "Push disabled. Skipping push of main branch."
fi
