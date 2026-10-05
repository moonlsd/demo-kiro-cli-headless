# Documentation Standards

Documentation is part of "done." Keep it accurate, close to the code, and useful
to the next developer.

## Guiding principle

- Document the **why**, not the **what**. Code shows what it does; comments and
  docs explain intent, trade-offs, and constraints that are not obvious from the
  code.
- Keep docs next to the thing they describe and update them in the same change
  that alters behavior. Stale docs are worse than none.

## Code-level documentation

- Write Javadoc on all public types and public methods whose contract is not
  self-evident. Describe purpose, parameters, return value, and thrown
  exceptions. Skip Javadoc that merely restates the signature.
- Document non-obvious logic, business rules, and workarounds with a short inline
  comment that explains the reasoning. Link to an issue/ticket for workarounds.
- Prefer self-documenting code (clear names, small methods) over comments that
  compensate for unclear code.
- Do not leave commented-out code. Delete it; version control is the history.
- Keep `TODO`/`FIXME` actionable and attributed (what, and ideally a ticket
  reference). Don't let them accumulate.

## API documentation

- Keep the README's endpoint table current whenever routes change: method, path,
  purpose, and auth requirement.
- When the API grows beyond a handful of endpoints, add springdoc-openapi so the
  spec and Swagger UI are generated from the code, and annotate controllers/DTOs
  so the generated docs are meaningful.
- Document the error-response shape once, centrally, and reference it rather than
  repeating it per endpoint.

## Project documentation (README)

Keep the README the single source of truth for getting started. It must always
let a new developer go from clone to running app. Maintain these sections:

- Project overview and tech stack (with versions).
- Prerequisites.
- How to build, run, and test (using `./mvnw`).
- Project structure overview.
- Endpoint reference.
- How to run via Docker.
- Configuration/environment variables and their defaults.

When you add a dependency, feature, endpoint, config key, or run step, update the
README in the same change.

## Commit & change hygiene

- Write commit messages that explain intent: a concise summary line plus a body
  describing *why* when the change is non-trivial.
- Keep commits focused; one logical change per commit makes history reviewable.
- Record notable architectural decisions briefly (in the README or a short
  `docs/decisions/` note) so future readers understand why a path was chosen.
