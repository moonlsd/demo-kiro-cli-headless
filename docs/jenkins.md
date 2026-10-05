# Running the SDLC pipeline on Jenkins

This is an example of porting the kiro-cli headless SDLC from GitHub Actions to
Jenkins. The example pipeline is [`../Jenkinsfile`](../Jenkinsfile).

The important idea: **the real work is in portable bash under `.github/scripts/`,
not in CI YAML.** Jenkins reuses those scripts unchanged. Only the CI-host glue
differs — triggers, credentials, checkout, and the pull-request API calls. Every
such spot is marked `>>> PLACEHOLDER <<<` in the `Jenkinsfile`; this doc explains
how to implement each.

> Status: reference example. The `Jenkinsfile` runs its own logic (validate,
> install kiro-cli, scaffold, run the stage) but the SCM push and PR operations are
> left as placeholders because they depend on your Git host. Fill them in before
> expecting a working PR flow.

## How it maps to the GitHub Actions pipeline

| GitHub Actions | Jenkins equivalent |
|----------------|--------------------|
| `start_sdlc` workflow (`workflow_dispatch`) | This pipeline run with `STAGE=start` |
| `sdlc_run` workflow (PR open / label) | This pipeline run with `STAGE=spec\|design\|implement` |
| `secrets.KIRO_API_KEY` | Jenkins credential `kiro-api-key` (Secret text) |
| `secrets.SDLC_PAT` | Jenkins credential `sdlc-git` (Username + PAT) |
| `vars.KIRO_CLI_INSTALL_CMD` | `KIRO_CLI_INSTALL_CMD` env in the `Jenkinsfile` |
| `actions/setup-java@v4` (implement only) | Jenkins JDK tool / Docker agent (placeholder) |
| `gh pr create` / `gh pr comment` | Host API/CLI (placeholder) |
| `pull_request: [opened, labeled]` triggers | A trigger model you choose (see below) |

The scripts themselves (`lib.sh`, `scaffold-feature.sh`, `run-stage.sh`) are
identical in both pipelines, including the implement-stage `./mvnw test` gate and
the deterministic doc-status transitions.

## Prerequisites

- A Jenkins instance with the **Pipeline** and **Git** plugins.
- An agent that can run `bash`, `git`, `curl`, and (for the implement stage)
  **JDK 21** so `./mvnw` works.
- Network access for the agent to install/run `kiro-cli` and reach your Git host.

## 1. Credentials

Create these under **Manage Jenkins → Credentials** (IDs match the `Jenkinsfile`):

| Credential ID | Kind | Holds | Used for |
|---------------|------|-------|----------|
| `kiro-api-key` | Secret text | Kiro API key | `kiro-cli` auth (exposed as `KIRO_API_KEY`) |
| `sdlc-git` | Username with password | Git username + PAT | `git push` and the PR API; must allow **write/push** |

The `sdlc-git` token needs the same scope the GitHub `SDLC_PAT` needed: push to the
repo and create/comment on pull requests. On GitHub, remember the default build
token cannot create PRs and its pushes do not trigger other automation — use a real
PAT (or App token), exactly as the Actions pipeline does.

## 2. Create the job

Create a **Pipeline** job pointing at this repo's `Jenkinsfile` (Pipeline script
from SCM). The job is parameterized by the `Jenkinsfile`:

- `STAGE` — `start`, `spec`, `design`, or `implement`
- `JIRA_ID` — e.g. `KIRODEMO-002` (all stages)
- `TITLE`, `DESCRIPTION` — used by `start` only

## 3. Implement the placeholders

### a. Checkout (`>>> PLACEHOLDER: checkout ...`)

Replace the `echo` with a real `checkout`. For `start`, check out the default branch
(the pipeline creates `feature/<JIRA_ID>` from it); for the other stages, check out
`feature/<JIRA_ID>`. Use the `sdlc-git` credential and ensure it allows **push**,
not just read. Example shape:

```groovy
checkout([$class: 'GitSCM',
  branches: [[name: branch]],
  userRemoteConfigs: [[url: 'git@github.com:ORG/REPO.git', credentialsId: 'sdlc-git']]])
```

### b. JDK 21 (`>>> PLACEHOLDER: ensure JDK 21 ...`, implement only)

Provide JDK 21 on the agent in whichever way suits your Jenkins:

- Configure a JDK tool and add `tools { jdk 'jdk21' }` to the pipeline, or
- Run on a Docker agent image that already has JDK 21, or
- Install it in that step.

Only the implement stage builds the project, so this is gated with
`when { expression { params.STAGE == 'implement' } }`.

### c. Git push (`>>> PLACEHOLDER: git push ...`)

After the scaffold (start) or the stage commit (spec/design/implement), push to the
branch using the token credential:

```bash
git push https://${GIT_USER}:${GIT_TOKEN}@HOST/ORG/REPO.git HEAD:feature/${JIRA_ID}
```

(For `start`, push the new `feature/${JIRA_ID}` branch.)

### d. Open the PR (`>>> PLACEHOLDER: open a PR ...`, start only)

Create the pull request with your host's CLI or API. Examples:

- **GitHub:** `gh pr create --base main --head "feature/${JIRA_ID}" --title "..." --body "..."`
- **GitLab:** `glab mr create ...` or `POST /projects/:id/merge_requests`
- **Bitbucket:** `POST /2.0/repositories/.../pullrequests` via `curl`

Build the PR body however you like; the Actions version includes a stage checklist.

### e. PR comment (`>>> PLACEHOLDER: comment on the PR ...`, optional)

Mirror the Actions "Comment on PR" step so each stage leaves a note telling the
reviewer what to do next (add `sdlc:design`, add `sdlc:implement`, or merge). Use
the same host CLI/API as (d).

### f. Triggers (`>>> PLACEHOLDER (triggers)`)

GitHub Actions gets "PR opened" and "label added" events for free. Jenkins has no
native PR-label concept, so pick one model:

- **A. Webhook from the Git host → Jenkins (highest fidelity).** Keep the
  label-driven UX: a PR label event hits a Jenkins webhook (GitHub plugin or a
  generic webhook trigger) that starts this job with the matching `STAGE`. Best if
  you stay on GitHub.
- **B. Manual "Build with Parameters" (simplest).** The engineer runs the job and
  selects `STAGE`. Drops the automatic label gate but is perfectly fine for a PoC on
  any Git host.
- **C. Poll an SCM marker.** Jenkins polls for a label/comment/file and derives the
  stage. Works anywhere but is laggy; not recommended.

For a demo, **B** gets you running fastest; **A** reproduces the GitHub experience.

## 4. Run it

With trigger model B (manual):

1. **Build with Parameters** → `STAGE=start`, fill `JIRA_ID`, `TITLE`,
   `DESCRIPTION`. This scaffolds the docs, pushes the branch, and opens the PR.
2. Review the PR. Run again with `STAGE=spec` if the spec did not auto-run (manual
   model has no auto-run on PR open).
3. Run `STAGE=design`, review, then `STAGE=implement`. The implement run builds and
   runs `./mvnw test`; it fails the build if tests fail and only then advances the
   docs to `IMPLEMENTED`.
4. Review the full diff on the PR and merge.

## What is intentionally NOT changed

- `.github/scripts/*` — reused verbatim. If you later want a cleaner split, the one
  worthwhile refactor is to extract the host-specific PR calls (currently `gh ...`
  in the Actions workflow) behind a small `scm-ops.sh` so both CIs share everything
  except that wrapper. Not required for this example.
- `.kiro/agents/*` and `.docs/**` — unchanged; the agents and specs are CI-agnostic.
