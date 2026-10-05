---
id: <ISSUE-ID>
title: <Short feature title>
type: design
status: DRAFT
owner: <name or team>
created: <YYYY-MM-DD>
updated: <YYYY-MM-DD>
approvers: []
supersedes: null
---

# <Feature title> — Detailed Design

> Depends on the approved `requirements.md`. Must comply with everything under
> [`../golden/`](../golden/); call out and justify any deviation. Status model:
> [`../golden/documentation-lifecycle.md`](../golden/documentation-lifecycle.md).

## Overview

A short summary of the approach and how it satisfies the requirements.

## Architecture & components

Where this fits in the layered architecture (`controller → service → repository`).
List the new/changed components per layer and their responsibilities.

```
(diagram or component list — e.g. request flow, sequence, or module map)
```

## API design

For each endpoint: method, path, request/response DTOs, status codes, and errors.
Follow `golden/api-standards.md`. Reference the central error-response shape rather
than redefining it.

| Method | Path | Request | Response | Status codes |
|--------|------|---------|----------|--------------|
| GET    | …    | …       | …        | 200, 404     |

## Data model & persistence

Entities, relationships, schema/migration changes, indexes, and transaction
boundaries. Follow `golden/data-standards.md`.

## Key design decisions

Record significant choices and the alternatives considered.

| Decision | Options considered | Choice & rationale |
|----------|--------------------|--------------------|
| …        | …                  | …                  |

## Error handling

Failure modes and how they are handled/translated to API responses. Reference the
centralized `@RestControllerAdvice` approach from the coding standards.

## Security

AuthN/AuthZ, input validation, data protection — specifics beyond
`golden/security-standards.md`.

## Observability

Logging (what/level), metrics, health, and how this feature is monitored in
production.

## Testing strategy

What will be unit-tested vs. slice-tested vs. integration-tested, and the key
scenarios. Align with the testing steering.

## Rollout & operations

Migration/backfill, feature flags, config/env vars (with defaults), rollback plan,
and any runbook notes.

## Risks & trade-offs

Technical risks introduced by this design and how they are mitigated.
