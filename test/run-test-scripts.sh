#!/bin/bash
# Run all test scripts

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../bin" && pwd)"

#export QUIET=${QUIET:-false}

# Source common functions
# shellcheck source=../bin/common.sh
source "$SCRIPT_DIR/common.sh"

TESTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# simply run all test scripts
for testscript in "$TESTS_DIR"/test-*.sh ; do
  test_run "$testscript"
done
