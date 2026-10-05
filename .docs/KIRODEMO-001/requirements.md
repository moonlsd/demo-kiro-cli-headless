---
id: KIRODEMO-001
title: Greeting API
type: requirements
status: IMPLEMENTED
owner: demo-team
created: 2026-10-05
updated: 2026-10-05
approvers: [demo-team, tech-lead]
supersedes: null
---

# Greeting API — Requirements

> Depends on the approved `business.md`. Status model: see
> [`../golden/documentation-lifecycle.md`](../golden/documentation-lifecycle.md).

## Scope

Expose two read-only HTTP endpoints that return a JSON greeting, plus the standard
health endpoint, to serve as a reference/smoke surface.

## Functional requirements

### FR-1 — Welcome message (MUST)

As a caller, I want a root endpoint that returns a welcome message so that I can
confirm the service is up and serving JSON.

**Acceptance criteria**

- GIVEN the app is running WHEN I `GET /` THEN I receive `200` with body
  `{"message":"Welcome to the demo Spring Boot application"}`.

### FR-2 — Personalized greeting (MUST)

As a caller, I want a greeting endpoint that optionally takes a name so that I can
verify request parameter handling.

**Acceptance criteria**

- GIVEN no `name` WHEN I `GET /hello` THEN I receive `200` with
  `{"message":"Hello, World!"}`.
- GIVEN `name=Kiro` WHEN I `GET /hello?name=Kiro` THEN I receive `200` with
  `{"message":"Hello, Kiro!"}`.

### FR-3 — Health endpoint (MUST)

As an operator/health check, I want a health endpoint so that liveness can be
monitored.

**Acceptance criteria**

- GIVEN the app is running WHEN I `GET /actuator/health` THEN I receive `200` with
  status `UP`.

## Non-functional requirements

| ID    | Category        | Requirement                                                   |
|-------|-----------------|---------------------------------------------------------------|
| NFR-1 | Performance     | Responds in < 50 ms locally (no I/O involved)                 |
| NFR-2 | Security        | Endpoints are public by design; no sensitive data returned    |
| NFR-3 | Observability   | Health exposed via Actuator per project configuration         |
| NFR-4 | Maintainability | Complies with coding/testing steering; covered by tests       |
| NFR-5 | API conventions | JSON responses per `../golden/api-standards.md`               |

## Data requirements

None. No data is read, written, or retained.

## Dependencies & integrations

None beyond Spring Boot Web and Actuator starters.

## Out of scope

Authentication, persistence, write operations, versioned API prefix (acceptable
for this reference surface only).

## Traceability

| Requirement | Design section          | Verified by (test)                                        |
|-------------|-------------------------|-----------------------------------------------------------|
| FR-1        | design.md § API design  | `HelloControllerTests#indexReturnsWelcomeMessage`         |
| FR-2        | design.md § API design  | `HelloControllerTests#helloReturnsGreetingWithDefaultName`, `#helloReturnsGreetingWithProvidedName` |
| FR-3        | design.md § API design  | manual / Actuator healthcheck in Dockerfile               |
