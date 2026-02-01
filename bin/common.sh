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

# Find a jar for a module and error if not found
# Usage: check_jar <module_path> <artifact_id>
# Returns: The path to the found jar
find_jar() {
    local module_path="$1"
    local artifact_id="$2"
    local jar

    # Prefer the non-versioned jar if it exists, otherwise use the versioned one
    jar=$(ls "${module_path}/target/${artifact_id}.jar" 2>/dev/null || ls "${module_path}/target/${artifact_id}-"*.jar 2>/dev/null | grep -v "original-" | head -n 1)

    if [ -z "$jar" ] || [ ! -f "$jar" ]; then
        log "jar not found for $1:$2"
        log "$(ls "${module_path}"/target)"
        error "Could not find jar for ${artifact_id}"
    fi
    echo "$jar"
}

# Get current git repo's remote URL
git_repo_get_remote_url() {
    local remote=${1:-origin}
    git remote get-url "$remote" 2>/dev/null
}

# Wait for a URL to respond with HTTP 200 (or any response from curl -s)
# Usage: url_get_wait_and_retry <url> [max_retries] [sleep_seconds]
url_get_wait_and_retry() {
    local url="$1"
    local max_retries="${2:-30}"
    local sleep_sec="${3:-2}"
    local retry_count=0

    while ! curl -s "$url" > /dev/null; do
        retry_count=$((retry_count+1))
        if [ "$retry_count" -ge "$max_retries" ]; then
            return 1
        fi
        sleep "$sleep_sec"
    done
    return 0
}
