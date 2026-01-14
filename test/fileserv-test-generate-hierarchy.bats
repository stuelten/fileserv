#!/usr/bin/env bats

setup() {
    # Set path to the script being tested relative to the project root
    SCRIPT="./bin/fileserv-test-generate-hierarchy"
    TEST_DIR="test_data_bats"
}

teardown() {
    # Clean up generated test data
    rm -rf "$TEST_DIR"
}

@test "script exists and is executable" {
    [ -x "$SCRIPT" ]
}

@test "minimal settings: 1 item, small size (10KB)" {
    run "$SCRIPT" --size 10kb --count 1 --ratio-dir-to-files 0 --depth 1 "$TEST_DIR"
    [ "$status" -eq 0 ]
    [ -d "$TEST_DIR" ]
    
    # With count 1, we get 1 directory (the target dir) and 0 files
    # NUM_DIRS = 1 / 1 = 1. NUM_FILES = 1 - 1 = 0.
    TOTAL_ITEMS=$(find "$TEST_DIR" | wc -l)
    [ "$TOTAL_ITEMS" -eq 1 ]
}

@test "default usage with minimal MB" {
    run "$SCRIPT" --size 1 --count 10 --ratio-dir-to-files 5 --depth 2 "$TEST_DIR"
    [ "$status" -eq 0 ]
    [ -d "$TEST_DIR" ]
    
    # Check total count (files + dirs)
    # The script calculates NUM_DIRS=$(( COUNT / (RATIO + 1) ))
    # For count 10, ratio 5: NUM_DIRS = 10 / 6 = 1.
    # NUM_FILES = 10 - 1 = 9.
    # Total items should be 10.
    
    TOTAL_ITEMS=$(find "$TEST_DIR" | wc -l)
    # find includes the root dir itself.
    # So if there are 10 items total (including root), find returns 10.
    [ "$TOTAL_ITEMS" -eq 10 ]
}

@test "hierarchy depth constraint" {
    run "$SCRIPT" --size 1 --count 20 --ratio-dir-to-files 1 --depth 3 "$TEST_DIR"
    [ "$status" -eq 0 ]
    
    # Max depth check
    MAX_DEPTH=$(find "$TEST_DIR" | awk -F/ '{print NF-1}' | sort -rn | head -1)
    # NF-1 for test_data_bats/dir...
    # test_data_bats is depth 1 relative to current dir? 
    # Let's be precise.
    # find test_data_bats -type d
    # test_data_bats -> depth 0 relative to itself
    # test_data_bats/dir1 -> depth 1
    # test_data_bats/dir1/dir2 -> depth 2
    # The script uses DEPTH as max depth from TARGET.
    
    ACTUAL_DEPTH=$(find "$TEST_DIR" -type d | awk -F/ '{print NF-1}' | sort -rn | head -1)
    # If $TEST_DIR is at root, and it contains subdirs, NF-1 will give:
    # test_data_bats -> 1
    # test_data_bats/dir -> 2
    # So relative depth is NF - 1 - (depth of $TEST_DIR).
    # depth of $TEST_DIR is 1.
    # So depth of subdirs is NF - 2.
    
    RELATIVE_DEPTH=$(( ACTUAL_DEPTH - 1 ))
    [ "$RELATIVE_DEPTH" -le 3 ]
}

@test "fails on invalid arguments" {
    run "$SCRIPT" --size 0 --count 0 "$TEST_DIR"
    [ "$status" -eq 1 ]
}

@test "short options: -s, -c, -r, -d" {
    run "$SCRIPT" -s 10kb -c 5 -r 1 -d 2 "$TEST_DIR"
    [ "$status" -eq 0 ]
    [ -d "$TEST_DIR" ]
    
    # -c 5, -r 1 => NUM_DIRS = 5 / 2 = 2. NUM_FILES = 5 - 2 = 3.
    # Total items in find should be 5.
    TOTAL_ITEMS=$(find "$TEST_DIR" | wc -l)
    [ "$TOTAL_ITEMS" -eq 5 ]
    
    # Check depth
    ACTUAL_DEPTH=$(find "$TEST_DIR" -type d | awk -F/ '{print NF-1}' | sort -rn | head -1)
    RELATIVE_DEPTH=$(( ACTUAL_DEPTH - 1 ))
    [ "$RELATIVE_DEPTH" -le 2 ]
}
