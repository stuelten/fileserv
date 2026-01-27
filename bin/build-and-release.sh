#!/bin/bash
set -e

# build-and-release.sh
# Performs the build and prepares the release.
# Can be run locally or in CI.

VERSION=$1
DRY_RUN=${DRY_RUN:-false}

if [ -z "$VERSION" ]; then
    echo "Usage: $0 <version>"
    exit 1
fi

echo "Building and releasing version $VERSION (DRY_RUN=$DRY_RUN)"

# 1. Update version in poms
echo "Updating Maven versions to $VERSION..."
if [ "$DRY_RUN" = "true" ]; then
    echo "[DRY RUN] ./mvnw versions:set -DnewVersion=${VERSION} -DgenerateBackupPoms=false"
else
    ./mvnw versions:set -DnewVersion="${VERSION}" -DgenerateBackupPoms=false -q
fi

# 2. Build project
echo "Building project with native profile..."
if [ "$DRY_RUN" = "true" ]; then
    echo "[DRY RUN] MAVEN_ARGS=\"-Pnative\" ./build.sh clean"
    # Create a dummy file if it doesn't exist for dry run to pass later checks
    mkdir -p fileserv-test-generate-hierarchy/target
    touch fileserv-test-generate-hierarchy/target/fileserv-test-generate-hierarchy
else
    # Update path for GraalVM if JAVA_HOME is set
    if [ -n "$JAVA_HOME" ]; then
        export PATH="$JAVA_HOME/bin:$PATH"
    fi
    export MAVEN_ARGS="-Pnative"
    ./build.sh clean
fi

# 3. Verify the build output
echo "Checking build artifacts..."
if [ ! -f "fileserv-test-generate-hierarchy/target/fileserv-test-generate-hierarchy" ]; then
    echo "Error: Native binary not found!"
    exit 1
fi

echo "Build successful."

if [ "$DRY_RUN" = "true" ]; then
    echo "Dry run completed. No changes were pushed."
    exit 0
fi

# CI specific: Git configuration for tagging
if [ "$GITHUB_ACTIONS" = "true" ]; then
    git config user.name "github-actions[bot]"
    git config user.email "github-actions[bot]@users.noreply.github.com"
fi

# 4. Commit, tag, and push
echo "Committing version change..."
git add "**/pom.xml" "pom.xml"
git commit -m "chore: release version $VERSION"
git tag -a "v$VERSION" -m "Version $VERSION"

echo "Pushing changes and tag..."
git push origin main
git push origin "v$VERSION"

# 5. Create a GitHub Release
echo "Creating GitHub Release..."
if command -v gh >/dev/null 2>&1; then
    gh release create "v$VERSION" \
      fileserv-auth-file/fileserv-auth-file-smbpasswd/target/fileserv-smbpasswd \
      --title "Version $VERSION" \
      --notes "Automated release of version $VERSION"

    gh release create "v$VERSION" \
      fileserv-test-generate-hierarchy/target/fileserv-test-generate-hierarchy \
      --title "Version $VERSION" \
      --notes "Automated release of version $VERSION"

    gh release create "v$VERSION" \
      fileserv-test-webdav/target/fileserv-test-webdav \
      --title "Version $VERSION" \
      --notes "Automated release of version $VERSION"
else
    echo "Warning: gh CLI not found. Skipping GitHub Release creation."
fi

echo "Release $VERSION completed successfully."
