#!/bin/bash
# Run all test scripts

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../bin" && pwd)"

# Source common functions
# shellcheck source=../bin/common.sh
source "$SCRIPT_DIR/common.sh"

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    -h|--help)
      echo "Usage: $0 [options]"
      echo "Options:"
      echo "  -q, --quiet    Minimize output"
      echo "  -v, --verbose  Detailed output"
      exit 0
      ;;
    -q|--quiet)
      QUIET=true
      shift
      ;;
    -v|--verbose)
      VERBOSE=true
      shift
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

export QUIET
export VERBOSE

TESTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# simply run all test scripts
for testscript in "$TESTS_DIR"/test-*.sh ; do
  test_run "$testscript"
done
