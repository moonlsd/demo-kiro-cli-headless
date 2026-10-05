#!/usr/bin/env bash
#
# Shared helpers for the SDLC CI scripts.
#
# Source this file; do not execute it directly:
#   source "$(dirname "$0")/lib.sh"
#
# Conventions:
#   - A feature's docs live in .docs/<JIRA_ID>/{business,requirements,design}.md
#   - The branch for a feature is feature/<JIRA_ID>
#   - Each doc carries YAML front matter with a `status:` field that acts as the
#     SDLC state (see .docs/golden/documentation-lifecycle.md).

set -euo pipefail

# Repo root (two levels up from .github/scripts).
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DOCS_DIR="${REPO_ROOT}/.docs"
TEMPLATES_DIR="${DOCS_DIR}/_templates"

log()  { printf '>> %s\n' "$*" >&2; }
die()  { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

# Validate a JIRA-style ID: <PROJECT>-<NUMBER>, e.g. KIRODEMO-001.
validate_jira_id() {
  local id="${1:-}"
  [[ "$id" =~ ^[A-Z][A-Z0-9]+-[0-9]+$ ]] || die "invalid JIRA ID: '${id}' (expected <PROJECT>-<NUMBER>)"
  printf '%s' "$id"
}

# Extract a JIRA ID from a branch name like feature/KIRODEMO-001.
jira_id_from_branch() {
  local branch="${1:-}"
  local id="${branch##*/}"
  validate_jira_id "$id"
}

feature_dir() {
  local id; id="$(validate_jira_id "${1:-}")"
  printf '%s/%s' "$DOCS_DIR" "$id"
}

# Read the `status:` value from a doc's front matter. Empty if absent.
read_doc_status() {
  local file="${1:-}"
  [[ -f "$file" ]] || { printf ''; return 0; }
  awk '
    NR==1 && $0=="---" { infm=1; next }
    infm && $0=="---"  { exit }
    infm && /^status:/ { sub(/^status:[[:space:]]*/, ""); print; exit }
  ' "$file"
}

# Set the `status:` value in a doc's front matter and bump `updated:` to today.
set_doc_status() {
  local file="${1:?doc path required}"
  local status="${2:?status required}"
  local today; today="$(date -u +%Y-%m-%d)"
  [[ -f "$file" ]] || die "cannot set status: file not found: $file"

  # Only touch the first front-matter block (between the first two '---' lines).
  awk -v st="$status" -v dt="$today" '
    BEGIN { infm=0; seen=0 }
    NR==1 && $0=="---" { infm=1; print; next }
    infm && $0=="---"  { infm=0; print; next }
    infm && /^status:/  { print "status: " st; next }
    infm && /^updated:/ { print "updated: " dt; next }
    { print }
  ' "$file" > "${file}.tmp" && mv "${file}.tmp" "$file"
  log "set status=${status} on $(basename "$file")"
}

# Current phase of a feature, derived from doc statuses.
# Echoes one of: MISSING, SPEC, DESIGN, IMPLEMENT, DONE
feature_phase() {
  local dir; dir="$(feature_dir "${1:-}")"
  [[ -d "$dir" ]] || { printf 'MISSING'; return 0; }
  local req des
  req="$(read_doc_status "${dir}/requirements.md")"
  des="$(read_doc_status "${dir}/design.md")"
  if [[ -z "$req" || "$req" == "DRAFT" ]]; then
    printf 'SPEC'
  elif [[ -z "$des" || "$des" == "DRAFT" ]]; then
    printf 'DESIGN'
  else
    printf 'IMPLEMENT'
  fi
}
