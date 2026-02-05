#!/bin/bash
# bin/release-tag.sh
#
# Tags the current commit and creates a GitHub release.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

show_help() {
  echo "Usage: $0 [OPTIONS] <VERSION> [ARTIFACTS...]"
  echo ""
  echo "Tags the current commit as v<VERSION> and creates a GitHub release."
  echo ""
  echo "Options:"
  echo "  -h, --help    Show this help message"
  echo "  --push        Push the tag and create GitHub release (default if GITHUB_ACTIONS=true)"
  echo "  --no-push     Do not push the tag or create GitHub release"
}

VERSION=""
ARTIFACTS=()
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
    else
      ARTIFACTS+=("$1")
    fi
    shift
    ;;
  esac
done

if [ -z "$VERSION" ]; then
    error "No version provided."
fi

TAG="v$VERSION"

log "Tagging version $TAG..."
if [ "$GITHUB_ACTIONS" = "true" ]; then
  git config user.name "github-actions[bot]"
  git config user.email "github-actions[bot]@users.noreply.github.com"
fi

# Delete tag if it exists locally (to allow re-running)
git tag -d "$TAG" 2>/dev/null || true
git tag -a "$TAG" -m "Version $VERSION"

if [ "$PUSH" = true ]; then
  log "Pushing tag..."
  git push origin "$TAG" --force

  log "Creating GitHub Release..."
  if command -v gh >/dev/null 2>&1; then
    # Filter ARTIFACTS to only include existing files
    EXISTING_ARTIFACTS=()
    for art in "${ARTIFACTS[@]}"; do
      if [ -f "$art" ]; then
        EXISTING_ARTIFACTS+=("$art")
      else
        warn "Artifact not found, skipping: $art"
      fi
    done

    gh release create "$TAG" \
      "${EXISTING_ARTIFACTS[@]}" \
      --title "Version $VERSION" \
      --notes "Automated release of version $VERSION" \
      --overwrite
  else
    warn "gh CLI not found. Skipping GitHub Release creation."
  fi
else
  warn "Push disabled. Skipping push of tag and GitHub Release creation."
fi
