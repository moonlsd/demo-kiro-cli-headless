# Feature Specifications (`.docs`)

This directory is the home for feature specifications. Every non-trivial change to
the application starts as a spec here, moves through a defined lifecycle, and stays
as the durable record of *why* the feature exists and *how* it was built.

## Layout

```
.docs/
├── README.md                 # this file
├── _templates/               # starting points for a new feature (copy, don't edit in place)
│   ├── business.md
│   ├── requirements.md
│   └── design.md
├── golden/                   # the knowledge base every feature must follow
│   └── ...
└── <ISSUE-ID>/               # one directory per feature, named after its tracker issue
    ├── business.md           # business purpose & intent
    ├── requirements.md       # functional & non-functional requirements
    └── design.md             # detailed technical design
```

## Feature directories

- One directory per feature, named exactly after its tracker issue ID:
  `KIRODEMO-001`, `KIRODEMO-002`, and so on. This keeps specs traceable to the
  backlog and to commits/PRs that reference the same ID.
- Each feature holds three core documents:
  - **`business.md`** — the problem, who it serves, and the intended outcome. The
    "why".
  - **`requirements.md`** — functional and non-functional requirements with
    acceptance criteria. The "what".
  - **`design.md`** — the technical design that satisfies the requirements. The
    "how".
- A feature may add supporting files (diagrams, an `adr/` folder for decisions,
  API samples). Keep them inside the feature directory.

## The golden knowledge base (`golden/`)

`golden/` holds cross-cutting standards that **all** features must comply with
(architecture principles, API/data/security standards, and the shared document
lifecycle). A feature spec does not restate these — it references them and records
only where it deviates (with justification). When a rule applies to every feature,
it belongs in `golden/`, not in an individual spec.

## Document lifecycle & status

Every document carries a status in its front matter so anyone can see which phase
the feature is in at a glance. The full model (statuses, transitions, and who
approves each) lives in [`golden/documentation-lifecycle.md`](golden/documentation-lifecycle.md).

Quick reference: `DRAFT → IN_REVIEW → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPLOYED`,
plus the terminal states `ON_HOLD`, `DEPRECATED`, and `REJECTED`.

## Starting a new feature

1. Create a tracker issue and note its ID (e.g. `KIRODEMO-002`).
2. Create `.docs/KIRODEMO-002/`.
3. Copy the three files from `_templates/` into it and fill them in.
4. Start each document at `DRAFT`; advance the status as the feature progresses.
5. Keep the docs in sync with reality — a doc marked `IMPLEMENTED` must match the
   code that shipped.

See [`KIRODEMO-001`](KIRODEMO-001/) for a worked example.
