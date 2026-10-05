---
title: Security Standards
status: APPROVED
owner: engineering
created: 2026-10-05
updated: 2026-10-05
---

# Security Standards

Baseline security requirements that apply to every feature. A feature's spec
confirms compliance and documents any feature-specific controls.

## Authentication & authorization

- Protect every non-public endpoint. Authenticate the caller and authorize the
  action; default to deny.
- Enforce authorization at the service boundary, not only in the UI or controller.
- Apply least privilege to users, roles, and service credentials.

## Input handling

- Treat all input as untrusted. Validate and constrain every request
  (Bean Validation) at the boundary.
- Use parameterized queries / the ORM for all data access — never build queries by
  string concatenation (prevents injection).
- Encode/escape output appropriately for its sink to prevent injection/XSS.

## Secrets & configuration

- Never hardcode secrets, credentials, tokens, or keys in source, config files, or
  logs. Inject them via environment variables / a secrets manager.
- Keep secrets out of the repository and out of error messages and traces.

## Data protection

- Use TLS for all external traffic. Encrypt sensitive data at rest.
- Follow the data classification in `data-standards.md`; never log PII or secrets.
- Return the minimum data necessary; do not expose internal identifiers or fields
  that aren't needed.

## Error handling & disclosure

- Error responses are generic and safe (see `api-standards.md`). Never leak stack
  traces, SQL, server internals, or whether a specific account exists.
- Log security-relevant events (authn/authz failures, privilege changes) at an
  appropriate level — without sensitive values.

## Dependencies

- Prefer well-maintained, official libraries. Pin via the Spring Boot BOM where
  possible.
- Keep dependencies current; address known-vulnerable versions promptly. Scan
  dependencies as part of the build/CI where available.

## Operational safeguards

- Expose only the Actuator endpoints the app needs, and protect sensitive ones.
- Apply sensible rate limits / timeouts on public endpoints to resist abuse.

## Feature responsibilities

Each feature's `requirements.md` lists its security NFRs, and its `design.md`
describes how authn/authz, validation, and data protection are implemented for that
feature. Any deviation from this baseline must be justified and approved at the
design gate.
