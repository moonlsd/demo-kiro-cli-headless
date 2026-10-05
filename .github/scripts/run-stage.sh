#!/usr/bin/env bash
#
# Run one SDLC stage via kiro-cli in headless mode, then commit the result to the
# current branch (the PR branch).
#
# Usage:
#   run-stage.sh <STAGE> <JIRA_ID>
#
# Stages implemented in this vertical slice:
#   spec   -> draft business.md + requirements.md, set them to IN_REVIEW
#
# Requires on the runner:
#   - kiro-cli on PATH, authenticated (see .github/workflows/*.yml for the secret)
#   - git identity configured by the caller (the workflow sets it)
#
# Design notes:
#   - The prompt is intentionally explicit about scope and the exact files to touch.
#     The sdlc-spec agent is additionally restricted to .docs/** so it cannot edit
#     source code even if asked.
#   - After kiro-cli runs, this script enforces the status transition itself rather
#     than trusting the model to do it, so the state machine stays deterministic.

set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

STAGE="${1:?stage required (spec)}"
JIRA_ID="$(validate_jira_id "${2:-}")"
DIR="$(feature_dir "$JIRA_ID")"
REL_DIR=".docs/${JIRA_ID}"

[[ -d "$DIR" ]] || die "feature docs not found: ${DIR}"
command -v kiro-cli >/dev/null 2>&1 || die "kiro-cli not found on PATH"

run_kiro() {
  local prompt="$1" agent="$2"
  log "invoking kiro-cli (agent=${agent}) ..."
  # Run from the repo root: kiro-cli discovers workspace agents from
  # ./.kiro/agents/ relative to the current directory, so the CWD must be the
  # repo root for --agent "$agent" to resolve.
  cd "$REPO_ROOT"
  # --no-interactive: no TTY prompts; --trust-all-tools: allow file edits unattended.
  # The agent definition constrains which tools/paths are actually permitted.
  kiro-cli chat "$prompt" \
    --no-interactive \
    --trust-all-tools \
    --agent "$agent"
}

commit_and_report() {
  local message="$1"
  cd "$REPO_ROOT"
  git add "$REL_DIR"
  if git diff --cached --quiet; then
    log "no changes produced by stage '${STAGE}'"
    return 0
  fi
  git commit -m "$message"
  log "committed: ${message}"
}

# Emit the SPEC-stage prompt on stdout. Kept as a function with a top-level
# heredoc to avoid fragile command-substitution-plus-heredoc nesting inside case.
spec_prompt() {
  cat <<EOF
You are running headless in CI for the SPEC phase of feature ${JIRA_ID}.

Scope: you may ONLY edit files under ${REL_DIR}/. Do NOT touch source code.

Read these first and comply with them:
- The golden knowledge base under .docs/golden/ (architecture, api, data, security,
  and the document lifecycle/status model).
- ${REL_DIR}/business.md, which contains the original request from the engineer
  near the top.

Produce the SPEC artifacts for this feature:
1. Complete ${REL_DIR}/business.md: fill every section (problem, goals, non-goals,
   stakeholders, success metrics, assumptions, risks) based on the original request.
   Do not leave template placeholder text.
2. Complete ${REL_DIR}/requirements.md: write concrete, testable functional
   requirements (FR-n with GIVEN/WHEN/THEN acceptance criteria) and non-functional
   requirements that reference the golden standards. Keep it consistent with the
   business intent.
3. Do NOT write design.md in this phase. Leave it untouched at DRAFT.

Keep the writing clear and concise. Follow the existing template structure and
headings. Do not change files outside ${REL_DIR}/.
EOF
}

case "$STAGE" in
  spec)
    run_kiro "$(spec_prompt)" "sdlc-spec"
    # Enforce the state transition deterministically.
    set_doc_status "${DIR}/business.md"     "IN_REVIEW"
    set_doc_status "${DIR}/requirements.md" "IN_REVIEW"
    commit_and_report "${JIRA_ID}: draft spec (business + requirements) [automated]"
    ;;

  *)
    die "unknown or not-yet-implemented stage: '${STAGE}' (vertical slice supports: spec)"
    ;;
esac

log "stage '${STAGE}' complete for ${JIRA_ID}"
