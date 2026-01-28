#!/bin/bash
set -e

# Source common functions
# shellcheck source=../bin/common.sh
source "$(dirname "$0")/../bin/common.sh"

# Automation script for full test setup

# 1. Build project
log "Building project..."
./mvnw clean package -DskipTests

# 1.1 Detect container engine
if command -v docker-compose >/dev/null 2>&1; then
    DOCKER_COMPOSE="docker-compose"
elif docker compose version >/dev/null 2>&1; then
    DOCKER_COMPOSE="docker compose"
elif command -v podman-compose >/dev/null 2>&1; then
    DOCKER_COMPOSE="podman-compose"
else
    error "Neither docker-compose nor podman-compose found."
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

# 2. Setup config directory
log "Setting up config directory..."
mkdir -p etc/auth

# 2.1 Generate plaintext users
log "Generating plaintext users..."
echo "alice:password123" > etc/fileserv-passwd
echo "bob:secret456" >> etc/fileserv-passwd

# 2.2 Generate smbpasswd users
log "Generating smbpasswd users..."
SMB_PASSWD_JAR="fileserv-auth-file/fileserv-auth-file-smbpasswd/target/fileserv-smbpasswd.jar"
java -jar "$SMB_PASSWD_JAR" -c etc/smbpasswd -a charlie smbpass789

# 2.3 Configure smb authenticator
log "Configuring smb authenticator..."
echo "file-smb:path=/app/etc/smbpasswd" > etc/auth/smb.conf

# 3. Prepopulate data dir
log "Populating data directory..."
rm -rf data/*
GEN_HIERARCHY_JAR="fileserv-test-generate-hierarchy/target/fileserv-test-generate-hierarchy.jar"
java -jar "$GEN_HIERARCHY_JAR" --size 5mb --count 50 --depth 3 data/

# 4. Start Docker container
log "Starting FileServ container..."
$DOCKER_COMPOSE down || true
$DOCKER_COMPOSE up -d --build

# Wait for server to start
log "Waiting for server to start..."
sleep 5

# 5. Run WebDAV tester
log "Running WebDAV tests..."
EXIT_CODE=0

WEBDAV_JAR="fileserv-test-webdav/target/fileserv-test-webdav.jar"

log "Testing Alice (Plaintext)..."
java -jar "$WEBDAV_JAR" http://localhost:8080/ -u alice -p password123 || EXIT_CODE=1

log "Testing Charlie (SMB)..."
java -jar "$WEBDAV_JAR" http://localhost:8080/ -u charlie -p smbpass789 || EXIT_CODE=1

# 6. Cleanup
log "Cleaning up..."
$DOCKER_COMPOSE down

if [ $EXIT_CODE -eq 0 ]; then
    log "SUCCESS: Full test setup completed successfully."
else
    error "FAILURE: One or more tests failed."
fi

exit $EXIT_CODE
