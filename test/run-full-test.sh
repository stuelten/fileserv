#!/bin/bash
set -e

# Automation script for full test setup

# 1. Build project
echo "Building project..."
./mvnw clean package -DskipTests

# 1.1 Detect container engine
if command -v docker-compose >/dev/null 2>&1; then
    DOCKER_COMPOSE="docker-compose"
elif docker compose version >/dev/null 2>&1; then
    DOCKER_COMPOSE="docker compose"
elif command -v podman-compose >/dev/null 2>&1; then
    DOCKER_COMPOSE="podman-compose"
else
    echo "Error: Neither docker-compose nor podman-compose found."
    exit 1
fi
echo "Using $DOCKER_COMPOSE for container management."

if [[ "$DOCKER_COMPOSE" == *"podman"* ]]; then
    echo "Checking Podman machine..."
    if ! podman machine inspect --format '{{.State}}' | grep -q "running"; then
        echo "Starting Podman machine..."
        podman machine start || true
    fi
fi

# 2. Setup config directory
echo "Setting up config directory..."
mkdir -p etc/auth

# 2.1 Generate plaintext users
echo "alice:password123" > etc/fileserv-passwd
echo "bob:secret456" >> etc/fileserv-passwd

# 2.2 Generate smbpasswd users
echo "Generating smbpasswd users..."
SMB_PASSWD_JAR="fileserv-auth-file/fileserv-auth-file-smbpasswd/target/fileserv-smbpasswd.jar"
java -jar "$SMB_PASSWD_JAR" -c etc/smbpasswd -a charlie smbpass789

# 2.3 Configure smb authenticator
echo "file-smb:path=/app/etc/smbpasswd" > etc/auth/smb.conf

# 3. Prepopulate data dir
echo "Populating data directory..."
rm -rf data/*
GEN_HIERARCHY_JAR="fileserv-test-generate-hierarchy/target/fileserv-test-generate-hierarchy.jar"
java -jar "$GEN_HIERARCHY_JAR" --size 5mb --count 50 --depth 3 data/

# 4. Start Docker container
echo "Starting FileServ container..."
$DOCKER_COMPOSE down || true
$DOCKER_COMPOSE up -d --build

# Wait for server to start
echo "Waiting for server to start..."
sleep 5

# 5. Run WebDAV tester
echo "Running WebDAV tests..."
EXIT_CODE=0

WEBDAV_JAR="fileserv-test-webdav/target/fileserv-test-webdav.jar"

echo "Testing Alice (Plaintext)..."
java -jar "$WEBDAV_JAR" http://localhost:8080/ -u alice -p password123 || EXIT_CODE=1

echo "Testing Charlie (SMB)..."
java -jar "$WEBDAV_JAR" http://localhost:8080/ -u charlie -p smbpass789 || EXIT_CODE=1

# 6. Cleanup
echo "Cleaning up..."
$DOCKER_COMPOSE down

if [ $EXIT_CODE -eq 0 ]; then
    echo "SUCCESS: Full test setup completed successfully."
else
    echo "FAILURE: One or more tests failed."
fi

exit $EXIT_CODE
