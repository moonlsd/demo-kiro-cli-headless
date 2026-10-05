#!/usr/bin/env bash
#
# Run one SDLC stage via kiro-cli in headless mode, then commit the result to the
# current branch (the PR branch).
#
# Usage:
#   run-stage.sh <STAGE> <JIRA_ID>
#
# Stages:
#   spec      -> draft business.md + requirements.md, set them to IN_REVIEW
#   design    -> draft design.md from the approved spec, set it to IN_REVIEW
#   implement -> write code satisfying the docs, run ./mvnw test, bump docs to
#                IMPLEMENTED (fails the stage if the build/tests fail)
#
# Requires on the runner:
#   - kiro-cli on PATH, authenticated (see .github/workflows/*.yml for the secret)
#   - git identity configured by the caller (the workflow sets it)
#   - for the implement stage: a JDK (the project targets Java 21) so ./mvnw runs
#
# Design notes:
#   - The prompt is intentionally explicit about scope and the exact files to touch.
#     The sdlc-spec agent is additionally restricted to .docs/** so it cannot edit
#     source code even if asked.
#   - After kiro-cli runs, this script enforces the status transition itself rather
#     than trusting the model to do it, so the state machine stays deterministic.

set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

STAGE="${1:?stage required (spec|design|implement)}"
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

# commit_and_report <message> [path ...]
# Stages the given paths (default: the feature docs dir) and commits if anything
# changed.
commit_and_report() {
  local message="$1"; shift
  local paths=("$@")
  [[ ${#paths[@]} -gt 0 ]] || paths=("$REL_DIR")
  cd "$REPO_ROOT"
  git add -- "${paths[@]}"
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

# Emit the DESIGN-stage prompt.
design_prompt() {
  cat <<EOF
You are running headless in CI for the DESIGN phase of feature ${JIRA_ID}.

Scope: you may ONLY edit ${REL_DIR}/design.md. Do NOT touch source code, and do
NOT modify business.md or requirements.md.

Read these first and comply with them:
- The golden knowledge base under .docs/golden/ (architecture, api, data, security).
- ${REL_DIR}/business.md and ${REL_DIR}/requirements.md (the approved spec).

Write a complete ${REL_DIR}/design.md that satisfies the approved requirements:
- Components per layer (controller/service/repository) and their responsibilities.
- API design (endpoints, DTOs, status codes) per the golden api-standards.
- Data model/persistence if applicable, per the golden data-standards.
- Key design decisions with rationale, error handling, and a testing strategy.
- A "Deviations from golden standards" section if anything departs, with justification.

Follow the existing design template headings. Do not leave placeholder text.
EOF
}

# Emit the IMPLEMENT-stage prompt.
implement_prompt() {
  cat <<EOF
You are running headless in CI for the IMPLEMENT phase of feature ${JIRA_ID}.

Implement the application code that satisfies the approved specs:
- ${REL_DIR}/requirements.md and ${REL_DIR}/design.md.

Comply with the golden knowledge base under .docs/golden/ and the project
conventions under .kiro/steering/. This is a Spring Boot (Java 21, Maven) project.

Requirements for your work:
1. Implement the feature following the layered architecture
   (controller -> service -> repository), constructor injection, and DTOs for web
   payloads. Match the existing code style.
2. Add unit/slice tests for the new behavior.
3. Build and test with: ./mvnw test
   Fix anything that fails and re-run until the build is green.
4. Keep changes scoped to this feature; do not refactor unrelated code.
5. You may edit files under src/** and ${REL_DIR}/. Do not edit CI config
   (.github/) or agent/steering config (.kiro/).

Do not stop until ./mvnw test passes.
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

  design)
    run_kiro "$(design_prompt)" "sdlc-design"
    set_doc_status "${DIR}/design.md" "IN_REVIEW"
    commit_and_report "${JIRA_ID}: draft design [automated]"
    ;;

  implement)
    run_kiro "$(implement_prompt)" "sdlc-implement"

    # Verification gate: the stage fails unless the build and tests pass. This is
    # enforced here (not just asked of the agent) so a green build is mandatory.
    cd "$REPO_ROOT"
    log "running verification: ./mvnw test"
    if ! ./mvnw -B test; then
      die "verification failed: ./mvnw test did not pass. Not advancing status."
    fi
    log "verification passed"

    # Only on success: mark the feature implemented.
    set_doc_status "${DIR}/business.md"     "IMPLEMENTED"
    set_doc_status "${DIR}/requirements.md" "IMPLEMENTED"
    set_doc_status "${DIR}/design.md"       "IMPLEMENTED"
    commit_and_report "${JIRA_ID}: implement feature + passing tests [automated]" \
      "src" "$REL_DIR"
    ;;

  *)
    die "unknown stage: '${STAGE}' (supported: spec, design, implement)"
    ;;
esac

log "stage '${STAGE}' complete for ${JIRA_ID}"
