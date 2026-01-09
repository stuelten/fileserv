FROM eclipse-temurin:25-jre
WORKDIR /app
COPY fileserv-app/target/fileserv-app.jar app.jar

# Create data directory
RUN mkdir -p /app/data

# Generate self-signed certificate if it doesn't exist at build time
RUN keytool -genkeypair -alias webdav -keyalg RSA -keysize 2048 \
      -storetype PKCS12 -keystore /app/keystore.p12 \
      -storepass changeit -keypass changeit \
      -dname "CN=localhost" -validity 365

# Expose default ports
EXPOSE 8080 8443

ENTRYPOINT ["java", "-Droot=/app/data", "-DbehindProxy=false", "-Dkeystore=/app/keystore.p12", "-jar", "app.jar"]
