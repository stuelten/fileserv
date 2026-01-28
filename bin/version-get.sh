#!/bin/bash
# version-get.sh
#
# get the version based on git commit messages since the last tag.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Source common functions
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

show_help() {
  echo "Usage: ./bin/version-get.sh [OPTIONS]"
  echo ""
  echo "Either get the version based on git commit messages since the last tag or use version from pom.xml."
  echo ""
  echo "Options:"
  echo "  -h, --help    Show this help message"
}

# Handle options
while [[ $# -gt 0 ]]; do
  case $1 in
  -h|--help)
    show_help
    exit 0
    ;;
  *)
    show_help
    error "Unknown parameter $1"
    ;;
  esac
done

# Default to the version from pom.xml if no tags found
get_base_version() {
    LATEST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
    if [ -n "$LATEST_TAG" ]; then
        echo "${LATEST_TAG#v}"
    else
        # Fallback to pom.xml version, removing -SNAPSHOT
        ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout | grep -v "\[" | tail -n 1 | sed 's/-SNAPSHOT//'
    fi
}

CURRENT_VERSION=$(get_base_version)
echo "$CURRENT_VERSION"
