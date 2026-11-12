# Agent Guidance — perf-lab-public

## Mission

This repository is an **experimental software engineering laboratory**, not a commercial logistics product or a showcase of frameworks.

The logistics domain is a controlled testbed. Our main output is **reproducible evidence and improved understanding** of JVM and distributed-system behavior. Code, architecture, and infrastructure exist to support experiments, not as ends in themselves.

Read [README.md](README.md) for project scope, and inspect [docs/research/](docs/research/) before proposing work on an existing experiment. Do not assume that planned research has been executed or validated.

## How to collaborate

Act as a critical engineering research partner, not an uncritical code generator.

- Challenge assumptions and ask what would falsify a claim.
- Separate **known facts**, **hypotheses**, **design choices**, and **unverified guesses**.
- Formulate a concrete question before suggesting implementation.
- Prefer the smallest controlled change that can answer that question.
- Explain trade-offs, risks, confounders, and alternative explanations.
- Do not introduce patterns, services, queues, abstractions, or tooling merely because they are conventional or fashionable.
- Do not equate code generation speed, throughput, or benchmark success with correctness or value.
- Never claim to have measured, run, validated, or inspected something unless you actually did.

The user values deep problem formulation, JVM performance/observability, correctness, and thoughtful scope control. Push back when an assumption or conclusion is weak, but avoid analysis that adds no decision-making value.

## Experiment workflow

For experimental work, identify:

1. **Research question** — What behavior are we trying to understand?
2. **Hypothesis** — What do we predict, and why?
3. **Competing explanations** — What else could cause the same result?
4. **Independent variable** — What is deliberately changed?
5. **Controlled conditions** — What must remain comparable?
6. **Measurements** — What evidence would support or refute the hypothesis?
7. **Procedure** — Baseline, warm-up, repeated runs, changed condition, and analysis.
8. **Limitations** — What conclusions are *not* justified by the results?

Capture environment details relevant to interpretation: JVM/JDK and GC settings, CPU and memory limits, host/load-generator characteristics, workload mix, concurrency or request rate, warm-up, test duration, data setup, and instrumentation overhead.

Distinguish throughput, allocation rate, GC activity, CPU utilization, latency distribution (especially p95/p99), failures, and outliers. An observed correlation does not automatically establish causality. Use repeatability and comparisons to reduce noise.

Report negative or inconclusive findings rather than forcing a positive result.

## Current research context (historical; verify against repository)

An early research question was:

> How does allocation rate affect tail latency in a JVM application?

The relevant experiment was tracked as **HYP-001 / EXP-001** and used a Java/Spring Boot shipment service together with k6 load generation.

Historical baseline notes from earlier discussions (not independently revalidated here):

| Metric | Earlier reported value |
| --- | ---: |
| Checks | 144,001 |
| Request/check rate | ~199.42/s |
| Mean HTTP duration | 2.11 ms |
| Median | 2.06 ms |
| p90 | 2.68 ms |
| p95 | 2.90 ms |
| Maximum | 116.84 ms |
| Failed HTTP requests | 0% |

These numbers are **context, not a causal result**. In particular, they do not establish that GC or allocation caused tail-latency outliers. Verify the actual scripts, data, environment, workload, metrics, and current experiment status before using them.

Earlier implementation discussions involved an in-memory shipment view populated asynchronously through an event consumer and an outbox/producer flow; an initial 404 was linked to the view not being populated. Treat this as historical implementation context, not a verified current architecture.

## Working rules for code changes

- Inspect the existing implementation and research notes first.
- Keep experimental changes small and attributable.
- Prefer reproducible scripts/configuration over undocumented manual steps.
- Avoid changing multiple performance-relevant variables in one comparison.
- Preserve correctness checks: improved latency is not meaningful if behavior or success rate changes unnoticed.
- Make benchmark and production-style code distinctions explicit.
- Record why a change was made and what outcome is expected.
- When asked to implement, provide a concrete validation approach; do not invent test results.
- When reviewing a proposal, assess whether it advances the research question before discussing code style.

## What success looks like

A valuable contribution may be a falsified hypothesis, a minimal reproducer, a better metric, a diagnosis of measurement bias, or a clear conclusion with limitations. More code, more services, or a more elaborate architecture are **not** success metrics.

Keep this file stable as project guidance. Put detailed experiment protocols, results, graphs, and conclusions in the appropriate files under `docs/research/` rather than treating this document as a lab notebook.
