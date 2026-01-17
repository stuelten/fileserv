#!/bin/bash
#
# Set a new version in maven pom.xml files.
# Either use the version given on the command line or use version-estimate.sh to get the next version.

set -e

# Change to the project root directory
cd "$(dirname "$0")/.."

NEW_VERSION=$1

# If no version is provided, estimate it using version-estimate.sh
if [ -z "$NEW_VERSION" ]; then
  if [ -f "bin/version-estimate.sh" ]; then
    echo "No version provided. Estimating next version..."
    NEW_VERSION=$(./bin/version-estimate.sh)-SNAPSHOT
  else
    echo "Error: No version provided and bin/version-estimate.sh not found."
    exit 1
  fi
fi

echo "Setting version to: $NEW_VERSION"

# Use the Maven Wrapper to set the new version across all modules
# -DgenerateBackupPoms=false prevents creating pom.xml.versionsBackup files
./mvnw versions:set -DnewVersion="$NEW_VERSION" -DgenerateBackupPoms=false

echo "Successfully updated pom.xml files to version $NEW_VERSION"
