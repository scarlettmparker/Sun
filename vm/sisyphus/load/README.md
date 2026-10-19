# sisyphus load tests

k6 harness for the published GraphQL endpoint. The query is reader-specific and
DB-backed; `AUTH=off` bypasses permissions, rate limiting and audit.

```sh
k6 run vm/sisyphus/load/graphql.js                    # 5000 VUs, 30s ramp, 90s hold
VUS=1000 RAMP=10s HOLD=30s k6 run vm/sisyphus/load/graphql.js
URL=http://10.99.0.6:8083/graphql k6 run vm/sisyphus/load/graphql.js
```

The endpoint is POST-only (DGS returns 405 for GraphQL GET).

## Recorded on the 1 vCPU / 512 MiB box

Full module set, `AUTH=off`, SerialGC, 192 MB heap, 160 MB metaspace, compact
object headers, tuned Postgres (32 MB `shared_buffers`). Permify and Redis stay
stopped because auth is off. Warm JVM:

| VUs | RPS | median | p95 | p99 | max | failed |
| --- | --- | --- | --- | --- | --- | --- |
| 500 | 1,919 | 237 ms | 280 ms | 309 ms | 499 ms | 0 |
| 1,000 | 1,878 | 476 ms | 587 ms | 925 ms | 3.8 s | 0 |
| 5,000 | 916 | 956 ms | 1.6 s | 60 s | 60 s | 5.2% |

The box settles at 350-370 MB RSS with ~150 MB still available and no OOM
kills. One core sustains roughly 1,900 RPS; past ~1,000 VUs requests queue and
the 5,000-VU point collapses into k6's 60 s timeout. The front-end Node cache is
what keeps production behind that knee.

SerialGC is decisive on one core: 1,919 RPS at 500 VUs versus 446 RPS with G1,
whose concurrent threads starve the only CPU. Cold-JVM runs are ~4x slower, so
warm up before comparing points.

Trimming to the reader modules saved under 10 MB (365 MB versus 360 MB) and
removed functionality, so the box runs every module.

Before the audit leak fix, the same 2 vCPU / 2 GiB box held 8,174 RPS at 5,000
VUs but sat at 650 MB RSS (1.33 GiB before the preparsed cache).
