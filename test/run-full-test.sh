#!/bin/bash
set -e

# Automation script for full test setup

# 1. Build project
echo "Building project with native profile..."
./mvnw clean package -Pnative -DskipTests

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

# 2. Setup config directory
echo "Setting up config directory..."
mkdir -p config/auth

# 2.1 Generate plaintext users
echo "alice:password123" > config/fileserv-passwd
echo "bob:secret456" >> config/fileserv-passwd

# 2.2 Generate smbpasswd users
echo "Generating smbpasswd users using native binary..."
./fileserv-auth-file/fileserv-auth-file-smbpasswd/target/fileserv-smbpasswd -c config/smbpasswd -a charlie smbpass789

# 2.3 Configure smb authenticator
echo "file-smb:path=/app/etc/smbpasswd" > config/auth/smb.conf

# 3. Prepopulate data dir
echo "Populating data directory using native binary..."
rm -rf data/*
./fileserv-test-generate-hierarchy/target/fileserv-test-generate-hierarchy --size 5mb --count 50 --depth 3 data/

# 4. Start Docker container
echo "Starting FileServ container..."
$DOCKER_COMPOSE down || true
$DOCKER_COMPOSE up -d

# Wait for server to start
echo "Waiting for server to start..."
sleep 5

# 5. Run WebDAV tester
echo "Running WebDAV tests..."
EXIT_CODE=0

echo "Testing Alice (Plaintext)..."
java -jar fileserv-test-webdav/target/fileserv-test-webdav.jar http://localhost:8080/ -u alice -p password123 || EXIT_CODE=1

echo "Testing Charlie (SMB)..."
java -jar fileserv-test-webdav/target/fileserv-test-webdav.jar http://localhost:8080/ -u charlie -p smbpass789 || EXIT_CODE=1

# 6. Cleanup
echo "Cleaning up..."
$DOCKER_COMPOSE down

if [ $EXIT_CODE -eq 0 ]; then
    echo "SUCCESS: Full test setup completed successfully."
else
    echo "FAILURE: One or more tests failed."
fi

exit $EXIT_CODE
