#!/bin/bash
set -e

# Ensure we are in the project root directory
cd "$(dirname "$0")"

echo "Building the project..."
./mvnw clean package -DskipTests

echo "Starting the application..."
# Generate self-signed certificate if it doesn't exist
KEYSTORE="keystore.p12"
if [ ! -f "$KEYSTORE" ]; then
    echo "Generating self-signed certificate..."
    keytool -genkeypair -alias webdav -keyalg RSA -keysize 2048 \
      -storetype PKCS12 -keystore "$KEYSTORE" \
      -storepass changeit -keypass changeit \
      -dname "CN=localhost" -validity 365
fi

# Create default passwords file if it doesn't exist
PASSWORDS_FILE="passwords.txt"
if [ ! -f "$PASSWORDS_FILE" ]; then
    echo "Creating default passwords file..."
    cat > "$PASSWORDS_FILE" <<EOF
# Format: username:password
alice:secret
bob:password
charly:123456
EOF
fi

java -jar fileserv-app/target/fileserv-app.jar \
     "./data" \
     --passwd="$PASSWORDS_FILE" \
     --behind-proxy=false
