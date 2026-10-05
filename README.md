# demo-kiro-cli-headless

A proof-of-concept that demonstrates a **documentation-first, AI-assisted SDLC**
driven from CI. An engineer kicks off a feature with a single trigger; a GitHub
Actions pipeline then runs [`kiro-cli`](https://cli.kiro.dev) in **headless mode**
to draft the specification, draft the design, and finally implement the code — each
stage landing on the same pull request with a human review gate in between.

The Spring Boot application in this repo is deliberately small. It is the *subject*
the pipeline operates on, not the point of the project. The point is the pipeline:
how an AI agent can be wired into CI to carry a feature from a one-line request
through spec, design, and a tested implementation, while a human stays in control at
every step.

This README is the single source of truth for the PoC — overview, pipeline design,
setup, and how to author feature specs are all below.

## Contents

- [What this demonstrates](#what-this-demonstrates)
- [How the pipeline works](#how-the-pipeline-works)
- [Repository structure](#repository-structure)
- [Setup](#setup)
- [Running the pipeline](#running-the-pipeline)
- [Pipeline internals](#pipeline-internals)
- [Feature specifications (`.docs/`)](#feature-specifications-docs)
- [The sample application](#the-sample-application)

## What this demonstrates

- Running `kiro-cli` non-interactively in GitHub Actions, authenticated from a
  repository secret.
- A spec → design → implement workflow where each phase is a separate, least-
  privilege agent, and progress is gated by human review (PR labels).
- Documentation as the source of truth: every feature is specified under `.docs/`
  before code is written, and the implement stage is gated on a green build.
- Project conventions encoded as Kiro **steering** so both humans and the AI agents
  follow the same rules.

## How the pipeline works

```
Engineer runs "start_sdlc" (jira_id, title, description)
        │
        ▼
[start_sdlc]  create branch feature/<JIRA_ID>
              scaffold .docs/<JIRA_ID>/ from templates (status: DRAFT)
              open a PR  ─────────────────► triggers the spec stage
        │
        ▼
[spec stage]      kiro-cli (agent: sdlc-spec) drafts business.md + requirements.md
                  → IN_REVIEW, pushes to the PR
        │
        ▼  ◇ human review ◇   edit/approve, then add label  sdlc:design
        │
        ▼
[design stage]    kiro-cli (agent: sdlc-design) drafts design.md from the spec
                  → IN_REVIEW, pushes to the PR
        │
        ▼  ◇ human review ◇   edit/approve, then add label  sdlc:implement
        │
        ▼
[implement stage] kiro-cli (agent: sdlc-implement) writes the code,
                  then CI runs ./mvnw test as a HARD GATE.
                  On green: docs → IMPLEMENTED, code + docs pushed to the PR
        │
        ▼  ◇ final review ◇   engineer reviews the full diff, merges,
                              or checks out the branch and edits
```

Two design choices make this work:

- **Phases advance by PR label, not automatically.** Adding `sdlc:design` /
  `sdlc:implement` is the deliberate human "proceed" signal. This is the review gate.
- **Each stage is a scoped agent.** The spec and design agents can only write under
  `.docs/`; only the implement agent can touch `src/**` and run the build. Nothing
  merges without a human approving the PR.

## Repository structure

```
.
├── .docs/                      # Feature specifications (the source of truth)
│   ├── _templates/             #   business / requirements / design templates
│   ├── golden/                 #   standards every feature must follow
│   └── <JIRA-ID>/              #   one dir per feature: business/requirements/design.md
│
├── .github/                    # The CI/CD pipeline
│   ├── workflows/
│   │   ├── sdlc-start.yml       #   "start_sdlc": branch + scaffold docs + open PR
│   │   └── sdlc-run.yml         #   runs a stage on PR open / label; pushes to the PR
│   └── scripts/
│       ├── lib.sh               #   JIRA-ID parsing, doc-status read/update, phase logic
│       ├── scaffold-feature.sh  #   create .docs/<JIRA-ID>/ from templates
│       └── run-stage.sh         #   build the prompt, run kiro-cli headless, commit
│
├── .kiro/                      # Kiro configuration (agents + steering)
│   ├── agents/
│   │   ├── sdlc-spec.json        # spec-stage agent   (writes only .docs/**)
│   │   ├── sdlc-design.json      # design-stage agent (writes only .docs/**)
│   │   └── sdlc-implement.json   # implement agent    (writes src/** + .docs/**, runs build)
│   └── steering/
│       ├── project-structure.md  # architecture & package layout conventions
│       ├── coding-standards.md   # Java / Spring coding rules
│       ├── testing.md            # testing conventions (loads for test files)
│       ├── documentation.md      # documentation expectations
│       └── feature-docs.md       # auto-loads golden + feature docs on JIRA-ID work
│
├── src/                        # The Spring Boot app (subject under automation)
│   ├── main/java/com/example/demo/
│   │   ├── DemoApplication.java
│   │   └── controller/HelloController.java
│   ├── main/resources/application.properties
│   └── test/java/com/example/demo/
│
├── Dockerfile, .dockerignore   # containerization
├── mvnw, mvnw.cmd, pom.xml      # Maven wrapper + build
└── README.md                   # this file (single source of truth)
```

### The Kiro pieces, explained

| Path | Role |
|------|------|
| `.kiro/agents/*.json` | Headless agent definitions. Each stage uses one, with least-privilege tool/path permissions. `kiro-cli` discovers these because they live under `.kiro/agents/`. |
| `.kiro/steering/*.md` | Always-on (or conditionally-loaded) rules that shape how code and docs are produced. The AI agents and human contributors follow the same conventions. |
| `.docs/golden/` | Cross-cutting standards (architecture, API, data, security, the doc lifecycle) that every feature spec must comply with. |
| `.docs/<JIRA-ID>/` | A feature's `business.md`, `requirements.md`, `design.md`, each with a lifecycle `status`. |

## Setup

### 1. Prerequisites

- A GitHub repository (this one) with Actions enabled.
- `kiro-cli` available to the runner (installed at job start on a hosted runner, or
  pre-installed on a self-hosted runner).
- JDK 21 — only the implement stage needs it, and the workflow installs it on demand.
- For local app development: JDK 21+. Maven is not required (use `./mvnw`).

### 2. Repository secrets and variables

Under **Settings → Secrets and variables → Actions**:

| Kind | Name | Required | Purpose |
|------|------|----------|---------|
| Secret | `KIRO_API_KEY` | yes | Credential `kiro-cli` uses to authenticate on the runner. Exposed as the `KIRO_API_KEY` env var during the stage run. |
| Secret | `SDLC_PAT` | yes | A Personal Access Token (or GitHub App token) used for checkout, push, and PR creation. Required because the default `GITHUB_TOKEN` (a) is not permitted to create pull requests, and (b) its pushes/PRs do not trigger other workflows — so the spec stage would never fire. Needs `contents: write` and `pull-requests: write` scope on this repo. |
| Variable | `KIRO_CLI_INSTALL_CMD` | hosted runners only | Command that installs `kiro-cli` if it is not already present. Non-interactive form: `curl -fsSL https://cli.kiro.dev/install \| bash -s -- --force`. The official installer accepts only `--force`, `--channel`, and `--help` (there is no `--no-confirm`; `--force` is what makes it non-interactive). It installs to `~/.local/bin`, which the workflow adds to `PATH`. Not needed on a self-hosted runner that ships kiro-cli. |

`GITHUB_TOKEN` is provided automatically by Actions and is used only for posting the
PR status comment; no setup needed for it.

### 3. PR labels

Create three labels used to advance phases:

- `sdlc:spec` — (re)run the spec stage (spec also auto-runs when the PR opens)
- `sdlc:design` — advance to design
- `sdlc:implement` — advance to implement (builds the project and runs `./mvnw test`)

### 4. Runner requirements

The stage job needs `kiro-cli` available and authenticated. Two options:

- **Self-hosted runner** with `kiro-cli` pre-installed (set `runs-on` to your
  runner label and skip `KIRO_CLI_INSTALL_CMD`).
- **GitHub-hosted runner** plus a `KIRO_CLI_INSTALL_CMD` variable that installs the
  CLI at job start.

The implement stage additionally sets up **JDK 21** (Temurin, with Maven caching)
via `actions/setup-java`. That step is conditional and runs only for the implement
stage; spec and design need no JDK.

## Running the pipeline

1. **Actions → start_sdlc → Run workflow.** Provide a JIRA-style `jira_id`
   (e.g. `KIRODEMO-002`), a `title`, and a one-line `description`.
2. The pipeline creates `feature/<JIRA_ID>`, scaffolds the spec docs, and opens a PR.
   The **spec** stage runs automatically and pushes drafted `business.md` +
   `requirements.md`.
3. Review on the PR. When satisfied, add the **`sdlc:design`** label → design stage
   drafts `design.md`.
4. Review again, then add **`sdlc:implement`** → implement stage writes code, runs
   the tests, and pushes code + docs if the build is green.
5. Review the full diff and merge — or check out the branch, edit, and push.

To re-run the *same* stage, remove and re-add its label (adding a label that is
already present does not re-fire the trigger). Moving to a new stage is just a fresh
label, so no removal is needed.

## Pipeline internals

### Files

| File | Purpose |
|------|---------|
| `.github/workflows/sdlc-start.yml` | Manual entry point. Creates the branch, scaffolds docs, opens the PR. |
| `.github/workflows/sdlc-run.yml`   | Runs a stage on PR open / label; pushes results back to the PR. |
| `.github/scripts/lib.sh`           | Shared helpers (JIRA-ID parsing, doc status read/update, phase detection). |
| `.github/scripts/scaffold-feature.sh` | Copies templates into `.docs/<JIRA-ID>/` and fills front matter. |
| `.github/scripts/run-stage.sh`     | Builds the stage prompt, invokes kiro-cli headless, commits the result. |
| `.kiro/agents/sdlc-spec.json`      | Spec agent — writes only under `.docs/`. |
| `.kiro/agents/sdlc-design.json`    | Design agent — writes only under `.docs/` (`design.md`). |
| `.kiro/agents/sdlc-implement.json` | Implement agent — writes `src/**` + `.docs/**`, may run build/test; denied writes to `.github/` and `.kiro/`. |

### Stage routing

- On **PR open**, the router runs the spec stage if the feature's docs are still at
  the spec phase (derived from doc `status`).
- On a **label** event, the stage is chosen directly by the label name
  (`sdlc:spec` → spec, `sdlc:design` → design, `sdlc:implement` → implement). The
  label is the human gate between phases.

### Security notes

- The spec and design agents (`sdlc-spec`, `sdlc-design`) are restricted to writing
  under `.docs/` and explicitly denied `fs_write` to `src/**`, so they cannot alter
  code.
- The implement agent (`sdlc-implement`) is intentionally broader: it writes
  `src/**` and `.docs/**` and may run build/test commands. It is denied writes to
  `.github/` and `.kiro/` so a run cannot rewrite the CI pipeline or its own agent
  definitions. For this PoC the elevated scope is accepted because work lands only on
  a feature branch and nothing merges without human review.
- The implement stage enforces `./mvnw test` in the workflow (not just in the agent
  prompt): the stage fails and the docs are not advanced to `IMPLEMENTED` unless the
  build is green.
- Workflows request only `contents: write` and `pull-requests: write`.
- `kiro-cli` runs with `--no-interactive --trust-all-tools`; the agent's own tool
  allowlist and permission rules are what actually bound its capabilities, so keep
  those tight.

## Feature specifications (`.docs/`)

Every feature is specified under `.docs/` before code is written, and the spec stays
as the durable record of *why* the feature exists and *how* it was built.

### Layout

```
.docs/
├── _templates/               # starting points for a new feature (copy, don't edit in place)
│   ├── business.md
│   ├── requirements.md
│   └── design.md
├── golden/                   # the knowledge base every feature must follow
└── <JIRA-ID>/                # one directory per feature, named after its tracker issue
    ├── business.md           # business purpose & intent — the "why"
    ├── requirements.md       # functional & non-functional requirements — the "what"
    └── design.md             # detailed technical design — the "how"
```

### The golden knowledge base

`.docs/golden/` holds cross-cutting standards that **all** features must comply with
(architecture, API, data, and security standards, plus the shared document
lifecycle). A feature spec does not restate these — it references them and records
only where it deviates, with justification. When a rule applies to every feature, it
belongs in `golden/`, not in an individual spec.

### Document lifecycle & status

Every document carries a `status` in its front matter so anyone can see which phase
the feature is in at a glance:

```
DRAFT → IN_REVIEW → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPLOYED
```

plus the terminal states `ON_HOLD`, `DEPRECATED`, and `REJECTED`. The full model
(transitions and who approves each gate) lives in
[`.docs/golden/documentation-lifecycle.md`](.docs/golden/documentation-lifecycle.md).
The pipeline sets these statuses as it progresses; the human review gate is the PR
label, not a status value.

### Starting a feature manually

The pipeline scaffolds this for you via `start_sdlc`, but to do it by hand:

1. Note the tracker ID (e.g. `KIRODEMO-002`).
2. Create `.docs/KIRODEMO-002/` and copy the three files from `_templates/` into it.
3. Start each document at `DRAFT`; advance the status as the feature progresses.
4. Keep the docs in sync with reality — a doc marked `IMPLEMENTED` must match the
   code that shipped.

See [`.docs/KIRODEMO-001/`](.docs/KIRODEMO-001/) for a worked example.

## The sample application

A minimal Spring Boot 3.3 / Java 21 service used as the automation subject.

```bash
./mvnw spring-boot:run      # run locally on http://localhost:8080
./mvnw clean package        # build the runnable JAR into target/
./mvnw test                 # run the tests
```

| Method | Path                 | Description                    |
|--------|----------------------|--------------------------------|
| GET    | `/`                  | Welcome message                |
| GET    | `/hello?name=<name>` | Greeting (defaults to `World`) |
| GET    | `/actuator/health`   | Health check                   |

```bash
curl http://localhost:8080/
curl "http://localhost:8080/hello?name=Kiro"
```

Run in Docker:

```bash
docker build -t demo:latest .
docker run --rm -p 8080:8080 demo:latest
```
