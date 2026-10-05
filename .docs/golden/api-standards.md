---
title: API Standards
status: APPROVED
owner: engineering
created: 2026-10-05
updated: 2026-10-05
---

# API Standards

Conventions every HTTP API in this application follows. A feature's `design.md`
specifies its endpoints against these rules and does not restate them.

## Resource naming & URLs

- Use noun-based, plural resource names: `/api/v1/users`, `/api/v1/users/{id}`.
- Use path segments for identity and query parameters for filtering, sorting, and
  pagination: `/api/v1/users?role=admin&page=0&size=20`.
- Lowercase, hyphen-separated path segments. No verbs in paths — the HTTP method
  is the verb.

## Versioning

- Prefix routes with a major version: `/api/v1/...`.
- Introduce a new version only for breaking changes; evolve compatibly within a
  version (add fields, don't repurpose or remove them).

## HTTP methods & status codes

| Method | Use                         | Success        |
|--------|-----------------------------|----------------|
| GET    | Read, no side effects       | 200            |
| POST   | Create / non-idempotent op  | 201 (+Location) / 200 |
| PUT    | Full replace (idempotent)   | 200 / 204      |
| PATCH  | Partial update              | 200            |
| DELETE | Remove (idempotent)         | 204            |

- `400` validation error, `401` unauthenticated, `403` unauthorized, `404` not
  found, `409` conflict, `422` semantically invalid, `500` server error.
- Never return `200` for an error. Choose the status that reflects the outcome.

## Request & response bodies

- JSON only. Use DTOs (prefer `record`s); never accept or return JPA entities.
- Field names in `camelCase`. Use ISO-8601 for dates/times (UTC).
- Validate every request body with Jakarta Bean Validation (`@Valid` + constraints).
- Responses contain only the data the caller needs; do not leak internal fields.

## Standard error response

All errors share one shape (RFC 7807-style problem detail), produced centrally by a
`@RestControllerAdvice`. Features reference this; they do not define their own.

```json
{
  "timestamp": "2026-10-05T10:15:30Z",
  "status": 404,
  "error": "Not Found",
  "message": "User 42 does not exist",
  "path": "/api/v1/users/42",
  "traceId": "..."
}
```

- Messages are safe for clients: no stack traces, SQL, or internal details.
- Validation failures include per-field messages.

## Pagination, filtering, sorting

- Page with `page` (0-based) and `size`; return total count/elements in the body or
  headers. Enforce a sane maximum `size`.
- Document default and maximum page size per endpoint in the design doc.

## Documentation

- Keep the project `README` endpoint table current for every route change.
- Once the API grows past a handful of endpoints, generate the spec with
  springdoc-openapi and annotate controllers/DTOs so the generated docs are useful.
