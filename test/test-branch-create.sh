#!/bin/bash
# Test script for branch-create.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../bin" && pwd)"

#export QUIET=${QUIET:-false}

# Source common functions
# shellcheck source=../bin/common.sh
source "$SCRIPT_DIR/common.sh"

assert_branch_create_dry_run() {
    local expected_output="$1"
    shift
    local actual_output
    # Capture only the last two lines (the dry run git commands) or everything?
    # Actually, branch-create.sh also logs "Creating branch ..."
    # Let's check for the presence of the expected dry run commands.
    
    actual_output=$("$SCRIPT_DIR/branch-create.sh" --dry-run "$@" 2>&1)
    
    if echo "$actual_output" | grep -q "$expected_output"; then
        log "PASS: [$*] contains '$expected_output'"
    else
        error "FAIL: [$*] - expected to find '$expected_output' in output:
---
$actual_output
---"
    fi
}

test_branch_create() {
    log "Testing branch-create.sh --dry-run..."

    # Test 1: Manual title and labels (Feature)
    assert_branch_create_dry_run "Dry run: git checkout -b feature/123-new_feature" --title "New Feature" --labels "enhancement" 123
    assert_branch_create_dry_run "Dry run: git push origin feature/123-new_feature" --title "New Feature" --labels "enhancement" 123

    # Test 2: Manual type, title and labels
    assert_branch_create_dry_run "Dry run: git checkout -b bugfix/456-urgent_fix" --type "bugfix" --title "Urgent Fix" --labels "critical" 456

    # Test 3: Manual title, no labels (Task)
    assert_branch_create_dry_run "Dry run: git checkout -b task/789-just_a_task" --title "Just a Task" 789

    # Test 4: Special characters in title
    assert_branch_create_dry_run "Dry run: git checkout -b feature/101-feat_req_1" --title "Feat Req #1" --labels "feature" 101

    # Test 5: Linking branch (Dry run)
    assert_branch_create_dry_run "Dry run: Link branch feature/101-feat_req_1 to issue 101" --title "Feat Req #1" --labels "feature" 101
}

test_branch_create
