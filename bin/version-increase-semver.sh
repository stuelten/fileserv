#!/bin/bash
# version-increase-semver.sh
#
# Estimates the next version based on git commit messages since the last tag.
#
# Follows a simple version of Conventional Commits:
# - 'feat:' or 'feat(...):' -> minor increment
# - 'fix:' or 'fix(...):' -> patch increment
# - 'BREAKING CHANGE:' or 'feat!:' -> major increment

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Source common functions
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

show_help() {
  echo "Usage: $0 [OPTIONS] [major|minor]"
  echo ""
  echo "Estimates the next version based on git commit messages since the last tag."
  echo "If 'major' or 'minor' is provided as an argument, it forces that increment."
  echo ""
  echo "Follows a simple version of Conventional Commits:"
  echo " - 'feat:' or 'feat(...):' -> minor increment"
  echo " - 'fix:' or 'fix(...):' -> patch increment"
  echo " - 'BREAKING CHANGE:' or 'feat!:' -> major increment"
  echo ""
  echo "Options:"
  echo "  -h, --help    Show this help message"
  echo ""
  echo "Arguments:"
  echo "  major         Force a major version increment (e.g., 1.2.3 -> 2.0.0)"
  echo "  minor         Force a minor version increment (e.g., 1.2.3 -> 1.3.0)"
}

# Handle options
FORCED_INCR=""

while [[ $# -gt 0 ]]; do
  case $1 in
  -h|--help)
    show_help
    exit 0
    ;;
  major|minor)
    if [ -n "$FORCED_INCR" ]; then
        show_help
        error "Only one increment type can be specified."
    fi
    FORCED_INCR=$1
    shift
    ;;
  *)
    show_help
    error "Unknown parameter $1"
    ;;
  esac
done

CURRENT_VERSION=$("$SCRIPT_DIR"/version-get.sh)
LATEST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "v0.0.0")

# Split version into components
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"

# Default increment is patch if there are any commits, otherwise keep current
INCR="none"

# If forced, skip commit log analysis
if [ -n "$FORCED_INCR" ]; then
    INCR="$FORCED_INCR"
else
    # Get commits since last tag
    if [ "$LATEST_TAG" = "v0.0.0" ]; then
        COMMITS=$(git log --pretty=format:%s)
    else
        COMMITS=$(git log "$LATEST_TAG..HEAD" --pretty=format:%s)
    fi

    if [ -z "$COMMITS" ]; then
        echo "$CURRENT_VERSION"
        exit 0
    fi

    # Determine highest increment level
    while IFS= read -r line; do
        if [[ "$line" == *"BREAKING CHANGE"* ]] || [[ "$line" =~ ^[a-z]+(\(.*\))?!: ]]; then
            INCR="major"
            break
        elif [[ "$line" =~ ^feat(\(.*\))?: ]]; then
            if [ "$INCR" != "major" ]; then
                INCR="minor"
            fi
        elif [[ "$line" =~ ^fix(\(.*\))?: ]]; then
            if [ "$INCR" == "none" ]; then
                INCR="patch"
            fi
        fi
    done <<< "$COMMITS"

    # If no conventional commits found but there are commits, default to patch
    if [ "$INCR" == "none" ] && [ -n "$COMMITS" ]; then
        INCR="patch"
    fi
fi

case "$INCR" in
    major)
        NEXT_VERSION="$((MAJOR + 1)).0.0"
        ;;
    minor)
        NEXT_VERSION="$MAJOR.$((MINOR + 1)).0"
        ;;
    patch)
        NEXT_VERSION="$MAJOR.$MINOR.$((PATCH + 1))"
        ;;
    *)
        NEXT_VERSION="$CURRENT_VERSION"
        ;;
esac

echo "$NEXT_VERSION"
