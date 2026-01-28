#!/bin/bash

# bin/common.sh - Shared functions for scripts

### Logging

# Default QUIET to false if not set
QUIET=${QUIET:-false}
# Default VERBOSE to false if not set
VERBOSE=${VERBOSE:-false}

log() {
    if [ "$QUIET" = false ]; then
        echo "$*"
    fi
}

debug() {
    if [ "$VERBOSE" = true ]; then
        echo "DEBUG: $*"
    fi
}

info() {
    echo "INFO: $*"
}

warn() {
    echo "WARN: $*" >&2
}

error() {
    echo "ERROR: $*" >&2
    exit 1
}

### Test

# Assert two values are equal
test_assert() {
    local expected="$1"
    local actual="$2"
    local msg="$3"
    if [ "$actual" == "$expected" ]; then
        log "PASS: $msg '$actual'"
    else
        error "FAIL: $msg - expected '$expected', got '$actual'"
    fi
}

# Call $1 and fail on error with some logging
test_run() {
    debug "Test $1"
    $1 || warn "Failed: $0 test '$1'"
}

### Other stuff

# Sanitize filename: use only lower case and replace non-alphanumeric with underscore
filename_sanitize() {
    local title="$1"
    echo "$title" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9]/_/g' | sed -E 's/_+/_/g' | sed 's/^_//;s/_$//'
}

# Get current git repo's remote URL
git_repo_get_remote_url() {
    local remote=${1:-origin}
    git remote get-url "$remote" 2>/dev/null
}
