#!/bin/bash
#
# Generate a branch name from issue data.
#

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

usage() {
    cat <<EOF
Usage: $(basename "$0") [options] <issue-number> <issue-title> [labels]

Generate a branch name based on issue number, title, and labels.

Options:
  -h, --help    Show this help message and exit.
  -t, --type    Manually set branch type (feature, bugfix, docs, task).

Arguments:
  <issue-number>  The issue number.
  <issue-title>   The issue title.
  [labels]        Comma-separated list of labels or JSON array of labels.

EOF
}

MANUAL_TYPE=""
while [[ "$#" -gt 0 ]]; do
    case "$1" in
        -h|--help)
            usage
            exit 0
            ;;
        -t|--type)
            MANUAL_TYPE="$2"
            shift 2
            ;;
        *)
            break
            ;;
    esac
done

ISSUE_NUMBER=$1
ISSUE_TITLE=$2
LABELS=$3

if [[ -z "$ISSUE_NUMBER" || -z "$ISSUE_TITLE" ]]; then
    usage
    exit 1
fi

# Normalize title
NORMALIZED_TITLE=$(filename_sanitize "$ISSUE_TITLE")

# Determine branch type
if [[ -n "$MANUAL_TYPE" ]]; then
    BRANCH_TYPE="$MANUAL_TYPE"
else
    # Check labels (handles both comma-separated and JSON-like strings from the workflow)
    if echo "$LABELS" | grep -qiE "enhancement|feature"; then
        BRANCH_TYPE="feature"
    elif echo "$LABELS" | grep -qi "bug"; then
        BRANCH_TYPE="bugfix"
    elif echo "$LABELS" | grep -qi "documentation"; then
        BRANCH_TYPE="docs"
    else
        BRANCH_TYPE="task"
    fi
fi

echo "$BRANCH_TYPE/$ISSUE_NUMBER-$NORMALIZED_TITLE"
