# Golden Knowledge Base

Cross-cutting standards that **every** feature must follow. These are the shared
rules; individual feature specs reference them instead of restating them, and
record only where (and why) they deviate.

## Contents

- [`documentation-lifecycle.md`](documentation-lifecycle.md) — document statuses,
  front matter, transitions, and phase gates. Governs all `.docs/` documents.
- [`architecture-principles.md`](architecture-principles.md) — layering, module
  boundaries, and design principles the whole codebase obeys.
- [`api-standards.md`](api-standards.md) — REST conventions, versioning, status
  codes, and the standard error-response shape.
- [`data-standards.md`](data-standards.md) — persistence, schema/migration, and
  data-handling rules.
- [`security-standards.md`](security-standards.md) — baseline security
  requirements for every feature.

## Relationship to Kiro steering

These documents describe the **standards for feature specs and the product**. The
machine-enforced engineering conventions live in `.kiro/steering/`
(project-structure, coding-standards, testing, documentation). Where they overlap,
the two must agree; the steering files are the authority for how code is written,
and this knowledge base is the authority for how features are specified.

## Changing a golden standard

Golden standards are themselves versioned docs. To change one:

1. Propose the change (short rationale) and review it like any design doc.
2. Update the standard and bump its `updated` date.
3. Note the change so existing specs can be re-checked against it.

A rule that applies to all features belongs here. A rule specific to one feature
belongs in that feature's spec.
