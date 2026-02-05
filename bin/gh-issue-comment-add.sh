#!/bin/bash
#
# Add a comment to a GitHub issue.
#

export VERBOSE=false
export QUIET=true

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Source common functions
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

usage() {
    cat <<EOF
Usage: $(basename "$0") [options] <issue-number> <comment-body>

Add a comment to a GitHub issue.

Options:
  -h, --help    Show this help message and exit.
  -v, --verbose Show verbose log messages.

Environment Variables:
  GH_TOKEN      GitHub API token.
  GH_TOKEN_FILE Path to a file containing the GitHub API token.
  GITHUB_REPO   GitHub repository in "owner/repo" format.

EOF
}

while [[ "$#" -gt 0 ]]; do
    case "$1" in
        -h|--help)
            usage
            exit 0
            ;;
        -v|--verbose)
            export VERBOSE=true
            export QUIET=false
            shift
            ;;
        *)
            break
            ;;
    esac
done

if [[ -n "$GH_TOKEN_FILE" ]]; then
    if [[ -f "$GH_TOKEN_FILE" ]]; then
        GH_TOKEN=$(cat "$GH_TOKEN_FILE")
    else
        error "GH_TOKEN_FILE '$GH_TOKEN_FILE' not found"
    fi
fi

if [[ -z "$GH_TOKEN" ]]; then
    error "No GitHub GH_TOKEN provided"
fi

if [[ -z "$GITHUB_REPO" ]]; then
    # Try to get repo from git remote
    REMOTE_URL=$(git_repo_get_remote_url)
    if [[ -n "$REMOTE_URL" ]]; then
        GITHUB_REPO=$(echo "$REMOTE_URL" | sed -E 's|.*github.com[:/](.*)\.git|\1|')
    fi
fi

ISSUE_NUMBER=$1
COMMENT_BODY=$2

if [[ -z "$ISSUE_NUMBER" ]]; then
    error "No issue number provided"
fi

if [[ -z "$COMMENT_BODY" ]]; then
    error "No comment body provided"
fi

URL="https://api.github.com/repos/$GITHUB_REPO/issues/$ISSUE_NUMBER/comments"
log "Adding comment to issue $ISSUE_NUMBER at $URL"

# Create JSON payload
PAYLOAD=$(jq -n --arg body "$COMMENT_BODY" '{body: $body}')

data=$(curl -sS -X POST -H "Authorization: token $GH_TOKEN" -H "Content-Type: application/json" -d "$PAYLOAD" "$URL")
curl_exit_code=$?

if [[ $curl_exit_code -ne 0 ]]; then
    error "GitHub API call failed with exit code $curl_exit_code"
fi

if echo "$data" | jq -e '.message' >/dev/null 2>&1; then
    msg=$(echo "$data" | jq -r '.message')
    error "adding comment to issue $ISSUE_NUMBER: $msg"
fi

log "Comment added successfully"
