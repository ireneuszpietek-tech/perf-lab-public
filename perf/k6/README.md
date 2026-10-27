# k6 experiments

## HYP-001 / EXP-001 — Repeatable GET baseline

### 1. Start the Experimental Logistics System

Prerequisites: JDK 21 (the shipment Gradle build targets Java 21), k6, and the repository's Gradle wrapper. Use two terminals, both from the **repository root**.

In terminal 1, start the shipment application (the relevant part of the Experimental Logistics System):

```bash
./gradlew :shipment:shipment-service:bootRun --args='--spring.profiles.active=dev'
```

The root composite build includes `shipment` and its local dependencies (`public-id-codec` and `platform-spring-boot`). Start Gradle from the repository root; using `-p domains/shipping/shipment` skips these sibling included builds and may fail dependency resolution. The shipment build declares the `shipment-service` project. Its Spring configuration uses port `8080`; the `dev` profile enables `/dev/token`. Keep the application running for the test.

Verify that HTTP is reachable (an unauthorized response is sufficient to show the server is listening):

```bash
curl -i http://localhost:8080/api/shipping/v1/parcels/test
```

**Note:** The startup command is based on the checked-in Gradle configuration and has not been tested end-to-end here. If startup fails, inspect the application log and local configuration before interpreting any k6 result. The current application YAML also includes PostgreSQL datasource properties; whether additional infrastructure is needed must be verified locally.

### 2. Run the GET baseline

In terminal 2, from the repository root:

```bash
RATE=200 SEED_COUNT=1000 WARMUP=2m MEASURE=10m \
k6 run perf/k6/EXP-001_HYP-001-tail-latency-driven-gc-pressure/exp-001_hyp-001-tail-latency-driven-gc-pressure.js
```

The k6 setup creates 1,000 parcels using POST, stores the returned tracking codes and checks that GET returns HTTP 200. Warmup and measurement then send **GET-only** traffic at the configured arrival rate. POST setup requests are not part of the measured GET phase.

Parameters: `RATE` (GET arrivals/s), `SEED_COUNT` (parcels created during setup), `WARMUP` and `MEASURE` (durations); optional `BASE_URL`, `AUTH_URL` and `MODE`.

**Validation:** Check that seeding succeeds, measured GETs return 200, and k6 reports no dropped iterations. Record the repository commit, JVM flags, test environment and k6 output before comparing runs. `RATE=200` is an example, not a validated stable throughput target. This scenario has not yet been verified end-to-end against a running service.

Research question and measurement plan: [HYP-001](../../docs/research/HYP-001-tail-latency-driven-gc-pressure/HYP-001-tail-latency-driven-allocation-rate-gc-pressure.md).

---

## Existing notes

docker run -d -p 9000:9000 -p 9443:9443 --name portainer --restart=always -v /var/run/docker.sock:/var/run/docker.sock -v portainer_data:/data portainer/portainer-ce:latest

$ k6 run smoke.js

curl -v http://127.0.0.1:8080/api/ping

[perf with flamegraphs]
30s, pid: 450099
~$ asprof collect -e cpu -d 30 -o flamegraph --color=java -f flame.html 450099



4. How to "see" that it's a queue rather than a "slow Tomcat"?
You can:
enable Actuator and inspect:
the number of active Tomcat threads,
thread pool metrics,
trigger /threaddump during the test:
you will see many http-nio-8080-exec-* threads in RUNNABLE/WAITING states,
compare:
400 RPS (clean p95 ~100ms, no queues),
2000 RPS burst (p95 ~10s, queuing).


[Phrases used in performance engineering]
"Performance and load characteristics as first-class architectural drivers"
"Architecture shaped by tail-latency and saturation constraints"
"Backpressure-driven system design"

Rules for designing, testing, and operating a system so that temporal behavior is predictable, diagnosable, and resilient to drift.

1) Single Responsibility of Load
Each test and each metric must have a single responsibility: answer one specific question.
Do not mix cold-start with steady-state.
Do not mix CPU and IO limits in a single scenario without separation.
The result must be interpretable without "reading the author's mind".

2) Explicit Regimes Principle
Regimes (warm/cold, cache hit/miss, GC pressure/no GC pressure, contention/no contention) must be explicit, named, and separated.
If more than one regime exists, report them separately.
"Average of two worlds" is an anti-pattern.

3) Steady-State First, Transients Separate
Always separate phases: warm-up → stabilization → measurement → cool-down.
Measure only after meeting stability criteria (e.g., no p50/p95 trend across sliding windows).
Treat transients (cold-start, failover, cache warm-up) as separate "time-to-stable" tests.

4) Distribution, Not Point Estimates
The outcome is a distribution, not a single number.
Minimum: p50/p95/p99 + error rate.
Additionally: CDF/histogram or at least run-to-run variance.
Averages can be misleading and harmful with long tails.

5) Tail-Latency is a First-Class Citizen
Design and evaluate the system for the tail, not the median.
Bottlenecks and queuing surface in p95/p99.
Optimizing p50 at the expense of p99 is often a regression.

6) Backpressure Integrity
Always design and test with explicit backpressure.
Queues, limits, timeouts, retry policies, and circuit breakers must be coherent.
Lack of backpressure usually yields "nice" numbers right up to catastrophe (retry storm, queue collapse).

7) Capacity Invariants (Queueing-Aware Design)
Treat capacity and queuing as architectural invariants.
Establish: maximum concurrency per tier, target utilization, latency budget, retry budget.
Use simple laws (e.g., Little's Law) for sanity checks: if metrics do not align, something is wrong with the measurement or the system.

8) Control the Controllables
Determine and freeze what is controllable to avoid confounding causes.
CPU governor, pinning, NUMA policy (if applicable), fixed JVM flags, fixed heap.
Consistent workload: key distribution, working set, concurrency level.
One variable per experiment (all others kept constant).

9) Observability-Driven Benchmarking
There is no sound performance evaluation without telemetry supporting conclusions.
Benchmarking without correlation (GC/CPU/IO/network/pools) is a "black box" test that frequently leads to faulty interpretations.
Rule: every claim about a result must have a potential "witness" in the telemetry.

10) No Coordinated Omission
Do not measure latency in a manner that conceals delays caused by a blocked load generator.
If the client "does not send because it is waiting" and you only measure sent requests, you lose visibility into the tail.
The load generator must be capable of sustaining the target rate regardless of delays (or report that it failed to do so).

11) Reproducibility Budget
Establish an acceptable variance budget and guard it like test quality.
If run-to-run CV/spread > threshold, the test is diagnostic, not comparative.
For small differences (e.g., 3–5%), you require a much lower noise floor than for large ones (e.g., 30–50%).

12) Change with Characterization, Not Hope
Every performance change should be "characterized" like a refactoring.
Before the change: baseline distribution.
After the change: distribution comparison + verification that the regime did not shift (e.g., different GC pressure, different pool saturation point).
If the result "improved" but telemetry reveals you accidentally stopped hitting DB/cache, that is not an optimization—it is an unintended scenario change.
