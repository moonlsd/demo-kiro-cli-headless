// =============================================================================
// Example Jenkins pipeline for the kiro-cli headless SDLC.
//
// This is a REFERENCE/EXAMPLE, not a drop-in. It mirrors the GitHub Actions
// pipeline (.github/workflows/*) by reusing the SAME portable scripts under
// .github/scripts/. Only the CI-host glue differs: triggers, credentials, and
// the pull-request API calls.
//
// Everything marked  >>> PLACEHOLDER <<<  must be implemented for your Git host
// (GitHub, Bitbucket, GitLab, ...) and your Jenkins setup. See docs/jenkins.md.
//
// Two jobs are modeled here as one parameterized pipeline, selected by STAGE:
//   STAGE=start      -> create branch, scaffold docs, open PR        (= start_sdlc)
//   STAGE=spec|design|implement -> run one SDLC stage on the PR branch (= sdlc_run)
// =============================================================================

pipeline {
  agent any

  parameters {
    choice(
      name: 'STAGE',
      choices: ['start', 'spec', 'design', 'implement'],
      description: 'start = scaffold + open PR; spec/design/implement = run that stage on an existing feature branch'
    )
    string(name: 'JIRA_ID',     defaultValue: '', description: 'JIRA-style ID, e.g. KIRODEMO-002 (required for all stages)')
    string(name: 'TITLE',       defaultValue: '', description: 'Short feature title (start stage only)')
    text(  name: 'DESCRIPTION', defaultValue: '', description: 'Requirement description (start stage only)')
  }

  options {
    // One run per feature at a time; mirrors the Actions concurrency group.
    disableConcurrentBuilds()
    timestamps()
  }

  environment {
    // --- Credentials ------------------------------------------------------
    // Create these in Jenkins (Manage Jenkins > Credentials). IDs are examples.
    //   kiro-api-key : Secret text  -> kiro-cli auth           (= secret KIRO_API_KEY)
    //   sdlc-git     : Username/PAT -> git push + PR API       (= secret SDLC_PAT)
    KIRO_API_KEY = credentials('kiro-api-key')

    // Non-interactive installer (hosted agents). On an agent that ships kiro-cli,
    // leave this empty and the install step is skipped.
    KIRO_CLI_INSTALL_CMD = 'curl -fsSL https://cli.kiro.dev/install | bash -s -- --force'

    // Attribution for commits the pipeline makes.
    GIT_AUTHOR_NAME  = 'sdlc-bot'
    GIT_AUTHOR_EMAIL = 'sdlc-bot@example.com'
  }

  stages {

    stage('Validate inputs') {
      steps {
        sh '''
          set -euo pipefail
          echo "${JIRA_ID}" | grep -Eq '^[A-Z][A-Z0-9]+-[0-9]+$' \
            || { echo "JIRA_ID must look like PROJECT-123"; exit 1; }
        '''
      }
    }

    // -------------------------------------------------------------------------
    // Checkout.
    //   start stage: check out the default branch (we branch from it).
    //   other stages: check out the feature/<JIRA_ID> PR branch.
    // >>> PLACEHOLDER: wire this to your SCM. The GitHub Actions pipeline uses
    //     actions/checkout with a PAT so pushes are attributed and can re-trigger.
    //     In Jenkins, use the Git plugin with the 'sdlc-git' credential, OR a
    //     scripted checkout step. Ensure the credential allows PUSH, not just read.
    // -------------------------------------------------------------------------
    stage('Checkout') {
      steps {
        script {
          def branch = (params.STAGE == 'start') ? 'main' : "feature/${params.JIRA_ID}"
          // >>> PLACEHOLDER: replace with your real checkout. Example shape:
          // checkout([$class: 'GitSCM',
          //   branches: [[name: branch]],
          //   userRemoteConfigs: [[url: 'git@github.com:ORG/REPO.git',
          //                        credentialsId: 'sdlc-git']]])
          echo ">>> PLACEHOLDER: checkout '${branch}' using the 'sdlc-git' credential with push rights"
        }
        sh '''
          set -euo pipefail
          git config user.name  "${GIT_AUTHOR_NAME}"
          git config user.email "${GIT_AUTHOR_EMAIL}"
          chmod +x .github/scripts/*.sh
        '''
      }
    }

    // -------------------------------------------------------------------------
    // JDK 21 — only the implement stage builds the project.
    // >>> PLACEHOLDER: provide JDK 21 on the agent. Options:
    //     - tools { jdk 'jdk21' } at pipeline top (configure the JDK in Jenkins), or
    //     - run this whole pipeline on a Docker agent image that has JDK 21, or
    //     - install it here. The Actions pipeline uses actions/setup-java@v4.
    // -------------------------------------------------------------------------
    stage('Set up JDK 21') {
      when { expression { params.STAGE == 'implement' } }
      steps {
        echo ">>> PLACEHOLDER: ensure JDK 21 is on PATH for ./mvnw (configure a Jenkins JDK tool or use a Docker agent)"
      }
    }

    stage('Install kiro-cli') {
      steps {
        sh '''
          set -euo pipefail
          if command -v kiro-cli >/dev/null 2>&1; then
            echo "kiro-cli already present at: $(command -v kiro-cli)"
          elif [ -n "${KIRO_CLI_INSTALL_CMD}" ]; then
            echo "Installing kiro-cli ..."
            bash -c "${KIRO_CLI_INSTALL_CMD}"
          else
            echo "kiro-cli not found and KIRO_CLI_INSTALL_CMD empty"; exit 1
          fi
          # The installer drops the binary in ~/.local/bin; make sure it is found.
          export PATH="$HOME/.local/bin:$PATH"
          command -v kiro-cli >/dev/null 2>&1 || { echo "kiro-cli not on PATH after install"; exit 1; }
          kiro-cli --version 2>/dev/null || true
        '''
      }
    }

    // -------------------------------------------------------------------------
    // START: scaffold the feature docs and open a PR.
    // Reuses scaffold-feature.sh verbatim; only the PR creation is host-specific.
    // -------------------------------------------------------------------------
    stage('Start: scaffold + open PR') {
      when { expression { params.STAGE == 'start' } }
      steps {
        withCredentials([usernamePassword(credentialsId: 'sdlc-git',
                                           usernameVariable: 'GIT_USER',
                                           passwordVariable: 'GIT_TOKEN')]) {
          sh '''
            set -euo pipefail
            export PATH="$HOME/.local/bin:$PATH"
            BRANCH="feature/${JIRA_ID}"

            git checkout -b "${BRANCH}"
            SDLC_OWNER="${GIT_USER}" .github/scripts/scaffold-feature.sh \
              "${JIRA_ID}" "${TITLE}" "${DESCRIPTION}"

            git add ".docs/${JIRA_ID}"
            git commit -m "${JIRA_ID}: scaffold spec docs (DRAFT) [automated]"

            # >>> PLACEHOLDER: push the branch using the token credential.
            #     e.g. git push https://${GIT_USER}:${GIT_TOKEN}@HOST/ORG/REPO.git "${BRANCH}"
            echo ">>> PLACEHOLDER: git push the new branch ${BRANCH}"

            # >>> PLACEHOLDER: open the pull request via your host's API/CLI.
            #     GitHub:    gh pr create --base main --head "${BRANCH}" --title ... --body ...
            #     Bitbucket: POST /2.0/repositories/.../pullrequests  (curl)
            #     GitLab:    glab mr create ...   OR  POST /projects/:id/merge_requests
            echo ">>> PLACEHOLDER: open a PR from ${BRANCH} into main"
          '''
        }
      }
    }

    // -------------------------------------------------------------------------
    // RUN A STAGE: spec / design / implement. Reuses run-stage.sh verbatim —
    // it invokes kiro-cli headless, enforces ./mvnw test for implement, updates
    // doc status, and commits. Only the push + PR comment are host-specific.
    // -------------------------------------------------------------------------
    stage('Run SDLC stage') {
      when { expression { params.STAGE in ['spec', 'design', 'implement'] } }
      steps {
        withCredentials([usernamePassword(credentialsId: 'sdlc-git',
                                           usernameVariable: 'GIT_USER',
                                           passwordVariable: 'GIT_TOKEN')]) {
          sh '''
            set -euo pipefail
            export PATH="$HOME/.local/bin:$PATH"

            # This does the real work (same script as GitHub Actions):
            .github/scripts/run-stage.sh "${STAGE}" "${JIRA_ID}"

            # >>> PLACEHOLDER: push the resulting commit back to the PR branch.
            #     e.g. git push https://${GIT_USER}:${GIT_TOKEN}@HOST/ORG/REPO.git \
            #            HEAD:feature/${JIRA_ID}
            echo ">>> PLACEHOLDER: push commit to feature/${JIRA_ID}"

            # >>> PLACEHOLDER (optional): comment on the PR with the stage result,
            #     mirroring the Actions 'Comment on PR' step.
            echo ">>> PLACEHOLDER: comment on the PR with the ${STAGE} result"
          '''
        }
      }
    }
  }

  post {
    success { echo "SDLC stage '${params.STAGE}' succeeded for ${params.JIRA_ID}" }
    failure { echo "SDLC stage '${params.STAGE}' FAILED for ${params.JIRA_ID} (implement fails here if ./mvnw test did not pass)" }
  }
}

// =============================================================================
// TRIGGERS — how a stage gets kicked off.
//
// GitHub Actions gets "PR opened" and "label added" events for free. Jenkins
// does not, so choose ONE model (see docs/jenkins.md for the trade-offs):
//
//   >>> PLACEHOLDER (triggers): pick and implement one of:
//
//   A. GitHub (or Bitbucket/GitLab) webhook -> Jenkins. Keep the label-driven UX:
//      a PR label event hits a Jenkins webhook that starts this job with the
//      matching STAGE. Highest fidelity to the Actions flow.
//
//   B. Manual "Build with Parameters": the engineer runs this job and picks STAGE.
//      Simplest; drops the label gate. Good enough for a PoC on any Git host.
//
//   C. Poll an SCM marker (label/comment/file) and derive STAGE. Works anywhere,
//      but laggy and clunky — not recommended.
// =============================================================================
