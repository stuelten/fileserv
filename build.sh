#!/bin/bash

# build.sh - Build and Test Script for FileServ

set -e

# Usage: ./build.sh [clean]

# Handle clean option
CLEAN=false
NATIVE=false
for arg in "$@"; do
    if [ "$arg" == "clean" ]; then
        CLEAN=true
    elif [ "$arg" == "native" ]; then
        NATIVE=true
    else
        echo "Error: Unknown parameter $arg"
        exit 1
    fi
done

echo "=== Building Maven Modules ==="
MVN_GOALS="install"
if [ "$CLEAN" = true ]; then
    MVN_GOALS="clean $MVN_GOALS"
fi
if [ "$NATIVE" = true ]; then
    MVN_GOALS="-Pnative $MVN_GOALS"
fi

# shellcheck disable=SC2086
./mvnw $MVN_GOALS -DskipTests=false $MAVEN_ARGS

echo ""
echo "=== Building Docker Image ==="
# Use docker or podman
DOCKER_CMD=$(command -v docker || command -v podman || true)
if [ -n "$DOCKER_CMD" ]; then
    if [[ "$DOCKER_CMD" == *"podman"* ]]; then
        echo "Checking Podman machine..."
        if ! podman machine inspect --format '{{.State}}' | grep -q "running"; then
            echo "Starting Podman machine..."
            podman machine start || true
        fi
    fi
    "$DOCKER_CMD" build -t fileserv .
else
    echo "Warning: Neither docker nor podman found. Skipping Docker build."
fi

echo ""
echo "=== Build and Test Completed Successfully ==="
