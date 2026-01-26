#!/bin/sh

# Start FileServApp
# We use exec to let the java process receive signals (like SIGTERM)

# Collect all arguments
ARGS=""

# 1. Check for properties files in /app/etc
if [ -f "/app/etc/fileserv.properties" ]; then
    ARGS="$ARGS --config /app/etc/fileserv.properties"
fi

# 2. Check for passwd file in /app/etc
if [ -f "/app/etc/fileserv-passwd" ]; then
    ARGS="$ARGS --passwd /app/etc/fileserv-passwd"
fi

# 3. Check for specific authenticator configs in /app/etc/auth/
# This allows adding multiple --auth parameters by putting files in a directory
if [ -d "/app/etc/auth" ]; then
    for auth_file in /app/etc/auth/*; do
        if [ -f "$auth_file" ]; then
            AUTH_CONF=$(cat "$auth_file")
            ARGS="$ARGS --auth $AUTH_CONF"
        fi
    done
fi

# 4. If a keystore exists in /app/etc, use it
if [ -f "/app/etc/keystore.p12" ]; then
    JAVA_OPTS="$JAVA_OPTS -Dkeystore=/app/etc/keystore.p12"
else
    JAVA_OPTS="$JAVA_OPTS -Dkeystore=/app/keystore.p12"
fi

exec java $JAVA_OPTS -DbehindProxy=false -jar fileserv-app.jar /app/data $ARGS --allow-http "$@"
