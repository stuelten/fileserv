#!/bin/bash
#
# Read data from a GitHub issue.
#

VERBOSE=false
QUIET=true

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Source common functions
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

usage() {
    cat <<EOF
Usage: $(basename "$0") [options] <issue-number>

Read data from a GitHub issue and print it as JSON.
The issue number must be given as the only parameter.

The API token must be set directly in the TOKEN or indirectly in the TOKEN_FILE
(path to file) environment variable.
The GITHUB_REPO environment variable can be set to the path to use
(usually "owner/repo"). The scripts defaults to discovery via git remote.

Options:
  -h, --help    Show this help message and exit.
  -v, --verbose Show verbose log messages.
  --json        Output the raw JSON response from GitHub API.

Environment Variables:
  TOKEN         GitHub API token.
  TOKEN_FILE    Path to a file containing the GitHub API token.
  GITHUB_REPO   GitHub repository in "owner/repo" format (e.g., "stuelten/fileserv").
                If not set, it attempts to discover the repo from the git remote.

Examples:
  TOKEN=ghp_... $(basename "$0") 123
  $(basename "$0") --json 123 | bin/issue-data-field-read.sh title
EOF
}

JSON_OUTPUT=false
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
        --json)
            JSON_OUTPUT=true
            shift
            ;;
        *)
            break
            ;;
    esac
done

if [[ -n "$TOKEN_FILE" ]]; then
    if [[ -f "$TOKEN_FILE" ]]; then
        TOKEN=$(cat "$TOKEN_FILE")
    else
        error "TOKEN_FILE '$TOKEN_FILE' not found"
    fi
fi

if [[ -z "$TOKEN" ]]; then
    error "No GitHub TOKEN provided (use TOKEN or TOKEN_FILE env var)"
fi

if [[ -z "$GITHUB_REPO" ]]; then
    # Try to get repo from git remote
    REMOTE_URL=$(git remote get-url origin 2>/dev/null)
    if [[ $? -eq 0 ]]; then
        # Handle both https and ssh formats
        # https://github.com/owner/repo.git -> owner/repo
        # git@github.com:owner/repo.git -> owner/repo
        GITHUB_REPO=$(echo "$REMOTE_URL" | sed -E 's|.*github.com[:/](.*)\.git|\1|')
    fi
fi

if [[ -z "$1" ]]; then
    error "No issue number provided"
fi

if ! [[ "$1" =~ ^[0-9]+$ ]]; then
    error "Issue number '$1' is not numerical"
fi

# reads the issue via curl
URL="https://api.github.com/repos/$GITHUB_REPO/issues/$1"
log "Read issue $1 from $URL"
data=$(curl -sS -H "Authorization: token $TOKEN" $URL)
curl_exit_code=$?

if [[ $curl_exit_code -ne 0 ]]; then
    error "GitHub API call failed with exit code $curl_exit_code"
fi

if [[ -z "$data" ]]; then
    error "Received empty response from GitHub API"
fi

# handle API errors (e.g., 404, 401) returned as JSON
if echo "$data" | jq -e '.message' >/dev/null 2>&1; then
    msg=$(echo "$data" | jq -r '.message')
    error "reading issue $1 from $URL: $msg"
fi

# output result
echo "$data"
