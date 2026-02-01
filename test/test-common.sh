#!/bin/bash
# Test script for common.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../bin" && pwd)"

#export QUIET=${QUIET:-false}

# Source common functions
# shellcheck source=../bin/common.sh
source "$SCRIPT_DIR/common.sh"

assert_filename_sanitize() {
    local input=$1
    local expected=$2
    local actual
    actual=$(filename_sanitize "$input")
    test_assert "$expected" "$actual" "'$input' ->"
}

test_filename_sanitize() {
    log "Testing function filename_sanitize..."

    # Basic tests
    assert_filename_sanitize "Hello World" "hello_world"
    assert_filename_sanitize "Test-Case" "test_case"
    assert_filename_sanitize "Already_Sanitized" "already_sanitized"

    # Special characters
    assert_filename_sanitize "Title with @#$% special chars!" "title_with_special_chars"
    assert_filename_sanitize "dots.and.dashes-and_underscores" "dots_and_dashes_and_underscores"
    assert_filename_sanitize "slashes/and\backslashes\\and//\/so|on:and;on!" "slashes_and_backslashes_and_so_on_and_on"

    # Multiple non-alphanumeric characters
    assert_filename_sanitize "Multiple    Spaces" "multiple_spaces"
    assert_filename_sanitize "Multiple---Dashes" "multiple_dashes"
    assert_filename_sanitize "Mixed...---@@@Characters" "mixed_characters"

    # Leading and trailing non-alphanumeric characters
    assert_filename_sanitize "---Leading" "leading"
    assert_filename_sanitize "Trailing!!!" "trailing"
    assert_filename_sanitize "---Both---" "both"

    # Numbers
    assert_filename_sanitize "Issue 123" "issue_123"
    assert_filename_sanitize "v1.2.3-final" "v1_2_3_final"

    # Empty or only special characters
    assert_filename_sanitize "" ""
    assert_filename_sanitize "!!!" ""
    assert_filename_sanitize "   " ""
}

test_filename_sanitize

test_logging() {
    log "Testing logging functions..."
    
    # Test log
    local output
    output=$(QUIET=false log "test log" 2>&1)
    test_assert "test log" "$output" "log output"
    
    output=$(QUIET=true log "test log" 2>&1)
    test_assert "" "$output" "log output (quiet)"
    
    # Test debug
    output=$(VERBOSE=true debug "test debug" 2>&1)
    test_assert "DEBUG: test debug" "$output" "debug output (verbose)"
    
    output=$(VERBOSE=false debug "test debug" 2>&1)
    test_assert "" "$output" "debug output (non-verbose)"
    
    # Test info
    output=$(info "test info" 2>&1)
    test_assert "INFO: test info" "$output" "info output"
    
    # Test warn
    output=$(warn "test warn" 2>&1)
    test_assert "WARN: test warn" "$output" "warn output"
}

test_logging

test_find_jar() {
    log "Testing find_jar..."
    local tmp_dir
    tmp_dir=$(mktemp -d)
    mkdir -p "${tmp_dir}/target"
    
    # Test with versioned jar
    touch "${tmp_dir}/target/test-artifact-1.0.0.jar"
    local jar
    jar=$(find_jar "${tmp_dir}" "test-artifact")
    test_assert "${tmp_dir}/target/test-artifact-1.0.0.jar" "$jar" "find_jar versioned"
    
    # Test with non-versioned jar (should prefer this)
    touch "${tmp_dir}/target/test-artifact.jar"
    jar=$(find_jar "${tmp_dir}" "test-artifact")
    test_assert "${tmp_dir}/target/test-artifact.jar" "$jar" "find_jar non-versioned preference"
    
    # Test original- jar exclusion
    rm "${tmp_dir}/target/test-artifact.jar"
    rm "${tmp_dir}/target/test-artifact-1.0.0.jar"
    touch "${tmp_dir}/target/original-test-artifact-1.0.0.jar"
    touch "${tmp_dir}/target/test-artifact-1.0.0-SNAPSHOT.jar"
    jar=$(find_jar "${tmp_dir}" "test-artifact")
    test_assert "${tmp_dir}/target/test-artifact-1.0.0-SNAPSHOT.jar" "$jar" "find_jar exclude original"
    
    rm -rf "$tmp_dir"
}

test_find_jar

test_git_remote() {
    log "Testing git_repo_get_remote_url..."
    # This might be tricky in CI if there's no git, but let's try
    local url
    url=$(git_repo_get_remote_url)
    if [ -n "$url" ]; then
        log "Found remote URL: $url"
    else
        log "No remote URL found (expected if not in a git repo or no origin)"
    fi
}

test_git_remote

test_url_get_wait() {
    log "Testing url_get_wait_and_retry..."
    
    # Use a subshell to avoid affecting the main shell with function definitions
    (
        curl() {
            return 0
        }
        export -f curl
        url_get_wait_and_retry "http://example.com" 1 1
        test_assert "0" "$?" "url_get_wait_and_retry success"
    )
    
    (
        curl() {
            return 1
        }
        export -f curl
        # Use small retries and sleep for test speed
        url_get_wait_and_retry "http://example.com" 2 1
        local res=$?
        test_assert "1" "$res" "url_get_wait_and_retry failure"
    )

    (
        local state_file
        state_file=$(mktemp)
        echo "0" > "$state_file"
        curl() {
            local count
            count=$(cat "$1")
            count=$((count + 1))
            echo "$count" > "$1"
            if [ "$count" -lt 2 ]; then
                return 1
            fi
            return 0
        }
        export -f curl
        url_get_wait_and_retry "$state_file" 3 1
        test_assert "0" "$?" "url_get_wait_and_retry success after retry"
        rm "$state_file"
    )

    exit 0
}

test_url_get_wait
