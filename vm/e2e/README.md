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
