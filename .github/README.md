# SDLC Automation (GitHub Actions + kiro-cli)

This directory holds a CI/CD pipeline that drives a documentation-first SDLC with
`kiro-cli` running in **headless** mode. An engineer starts a feature with one
manual trigger; the pipeline scaffolds specs, opens a PR, and runs AI stages on
that PR, with a human review gate between each stage.

> **Status: all stages implemented.** start, spec, design, and implement are
> runnable. The implement stage builds the project and gates on `./mvnw test`.

## Flow

```
Engineer runs "start_sdlc" (jira_id, title, description)
        │
        ▼
[start_sdlc]  create branch feature/<JIRA_ID>
              scaffold .docs/<JIRA_ID>/ from _templates (status: DRAFT)
              open PR  ──────────────► triggers sdlc_run (spec)
        │
        ▼
[sdlc_run: spec]  kiro-cli drafts business.md + requirements.md
                  sets them to IN_REVIEW, pushes to the PR, comments
        │
        ▼  ◇ MANUAL REVIEW ◇  edit or approve, then add label `sdlc:design`
        │
        ▼
[sdlc_run: design]  kiro-cli writes design.md (reads the approved spec),
                    sets it to IN_REVIEW, pushes to the PR
        │
        ▼  ◇ MANUAL REVIEW ◇  edit or approve, then add label `sdlc:implement`
        │
        ▼
[sdlc_run: implement]  kiro-cli writes code satisfying the docs, then the stage
                       runs ./mvnw test (hard gate). On green, docs -> IMPLEMENTED
                       and the commit (src + docs) is pushed to the PR
        │
        ▼  ◇ FINAL REVIEW ◇  engineer reviews the full diff, merges, or checks
                             out the branch and edits
```

## Files

| File | Purpose |
|------|---------|
| `.github/workflows/sdlc-start.yml` | Manual entry point. Creates the branch, scaffolds docs, opens the PR. |
| `.github/workflows/sdlc-run.yml`   | Runs a stage on PR open / label; pushes results back to the PR. |
| `.kiro/agents/sdlc-spec.json`      | Least-privilege kiro-cli agent for the spec stage (writes only under `.docs/`). Lives under `.kiro/agents/` because that is where kiro-cli discovers workspace agents. |
| `.kiro/agents/sdlc-design.json`    | kiro-cli agent for the design stage. Same `.docs/`-only restriction; writes `design.md`. |
| `.kiro/agents/sdlc-implement.json` | kiro-cli agent for the implement stage. Broader: writes `src/**` and `.docs/**` and may run build/test commands; denied writes to `.github/` and `.kiro/`. |
| `.github/scripts/lib.sh`           | Shared helpers (JIRA ID parsing, doc status read/update, phase detection). |
| `.github/scripts/scaffold-feature.sh` | Copies templates into `.docs/<JIRA_ID>/` and fills front matter. |
| `.github/scripts/run-stage.sh`     | Builds the stage prompt, invokes kiro-cli headless, commits the result. |

## The state machine

The SDLC phase is derived from the `status` front matter of the docs in
`.docs/<JIRA_ID>/` (see `.docs/golden/documentation-lifecycle.md`). `lib.sh`'s
`feature_phase` maps statuses to a phase:

- `requirements.md` DRAFT  → **SPEC**
- `requirements.md` ready, `design.md` DRAFT → **DESIGN**
- both ready → **IMPLEMENT**

Advancement between phases is a deliberate human action: add the matching label to
the PR. This is the manual gate.

## Required configuration

Set these in **Settings → Secrets and variables → Actions** before running:

### Secrets

| Secret | Required | Purpose |
|--------|----------|---------|
| `KIRO_API_KEY` | yes | Credential kiro-cli uses to authenticate on the runner. The workflow reads it from this repository secret and exposes it as the `KIRO_API_KEY` env var during the stage run. |
| `SDLC_PAT` | yes | A Personal Access Token (or GitHub App token) used for checkout, push, and PR creation. Required because the default `GITHUB_TOKEN` (a) is not permitted to create pull requests, and (b) its pushes/PRs do not trigger other workflows — so the spec stage would never fire. Needs `contents: write` and `pull-requests: write` scope on this repo. |

`GITHUB_TOKEN` is provided automatically by Actions and is used only for posting the
PR status comment; no setup needed for it.

### Variables

| Variable | Required | Purpose |
|----------|----------|---------|
| `KIRO_CLI_INSTALL_CMD` | only on hosted runners | Shell command that installs `kiro-cli` on the runner if it is not already present. Not needed on a self-hosted runner that ships kiro-cli. Use the fully non-interactive form: `curl -fsSL https://cli.kiro.dev/install \| bash -s -- --force`. The official installer accepts only `--force`, `--channel`, and `--help` — there is no `--no-confirm`; `--force` is what makes it non-interactive. It installs to `~/.local/bin`, which the workflow adds to `PATH`. |

### Labels

Create these PR labels (used to advance phases):

- `sdlc:spec` — re-run the spec stage (spec also auto-runs on PR open)
- `sdlc:design` — advance to design
- `sdlc:implement` — advance to implement (builds the project and runs `./mvnw test`)

## Running it

1. Push these files to the default branch so the workflows are registered.
2. Actions tab → **start_sdlc** → **Run workflow**, supply:
   - `jira_id`: e.g. `KIRODEMO-002`
   - `title`: short feature title
   - `description`: the requirement in a sentence or two
3. A branch `feature/KIRODEMO-002` and a PR are created; the spec stage runs
   automatically and pushes drafted `business.md` + `requirements.md` to the PR.
4. Review the PR. Edit inline or check out the branch and push changes. When happy,
   add the next `sdlc:*` label to advance (design/implement land in the next
   increment).

## Runner requirements

The stage job needs `kiro-cli` available and authenticated. Two options:

- **Self-hosted runner** with `kiro-cli` pre-installed (simplest; set `runs-on` to
  your runner label and skip `KIRO_CLI_INSTALL_CMD`).
- **GitHub-hosted runner** plus a `KIRO_CLI_INSTALL_CMD` variable that installs the
  CLI at job start.

## Runner requirements (implement stage)

The implement stage builds the project, so its job sets up **JDK 21** (Temurin,
with Maven caching) via `actions/setup-java`. This step is conditional and only
runs for the implement stage. The spec and design stages need no JDK.

## Security notes

- The spec and design agents (`sdlc-spec`, `sdlc-design`) are restricted to writing
  under `.docs/` and explicitly denied `fs_write` to `src/**`, so they cannot alter
  code.
- The implement agent (`sdlc-implement`) is intentionally broader: it writes
  `src/**` and `.docs/**` and may run build/test commands. It is denied writes to
  `.github/` and `.kiro/` so a run cannot rewrite the CI pipeline or its own agent
  definitions. For this demo the elevated scope is accepted because work lands only
  on a feature branch and nothing merges without human review.
- The implement stage enforces `./mvnw test` in the workflow (not just in the agent
  prompt): the stage fails and the docs are not advanced to `IMPLEMENTED` unless the
  build is green.
- Workflows request only `contents: write` and `pull-requests: write`.
- `kiro-cli` runs with `--no-interactive --trust-all-tools`; the agent's own tool
  allowlist and permission rules are what actually bound its capabilities, so keep
  those tight.
