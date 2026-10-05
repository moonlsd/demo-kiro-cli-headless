---
id: KIRODEMO-001
title: Greeting API
type: business
status: IMPLEMENTED
owner: demo-team
created: 2026-10-05
updated: 2026-10-05
approvers: [demo-team]
supersedes: null
---

# Greeting API — Business Purpose & Intent

> Status model and front-matter rules: see
> [`../golden/documentation-lifecycle.md`](../golden/documentation-lifecycle.md).
>
> This is a worked example that documents the greeting endpoints shipped in the
> initial project scaffold. It shows how a feature spec is structured.

## Problem / opportunity

The project needs a minimal, verifiable HTTP surface so that the application
skeleton, build, and deployment pipeline can be exercised end to end before real
features are built. A simple greeting endpoint provides that reference behavior.

## Goals

- Provide a trivial, well-defined endpoint that proves the app serves HTTP and
  returns JSON.
- Give new contributors a concrete example of the layering and documentation
  conventions.

## Non-goals

- No persistence, authentication, or business domain logic.
- Not intended for production end-user value beyond a reference/smoke surface.

## Stakeholders & users

- **Users**: developers and CI/health checks calling the endpoint.
- **Stakeholders**: the engineering team establishing project conventions.

## Success metrics

- The endpoint returns `200` with the expected JSON in local and CI runs.
- A new contributor can trace the feature from this spec to the code and tests.

## Assumptions & constraints

- No external dependencies or data stores are involved.
- Must fit the layered architecture and conventions in `../golden/`.

## Risks & open questions

- None material. This surface is intentionally minimal and may be removed once
  real features exist.

## References

- Code: `src/main/java/com/example/demo/controller/HelloController.java`
- Project `README.md` endpoint table.
