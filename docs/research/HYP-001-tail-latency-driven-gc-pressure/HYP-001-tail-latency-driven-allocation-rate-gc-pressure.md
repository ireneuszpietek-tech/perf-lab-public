# HYP-001 — How does allocation rate affect JVM tail latency?

## Question

How does object allocation per request affect p95/p99 latency in a JVM-based REST service?

The goal is to **observe and understand** the relationship between allocations, garbage collection (GC), and latency under load—not to assume that GC is necessarily the bottleneck.

## Experiment

Compare two implementations of the **same request path**:

- **A — baseline:** current implementation.
- **B — lower allocation:** a small change intended to allocate fewer bytes per request, without changing the response or business behavior.

Use the same input data, request mix, offered request rate, JVM/GC settings, heap size, and environment for both runs. Warm up before measuring. Repeat runs to check whether differences exceed normal variation.

Before interpreting results, verify that the intended operation actually ran. In particular, a GET returning `404` may exercise a different path from a successful GET.

The existing [k6 scenario](../../../perf/k6/EXP-001_HYP-001-tail-latency-driven-gc-pressure/exp-001_hyp-001-tail-latency-driven-gc-pressure.js) is a **starting point**, not a validated experiment: its random GET IDs may produce `404` responses, and the load generator/settings need checking before an A/B comparison.

## Measurements

| Measure | Why |
| --- | --- |
| Allocated bytes/request and allocation rate (MB/s) | Verify that B actually changes allocation pressure |
| GC count and pause duration | Check whether GC behavior changes |
| p50 / p95 / p99 request latency | Observe the whole latency distribution, especially the tail |
| Requests/s, failures, dropped k6 iterations | Check whether both runs delivered comparable workloads |
| CPU utilization (if available) | Look for other possible bottlenecks |

Where possible, collect k6 results together with JFR and GC logs. Keep measurement windows aligned; do not infer causality from a coincident GC event and an aggregate p99 alone.

## Observations

*Not measured/documented here yet.*

Record actual numbers for A and B, the environment and test parameters, and links to raw results. Separate measurements from explanations.

## Conclusions

*Pending experiment.*

Questions to answer after measurement:

- Did allocations per request decrease?
- At a comparable offered load and successful request mix, did p95/p99 change beyond run-to-run noise?
- Did GC behavior change enough to explain the latency difference, or are other mechanisms more plausible?
- What does this experiment **not** establish?

A useful conclusion can be “allocation decreased but latency did not materially improve.” No ADR is required unless a consequential engineering decision follows.
