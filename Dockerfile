FROM eclipse-temurin:21-jre
WORKDIR /app

COPY fileserv-app/target/fileserv-app.jar fileserv-app.jar
COPY fileserv-core/target/classes/version.properties ./
COPY docker-entrypoint.sh ./
RUN chmod +x docker-entrypoint.sh

# r/o config goes into config
RUN mkdir -p /app/config
COPY config/* /app/config/

# r/w config read from etc
RUN mkdir -p /app/etc
VOLUME ["/app/etc"]

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
