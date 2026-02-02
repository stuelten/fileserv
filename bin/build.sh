#!/bin/bash
# bin/build.sh - Build and Test Script for FileServ

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Source common functions
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

VERSION="0.2.2"

show_help() {
  echo "Usage: ./bin/build.sh [OPTIONS] [clean]"
  echo ""
  echo "Options:"
  echo "  --help                   Show this help message"
  echo "  --version                Show version information"
  echo "  --skipDocker             Skip building docker container"
  echo "  --skipTests              Skip running tests during build"
  echo "  -q, --quiet              Minimize output"
  echo "  -v, --verbose            Show detailed output"
  echo ""
  echo "Arguments:"
  echo "  clean                    Clean build artifacts before building"
  echo "  buildNativeBinaries    Build native binaries using GraalVM"
}

# Handle options
CLEAN=false
NATIVE=false
SKIP_DOCKER=false
SKIP_TESTS=false

while [[ $# -gt 0 ]]; do
  case $1 in
  --help)
    show_help
    exit 0
    ;;
  --version)
    echo "FileServ version $VERSION"
    exit 0
    ;;
  --skipDocker)
    SKIP_DOCKER=true
    shift
    ;;
  --skipTests)
    SKIP_TESTS=true
    shift
    ;;
  -q|--quiet)
    QUIET=true
    shift
    ;;
  -v|--verbose)
    VERBOSE=true
    shift
    ;;
  buildNativeBinaries)
    NATIVE=true
    shift
    ;;
  clean)
    CLEAN=true
    shift
    ;;
  *)
    show_help
    error "Unknown parameter $1"
    ;;
  esac
done

export QUIET
export VERBOSE

log "=== Building Maven Modules ==="

MVN_GOALS="install"
if [ "$CLEAN" = true ]; then
  MVN_GOALS="clean $MVN_GOALS"
fi

# We always want the shaded-jar for docker
MVN_PROFILES="-Pshaded-jar"
if [ "$NATIVE" = true ]; then
  MVN_PROFILES="$MVN_PROFILES,native"
fi

MVN_OPTS=""
if [ "$QUIET" = true ]; then
  MVN_OPTS="$MVN_OPTS --batch-mode --quiet"
fi
if [ "$VERBOSE" = true ]; then
  MVN_OPTS="$MVN_OPTS --debug"
fi

# shellcheck disable=SC2086
"$SCRIPT_DIR/../mvnw" $MVN_OPTS $MVN_GOALS $MVN_PROFILES -DskipTests=$SKIP_TESTS $MAVEN_ARGS || error "Error building maven"

log ""
log "=== Building Docker Image ==="

if [ "$SKIP_DOCKER" = true ]; then
  info "Skipping Docker build."
else
  # Use docker or podman
  DOCKER_CMD=$(command -v docker || command -v podman || true)
  if [ -n "$DOCKER_CMD" ]; then
    rebuild=false
    # Check if the image exists
    if ! "$DOCKER_CMD" image inspect fileserv >/dev/null 2>&1; then
      rebuild=true
      reason="image missing"
    else
      # Get the timestamp of the image
      IMAGE_TIME=$("$DOCKER_CMD" image inspect fileserv --format '{{.Created}}')
      
      # Use a temporary marker file with the image's creation time
      # to avoid platform-specific issues with 'find -newermt'
      MARKER=$(mktemp)
      # docker/podman use RFC3339-like format which touch -d can often handle
      if touch -d "$IMAGE_TIME" "$MARKER" 2>/dev/null; then
        # When comparing files that were just built, there might be slight clock skews
        # or filesystem precision issues. We subtract 1 second from marker to be safe.
        if [[ "$OSTYPE" == "darwin"* ]]; then
          # macOS touch
          STAMP=$(stat -f "%m" "$MARKER")
          touch -t $(date -r $((STAMP - 1)) +%Y%m%d%H%M.%S) "$MARKER"
        else
          # Linux touch
          touch -d "@$(($(date +%s -d "$IMAGE_TIME") - 1))" "$MARKER"
        fi

        NEWER=$(find "Dockerfile" \
                     "docker-entrypoint.sh" \
                     "config" \
                     "fileserv-app/target/fileserv-app.jar" \
                     "fileserv-core/target/classes/version.properties" \
                     -newer "$MARKER" -type f 2>/dev/null | head -n 1)
      else
        # Fallback if touch -d fails (e.g. incompatible format)
        # On some systems touch -d might be picky.
        # As a fallback we can try to use the image creation time directly in find if supported,
        # but let's try to be more robust.
        # If we can't parse the time, we might have to rebuild to be safe.
        rebuild=true
        reason="cannot parse image timestamp ($IMAGE_TIME)"
      fi
      rm -f "$MARKER"

      if [ "$rebuild" = false ] && [ -n "$NEWER" ]; then
        rebuild=true
        reason="files changed ($NEWER)"
      fi
    fi

    if [ "$rebuild" = true ]; then
      log "Building Docker image fileserv ($reason)..."
      DOCKER_BUILD_OPTS=""
      if [[ "$DOCKER_CMD" == *"podman"* ]]; then
        log "Checking Podman machine..."
        if [ "$QUIET" = true ]; then
          DOCKER_BUILD_OPTS="$DOCKER_BUILD_OPTS --quiet --log-level warn"
        fi
        if ! podman machine inspect --format '{{.State}}' | grep -q "running"; then
          log "Starting Podman machine..."
          podman machine start || true
        fi
      fi
      # shellcheck disable=SC2086
      "$DOCKER_CMD" build $DOCKER_BUILD_OPTS -t fileserv "$SCRIPT_DIR/.." || error "Error build docker"
    else
      log "Docker image fileserv is up to date. Skipping."
    fi
  else
    warn "Neither docker nor podman found. Skipping Docker build."
  fi
fi

log ""
log "=== Build and Test Completed Successfully ==="
