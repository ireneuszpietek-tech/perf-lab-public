# Hypothesis HYP-001 — Tail Latency Driven by Allocation Rate and GC Pressure

## Driver

During baseline load testing of a simple REST-based service (Spring Boot, Kotlin),
the system exhibits a disproportionate increase in tail latency (p95/p99) under moderate concurrency,
despite low external I/O, no database access, and stable average CPU utilization.

Initial observations suggest that:
- request processing allocates a significant number of short-lived objects
(DTOs, collections, JSON structures),
- allocation pressure increases linearly with concurrency,
- garbage collection activity (especially young GC frequency and pause clustering)
correlates strongly with spikes in tail latency.

Because the system has no external dependencies (DB, cache, network I/O),
any tail latency degradation must originate from in-process JVM behavior
rather than from infrastructure or coordination effects.

This makes allocation rate and GC behavior a prime suspect for explaining
non-linear latency growth at higher percentiles.

## Rationale
In JVM-based systems, tail latency is often dominated not by business logic,
but by memory management effects, including:
- allocation rate exceeding eden evacuation capacity,
- GC pauses clustering under bursty allocation patterns,
- increased object promotion due to survivor space pressure,
- cache-unfriendly object graphs and excessive copying.

These effects frequently remain invisible at the p50 or average latency level,
yet dominate p95/p99 behavior — precisely where SLO violations occur.

If tail latency is primarily driven by allocation and GC pressure,
then performance tuning focused on CPU, threads, or concurrency primitives
will be ineffective unless allocation behavior is addressed.

Understanding this relationship is essential for:
- establishing a reliable performance baseline,
- interpreting future experiments involving concurrency or I/O,
- preventing misleading conclusions in later JVM-level investigations.

## Hypothesis
Tail latency degradation (p95/p99) under load is primarily caused by
high allocation rates and GC pressure, not by computational complexity or contention.

Specifically:
- increasing concurrency leads to a superlinear increase in allocation rate,
- GC activity (frequency and pause clustering) correlates with latency spikes,
- reducing per-request allocation volume will significantly improve p95/p99 latency,
even when request throughput and business logic remain unchanged.

In short:
- If allocation pressure is reduced, tail latency improves;
- if allocation pressure remains high, tail latency degrades — independent of I/O.

This is a causal hypothesis, not merely correlational.

## Expected Impact

If HYP-0001 is true:
-reducing object allocation per request will:
-lower GC frequency and pause clustering,
-flatten p95/p99 latency curves under load,
-improve latency stability without changing system semantics.

JVM profiling will show:
- reduced allocation rate (MB/s),
- fewer young GC cycles per unit time,
- lower variance in GC pause timing.

Future performance experiments will gain a cleaner baseline,
allowing clearer attribution of effects to concurrency, I/O, or architectural changes.

Architectural and coding decisions (e.g., DTO design, serialization strategy,
collection usage) may be formally justified and standardized via ADRs.

If HYP-0001 is false:
- tail latency remains high despite reduced allocation rates,
- GC activity does not correlate with latency spikes,
- the dominant cause lies elsewhere (e.g., scheduling, CPU cache effects,
lock contention, framework internals).

## Confidence

Moderate.

Allocation-heavy request processing is a known JVM performance risk,
but the magnitude of its impact on tail latency in this specific system
must be validated experimentally.

## Experiment (EXP-0001)
### Method

Baseline (Version A — current implementation):

Run load test using k6 with a mixed request profile:
- 80% GET
- 20% CREATE
- Target load: gradual ramp to sustained concurrency (e.g., 100–300 VUs).
- No external I/O; in-memory data structures only.

Collect:
k6 metrics:
- p50 / p95 / p99 latency,
- throughput,
- error rate.

JVM artifacts:
JFR recording with:
- allocation events,
- GC events and pauses,
- CPU samples.
- heap usage over time.

Observe:
- allocation rate (MB/s),
- GC frequency and pause clustering,
- correlation between GC activity and p99 latency spikes.

Intervention (Version B — reduced allocation variant):

Apply a minimal, semantics-preserving change that reduces allocations, such as:
- reduced object copying,
- simpler DTO structures,
- less intermediate collection creation,
- adjusted serialization behavior.

Repeat the exact same load test and collect identical artifacts.

Artifacts to Collect

JFR recordings (before and after intervention)

k6 output (latency distributions, RPS)

GC and allocation rate summaries

CPU flamegraphs (if applicable)

Metrics
Primary Metrics

p95 / p99 latency (before vs after)

Allocation rate (MB/s)

GC pause frequency and distribution

Young GC count per unit time

Secondary Metrics

CPU utilization

Heap occupancy trends

Object promotion rate (if observable)

Falsification Conditions

HYP-0001 is confirmed if:

allocation rate decreases materially after intervention,

GC activity (frequency or pause clustering) decreases,

p95/p99 latency improves significantly,

throughput remains stable or improves.

HYP-0001 is falsified if:

tail latency remains unchanged despite reduced allocation,

GC behavior does not correlate with latency spikes,

latency degradation is driven by unrelated JVM or framework mechanisms.

Decision Path (CON / ADR)
If HYP-0001 is confirmed

We conclude:

allocation behavior is a first-class performance concern,

tail latency in JVM services is strongly influenced by memory churn,

performance baselines must include allocation and GC metrics.

Potential ADRs:

ADR-0001 — Allocation-Aware Performance Baseline

ADR-0002 — DTO and Serialization Allocation Guidelines

ADR-0003 — JVM Profiling Required for Performance Experiments

Next Steps:

Introduce allocation-aware regression tests,

Extend experiments to concurrency and contention scenarios (HYP-0002).

If HYP-0001 is falsified

We conclude:

allocation pressure is not the dominant factor,

tail latency is driven by other mechanisms.

Next Steps:

Investigate contention and scheduling effects (HYP-0002),

Analyze framework-level execution paths and thread management.