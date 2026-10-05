---
title: Data Standards
status: APPROVED
owner: engineering
created: 2026-10-05
updated: 2026-10-05
---

# Data Standards

Rules for how features model, persist, and handle data.

## Persistence layer

- Access data only through the repository layer. Services call repositories;
  controllers never do.
- Prefer Spring Data derived query methods; use `@Query` only when a derived method
  is impractical, and document why in the design.
- Own transaction boundaries in the service layer. Mark read-only paths
  `@Transactional(readOnly = true)`.
- Keep entities (persistence) separate from DTOs (transport). Map at the boundary.

## Schema & migrations

- Manage schema with versioned migrations (Flyway or Liquibase). Never rely on
  `ddl-auto` to change a real database.
- Migrations are immutable once merged — add a new migration to change schema;
  never edit an applied one.
- Every schema change ships in the same feature as the code that needs it, and is
  described in the design doc.

## Modeling conventions

- Use a surrogate primary key (e.g. generated `id`) unless a natural key is clearly
  better and justified.
- Name tables/columns consistently (`snake_case` in the database, mapped to
  `camelCase` fields). Plural table names.
- Add indexes for columns used in lookups/joins; call out expected query patterns
  in the design.
- Store timestamps in UTC. Capture `created_at`/`updated_at` where auditability
  matters.
- Prefer non-null columns with sensible defaults; make nullability a deliberate
  decision.

## Data classification & protection

- Classify data in the requirements doc (public / internal / confidential / PII).
- Never log secrets or PII. Mask or omit sensitive fields in logs and API
  responses.
- Encrypt sensitive data at rest and in transit per the security standards.
- Define retention and deletion for personal/regulated data; don't keep data longer
  than needed.

## Consistency & integrity

- Enforce invariants with database constraints (FKs, unique, not-null) in addition
  to application validation — do not rely on the app alone.
- Design for idempotency on write operations that may be retried.
- Be explicit about consistency expectations when caching or async processing is
  introduced.

## Deviations

Record any departure from these rules, with justification, in the feature's
`design.md`.
