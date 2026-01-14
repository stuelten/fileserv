#!/usr/bin/env bats

setup() {
    # Set path to the native executable
    if [ -f "./target/fileserv-test-generate-hierarchy" ]; then
        SCRIPT="./target/fileserv-test-generate-hierarchy"
    else
        SCRIPT="./fileserv-test-generate-hierarchy/target/fileserv-test-generate-hierarchy"
    fi
    TEST_DIR="test_data_java_bats"
}

teardown() {
    # Clean up generated test data
    rm -rf "$TEST_DIR"
}

@test "java native executable exists and is executable" {
    [ -x "$SCRIPT" ]
}

@test "java: minimal settings: 1 item, small size (10KB)" {
    run "$SCRIPT" --size 10kb --count 1 --ratio-dir-to-files 0 --depth 1 "$TEST_DIR"
    [ "$status" -eq 0 ]
    [ -d "$TEST_DIR" ]
    
    # With count 1 and size > 0, the Java implementation ensures at least one file is created.
    # So we get targetDir + 1 file = 2 items.
    TOTAL_ITEMS=$(find "$TEST_DIR" | wc -l)
    [ "$TOTAL_ITEMS" -eq 2 ]
    
    # Check size (approx 10KB)
    ACTUAL_SIZE=$(du -k "$TEST_DIR" | cut -f1)
    [ "$ACTUAL_SIZE" -ge 10 ]
}

@test "java: default usage with minimal MB" {
    run "$SCRIPT" --size 1 --count 10 --ratio-dir-to-files 5 --depth 2 "$TEST_DIR"
    [ "$status" -eq 0 ]
    [ -d "$TEST_DIR" ]
    
    # Check total count (files + dirs)
    # The script calculates NUM_DIRS = 10 / 6 = 1.
    # NUM_FILES = 10 - 1 = 9.
    # Total items should be 10.
    
    TOTAL_ITEMS=$(find "$TEST_DIR" | wc -l)
    # find includes the root dir itself.
    [ "$TOTAL_ITEMS" -eq 10 ]
}

@test "java: hierarchy depth constraint" {
    run "$SCRIPT" --size 1 --count 20 --ratio-dir-to-files 1 --depth 3 "$TEST_DIR"
    [ "$status" -eq 0 ]
    
    # Max depth check
    MAX_REL_DEPTH=0
        find "$TEST_DIR" -type d | while read d; do
        REL_PATH=${d#$TEST_DIR}
        REL_PATH=${REL_PATH#/}
        if [ -z "$REL_PATH" ]; then
            CURRENT_DEPTH=0
        else
            CURRENT_DEPTH=$(echo "$REL_PATH" | tr '/' '\n' | wc -l)
        fi
        if [ "$CURRENT_DEPTH" -gt "$MAX_REL_DEPTH" ]; then
            MAX_REL_DEPTH=$CURRENT_DEPTH
        fi
    done
    
    [ "$MAX_REL_DEPTH" -le 3 ]
}

@test "java: fails on invalid arguments" {
    # Picocli will return non-zero for missing parameters or invalid options
    run "$SCRIPT" --size 0 --count 0
    [ "$status" -ne 0 ]
}

@test "java: short options: -s, -c, -r, -d" {
    run "$SCRIPT" -s 10kb -c 5 -r 1 -d 2 "$TEST_DIR"
    [ "$status" -eq 0 ]
    [ -d "$TEST_DIR" ]
    
    # -c 5, -r 1 => NUM_DIRS = 5 / 2 = 2. NUM_FILES = 5 - 2 = 3.
    # Total items in find should be 5.
    TOTAL_ITEMS=$(find "$TEST_DIR" | wc -l)
    [ "$TOTAL_ITEMS" -eq 5 ]
}
