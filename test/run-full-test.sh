#!/bin/bash
# Prepare the setup and build artifacts, then test them

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../bin" && pwd)"

# Source common functions
# shellcheck source=../bin/common.sh
source "$SCRIPT_DIR/common.sh"

# call build script by default
SKIP_BUILD=false

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
  -h | --help)
    echo "Usage: $0 [options]"
    echo "Options:"
    echo "  -q, --quiet      Minimize output"
    echo "  -v, --verbose    Detailed output"
    echo "      --skipBuild  Skip the (re-)build of the project"
    exit 0
    ;;
  -q | --quiet)
    QUIET=true
    shift
    ;;
  -v | --verbose)
    VERBOSE=true
    shift
    ;;
  --skipBuild)
    SKIP_BUILD=true
    shift
    ;;
  *)
    echo "Unknown option: $1"
    exit 1
    ;;
  esac
done

# Pass QUIET and VERBOSE to subscripts
export QUIET
export VERBOSE

JAVA_OPTS=""
if [ "$QUIET" = true ]; then
  JAVA_OPTS="$JAVA_OPTS --quiet"
fi
if [ "$VERBOSE" = true ]; then
  JAVA_OPTS="$JAVA_OPTS --verbose"
fi

# 0. Test build and release stuff
TESTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
"$TESTS_DIR"/run-test-scripts.sh

# 1. Build project

## 1.1 Detect container engine
if command -v docker-compose >/dev/null 2>&1; then
  DOCKER_COMPOSE="docker-compose"
elif docker compose version >/dev/null 2>&1; then
  DOCKER_COMPOSE="docker compose"
elif command -v podman >/dev/null 2>&1; then
  DOCKER_COMPOSE="podman compose"
else
  echo "No compose in '$PATH'"
  error "Neither docker compose, docker-compose nor podman compose found."
  exit 1
fi
log "Using $DOCKER_COMPOSE for container management."

if [[ "$DOCKER_COMPOSE" == *"podman"* ]]; then
  log "Checking Podman machine..."
  if ! podman machine inspect --format '{{.State}}' | grep -q "running"; then
    log "Starting Podman machine..."
    podman machine start || true
  fi
fi

## 1.2 Build java part
if [[ $SKIP_BUILD == false ]]; then
  log "Building project with native profile..."
  # Update the path for GraalVM if JAVA_HOME is set
  if [ -n "$JAVA_HOME" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
  fi
  # shellcheck source=../bin/build.sh
  "$SCRIPT_DIR/build.sh" --quiet clean buildNativeBinaries
else
  log "Skip building artifacts"
fi

# 2. Set up the config directory

log "Setting up config directory..."
mkdir -p etc/auth

## 2.1 Generate plaintext users
log "Generating plaintext users..."
echo "alice:password123" >etc/fileserv-passwd
echo "bob:secret456" >>etc/fileserv-passwd

## 2.2 Generate smbpasswd users
log "Generating smbpasswd users..."
SMB_PASSWD_JAR=$(find_jar "fileserv-auth-file/fileserv-auth-file-smbpasswd" "fileserv-auth-file-smbpasswd")

# shellcheck disable=SC2086
java -jar "$SMB_PASSWD_JAR" $JAVA_OPTS -c etc/smbpasswd -a charlie smbpass789

## 2.3 Configure smb authenticator
log "Configuring smb authenticator..."
echo "file-smb:path=/app/etc/smbpasswd" >etc/auth/smb.conf

# 3. Prepopulate data dir

log "Populating data directory..."
rm -rf data/*
GEN_HIERARCHY_JAR=$(find_jar "fileserv-test-generate-hierarchy" "fileserv-test-generate-hierarchy")

# shellcheck disable=SC2086
java -jar "$GEN_HIERARCHY_JAR" $JAVA_OPTS --size 5mb --count 50 --depth 3 data/

# 4. Start the Docker container
log "Starting FileServ container..."
$DOCKER_COMPOSE down || true
$DOCKER_COMPOSE up -d --build

## 4.1 Wait for the server to start
log "Waiting for server to start..."
sleep 5

# 5. Run WebDAV tester
log "Running WebDAV tests..."
EXIT_CODE=0

WEBDAV_JAR=$(find_jar "fileserv-test-webdav" "fileserv-test-webdav")

log "Testing Alice (Plaintext)..."
# shellcheck disable=SC2086
java -jar "$WEBDAV_JAR" $JAVA_OPTS http://localhost:8080/ -u alice -p password123 || EXIT_CODE=1

log "Testing Charlie (SMB)..."
# shellcheck disable=SC2086
java -jar "$WEBDAV_JAR" $JAVA_OPTS http://localhost:8080/ -u charlie -p smbpass789 || EXIT_CODE=1

# 6. Cleanup
log "Cleaning up..."
$DOCKER_COMPOSE down

if [[ $EXIT_CODE -eq 0 ]]; then
  log "SUCCESS: Full test setup completed successfully."

  # clean up
  rm etc/fileserv-passwd
  rm etc/auth/smb.conf
  rm etc/smbpasswd
  rm -rf data/*
else
  error "FAILURE: One or more tests failed."
fi

exit "$EXIT_CODE"
