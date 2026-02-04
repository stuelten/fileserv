#!/bin/bash
#
# Set a new version in maven pom.xml files.
#
# Either use the version given on the command line or use version-increase-semver.sh to get the next version.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Source common functions
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

# Change to the project root directory
cd "$SCRIPT_DIR/.."

show_help() {
  echo "Usage: $0 [OPTIONS] [major|minor|NEW_VERSION]"
  echo ""
  echo "Sets a new version."   
  echo "Updates versions in maven pom.xml files and VERSION file."
  echo ""
  echo "Options:"
  echo "  -h, --help    Show this help message"
  echo ""
  echo "Arguments:"
  echo "  major|minor   Force a major or minor version increment."
  echo "  NEW_VERSION   The explicit version to set."
  echo "                If not provided, version-increase-semver.sh is used."
}

# Handle options
NEW_VERSION=""
SEMVER_ARG=""

while [[ $# -gt 0 ]]; do
  case $1 in
  -h|--help)
    show_help
    exit 0
    ;;
  major|minor)
    if [ -n "$NEW_VERSION" ] || [ -n "$SEMVER_ARG" ]; then
      show_help
      error "Too many arguments: $1"
    fi
    SEMVER_ARG=$1
    shift
    ;;
  -*)
    show_help
    error "Unknown parameter $1"
    ;;
  *)
    if [ -z "$NEW_VERSION" ] && [ -z "$SEMVER_ARG" ]; then
      NEW_VERSION=$1
      shift
    else
      show_help
      error "Too many arguments: $1"
    fi
    ;;
  esac
done

# If no version or semver arg is provided, estimate it using version-increase-semver.sh
if [ -z "$NEW_VERSION" ]; then
  if [ -n "$SEMVER_ARG" ]; then
    log "Increasing version ($SEMVER_ARG)..."
    NEW_VERSION=$(./bin/version-increase-semver.sh "$SEMVER_ARG")
  else
    log "No version provided. Automatically increase version..."
    NEW_VERSION=$(./bin/version-increase-semver.sh)
  fi
fi

log "Setting version to: $NEW_VERSION"

# Use the Maven Wrapper to set the new version across all modules
# -DgenerateBackupPoms=false prevents creating pom.xml.versionsBackup files
./mvnw versions:set -DnewVersion="$NEW_VERSION" -DgenerateBackupPoms=false

# Update VERSION file
echo "$NEW_VERSION" > VERSION

log "Successfully updated to version $NEW_VERSION"
