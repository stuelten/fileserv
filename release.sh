#!/bin/bash

# release.sh - Release script for fileserv
# 0. Do pre-checks
# 1. Update version
# 2. Build everything
# 3. Tag and push
# 4. Update homebrew formula

set -e

# --- Pre-checks ---

fail() {
    echo "Error: $1" >&2
    exit 1
}

check_git() {
    echo "Checking Git status..."
    git rev-parse --is-inside-work-tree >/dev/null 2>&1 || fail "Not a git repository."
    
    # Check if the working directory is clean (excluding pom.xml files that we might have modified manually,
    # but for a release it's safer to have it clean)
    if [ -n "$(git status --porcelain)" ]; then
        fail "Warning: Working directory is not clean."
    fi
    
    # Check push access (dry run)
    git push --dry-run origin main >/dev/null 2>&1 || fail "Cannot push to origin main. Check git access."
}

check_graalvm() {
    echo "Checking GraalVM setup..."
    if [ -n "$JAVA_HOME" ]; then
        PATH="$JAVA_HOME/bin:$PATH"
    fi

    if ! command -v native-image >/dev/null 2>&1; then
        fail "'native-image' not found. Ensure GraalVM is installed and JAVA_HOME is set correctly."
    fi
    native-image --version
}

check_gh() {
    echo "Checking GitHub CLI..."
    command -v gh >/dev/null 2>&1 || fail "GitHub CLI (gh) not found."
    # We check for authentication.
    # Calling 'gh auth status' returns 0 if authenticated to at least one host.
    # However, we need it to be authenticated to github.com specifically for this repo.
    gh auth status --hostname github.com >/dev/null 2>&1 || fail "gh not authenticated for github.com. Run 'gh auth login'."
}

check_homebrew() {
    TAP_DIR="../homebrew-tap"
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "Checking Homebrew..."
        command -v brew >/dev/null 2>&1 || fail "Homebrew not found."
    fi
    
    if [ ! -d "$TAP_DIR" ]; then
        fail "Homebrew tap directory not found at $TAP_DIR."
    fi
}

check_maven() {
    echo "Checking Maven wrapper..."
    [ -x "./mvnw" ] || fail "./mvnw not found or not executable."
}

# Run all pre-checks
echo "=== Running Pre-checks ==="
check_git
check_maven
check_graalvm
check_gh
check_homebrew
echo "=== Pre-checks passed ==="

# --- Versioning ---

if [ $# -eq 0 ]; then
    echo "No version given. Attempting to auto-increment..."
    # Get current version from Maven
    CURRENT_VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout | grep -v "\[" | tail -n 1)
    
    # Remove -SNAPSHOT if present
    BASE_VERSION=${CURRENT_VERSION%-SNAPSHOT}
    
    # Extract major, minor, micro
    IFS='.' read -r major minor micro <<< "$BASE_VERSION"
    
    if [ -z "$micro" ]; then
        fail "Could not parse version $CURRENT_VERSION"
    fi
    
    # Increment micro
    NEXT_MICRO=$((micro + 1))
    VERSION="$major.$minor.$NEXT_MICRO"
    echo "Auto-incremented version: $CURRENT_VERSION -> $VERSION"
elif [ $# -eq 1 ]; then
    VERSION=$1
else
    echo "Usage: $0 [<version>]"
    echo "Example: $0 0.1.0"
    exit 1
fi

TAP_DIR="../homebrew-tap"

echo "=== Releasing version $VERSION ==="

# 1. Update version of fileserv and test app(s)
echo "Updating Maven versions..."
./mvnw versions:set -DnewVersion="${VERSION}" -DgenerateBackupPoms=false

# 2. Build everything
# (Path update for GraalVM already done in pre-check but for safety/locality:)
if [ -n "$JAVA_HOME" ]; then
    PATH="$JAVA_HOME/bin:$PATH"
fi

echo "Building project..."
export MAVEN_ARGS="-Pnative"
./build.sh clean

# We need the native binary for the test app
echo "Checking native binary..."
if [ ! -f "fileserv-test-generate-hierarchy/target/fileserv-test-generate-hierarchy" ]; then
    fail "Native binary not found after build!"
fi

# 3. Tag and push the new version
echo "Committing and tagging version $VERSION..."
# shellcheck disable=SC2035
git add pom.xml **/pom.xml
git commit -m "Release version $VERSION"
git tag -a "v$VERSION" -m "Version $VERSION"
git push origin main
git push origin "v$VERSION"

# 3.5 Create a GitHub Release and upload binary
echo "Creating GitHub Release and uploading binary..."
gh release create "v$VERSION" fileserv-test-generate-hierarchy/target/fileserv-test-generate-hierarchy --title "Version $VERSION" --notes "Release version $VERSION"

# 4. Update the homebrew stuff
echo "Updating Homebrew formula..."

BINARY_URL="https://github.com/stuelten/fileserv/releases/download/v$VERSION/fileserv-test-generate-hierarchy"
SHA256=$(shasum -a 256 fileserv-test-generate-hierarchy/target/fileserv-test-generate-hierarchy | awk '{print $1}')

FORMULA_FILE="$TAP_DIR/Formula/fileserv-test-apps.rb"

# Update version
sed -i '' "s/version \".*\"/version \"$VERSION\"/" "$FORMULA_FILE"

# Update URL to the binary release
sed -i '' "s|url \".*\"|url \"$BINARY_URL\"|" "$FORMULA_FILE"

# Update SHA256
sed -i '' "s/sha256 \".*\"/sha256 \"$SHA256\"/" "$FORMULA_FILE"

# Commit and push the formula change
echo "Committing and pushing Homebrew formula update..."
(cd "$TAP_DIR" && git add Formula/fileserv-test-apps.rb && git commit -m "Release fileserv-test-apps $VERSION" && git push origin main)

echo "Homebrew formula updated and pushed in $TAP_DIR."

echo "=== Release $VERSION completed successfully ==="
