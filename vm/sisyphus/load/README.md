# sisyphus load tests

k6 harness for the published GraphQL endpoint. The query is reader-specific and
DB-backed; `AUTH=off` bypasses permissions, rate limiting and audit.

```sh
k6 run vm/sisyphus/load/graphql.js                    # 5000 VUs, 30s ramp, 90s hold
VUS=1000 RAMP=10s HOLD=30s k6 run vm/sisyphus/load/graphql.js
URL=http://10.99.0.6:8083/graphql k6 run vm/sisyphus/load/graphql.js
```

The endpoint is POST-only (DGS returns 405 for GraphQL GET).

## Recorded on the 2 vCPU / 2 GiB box

`query { hadesQueries { texts(pagination: {page: 1, size: 5}) { ... } } }`,
empty reader table, warm JVM, 0 failed requests:

| VUs | RPS | median | p95 | p99 | max |
| --- | --- | --- | --- | --- | --- |
| 20 | 3,642 | 2.8 ms | 9.6 ms | 23 ms | 91 ms |
| 5,000 | 6,321 | 112 ms | 236 ms | 629 ms | 52 s |

A second 5,000-VU run peaked at 7,533 RPS (p95 162 ms, p99 843 ms). CPU is the
limit: the cgroup is pinned at ~2 cores and memory settles around 1.2 GiB.
Tomcat queues past `max-connections=1000` rather than rejecting, so failures
stay at zero while the tail grows. Cold-JVM runs are noticeably slower; warm up
before comparing points.
