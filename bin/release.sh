#!/bin/bash
# release.sh
#
# Performs the build and prepares the release.
# Can be run locally or in CI.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Source common functions
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

show_help() {
  echo "Usage: $0 [OPTIONS] [major|minor|VERSION]"
  echo ""
  echo "Performs the build and prepares the release."
  echo "Can be run locally or in CI."
  echo ""
  echo "Options:"
  echo "  -h, --help    Show this help message"
  echo "  --push        Force pushing to git (default in CI)"
  echo "  --no-push     Disable pushing to git (default locally)"
  echo ""
  echo "Arguments:"
  echo "  major|minor   Force a major or minor version increment."
  echo "  VERSION       The explicit version to release."
}

# Handle options
VERSION=""
SEMVER_ARG=""
PUSH_ARG="--no-push"
[ "$GITHUB_ACTIONS" == "true" ] && PUSH_ARG="--push"

while [[ $# -gt 0 ]]; do
  case $1 in
  -h | --help)
    show_help
    exit 0
    ;;
  --push)
    PUSH_ARG="--push"
    shift
    ;;
  --no-push)
    PUSH_ARG="--no-push"
    shift
    ;;
  major | minor)
    if [ -n "$VERSION" ] || [ -n "$SEMVER_ARG" ]; then
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
    if [ -z "$VERSION" ] && [ -z "$SEMVER_ARG" ]; then
      VERSION=$1
      shift
    else
      show_help
      error "Too many arguments: $1"
    fi
    ;;
  esac
done

# Increase version automatically of not given as a parameter
if [ -z "$VERSION" ]; then
  log "Increasing version..."
  VERSION=$("$SCRIPT_DIR/version-increase-semver.sh" "$SEMVER_ARG")
fi

log "Building and releasing version $VERSION"

# 1. Create and checkout release branch
"$SCRIPT_DIR/release-branch-create.sh" $PUSH_ARG "$VERSION"

# 2. Build project
log "Building project with all targets..."
# Update path for GraalVM if JAVA_HOME is set
if [ -n "$JAVA_HOME" ]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi
"$SCRIPT_DIR/build.sh" --quiet clean all

# 3. Verify the build output
log "Checking build artifacts..."
NATIVE_BINARIES=(
  "fileserv-app/fileserv-app/target/fileserv-app"
  "fileserv-auth-file/fileserv-auth-file-smbpasswd/target/fileserv-smbpasswd"
  "fileserv-test-generate-hierarchy/target/fileserv-test-generate-hierarchy"
  "fileserv-test-webdav/target/fileserv-test-webdav"
)

for artifact in "${NATIVE_BINARIES[@]}"; do
  if [ ! -f "$artifact" ]; then
    error "Native binary not found: $artifact"
  fi
done

log "Build successful."

# 4. Tag and Release
"$SCRIPT_DIR/release-tag.sh" $PUSH_ARG "$VERSION" "${NATIVE_BINARIES[@]}"

# 5. Bump to next snapshot on main
NEXT_SNAPSHOT=$(echo "$VERSION" | awk -F. '{print $1"."($2+1)".0-SNAPSHOT"}')
"$SCRIPT_DIR/release-post.sh" $PUSH_ARG "$NEXT_SNAPSHOT"

log "Release $VERSION completed successfully."
