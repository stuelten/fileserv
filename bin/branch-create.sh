#!/bin/bash
#
# Create a branch for a GitHub issue.
#

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

usage() {
    cat <<EOF
Usage: $(basename "$0") [options] <issue-number>

Create a branch for a GitHub issue and push it to origin.

Options:
  -h, --help            Show this help message and exit.
  -t, --type <type>     Manually set branch type (feature, bugfix, docs, task).
  --title <title>       Manually set branch title.
  --labels <labels>     Manually set labels (comma-separated).
  --dry-run             Do not actually create or push the branch.

Environment Variables:
  TOKEN                 GitHub API token (required if fetching issue data).
  GITHUB_REPO           GitHub repository in "owner/repo" format.

EOF
}

MANUAL_TYPE=""
MANUAL_TITLE=""
MANUAL_LABELS=""
DRY_RUN=false

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
        --title)
            MANUAL_TITLE="$2"
            shift 2
            ;;
        --labels)
            MANUAL_LABELS="$2"
            shift 2
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        *)
            break
            ;;
    esac
done

ISSUE_NUMBER=$1

if [[ -z "$ISSUE_NUMBER" ]]; then
    error "No issue number provided"
fi

log "Fetching data for issue $ISSUE_NUMBER..."
ISSUE_DATA=$("$SCRIPT_DIR/gh-issue-data-read.sh" "$ISSUE_NUMBER")
if [[ $? -ne 0 ]]; then
    error "Failed to fetch issue data"
fi

if [[ -n "$MANUAL_TITLE" ]]; then
    ISSUE_TITLE="$MANUAL_TITLE"
else
    ISSUE_TITLE=$(echo "$ISSUE_DATA" | jq -r '.title')
fi
if [[ -n "$MANUAL_LABELS" ]]; then
    LABELS="$MANUAL_LABELS"
else
    LABELS=$(echo "$ISSUE_DATA" | jq -r '.labels[].name' | tr '\n' ',' | sed 's/,$//')
fi

BRANCH_NAME=$("$SCRIPT_DIR/branch-name-generate.sh" ${MANUAL_TYPE:+-t "$MANUAL_TYPE"} "$ISSUE_NUMBER" "$ISSUE_TITLE" "$LABELS")
if [[ $? -ne 0 ]]; then
    error "Failed to generate branch name"
fi

log "Creating branch $BRANCH_NAME..."

if [ "$DRY_RUN" = true ]; then
    echo "Dry run: git checkout -b $BRANCH_NAME"
    echo "Dry run: git push origin $BRANCH_NAME"
    echo "Dry run: gh pr create --title \"$ISSUE_TITLE\" --body \"Closes #$ISSUE_NUMBER\""
else
    git config --global user.name "GitHub Actions"
    git config --global user.email "actions@github.com"
    git checkout -b "$BRANCH_NAME"
    git push origin "$BRANCH_NAME"

    log "Creating pull request..."
    gh pr create --title "$ISSUE_TITLE" --body "Closes #$ISSUE_NUMBER"
fi
