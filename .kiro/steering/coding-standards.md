# Coding Standards

Conventions for writing clear, maintainable Java in this Spring Boot project.

## General style

- Follow standard Java conventions: `PascalCase` for types, `camelCase` for
  methods/fields, `UPPER_SNAKE_CASE` for constants.
- Keep methods short and single-purpose. If a method needs a comment to explain a
  block, consider extracting that block into a well-named method.
- Prefer immutability: `final` fields, `record` types for DTOs and value objects,
  unmodifiable collections where practical.
- Avoid `null` in APIs. Return `Optional<T>` for "may be absent" lookups; use
  empty collections instead of `null`.
- Keep classes focused (single responsibility). Split a class that accumulates
  unrelated responsibilities.

## Spring-specific practices

- Use the narrowest stereotype that fits: `@RestController`, `@Service`,
  `@Repository`, `@Configuration`.
- Constructor injection only (see project-structure steering). No field injection.
- Scope `@Transactional` to the service layer. Mark read paths
  `@Transactional(readOnly = true)`.
- Prefer Spring Data derived queries; use `@Query` only when a derived method is
  impractical.

## Web layer

- Use DTOs (preferably `record`s) for request and response bodies. Never accept or
  return JPA entities over HTTP.
- Validate request bodies with Jakarta Bean Validation (`@Valid`, `@NotNull`,
  `@Size`, etc.). The project already includes `spring-boot-starter-validation`.
- Return meaningful HTTP status codes. Use `ResponseEntity` when you need control
  over status/headers; otherwise rely on `@ResponseStatus`.
- Keep URL paths noun-based and plural for collections (e.g. `/api/v1/users`,
  `/api/v1/users/{id}`). Version the API when breaking changes are expected.

## Error handling

- Centralize exception handling in a `@RestControllerAdvice` class; do not scatter
  `try/catch` across controllers.
- Throw specific, meaningful exceptions. Translate them to a consistent error
  response shape (e.g. a problem-detail body with timestamp, status, message).
- Never swallow exceptions silently or log-and-continue in a way that hides
  failures. Never expose stack traces or internal details in HTTP responses.

## Logging

- Use SLF4J (`org.slf4j.Logger`) via
  `LoggerFactory.getLogger(ClassName.class)`. Do not use `System.out`/`err`.
- Log at appropriate levels: `ERROR` for failures needing attention, `WARN` for
  recoverable anomalies, `INFO` for lifecycle/business milestones, `DEBUG` for
  diagnostics.
- Use parameterized logging (`log.info("user {} created", id)`), not string
  concatenation. Never log secrets, credentials, tokens, or PII.

## Inclusive language

- Use inclusive terminology in code, comments, and docs: `allowlist`/`denylist`
  (not whitelist/blacklist), `primary`/`replica` (not master/slave).

## Before considering a change done

- The code compiles and `./mvnw test` passes.
- New/changed behavior is covered by tests (see testing steering).
- Public types and non-obvious logic are documented (see documentation steering).
- No unused imports, dead code, or commented-out blocks are left behind.
