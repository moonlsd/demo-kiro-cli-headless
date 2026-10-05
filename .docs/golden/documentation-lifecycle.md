# Document Lifecycle & Status Model

Applies to every document under `.docs/`. The status makes the phase of a feature
visible at a glance and governs what work may proceed.

## Front matter

Every spec document starts with a YAML front-matter block:

```yaml
---
id: KIRODEMO-001            # feature (tracker) ID this document belongs to
title: Short feature title
type: business | requirements | design
status: DRAFT               # see statuses below
owner: name or team
created: YYYY-MM-DD
updated: YYYY-MM-DD         # bump on every meaningful change
approvers: []               # who signed off (filled when APPROVED)
supersedes: null            # optional: ID/doc this replaces
---
```

Keep `updated` current and record approvers when a doc reaches `APPROVED`.

## Statuses

| Status        | Meaning                                                                 |
|---------------|-------------------------------------------------------------------------|
| `DRAFT`       | Being written. Incomplete; not ready for review. May change freely.     |
| `IN_REVIEW`   | Complete enough to review. Awaiting feedback/sign-off.                   |
| `APPROVED`    | Reviewed and signed off. Stable basis for the next phase.               |
| `IN_PROGRESS` | Implementation underway against this approved spec.                     |
| `IMPLEMENTED` | Code is written, merged, and matches this doc; verified by tests.       |
| `DEPLOYED`    | Running in production (note the environment/version in the doc).        |
| `ON_HOLD`     | Paused. Record why and the condition to resume.                         |
| `DEPRECATED`  | No longer the source of truth; kept for history. Point to its successor.|
| `REJECTED`    | Decided against. Kept to preserve the decision and its rationale.       |

## Typical transitions

```
DRAFT → IN_REVIEW → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPLOYED
                      │                                       │
                      └──────────────► ON_HOLD ◄──────────────┘
APPROVED/IN_REVIEW → REJECTED
IMPLEMENTED/DEPLOYED → DEPRECATED (when superseded)
```

Rules of thumb:

- The three documents of a feature can hold **different** statuses. Normal order is
  `business` leads, then `requirements`, then `design` — design should not be
  `APPROVED` before the requirements it implements are `APPROVED`.
- Do not start coding (`IN_PROGRESS`) until `requirements.md` and `design.md` are
  `APPROVED`, except for a time-boxed spike (note it in the doc).
- Moving a doc to `IMPLEMENTED` requires the code to actually match it. If reality
  diverged, update the doc first, then set the status.
- When a feature is replaced, mark the old docs `DEPRECATED` and set `supersedes`
  on the new one.

## Phase gates (who approves)

- `IN_REVIEW → APPROVED` for **business**: product/feature owner.
- `IN_REVIEW → APPROVED` for **requirements**: product owner + tech lead.
- `IN_REVIEW → APPROVED` for **design**: tech lead / reviewing engineer(s).

Record the approvers in front matter so sign-off is auditable.
