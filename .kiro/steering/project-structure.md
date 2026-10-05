# Project Structure & Architecture

This is a Spring Boot (3.3.x, Java 21) application built with Maven. Follow the
conventions below so the codebase stays predictable and easy to navigate.

## Build & tooling

- Use the Maven wrapper (`./mvnw`) for every build/test command. Never assume a
  globally installed `mvn`.
- Target Java 21. Do not introduce language features or APIs beyond the project's
  Java version.
- Let the `spring-boot-starter-parent` manage dependency versions. Do **not** pin
  explicit versions for anything the parent BOM already governs.
- Add a dependency only when it is actually used. Prefer official Spring Boot
  starters over assembling individual libraries.

## Package layout

Root package: `com.example.demo`. Organize by layer within it:

```
com.example.demo
├── DemoApplication.java        # single @SpringBootApplication entry point
├── config/                     # @Configuration classes, bean definitions
├── controller/                 # @RestController HTTP endpoints (web layer only)
├── service/                    # @Service business logic
├── repository/                 # data access (@Repository / Spring Data interfaces)
├── domain/ (or model/)         # entities and core domain types
├── dto/                        # request/response payloads (never expose entities)
└── exception/                  # custom exceptions + @ControllerAdvice handlers
```

Rules:

- Keep the entry point minimal — no business logic in `DemoApplication`.
- One top-level public type per file; the file name matches the type.
- A new feature's classes live in the layer package that matches their
  responsibility, not in a per-feature folder, unless the team explicitly adopts
  package-by-feature later.

## Layering discipline

- Controllers handle HTTP only: validation, mapping, status codes. They delegate
  to services and never touch repositories directly.
- Services hold business logic and own transaction boundaries (`@Transactional`).
- Repositories handle persistence only.
- Dependencies point inward: `controller -> service -> repository`. Never the
  reverse, and never skip a layer.
- Map between entities and DTOs at the service or controller boundary. Never
  return JPA entities directly from a controller.

## Configuration

- Keep environment config in `src/main/resources/application.properties` (or
  `application.yml`). Use Spring profiles (`application-<profile>.properties`) for
  environment-specific values.
- Never hardcode secrets, URLs, or credentials in code. Externalize via
  properties and environment variables.
- Bind related properties with `@ConfigurationProperties` POJOs rather than
  scattering `@Value` injections.
- Keep the Actuator surface minimal; expose only the endpoints the app needs.

## Dependency injection

- Use **constructor injection** exclusively. Declare collaborator fields `final`.
- Do not use field injection (`@Autowired` on a field) in production code.
- Prefer a single constructor so Spring injects it without `@Autowired`.
