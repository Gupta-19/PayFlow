# PayFlow — Session Notes

Last updated: 2026-11-11. Read this at the start of a new session to pick up where we left off.
The full build plan is in [plan.md](plan.md) (13 steps; Java 21 + Spring Boot 4 wallet/payments API).

## Where things stand

- **Current position:** Step 1 is done apart from committing and pushing the generated project. Next is **Step 2** (docker-compose, application.yml, RSA keys).
- **Project folder:** `C:\Users\91971\OneDrive\Desktop\Payment project Java\PayFlow`
  - Spring Boot **4.1.2** project generated from start.spring.io (Maven, Java 21, `com.ketan.payflow`, all 12 dependencies from plan §1.2). `application.properties` is still the generated one; Step 2 replaces it with `application.yml`.
  - Step 1.4 `.gitignore` entries added (`keys/`, `.env`, `.idea/`; `target/` and `*.iml` were already there).
- **Git remote:** `origin` → `https://github.com/Gupta-19/PayFlow.git` (GitHub user `Gupta-19`)

## Setup status (verified 2026-11-11)

All tools work: Java 21.0.13, WSL 2, Docker engine 29.9.3 + Compose v5.7.0, jq 1.8.2. `gh` is signed in as `Gupta-19` and `gh auth setup-git` was run, so `git push` uses gh's token.

## Tools installed or checked

| Tool | State |
|---|---|
| JDK 21 (Temurin 21.0.13) | Installed at `C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot`. User `JAVA_HOME` changed from JDK 25 to JDK 21. Machine `JAVA_HOME` is also 21. JDK 25 is still installed. |
| Git | Upgraded 2.31 → 2.56.0 via winget. Credential helper is now `manager` (current Git Credential Manager, browser sign-in). |
| GitHub CLI (`gh`) | Installed via winget. Signed in as `Gupta-19`. |
| jq | Installed via winget |
| openssl | Not installed separately. Use the copy bundled with Git: `C:\Program Files\Git\usr\bin\openssl.exe` (OpenSSL 3.5.9). Run the plan's `openssl` / `mkdir -p` commands from **Git Bash**. |
| curl | Built into Windows (`curl.exe`) |
| VS Code + Extension Pack for Java | Already installed |
| Docker Desktop | Installed and running on WSL 2 |
| Maven | Not needed; the project will use the `./mvnw` wrapper |

## Decisions and explanations already given

- **Database:** Don't install PostgreSQL or Redis on Windows. Postgres 16 and Redis 7 run in Docker through `docker-compose.yml` (plan Step 2). Flyway creates the schema (Step 3). Tests use Testcontainers. DB login: `localhost:5432`, database `payflow`, user `payflow`, password `payflow`.
- **Why WSL is needed:** Containers need a Linux kernel. Docker Desktop on Windows runs a small Linux VM through WSL 2. The machine is **Windows 11 Home**, which has no Hyper-V, so WSL 2 is the only option.

## Git push error (resolved by `gh auth login` + `gh auth setup-git`)

- Error: `remote: Invalid username or token. Password authentication is not supported` when pushing to `Gupta-19/PayFlow`.
- Cause: GitHub no longer accepts account passwords for Git. VS Code's terminal also sets `GIT_ASKPASS`, which can supply a stale VS Code GitHub token.
- There was no `git:https://github.com` entry in Windows Credential Manager. The entry `GitHub - https://api.github.com/Gupta-19` belongs to GitHub Desktop and isn't used by command-line Git.
- Fix: `gh auth login`, or push from a PowerShell window outside VS Code and sign in through the browser (or use a Personal Access Token with `repo` scope). Optionally set `"git.terminalAuthentication": false` in VS Code, or sign out of GitHub in VS Code and sign back in.

## Next steps

- **Step 2:** Add `docker-compose.yml` and `application.yml`, generate the RSA keys, then run `docker compose up -d`. Claude offered to set this up once Docker is running.
