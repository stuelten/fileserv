#!/bin/bash
# Test script for branch-name-generate.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../bin" && pwd)"

#export QUIET=${QUIET:-false}

# Source common functions
# shellcheck source=../bin/common.sh
source "$SCRIPT_DIR/common.sh"

assert_branch_name() {
    local expected=$1
    shift
    local actual
    actual=$("$SCRIPT_DIR/branch-name-generate.sh" "$@")
    test_assert "$expected" "$actual" "[$*] ->"
}

test_branch_name() {
    log "Testing branch-name-generate.sh..."

    assert_branch_name "feature/123-new_feature" 123 "New Feature" "enhancement"
    assert_branch_name "bugfix/456-fix_bug" 456 "Fix Bug" "bug"
    assert_branch_name "docs/789-update_readme" 789 "Update README" "documentation"
    assert_branch_name "task/101-some_work" 101 "Some Work" ""
    assert_branch_name "feature/123-my_custom_title" -t "feature" 123 "My Custom Title"
    assert_branch_name "feature/123-special_chars" 123 "Special @#$% chars!" "enhancement"
}

test_branch_name
