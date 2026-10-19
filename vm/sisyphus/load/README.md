# sisyphus load tests

k6 harness for the published GraphQL endpoint. The query is reader-specific and
DB-backed. `AUTH=off` bypasses permissions, rate limiting and audit.

```sh
k6 run vm/sisyphus/load/graphql.js                    # 5000 VUs, 30s ramp, 90s hold
VUS=1000 RAMP=10s HOLD=30s k6 run vm/sisyphus/load/graphql.js
URL=http://10.99.0.6:8083/graphql k6 run vm/sisyphus/load/graphql.js
```

The endpoint is POST-only (DGS returns 405 for GraphQL GET). Warm the JVM before
recording a point, because a cold run is roughly three times slower.

## Seed data

The reader table starts empty, which hides every DB cost. Load the 10k-row seed
before measuring:

```sh
incus exec sisyphus -- sudo -u postgres psql -d sun < vm/sisyphus/load/seed.sql
```

## Recorded on the 1 vCPU / 512 MiB box

Full module set, `AUTH=off`, SerialGC, 192 MB heap, 160 MB metaspace, compact
object headers, 24 Tomcat threads, 1,000 connections, 4 Hikari connections.
10k seeded rows (9,500 active), warm JVM:

| VUs | RPS | median | p95 | p99 | failed |
| --- | --- | --- | --- | --- | --- |
| 1,000 | 2,056 | 462 ms | 510 ms | 579 ms | 0 |
| 5,000 | 1,864 | 477 ms | 561 ms | 60 s | 1.46% |

Memory settles around 358 MB RSS with no OOM kills. The 5,000-VU failures are
requests that never get a connection because Tomcat holds 1,000 sockets while
k6 opens 5,000. Raising the cap to 2,000 or 8,192 moved CPU into the kernel and
cut throughput. Virtual threads OOM'd the 192 MB heap at 5,000 VUs. 1,000
connections is the best point on this box.

## What moved the numbers

- `totalCount` used to run a 1.4 ms index scan over every active row on every
  request. It now reads a trigger-maintained counter, so the count is exact and
  O(1).
- The list query uses a projection that skips the `content` column.
- `(status, level, id)` and `(status, owner_id, level, id)` indexes cover the
  list and filter paths.
- Per-request HTTP and GraphQL observations are off, and the security firewall
  no longer regex-scans every header.
- The audit aspect is absent when audit is disabled, and hot mappers no longer
  log per row.
- JWT requests parse the token once and check account status with a projection
  instead of loading the whole account.

Before the audit leak fix, the 2 vCPU / 2 GiB box held 8,174 RPS at 5,000 VUs
but sat at 650 MB RSS. The old 1 vCPU numbers were measured against an empty
reader table and are not comparable.
