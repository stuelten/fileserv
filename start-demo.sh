#!/bin/bash
set -e

# Ensure we are in the project root directory
cd "$(dirname "$0")"

echo "Building the project (without tests)..."
./mvnw clean package -DskipTests

echo "=========================================================================="
echo "Starting the application..."
echo "=========================================================================="

# "Generate self-signed certificate if it doesn't exist"
KEYSTORE="keystore.p12"
if [ -f "$KEYSTORE" ]; then
    echo "Use certificates from $KEYSTORE"
else
    echo "Generating self-signed certificate..."
    keytool -genkeypair -alias webdav -keyalg RSA -keysize 2048 \
      -storetype PKCS12 -keystore "$KEYSTORE" \
      -storepass changeit -keypass changeit \
      -dname "CN=localhost" -validity 365
fi

# Create a default passwords file if it doesn't exist"
FILESERV_PASSWD="fileserv-passwd"
if [ -f "$FILESERV_PASSWD" ]; then
    echo "Use users/passwords from $FILESERV_PASSWD"
else
    echo "Creating default passwords file with random passwords..."
    echo "# Format: username:password" >> "$FILESERV_PASSWD"
    for user in alice bob charly; do
        # Generate a random 6-digit password
        password=$(printf "%06d" $((RANDOM % 1000000)))
        echo "$user:$password" >> "$FILESERV_PASSWD"
    done
fi
echo "Users who can authenticate:"
cat "$FILESERV_PASSWD"

echo "Start FileServApp..."
java -jar fileserv-app/target/fileserv-app.jar \
     "./data" \
     --passwd="$FILESERV_PASSWD" \
     --behind-proxy=false
