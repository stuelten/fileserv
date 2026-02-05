#!/bin/bash
#
# Extract a field from GitHub issue JSON.
#
# Usage: bin/gh-issue-data-field-read.sh <field>
# <field> can be 'title' or 'label'.
# JSON data is read from stdin.

VERBOSE=false
QUIET=true

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Source common functions
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

usage() {
    cat <<EOF
Usage: $(basename "$0") [options] <field>

Extract a field from the GitHub issue JSON.
See issue-data-read.sh for reading the GitHub issue.

Options:
  -h, --help    Show this help message and exit.
  -v, --verbose Show verbose log messages.

Arguments:
  <field>       The field to extract. Supported fields:
                title, state, created_at, author, assignee, label.

Input:
  Expects GitHub issue JSON data on stdin.

Example:
  gh-issue-data-read.sh 123 | $(basename "$0") title
EOF
}

while [[ "$#" -gt 0 ]]; do
    case "$1" in
        -h|--help)
            usage
            exit 0
            ;;
        -v|--verbose)
            VERBOSE=true
            QUIET=false
            shift
            ;;
        *)
            break
            ;;
    esac
done

FIELD=$1

if [[ -z "$FIELD" ]]; then
    error "No field specified. Use 'title' or 'label'."
fi

# Read JSON from stdin
log "Reading JSON from stdin for field: $FIELD"
DATA=$(cat)

if [[ -z "$DATA" ]]; then
    error "No JSON data received on stdin"
fi

case "$FIELD" in
    title)
        echo "$DATA" | jq -r '.title'
        ;;
    state)
        echo "$DATA" | jq -r '.state'
        ;;
    created_at)
        echo "$DATA" | jq -r '.created_at'
        ;;
    author)
        echo "$DATA" | jq -r '.user.login'
        ;;
    assignee)
        echo "$DATA" | jq -r '.assignee.login'
        ;;
    label)
        echo "$DATA" | jq -r '.labels[].name // ""'
        ;;
    *)
        error "Unknown field '$FIELD'. Supported fields: title, state, created_at, author, assignee, label."
        ;;
esac
