R&D Goals and Baseline Performance Objectives
for the Logistics Core System


## Iterative Approach
The system is intentionally built in small, iterative steps:

- Core domain and ports are considered stable.
- Adapters (persistence, messaging, security providers, etc.) start as in-memory/test implementations.
- Over time, in-memory adapters will be replaced with more realistic or production-like adapters (e.g., PostgreSQL, Kafka, external JWT providers).
- Each iteration focuses on:
    - one small domain or technical change,
    - one clear hypothesis,
    - one experiment,
    - one documented conclusion.

### Purpose
This document defines the baseline performance goals, architectural drivers, and research objectives for the Logistics Core System R&D environment.
It provides a shared frame of reference for hypothesis-driven engineering, performance experiments, and architectural decision-making.
The goal is NOT to create production-grade SLAs, but to establish practical laboratory constraints that guide experiments and help interpret results.

### Baseline Scenario
Unless otherwise specified, all performance measurements assume:
- RPS: 200 requests per second total load
- Endpoints: CreateShipment and GetShipment
- Environment: single node, 2–4 vCPU, 8–16 GB RAM
- Storage: in-memory repository (initial phase)
- Security: Spring Security filter chain enabled (unless an experiment explicitly disables it)
- Traffic Model: 70% reads (GetShipment), 30% writes (CreateShipment)
- Test Tooling: k6, JFR, system metrics
- This scenario represents the initial “laboratory load profile” against which hypotheses will be evaluated.

### Baseline Performance Goals
These goals are directional and intended to give meaning to experimental data.

#### Latency Targets (initial)
- GetShipment: p95 < 80 ms
- CreateShipment: p95 < 150 ms
- p99 for both flows should not exceed 2× p95
- End-to-end latency should remain stable across 10 → 100 → 200 RPS ramp-up

#### CPU Budget
- Total CPU utilization at baseline load should remain < 70%, leaving headroom for growth
- No single subsystem (security, domain, persistence, serialization, etc.) should consume > 30–40% of the total CPU budget without a justified reason

#### JVM / GC Behavior
- Allocation rate should remain stable; no unexpected bursts during steady load
- GC pause times should remain < 50 ms p95 under baseline load
- No evidence of thread starvation or saturation in Tomcat thread pools

#### System Stability Goals
- Throughput should scale linearly between 10 → 100 → 200 RPS
- No sudden latency cliffs, scheduling anomalies, or unexplained CPU spikes
- Observability signals (CPU, GC, threads, timings) should be interpretable and repeatable

### Research Objectives (Hypothesis-Driven)
The core purpose of this system is learning, not shipping features.
Therefore, research objectives define what the team wants to understand deeply.

4.1 Layer-by-Layer Understanding

Understand where time is spent in a request:
security → network → Tomcat → filters → controller → domain → repository

Identify top contributors to CPU, latency, and allocations

4.2 Security Filter Chain Analysis

Determine whether the Spring Security filter chain is a dominant CPU cost

Understand which filters are the top contributors

Learn how security affects throughput and latency under increasing load

4.3 Domain Layer Behavior

Validate that domain logic remains lightweight and predictable

Understand how domain code scales when persistence is later introduced

4.4 JVM / Runtime Behavior

Build intuition around:

allocation patterns

safepoints

thread states

GC behavior under load

Tomcat thread pool saturation

Learn how small code changes affect runtime characteristics

4.5 Future Research Areas

As the system grows:

impact of real database (PostgreSQL)

effect of messaging / async flows

consistency vs throughput trade-offs

CQRS vs simple CRUD

distributed IDs, ordering guarantees, and delivery semantics

5. Decision-Making Framework

All R&D experiments follow the cycle:

Driver – Why we are investigating this area

Hypothesis – Falsifiable statement predicting system behavior

Experiment – k6 scenarios, JFR recordings, metrics collection

Observation – What the system actually did

Conclusion – Was the hypothesis confirmed or falsified?

Decision – What action we take based on the result

Documentation – ADR, notes, graphs, reproducible scenarios

This ensures that every measurement leads to a learning outcome or architectural decision.

6. Out of Scope (for now)

To keep focus, the following are explicitly out of scope for early research:

distributed consistency mechanisms

advanced messaging patterns

multi-node clustering

database replication and durability

production-grade security policies

multi-tenant performance profiles

These will be introduced only after baseline behavior is deeply understood.

7. Revision Policy

This document evolves as understanding grows.
Each major R&D iteration can refine:

latency targets

CPU budgets

research questions

acceptable thresholds

load scenarios

The goal is continuous alignment between what we measure and what we want to learn.