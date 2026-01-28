#!/bin/bash

# bin/common.sh - Shared functions for scripts

# Default QUIET to false if not set
QUIET=${QUIET:-false}

log() {
    if [ "$QUIET" = false ]; then
        echo "$*" >&2
    fi
}

warn() {
    echo "Warning: $*" >&2
}

error() {
    echo "Error: $*" >&2
    exit 1
}
