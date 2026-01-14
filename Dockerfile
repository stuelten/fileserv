FROM eclipse-temurin:21-jre
WORKDIR /app
COPY fileserv-app/target/fileserv-app.jar fileserv-app.jar
COPY fileserv-core/target/classes/version.properties ./
COPY fileserv-passwd* ./
COPY docker-entrypoint.sh ./
RUN chmod +x docker-entrypoint.sh

# Create data directory
RUN mkdir -p /app/data
VOLUME ["/app/data"]

# Generate self-signed certificate if it doesn't exist at build time
RUN keytool -genkeypair -alias webdav -keyalg RSA -keysize 2048 \
      -storetype PKCS12 -keystore /app/keystore.p12 \
      -storepass changeit -keypass changeit \
      -dname "CN=localhost" -validity 365

# Expose default ports
EXPOSE 8080 8443

ENTRYPOINT ["/app/docker-entrypoint.sh"]
