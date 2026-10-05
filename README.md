# Demo

A simple Spring Boot application following standard conventions and best practices.

## Tech stack

- Java 21
- Spring Boot 3.3.5 (Web, Actuator, Validation)
- Maven (with wrapper)
- JUnit 5

## Prerequisites

- JDK 21+
- Docker (optional, for containerized runs)

Maven is not required locally; use the bundled wrapper (`./mvnw`).

## Project structure

```
.
├── src
│   ├── main
│   │   ├── java/com/example/demo
│   │   │   ├── DemoApplication.java       # Application entry point
│   │   │   └── controller/HelloController.java
│   │   └── resources
│   │       └── application.properties
│   └── test
│       └── java/com/example/demo         # Unit and context tests
├── Dockerfile
├── .dockerignore
├── .gitignore
└── pom.xml
```

## Running locally

```bash
./mvnw spring-boot:run
```

The app starts on http://localhost:8080.

## Building

```bash
./mvnw clean package
```

The runnable JAR is produced in `target/`.

## Running tests

```bash
./mvnw test
```

## Endpoints

| Method | Path                 | Description                     |
|--------|----------------------|---------------------------------|
| GET    | `/`                  | Welcome message                 |
| GET    | `/hello?name=<name>` | Greeting (defaults to `World`)  |
| GET    | `/actuator/health`   | Health check                    |

Examples:

```bash
curl http://localhost:8080/
curl "http://localhost:8080/hello?name=Kiro"
curl http://localhost:8080/actuator/health
```

## Docker

Build the image:

```bash
docker build -t demo:latest .
```

Run the container:

```bash
docker run --rm -p 8080:8080 demo:latest
```
## Feature specifications (`.docs/`)

Features are specified before they are built. Each feature has a directory named
after its tracker ID (e.g. `.docs/KIRODEMO-001/`) containing `business.md`,
`requirements.md`, and `design.md`, each carrying a lifecycle `status`. Shared
standards that every feature must follow live in `.docs/golden/`. See
[`.docs/README.md`](.docs/README.md) for the full workflow and templates.

## SDLC automation (CI)

An optional GitHub Actions pipeline drives this documentation-first SDLC with
`kiro-cli` in headless mode: an engineer triggers `start_sdlc` with a JIRA ID and
description, the pipeline opens a PR with drafted specs, and AI stages run on the PR
with a human review gate between each. See [`.github/README.md`](.github/README.md)
for setup (required secrets/labels) and usage.
