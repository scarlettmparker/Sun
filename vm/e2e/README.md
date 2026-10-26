# Sun E2E environment

A disposable Postgres and GraphQL backend for the Sun frontends.

## Start

```bash
./vm/e2e/reset-db.sh
./vm/e2e/start-backend.sh
```

`reset-db.sh` drops the public schema. Restart the backend afterwards so
Hibernate recreates the schema.

The backend listens on `127.0.0.1:8085` and uses:

```text
postgres://sun:sun@127.0.0.1:5436/sun_e2e
```

## Run an app against it

Build the app first, then override the production env for the E2E port:

```bash
cd ../Guided-Reader
NODE_ENV=production \
SERVER_HOST=127.0.0.1 \
SERVER_PORT=5200 \
GRAPHQL_ENDPOINT=http://127.0.0.1:8085/graphql \
VITE_SERVER_BASE=http://127.0.0.1:5200 \
ALLOWED_HOSTS=localhost,127.0.0.1 \
ALLOWED_ORIGINS=localhost,127.0.0.1 \
bun --preload ./css-plugin.ts server.js
```

Then run the app's Playwright suite:

```bash
APP_URL=http://127.0.0.1:5200 \
BACKEND_URL=http://127.0.0.1:8085 \
DATABASE_URL=postgres://sun:sun@127.0.0.1:5436/sun_e2e \
./e2e/run.sh
```

## Stop

```bash
docker compose -f vm/e2e/compose.e2e.yml down
```

Add `-v` to delete the E2E database volume as well.

## Local runner

`runner.ts` accepts a repo and commit, checks it out in an isolated worktree,
and writes the report under `~/.local/share/sun-e2e/reports`. App repos build
the backend from Sun `main`, then build and run their Playwright suite. A `Sun`
run builds the jar from the requested commit and runs both apps at their `main`.
Jars are cached per SHA. The backend stays up between runs and stops after 10
minutes idle. Each app suite starts with a truncated database, so the backend is
not restarted unless the Sun commit changes. Reports go under
`reports/<run>/<repo>`. Worktrees are removed after each run. Reports and cached
jars older than 14 days are pruned at startup.

Install the systemd user service:

```bash
./vm/e2e/install.sh
```

The installer writes `~/.config/sun-e2e.env` with a runner token. Copy that
token into the Woodpecker secret named `E2E_RUNNER_TOKEN`.

Trigger a run:

```bash
./vm/e2e/trigger.sh <repo> <sha>
```

The runner API:

- `POST /runs` with `{ "repo": "Guided-Reader", "sha": "<sha>" }`
- `GET /runs/:id`
- `GET /runs`
- `GET /health`

Every endpoint except `/health` requires
`Authorization: Bearer $E2E_RUNNER_TOKEN`.

Reports contain `build.log`, `app.log`, `test.log`, the Playwright HTML report,
and `run.json`. The long-lived backend log is at
`~/.local/share/sun-e2e/backend.log`.

## Public endpoint

The PC does not accept inbound traffic, so the runner is exposed through the VPS.

- `sun-e2e-tunnel.service` keeps an SSH reverse tunnel to `127.0.0.1:8090` on the VPS.
- The VPS nginx site `e2e-runner.scarlettparker.co.uk` proxies to `127.0.0.1:8090`.
- The Cloudflare A record points at the VPS.

Woodpecker secrets:

- `e2e_runner_url` = `https://e2e-runner.scarlettparker.co.uk`
- `e2e_runner_token` = the token from `~/.config/sun-e2e.env`
