#!/usr/bin/env bash
#
# Scaffold a feature's spec docs from the templates.
#
# Usage:
#   scaffold-feature.sh <JIRA_ID> <TITLE> <DESCRIPTION>
#
# Creates .docs/<JIRA_ID>/{business,requirements,design}.md from .docs/_templates,
# filling in the front matter (id, title, dates, owner) and leaving status at DRAFT.
# The DESCRIPTION is injected into business.md so the spec stage has the engineer's
# original intent to work from. Idempotent: refuses to overwrite an existing feature.

set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

JIRA_ID="$(validate_jira_id "${1:-}")"
TITLE="${2:-$JIRA_ID}"
DESCRIPTION="${3:-}"
OWNER="${SDLC_OWNER:-sdlc-bot}"
TODAY="$(date -u +%Y-%m-%d)"

DIR="$(feature_dir "$JIRA_ID")"
[[ -d "$DIR" ]] && die "feature already scaffolded: ${DIR}"
[[ -d "$TEMPLATES_DIR" ]] || die "templates dir missing: ${TEMPLATES_DIR}"

mkdir -p "$DIR"

# Fill the template front-matter placeholders for one doc.
render() {
  local src="$1" dst="$2"
  sed \
    -e "s|<ISSUE-ID>|${JIRA_ID}|g" \
    -e "s|<Short feature title>|${TITLE}|g" \
    -e "s|<Feature title>|${TITLE}|g" \
    -e "s|<name or team>|${OWNER}|g" \
    -e "s|<YYYY-MM-DD>|${TODAY}|g" \
    "$src" > "$dst"
}

render "${TEMPLATES_DIR}/business.md"     "${DIR}/business.md"
render "${TEMPLATES_DIR}/requirements.md" "${DIR}/requirements.md"
render "${TEMPLATES_DIR}/design.md"       "${DIR}/design.md"

# Seed the engineer's description into business.md under the problem heading so it
# is not lost. The spec stage expands this into a full spec.
if [[ -n "$DESCRIPTION" ]]; then
  awk -v desc="$DESCRIPTION" '
    { print }
    /^## Problem \/ opportunity/ && !done {
      print ""
      print "> Original request from the engineer who started this feature:"
      print ">"
      print "> " desc
      done=1
    }
  ' "${DIR}/business.md" > "${DIR}/business.md.tmp" && mv "${DIR}/business.md.tmp" "${DIR}/business.md"
fi

log "scaffolded ${DIR} (business, requirements, design) at status DRAFT"
printf '%s\n' "$DIR"
