# Experimental Logistics System

**A logistics domain used as a testbed for software engineering experiments.**

This repository is not an attempt to build a production parcel-delivery business. It uses a deliberately constrained logistics model to investigate how software behaves under load, concurrency, resource limits, and failure.

The primary deliverable is **evidence**: reproducible experiments, measurements, and conclusions about system behavior. The logistics application is the environment in which those questions can be tested.

## Why logistics?

Parcels, couriers, lockers, and scanner events create useful constraints without requiring a huge business model:

- A locker has finite capacity; simultaneous reservations can compete for the same space.
- A courier can handle only a bounded amount of work.
- Events can arrive late, twice, or out of order.
- Incoming work can exceed processing capacity, producing queues, backpressure, and rejection.
- Retries and failures can change the load placed on an already stressed system.

The objective is to introduce complexity **through behavior rather than through hundreds of fields or services**.

## How research works

The project follows a hypothesis-driven engineering loop:

```text
Observation / question
        |
        v
    Hypothesis
        |
        v
 Experiment + baseline
        |
        v
 Measurements / profiling
        |
        v
 Analysis and conclusion
        |
        v
 Keep, revise, or reject the hypothesis
```

An experiment should make its workload, environment, measurement method, and limitations explicit. A performance number without that context is not a useful conclusion.

The research material lives in [docs/research/](docs/research/). Existing topics include allocation and GC pressure, security-filter CPU overhead, and exception-handling behavior. Their documentation should be read individually: the presence of a hypothesis or scenario does **not** mean that its claims have already been demonstrated.

## What can be investigated?

The system provides a setting for questions such as:

- **Performance:** Where does throughput stop scaling? How do allocation rate and GC pauses affect tail latency?
- **Concurrency:** What happens when competing operations update shared state or contend for limited resources?
- **Reliability:** How do retries, duplicate events, delays, and failures change system behavior?
- **Capacity:** What changes when queues, workers, couriers, or lockers reach their limits?
- **Architecture:** Which boundaries, abstractions, and infrastructure costs are justified by observed behavior?

Not every mechanism described here is necessarily implemented or validated. This repository contains a mixture of application code, experimental code, research notes, architecture decisions, and work in progress.

## Repository map

| Location | Role |
| --- | --- |
| [domains/shipping/](domains/shipping/) | Logistics domain and execution units, including shipment and edge ingestion |
| [perf/](perf/) | Load-testing scenarios and performance experiments |
| [docs/research/](docs/research/) | Hypotheses, experiment plans, observations, and conclusions |
| [docs/architecture/](docs/architecture/) | Architecture documentation and decision records |
| [platform-spring-boot/](platform-spring-boot/) | Shared Spring Boot infrastructure |
| [libs/](libs/) | Shared libraries |
| [infra/](infra/) | Local supporting infrastructure |
| [tests/](tests/) | Test utilities and API checks |
| [clients/](clients/) | Client-side work |

The repository uses a Gradle composite-build structure. The architecture is a means to support experiments, **not a result to defend regardless of measurements**.

## Getting started

Start by exploring an existing hypothesis in [docs/research/](docs/research/) and locating the corresponding implementation or workload in the repository.

Useful entry points:

```bash
# Explore the available Gradle tasks for this checkout
./gradlew tasks

# Inspect the local development targets
cat Makefile
```

Depending on the experiment, additional tools may include Docker Compose, k6, Java Flight Recorder (JFR), JDK Mission Control, or async-profiler. See the relevant experiment and module documentation for its actual prerequisites and commands; do not assume that every planned scenario is runnable end to end.

## Engineering principles

1. **Questions before tools.** Introduce technology to address a concrete problem or to test a hypothesis.
2. **Measure rather than assume.** Distinguish observed results from design intentions.
3. **State the regime.** Specify workload, warm-up, hardware, JVM configuration, and resource limits when interpreting performance.
4. **Look at distributions.** Tail latency, error rates, queue depth, and variance may matter more than average throughput.
5. **Prefer controlled complexity.** Add domain and infrastructure mechanisms only when they create an interesting constraint or testable failure mode.
6. **Allow negative results.** A falsified hypothesis or an optimization that fails to help is still a useful result.

## Scope

This is an evolving engineering laboratory, not a product roadmap or a reference implementation of a commercial logistics platform. Some areas are exploratory and some documentation describes intended behavior rather than completed functionality.

The goal is to use a comprehensible domain to ask harder questions about software systems—and to answer them with experiments.
