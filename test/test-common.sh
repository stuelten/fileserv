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
