# SDLC Automation (GitHub Actions + kiro-cli)

This directory holds a CI/CD pipeline that drives a documentation-first SDLC with
`kiro-cli` running in **headless** mode. An engineer starts a feature with one
manual trigger; the pipeline scaffolds specs, opens a PR, and runs AI stages on
that PR, with a human review gate between each stage.

> **Status: vertical slice.** The **start** and **spec** stages are implemented and
> runnable. **design** and **implement** are stubbed in the router and documented
> below as the next increments.

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
[sdlc_run: design]    (next increment) kiro-cli writes design.md
        ▼  ◇ MANUAL REVIEW ◇  add label `sdlc:implement`
        ▼
[sdlc_run: implement] (next increment) kiro-cli writes code + runs ./mvnw test
        ▼  ◇ FINAL REVIEW ◇  engineer merges, or checks out & edits
```

## Files

| File | Purpose |
|------|---------|
| `workflows/sdlc-start.yml` | Manual entry point. Creates the branch, scaffolds docs, opens the PR. |
| `workflows/sdlc-run.yml`   | Runs a stage on PR open / label; pushes results back to the PR. |
| `agents/sdlc-spec.json`    | Least-privilege kiro-cli agent for the spec stage (writes only under `.docs/`). |
| `scripts/lib.sh`           | Shared helpers (JIRA ID parsing, doc status read/update, phase detection). |
| `scripts/scaffold-feature.sh` | Copies templates into `.docs/<JIRA_ID>/` and fills front matter. |
| `scripts/run-stage.sh`     | Builds the stage prompt, invokes kiro-cli headless, commits the result. |

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

`GITHUB_TOKEN` is provided automatically by Actions; no setup needed.

### Variables

| Variable | Required | Purpose |
|----------|----------|---------|
| `KIRO_CLI_INSTALL_CMD` | only on hosted runners | Shell command that installs `kiro-cli` on the runner if it is not already present. Not needed on a self-hosted runner that ships kiro-cli. |

### Labels

Create these PR labels (used to advance phases):

- `sdlc:spec` — re-run the spec stage (spec also auto-runs on PR open)
- `sdlc:design` — advance to design *(next increment)*
- `sdlc:implement` — advance to implement *(next increment)*

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

## Security notes

- The spec agent (`agents/sdlc-spec.json`) is restricted to writing under `.docs/`
  and explicitly denied `fs_write` to `src/**`, so a spec run cannot alter code.
- Workflows request only `contents: write` and `pull-requests: write`.
- `kiro-cli` runs with `--no-interactive --trust-all-tools`; the agent's own tool
  allowlist and permission rules are what actually bound its capabilities, so keep
  those tight. The implement stage (next increment) will use a separate agent that
  additionally permits `src/**` writes and running `./mvnw`.
