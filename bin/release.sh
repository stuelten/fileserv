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
  echo ""
  echo "Arguments:"
  echo "  major|minor   Force a major or minor version increment."
  echo "  VERSION       The explicit version to release."
}

# Handle options
VERSION=""
SEMVER_ARG=""

while [[ $# -gt 0 ]]; do
  case $1 in
  -h | --help)
    show_help
    exit 0
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

# 1. Update version in poms
log "Updating Maven versions to $VERSION..."
"$SCRIPT_DIR/version-set.sh" "$VERSION"

# 2. Build project
log "Building project with native profile..."
# Update path for GraalVM if JAVA_HOME is set
if [ -n "$JAVA_HOME" ]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi
"$SCRIPT_DIR/build.sh" --quiet --buildNativeBinaries clean

# 3. Verify the build output
log "Checking build artifacts..."
NATIVE_BINARIES=(
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

# CI specific: Git configuration for tagging
if [ "$GITHUB_ACTIONS" = "true" ]; then
  git config user.name "github-actions[bot]"
  git config user.email "github-actions[bot]@users.noreply.github.com"
fi

# 4. Commit, tag, and push
log "Committing version change..."
git add "**/pom.xml" "pom.xml"
git commit -m "chore: release version $VERSION"
git tag -a "v$VERSION" -m "Version $VERSION"

log "Pushing changes and tag..."
git push origin main
git push origin "v$VERSION"

# 5. Create a GitHub Release
log "Creating GitHub Release..."
if command -v gh >/dev/null 2>&1; then
  gh release create "v$VERSION" \
    "${NATIVE_BINARIES[@]}" \
    --title "Version $VERSION" \
    --notes "Automated release of version $VERSION"
else
  warn "gh CLI not found. Skipping GitHub Release creation."
fi

log "Release $VERSION completed successfully."
