#!/bin/bash

# bin/common.sh - Shared functions for FileServ scripts

# Default QUIET to false if not set
QUIET=${QUIET:-false}

log() {
    if [ "$QUIET" = false ]; then
        echo "$*"
    fi
}

warn() {
    echo "Warning: $*" >&2
}

error() {
    echo "Error: $*" >&2
}
