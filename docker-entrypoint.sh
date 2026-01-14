#!/bin/sh

# Start FileServApp
# We use exec to let the java process receive signals (like SIGTERM)

ARGS=""
if [ -f "/app/fileserv-passwd" ]; then
    ARGS="--passwd /app/fileserv-passwd"
fi

exec java -DbehindProxy=false -Dkeystore=/app/keystore.p12 -jar fileserv-app.jar /app/data $ARGS "$@"
