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
  echo "  --quiet                  Minimize output"
  echo "  --verbose                Show detailed output"
  echo "  --buildNativeBinaries    Build native binaries using GraalVM"
  echo ""
  echo "Arguments:"
  echo "  clean                    Clean build artifacts before building"
}

# Handle options
CLEAN=false
NATIVE=false
SKIP_DOCKER=false
SKIP_TESTS=false
QUIET=false
VERBOSE=false

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
  --quiet)
    QUIET=true
    shift
    ;;
  --verbose)
    VERBOSE=true
    shift
    ;;
  --buildNativeBinaries)
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

log "=== Building Maven Modules ==="

MVN_GOALS="install"
if [ "$CLEAN" = true ]; then
  MVN_GOALS="clean $MVN_GOALS"
fi
if [ "$NATIVE" = true ]; then
  MVN_GOALS="-Pnative $MVN_GOALS"
fi

MVN_OPTS=""
if [ "$QUIET" = true ]; then
  MVN_OPTS="$MVN_OPTS --quiet"
fi
if [ "$VERBOSE" = true ]; then
  MVN_OPTS="$MVN_OPTS --debug"
fi

# shellcheck disable=SC2086
"$SCRIPT_DIR/../mvnw" $MVN_OPTS $MVN_GOALS -DskipTests=$SKIP_TESTS $MAVEN_ARGS || error "Error building maven"

log ""
log "=== Building Docker Image ==="

if [ "$SKIP_DOCKER" = true ]; then
  info "Skipping Docker build."
else
  # Use docker or podman
  DOCKER_CMD=$(command -v docker || command -v podman || true)
  if [ -n "$DOCKER_CMD" ]; then
    if [[ "$DOCKER_CMD" == *"podman"* ]]; then
      log "Checking Podman machine..."
      if ! podman machine inspect --format '{{.State}}' | grep -q "running"; then
        log "Starting Podman machine..."
        podman machine start || true
      fi
    fi
    DOCKER_BUILD_OPTS=""
    if [ "$QUIET" = true ]; then
      DOCKER_BUILD_OPTS="--quiet"
    fi
    # shellcheck disable=SC2086
    "$DOCKER_CMD" build $DOCKER_BUILD_OPTS -t fileserv "$SCRIPT_DIR/.." || error "Error build docker"
  else
    warn "Neither docker nor podman found. Skipping Docker build."
  fi
fi

log ""
log "=== Build and Test Completed Successfully ==="
