#!/bin/bash
set -e

# estimate-version.sh
# Estimates the next version based on git commit messages since the last tag.
# Follows a simple version of Conventional Commits:
# - 'feat:' or 'feat(...):' -> minor increment
# - 'fix:' or 'fix(...):' -> patch increment
# - 'BREAKING CHANGE:' or 'feat!:' -> major increment

# Default to version from pom.xml if no tags found
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
LATEST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "v0.0.0")

# Split version into components
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"

# Default increment is patch if there are any commits, otherwise keep current
INCR="none"

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
