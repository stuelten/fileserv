#!/bin/bash
# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup script (compatible with Maven Wrapper 3.3.x)
# ----------------------------------------------------------------------------

set -euo pipefail

# Determine the project base directory
PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`"/$link"
  fi
done
BASEDIR=`cd "$(dirname "$PRG")" >/dev/null; pwd -P`

MVNW_VERBOSE=${MVNW_VERBOSE:-false}

# Setup wrapper paths
WRAPPER_DIR="$BASEDIR/.mvn/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/maven-wrapper.jar"
WRAPPER_PROPERTIES="$WRAPPER_DIR/maven-wrapper.properties"

# Read configuration
if [ -r "$WRAPPER_PROPERTIES" ]; then
  . "$WRAPPER_PROPERTIES" 2>/dev/null || true
fi

die() { echo "$*" >&2; exit 1; }

# Locate Java
if [ -n "${JAVA_HOME:-}" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD="java"
fi

if ! command -v "$JAVACMD" >/dev/null 2>&1; then
  die "ERROR: Java not found. Set JAVA_HOME or ensure 'java' is on PATH."
fi

# Ensure wrapper JAR is present; download if missing (official behavior)
if [ ! -f "$WRAPPER_JAR" ]; then
  [ "$MVNW_VERBOSE" = true ] && echo "Downloading Maven Wrapper JAR ..."
  mkdir -p "$WRAPPER_DIR"

  # Parse wrapperUrl from properties file
  WRAPPER_URL=$(grep -E '^wrapperUrl=' "$WRAPPER_PROPERTIES" | cut -d'=' -f2-)
  [ -n "$WRAPPER_URL" ] || die "wrapperUrl not set in $WRAPPER_PROPERTIES"

  # Download using curl, wget or powershell
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL -o "$WRAPPER_JAR" "$WRAPPER_URL" || die "Failed to download $WRAPPER_URL"
  elif command -v wget >/dev/null 2>&1; then
    wget -q -O "$WRAPPER_JAR" "$WRAPPER_URL" || die "Failed to download $WRAPPER_URL"
  elif command -v powershell >/dev/null 2>&1; then
    powershell -NoProfile -NonInteractive -Command "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; (New-Object System.Net.WebClient).DownloadFile('$WRAPPER_URL', '$WRAPPER_JAR')" || die "Failed to download $WRAPPER_URL"
  else
    die "No downloader found (curl, wget or powershell) to fetch $WRAPPER_URL"
  fi
fi

# Build classpath
CLASSPATH="$WRAPPER_JAR"

# JVM options
MAVEN_OPTS=${MAVEN_OPTS:-}

# Pass on wrapper configuration location to the wrapper main
WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

# Resolve Maven distribution; the wrapper will handle downloading it using distributionUrl
if [ "$MVNW_VERBOSE" = true ]; then
  echo "Using BASEDIR: $BASEDIR"
  echo "Using JAVA: $JAVACMD"
  echo "Using WRAPPER_JAR: $WRAPPER_JAR"
fi

exec "$JAVACMD" $MAVEN_OPTS \
  -Dmaven.multiModuleProjectDirectory="$BASEDIR" \
  -classpath "$CLASSPATH" \
  "${WRAPPER_LAUNCHER}" "$@"
