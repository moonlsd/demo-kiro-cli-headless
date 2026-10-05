---
inclusion: auto
name: feature-docs
description: Activate whenever work concerns a feature identified by a JIRA-style issue ID (e.g. KIRODEMO-001, KIRODEMO-123, or any <PROJECT>-<NUMBER>). Covers implementing, designing, modifying, reviewing, testing, or discussing such a feature, or any request that mentions a feature ID or a path under .docs/<ISSUE-ID>/.
---

# Working on a Feature (JIRA ID)

When a task concerns a feature identified by a JIRA-style issue ID
(`<PROJECT>-<NUMBER>`, e.g. `KIRODEMO-001`), the feature's specifications in
`.docs/` are the source of truth. You MUST ground your work in them.

## Always load the golden knowledge base

These cross-cutting standards apply to every feature. Read and comply with them on
any feature work:

- #[[file:.docs/golden/documentation-lifecycle.md]]
- #[[file:.docs/golden/architecture-principles.md]]
- #[[file:.docs/golden/api-standards.md]]
- #[[file:.docs/golden/data-standards.md]]
- #[[file:.docs/golden/security-standards.md]]

If a golden standard conflicts with the request, raise it rather than silently
violating the standard. A feature may deviate only when its `design.md` records the
deviation with justification.

## Always load the feature's own docs

For the specific feature ID in play (call it `<ISSUE-ID>`), read all three specs
before changing code:

- `.docs/<ISSUE-ID>/business.md` — purpose & intent (the *why*)
- `.docs/<ISSUE-ID>/requirements.md` — functional & non-functional requirements
  with acceptance criteria (the *what*)
- `.docs/<ISSUE-ID>/design.md` — detailed technical design (the *how*)

Steps:

1. Identify the feature ID from the request, branch name, commit, or an edited path
   under `.docs/<ISSUE-ID>/`.
2. If `.docs/<ISSUE-ID>/` does not exist, say so and offer to scaffold it from
   `.docs/_templates/` before writing code. Do not invent requirements.
3. Read the three feature docs and treat their requirements and design as binding.
   If the request contradicts an approved spec, flag the mismatch and confirm
   before diverging.

## Respect document status

Check each doc's `status` front matter (see the lifecycle standard):

- Do not begin implementation (`IN_PROGRESS`) until `requirements.md` and
  `design.md` are `APPROVED`, except for a clearly time-boxed, documented spike.
- If the code you are about to write diverges from an `IMPLEMENTED`/`DEPLOYED` doc,
  update the doc in the same change — a doc at that status must match the code.

## Keep docs and code in sync

Documentation is part of "done":

- When implementation changes behavior, update the matching spec and its
  `status`/`updated` fields in the same change.
- Keep the requirements `Traceability` table pointing at the real tests that verify
  each requirement.
- When a route, DTO, config key, or run step changes, also update the project
  `README.md` per the documentation steering.

## New feature with no ID yet

If the user describes a feature but gives no ID, ask for the JIRA ID (or propose the
next `KIRODEMO-NNN`), then scaffold `.docs/<ISSUE-ID>/` from `_templates/` starting
at `DRAFT` before implementing.
