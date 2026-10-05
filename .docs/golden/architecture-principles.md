---
title: Architecture Principles
status: APPROVED
owner: engineering
created: 2026-10-05
updated: 2026-10-05
---

# Architecture Principles

Baseline principles every feature's design must honor. Enforced in code by the
`.kiro/steering/project-structure.md` conventions; this document is the
specification-level statement of the same intent.

## Layered architecture

- The application is organized by layer under `com.example.demo`:
  `controller → service → repository`, with supporting `config`, `domain`/`model`,
  `dto`, and `exception` packages.
- Dependencies point inward only. Controllers depend on services; services depend
  on repositories. Never the reverse, and never skip a layer (a controller must
  not touch a repository directly).
- Each layer has one job:
  - **Controller**: HTTP concerns — routing, validation, mapping, status codes.
  - **Service**: business logic and transaction boundaries.
  - **Repository**: persistence only.

## Separation of concerns

- Keep HTTP/transport types (DTOs) separate from domain/persistence types
  (entities). Map between them at the boundary; never expose entities over the API.
- A class has a single responsibility. Split classes that accumulate unrelated
  duties.

## Simplicity first

- Prefer the simplest design that meets the requirements. Introduce abstraction,
  caching, async processing, or new infrastructure only when a requirement demands
  it — and justify it in the design doc.
- Reuse existing patterns and components before adding new ones.

## Explicit boundaries & contracts

- Interactions with other features/services go through explicit, documented
  contracts (API/DTO). Avoid reaching into another module's internals.
- Make configuration explicit via `@ConfigurationProperties`; never hardcode
  environment-specific values.

## Statelessness & scalability

- Keep services stateless where possible so the app scales horizontally. Push
  state to the datastore/cache, not to instance memory.

## Testability by design

- Design for testability: constructor injection, small units, and clear seams so
  logic can be unit-tested without a running context.

## Deviations

Any design that departs from these principles must state the deviation and its
justification in the feature's `design.md`, and be approved at the design gate.
