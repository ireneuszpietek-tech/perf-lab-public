# Local runbook: shipment service and EXP-001

This guide is for someone who wants to run the JVM shipment testbed and reproduce the **HYP-001 / EXP-001** k6 workload on their own machine.

**Verification status:** The commands and paths below were inspected against repository files on 2026-11-12, but the full startup and test sequence has **not been executed on a clean machine**. Treat this as an initial runbook, not yet a verified one-command setup. If something fails, capture the exact error and improve this document.

See [the main README](../README.md) for research goals and [AGENTS.md](../AGENTS.md) for collaboration principles.

## Prerequisites

- Git
- **JDK 21** (`java -version` and `JAVA_HOME`)
- Network access for Gradle's dependency downloads on first build
- **k6** installed and available on `PATH` (`k6 version`)
- A local shell (commands below target Linux/macOS; adapt environment-variable syntax for PowerShell)
- If using the configured PostgreSQL datasource, a reachable PostgreSQL instance; see the database note below

Use the checked-in Gradle wrapper; no system-wide Gradle installation is required. Its distribution is **Gradle 9.8.0**.

## 1. Clone and inspect

```bash
git clone https://github.com/ireneuszpietek-tech/perf-lab-public.git
cd perf-lab-public
java -version
./gradlew --version
k6 version
```

The root project is a **Gradle composite build**. The shipment execution unit is under `domains/shipping/shipment/`. Read that build's `settings.gradle.kts` when diagnosing module names.

## 2. Start the shipment service

From the repository root:

```bash
./gradlew :shipment:shipment-service:bootRun --args='--spring.profiles.active=dev'
```

**Run this command from the repository root, without \`-p domains/shipping/shipment\`.** The root \`settings.gradle.kts\` includes both \`shipment\` and its local dependencies (\`libs/public-id-codec\` and \`platform-spring-boot\`) as composite builds. Using \`-p\` makes shipment a standalone build and prevents those substitutions, leading to \`Could not find ip.logistics.codec:public-id-codec:1.0.0\`.\n\nThe service's checked-in configuration specifies port **8080**. The `dev` profile is used for the load test's development token endpoint.

**Database caveat:** The checked-in `application.yml` contains:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/app
    username: app
    password: pass
```

The hostname `postgres` may only resolve inside a Docker/network setup, not on a clean host. Also, do not assume the dev execution path requires a database merely because this setting is present. If startup reports a database connection failure, determine which persistence adapter/profile is active before changing architecture or starting additional infrastructure.

If a local PostgreSQL service is actually required, configure its host and credentials explicitly (for example with `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`), and document the schema/init requirements. **No database bootstrap command has been verified as part of this runbook.**

If Gradle fails resolving `ip.logistics.codec:public-id-codec:1.0.0`, inspect the included build `libs/public-id-codec` and composite-build dependency substitution. This dependency-resolution problem has occurred during previous work; do not mistake it for a k6 failure.

## 3. Check the service before load

In a second terminal:

```bash
curl -i 'http://localhost:8080/dev/token?sub=123'
```

Expected by the k6 script: HTTP **200** with JSON containing `access_token`. Do not run the load test unless that works. This is a **development-only** endpoint; do not expose it publicly.

The k6 script then creates parcels via:

- `POST /api/shipping/v1/parcels` (expects HTTP 201 and a `trackingCode`)
- `GET /api/shipping/v1/parcels/{trackingCode}` (expects HTTP 200)

It will terminate during setup if the seed or read-path probe fails. This is intentional: a 404 must not count as successful benchmark traffic.

## 4. Run the first benchmark

From the repository root in a second terminal:

```bash
RATE=200 SEED_COUNT=1000 WARMUP=2m MEASURE=10m MODE=baseline \
  k6 run perf/k6/EXP-001_HYP-001-tail-latency-driven-gc-pressure/exp-001_hyp-001-tail-latency-driven-gc-pressure.js
```

The script's defaults are `BASE_URL=http://localhost:8080`, `AUTH_URL=$BASE_URL/dev/token?sub=123`, `RATE=200`, `SEED_COUNT=1000`, `WARMUP=2m`, `MEASURE=10m`, and `MODE=baseline`. You can override these with environment variables.

**What this actually does:** k6 obtains a dev token, POSTs `SEED_COUNT` parcels, probes that one seeded parcel is readable, then runs a constant-arrival-rate GET workload. The setup POSTs are outside the timed GET load. The warm-up and measurement phases are tagged separately; use **`phase:measure`** for conclusions about measured behavior.

**Important:** The script sets two separate arrival-rate scenarios with `startTime=WARMUP` for the measured phase. Confirm the phases do not overlap unexpectedly when changing durations. Check `dropped_iterations` and effective completed request rate; the configured `RATE` alone does not prove the target load was achieved.

For a shorter local smoke test (not a comparable benchmark):

```bash
RATE=20 SEED_COUNT=20 WARMUP=10s MEASURE=30s MODE=smoke \
  k6 run perf/k6/EXP-001_HYP-001-tail-latency-driven-gc-pressure/exp-001_hyp-001-tail-latency-driven-gc-pressure.js
```

## 5. Interpret results carefully

Do not treat a successful k6 run as proof that allocation rate affects tail latency.

At minimum, capture:

| Category | Record |
| --- | --- |
| Source | Git commit SHA and any local changes |
| Environment | OS, CPU, RAM, JDK build/version, JVM flags and GC |
| Workload | RATE, SEED_COUNT, WARMUP, MEASURE, MODE, URL |
| k6 results | throughput/completed requests, dropped iterations, error rate, p50/p95/p99, max |
| JVM results | GC events/pauses, allocation rate, CPU, memory (when instrumentation is available) |
| Interpretation | Hypothesis, competing explanations, limitations, and next experiment |

Save raw results and observations with the relevant research documents under [docs/research/](research/). Preserve run-to-run comparability and repeat measurements. If the environment differs, say so explicitly.

One earlier baseline discussed for EXP-001 reported ~199.42 checks/s, mean 2.11 ms, p95 2.90 ms, and 0% HTTP failures. Those **historical figures are not acceptance thresholds or proof of causation**.

## Troubleshooting / known gaps

| Symptom | Investigate |
| --- | --- |
| Gradle cannot resolve `public-id-codec` | Composite build inclusion and group/artifact/version alignment |
| `/dev/token` returns 404 or non-200 | Dev profile, service startup, endpoint configuration |
| Seeding POST fails | API contract, authentication, dependency startup, response body |
| Seeded parcel GET returns 404 | In-memory view / event consumer propagation; previous work encountered this issue |
| Connection fails for host `postgres` | Network naming, whether database is actually needed, datasource override |
| k6 has dropped iterations | Insufficient VUs/load-generator capacity or overloaded target |
| Test produces low latency but no GC evidence | Collect JVM metrics; do not infer allocation/GC effects from HTTP latency alone |

## Definition of a reproducible run

A contributor should be able to check out a specific commit, install documented prerequisites, start required dependencies and the app, confirm a health/readiness path, seed the test data, run k6, and save evidence without relying on private knowledge.

**Outstanding validation:** verify clean-clone startup on a separate machine/CI, document any actual database or supporting-infrastructure bootstrapping, and add a fully reproducible JVM instrumentation/results export procedure for EXP-001. These are gaps, not silently assumed working features.
