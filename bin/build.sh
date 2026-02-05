#!/bin/bash
# bin/build.sh - Build and Test Script for FileServ

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Source common functions
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

show_help() {
  echo "Usage: $0 [OPTIONS] [TARGETS...]"
  echo ""
  echo "Targets:"
  echo "  clean          Clean build artifacts"
  echo "  java           Build standard Java artifacts"
  echo "  shaded-jar     Build shaded JARs (all-in-one JARs)"
  echo "  native         Build native binaries using GraalVM"
  echo "  docker         Build Docker image"
  echo "  all            Build all targets (java, shaded-jar, native, docker)"
  echo ""
  echo "Options:"
  echo "  --help         Show this help message"
  echo "  --skipTests    Skip running tests during build"
  echo "  -q, --quiet    Minimize output"
  echo "  -v, --verbose  Show detailed output"
  echo ""
  echo "Example:"
  echo "  $0 clean java shaded-jar"
}

# Handle options and targets
CLEAN=false
BUILD_JAVA=false
BUILD_SHADED=false
BUILD_NATIVE=false
BUILD_DOCKER=false
SKIP_TESTS=false
TARGETS_PROVIDED=false

# Collect non-option arguments as targets
while [[ $# -gt 0 ]]; do
  case $1 in
  --help)
    show_help
    exit 0
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
  clean)
    CLEAN=true
    TARGETS_PROVIDED=true
    shift
    ;;
  java)
    BUILD_JAVA=true
    TARGETS_PROVIDED=true
    shift
    ;;
  shaded-jar)
    BUILD_SHADED=true
    TARGETS_PROVIDED=true
    shift
    ;;
  native)
    BUILD_NATIVE=true
    TARGETS_PROVIDED=true
    shift
    ;;
  docker)
    BUILD_DOCKER=true
    TARGETS_PROVIDED=true
    shift
    ;;
  all)
    BUILD_JAVA=true
    BUILD_SHADED=true
    BUILD_NATIVE=true
    BUILD_DOCKER=true
    TARGETS_PROVIDED=true
    shift
    ;;
  *)
    show_help
    error "Unknown parameter or target: $1"
    ;;
  esac
done

# Default behavior if no targets provided: build java, shaded-jar and docker
if [[ "$TARGETS_PROVIDED" = "false" ]]; then
  BUILD_JAVA=true
  BUILD_SHADED=true
  BUILD_NATIVE=true
  BUILD_DOCKER=true
fi

export QUIET
export VERBOSE

log "=== Building Maven Modules ==="

MVN_GOALS="install"
if [[ "$CLEAN" = "true" ]]; then
  MVN_GOALS="clean $MVN_GOALS"
fi

MVN_PROFILES=""
if [[ "$BUILD_SHADED" = "true" ]]; then
  MVN_PROFILES="-Pshaded-jar"
fi
if [[ "$BUILD_NATIVE" = "true" ]]; then
  if [[ -n "$MVN_PROFILES" ]]; then
    MVN_PROFILES="$MVN_PROFILES,native"
  else
    MVN_PROFILES="-Pnative"
  fi
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

if [ "$BUILD_DOCKER" = false ]; then
  info "Skipping Docker build."
else
  DOCKER_OPTS=""
  [ "$QUIET" = true ] && DOCKER_OPTS="$DOCKER_OPTS --quiet"
  [ "$VERBOSE" = true ] && DOCKER_OPTS="$DOCKER_OPTS --verbose"
  [ "$CLEAN" = true ] && DOCKER_OPTS="$DOCKER_OPTS --force"

  # shellcheck disable=SC2086
  "$SCRIPT_DIR/docker-build.sh" $DOCKER_OPTS
fi

log ""
log "=== Build Completed Successfully ==="
