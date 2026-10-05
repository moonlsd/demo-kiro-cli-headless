---
inclusion: fileMatch
fileMatchPattern: '**/src/test/**/*.java'
---

# Testing Standards

Applies when working with test code. Keep the suite fast, focused, and
trustworthy.

## Framework & tooling

- Use JUnit 5 (Jupiter) and the utilities bundled in
  `spring-boot-starter-test` (AssertJ, Mockito, JSONPath, `MockMvc`).
- Prefer AssertJ assertions (`assertThat(...)`) for readable, fluent checks.
- Run tests with `./mvnw test`. The full suite must pass before a change is done.

## What to test

- Every new feature and every bug fix ships with tests. For a bug fix, add a test
  that fails without the fix and passes with it.
- Cover the meaningful behavior and edge cases (empty, boundary, invalid input,
  error paths), not just the happy path.
- Do not chase a coverage percentage with trivial tests; test behavior that
  matters.

## Test types & scope

- **Unit tests**: default choice for services and plain logic. Mock collaborators
  with Mockito; do not start a Spring context.
- **Web slice tests**: use `@WebMvcTest(TheController.class)` with `MockMvc` to
  test controllers in isolation; mock the service layer with `@MockBean`.
- **Data slice tests**: use `@DataJpaTest` for repositories.
- **Full context tests**: use `@SpringBootTest` sparingly, only when you must
  verify wiring end to end. The one context-load smoke test is enough for most
  apps.
- Keep slice tests the norm; they are far faster than booting the whole context.

## Conventions

- Name test classes `<TypeUnderTest>Tests` and place them in the same package as
  the type under test (mirrored under `src/test/java`).
- Name test methods to describe behavior, e.g.
  `returnsNotFoundWhenUserMissing()`.
- Follow Arrange–Act–Assert structure; keep each test focused on one behavior.
- Keep tests independent and deterministic — no shared mutable state, no reliance
  on execution order, no real network/clock dependencies.
- Externalize test-only config in `src/test/resources` (e.g. an in-memory
  database) rather than mutating production properties.
