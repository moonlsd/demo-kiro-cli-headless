---
id: KIRODEMO-001
title: Greeting API
type: design
status: IMPLEMENTED
owner: demo-team
created: 2026-10-05
updated: 2026-10-05
approvers: [tech-lead]
supersedes: null
---

# Greeting API — Detailed Design

> Depends on the approved `requirements.md`. Complies with everything under
> [`../golden/`](../golden/). Status model:
> [`../golden/documentation-lifecycle.md`](../golden/documentation-lifecycle.md).

## Overview

A single `@RestController` serves the two greeting endpoints; the health endpoint
is provided by Spring Boot Actuator. No service or repository layer is needed
because there is no business logic or state — the controller returns a computed
map directly. This is a deliberate, documented simplification for a reference
surface (see Deviations).

## Architecture & components

- **Controller**: `com.example.demo.controller.HelloController` — maps `GET /` and
  `GET /hello`, returns a JSON object.
- **Actuator**: supplies `GET /actuator/health` via configuration in
  `application.properties`.

```
Client ──HTTP──> HelloController ──returns Map<String,String>──> JSON response
Client ──HTTP──> Actuator /health
```

## API design

Follows `../golden/api-standards.md` (JSON, camelCase, meaningful status codes).

| Method | Path                 | Request        | Response                        | Status |
|--------|----------------------|----------------|---------------------------------|--------|
| GET    | `/`                  | —              | `{ "message": "..." }`          | 200    |
| GET    | `/hello`             | `name` (query, default `World`) | `{ "message": "Hello, <name>!" }` | 200 |
| GET    | `/actuator/health`   | —              | `{ "status": "UP" }`            | 200    |

No request bodies, so no Bean Validation is required here. Errors use the platform
default; a central `@RestControllerAdvice` is unnecessary until endpoints can fail
meaningfully.

## Data model & persistence

None.

## Key design decisions

| Decision                      | Options considered                     | Choice & rationale                                                        |
|-------------------------------|----------------------------------------|---------------------------------------------------------------------------|
| Skip service/repository layers| Full layering vs. controller-only      | Controller-only: no logic/state to justify extra layers for a demo surface |
| Return `Map` vs. a DTO record | `Map<String,String>` vs. `record`      | `Map` for a one-field demo; real features use DTO `record`s per standards  |
| No API version prefix         | `/api/v1/...` vs. bare paths           | Bare paths acceptable for a throwaway reference surface                     |

## Error handling

Not applicable — these endpoints cannot fail under normal operation. Real features
must use the centralized error handling described in the coding standards.

## Security

Endpoints are intentionally public and return no sensitive data. Actuator exposure
is limited to `health` and `info` in `application.properties`.

## Observability

Health is exposed via Actuator and wired into the container `HEALTHCHECK` in the
`Dockerfile`. No custom metrics for this surface.

## Testing strategy

- Web slice tests with `@WebMvcTest(HelloController.class)` + `MockMvc` cover FR-1
  and FR-2 (see `HelloControllerTests`).
- A `@SpringBootTest` context-load smoke test (`DemoApplicationTests`) verifies
  wiring.

## Rollout & operations

No migrations or feature flags. Config: `server.port` (default `8080`) and the
Actuator exposure keys in `application.properties`. Rollback is a redeploy of the
previous image.

## Risks & trade-offs

- The controller-only shortcut diverges from the standard layering; acceptable
  only because there is no logic/state. Documented so it is not copied blindly.

## Deviations from golden standards

- **Layering** (`architecture-principles.md`): no service/repository layer —
  justified above.
- **DTO records** (`api-standards.md`): returns a `Map` rather than a DTO `record`
  — justified for a single-field demo response. Future features must use records.
- **API versioning** (`api-standards.md`): no `/api/v1` prefix — acceptable for a
  reference surface only.
