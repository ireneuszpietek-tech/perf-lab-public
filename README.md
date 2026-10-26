# Logistics Core System (`perf-lab`)

> An engineering research platform and JVM-based simulation designed to explore distributed systems architecture, runtime behavior, tail latency, and reliability under realistic load.

[![Java](https://img.shields.io/badge/Java-21%2B%20%2F%2024-orange.svg)](https://openjdk.org/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4%2B-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Gradle Composite](https://img.shields.io/badge/Gradle-8.14%20Composite-02303A.svg)](https://docs.gradle.org/current/userguide/composite_builds.html)
[![Architecture](https://img.shields.io/badge/Architecture-DDD%20%7C%20Hexagonal%20%7C%20Vertical%20Slices-purple.svg)](docs/architecture/)
[![Research](https://img.shields.io/badge/Research-Hypothesis--Driven%20Engineering-red.svg)](docs/research/)

---

## Table of Contents

- [1. Overview & Philosophy](#1-overview--philosophy)
  - [Design Philosophy](#design-philosophy)
  - [Hypothesis-Driven Engineering (HDE)](#hypothesis-driven-engineering-hde)
- [2. Domain Model & Invariants](#2-domain-model--invariants)
  - [Parcel Aggregate](#parcel-aggregate)
  - [Locker (Finite Resource)](#locker-finite-resource)
  - [Courier (Capacity & Concurrency)](#courier-capacity--concurrency)
  - [Delivery Workflow & Event Choreography](#delivery-workflow--event-choreography)
- [3. Architecture & Modular Monolith](#3-architecture--modular-monolith)
  - [High-Level Processing Pipeline](#high-level-processing-pipeline)
  - [Execution Units & Leaf Modules Taxonomy](#execution-units--leaf-modules-taxonomy)
  - [Vertical Slice & Application Layer Rules](#vertical-slice--application-layer-rules)
  - [Cross-Cutting Platform (`platform-spring-boot`)](#cross-cutting-platform-platform-spring-boot)
  - [Shared Libraries (`libs/public-id-codec`)](#shared-libraries-libspublic-id-codec)
  - [Edge & IoT Ingestion Pipeline](#edge--iot-ingestion-pipeline)
- [4. Architecture Decision Records (ADRs)](#4-architecture-decision-records-adrs)
- [5. Runtime Model, Backpressure & Saturation](#5-runtime-model-backpressure--saturation)
  - [Runtime Control Points](#runtime-control-points)
  - [Backpressure & Saturation Dynamics](#backpressure--saturation-dynamics)
  - [Failure Injection](#failure-injection)
  - [Time & Scheduling Model](#time--scheduling-model)
- [6. Performance Engineering & Benchmarking](#6-performance-engineering--benchmarking)
  - [The 12 Performance Principles](#the-12-performance-principles)
  - [Active Research Hypotheses](#active-research-hypotheses)
  - [Experiment Scenarios](#experiment-scenarios)
- [7. Observability & Telemetry](#7-observability--telemetry)
  - [Telemetry Signals](#telemetry-signals)
  - [Diagnostics Tooling](#diagnostics-tooling)
- [8. Repository Structure](#8-repository-structure)
- [9. Quickstart & Operational Guide](#9-quickstart--operational-guide)
  - [Prerequisites](#prerequisites)
  - [Building the System](#building-the-system)
  - [Running Infrastructure](#running-infrastructure)
  - [Starting the Application](#starting-the-application)
  - [Executing Tests & Benchmarks](#executing-tests--benchmarks)
  - [Profiling with async-profiler & JFR](#profiling-with-async-profiler--jfr)
- [10. What This Project Is Not](#10-what-this-project-is-not)

---

## 1. Overview & Philosophy

**Logistics Core System (`perf-lab`)** is an R&D testbed built to explore system design, runtime dynamics, tail latency, and reliability under load. 

Rather than reproducing the administrative overhead of a commercial ERP with hundreds of domain properties, this project creates realistic complexity through **emergent system behaviors**:

- Asynchronous event choreography and message lag
- Bounded queues and active backpressure
- Resource contention and capacity saturation
- Retries, jitter, and retry storm dynamics
- Duplicate, delayed, and out-of-order events
- Allocation rates, GC pause clustering, and tail latency ($p95$, $p99$)
- Monotonic state transitions and optimistic concurrency

### Design Philosophy

> **Complexity should come from behavior, not from the number of domain fields.**

A parcel does not need 200 database columns to produce an interesting distributed system problem. A lean domain model subjected to concurrency, physical queue limits, failure injection, and finite resource constraints naturally exhibits the runtime failure modes observed in production distributed architectures.

Every new mechanism or feature introduced in this repository must satisfy at least one of the following criteria:
1. **Measurable runtime effect** (e.g., changes queueing time, throughput, or tail latency).
2. **New failure mode** (e.g., message loss, lock contention, downstream degradation).
3. **New resource constraint** (e.g., locker bay limits, courier travel time, thread pool limits).
4. **New observability signal** (e.g., allocation rate, GC pause distribution, event lag).

### Hypothesis-Driven Engineering (HDE)

All architectural decisions and performance optimizations follow an empirical research loop:

```text
Driver ──▶ Hypothesis (HYP) ──▶ Experiment (EXP) ──▶ Measurement (Telemetry / JFR)
                                                                 │
                                                                 ▼
ADR ◀── Decision ◀── [Validation / Invalidation] (CON) ◀─────────┘
```

See [docs/research/README.md](docs/research/README.md) for full documentation of the research methodology.

---

## 2. Domain Model & Invariants

The core domain intentionally maintains small, well-bounded aggregates with precise invariants.

### Parcel Aggregate

Represents a shipment transitioning through delivery states.

```text
       ┌───────────┐
       │  CREATED  │
       └─────┬─────┘
             │
             ▼
       ┌───────────┐
       │  PLANNED  │
       └─────┬─────┘
             │
             ▼
     ┌────────────────┐
     │OUT_FOR_DELIVERY│
     └───────┬────────┘
             │
             ├──────────────────────────┐
             ▼                          ▼
       ┌───────────┐              ┌───────────┐
       │ DELIVERED │              │  FAILED   │
       └───────────┘              └─────┬─────┘
                                        │
                                        ▼
                                  ┌───────────┐
                                  │ REQUEUED  │
                                  └───────────┘
```

**Key Aggregate Attributes:**
- `id`: Internal immutable identifier (`ParcelId` wrapping UUIDv7, per [ADR-0010](docs/architecture/adr/ADR-0010_Public_Private_Identifier_Pattern.md)).
- `publicId`: Deterministic, URL-safe Base32-encoded identifier (per [ADR-0012](docs/architecture/adr/ADR-0012_Base32_Encoding_Identifiers.md)).
- `status`: Monotonic delivery state (`CREATED`, `PLANNED`, `OUT_FOR_DELIVERY`, `DELIVERED`, `FAILED`, `REQUEUED`).
- `attempt`: Current delivery attempt count (drives retry budgets and circuit breakers).
- `targetLockerId`: Assigned delivery terminal destination.
- `version`: Monotonic optimistic concurrency token.

### Locker (Finite Resource)

Represents a physical parcel locker terminal with bounded capacity.

- `id`: Unique locker terminal identifier.
- `capacity`: Total physical lockers/compartments available.
- `occupied`: Currently occupied compartments.

$$\text{Invariant:} \quad \mathbf{occupied \le capacity}$$

Exceeding capacity rejects delivery reservations, triggering rerouting, backpressure, or delivery requeuing.

### Courier (Capacity & Concurrency)

Represents a delivery courier as a bounded computational and physical resource.

- `id`: Unique courier resource identifier.
- `capacity`: Maximum concurrent parcels a courier can carry.
- `busyUntil`: Absolute timestamp (`Instant` in UTC) until which the courier is committed to ongoing deliveries.

This model produces natural resource contention, scheduling bottlenecks, and queue backlogs without requiring complex GIS routing engines.

### Delivery Workflow & Event Choreography

Delivery execution is modeled as an asynchronous, event-driven workflow:

- `ParcelCreated`: Parcel registered into the system.
- `DeliveryPlanned`: Target locker and batch assignment calculated.
- `CourierAssigned`: Courier resource reserved for transit.
- `AttemptDelivery`: Delivery in progress to destination.
- `DeliverySucceeded`: Parcel safely deposited into locker.
- `DeliveryFailed`: Delivery attempt unsuccessful (locker full, recipient unavailable).
- `ParcelRequeued`: Delivery retried under exponential backoff and jitter.
- `LockerReserved`: Compartment allocation committed.
- `LockerFull`: Compartment allocation rejected due to saturation.

Every event carries standard operational metadata to support tracing, deduplication, and out-of-order analysis:

| Metadata Attribute | Purpose |
| :--- | :--- |
| `eventId` | Unique monotonic UUIDv7 event identifier. |
| `occurredAt` | ISO-8601 UTC timestamp (`Instant`) per [ADR-0016](docs/architecture/adr/ADR-0016_Standardized_Handling_Time_Timezones_Timestamps.md). |
| `correlationId` | End-to-end request identifier across all hops and threads. |
| `causationId` | Direct predecessor event ID establishing the causal chain. |

---

## 3. Architecture & Modular Monolith

The repository follows a **Domain-First, Vertically Sliced Hexagonal Architecture** packaged as a **Gradle Composite Build** ([ADR-0004](docs/architecture/adr/ADR-0004_Monorepo_Structure_Based_on_Bounded_Contexts_with_Domain_First_Approach.md), [ADR-0005](docs/architecture/adr/ADR-0005_Module_Taxonomy_System_Execution_Units_Leaf_Modules.md), [ADR-0006](docs/architecture/adr/ADR-0006_local_Repository_Structure_Gradle_Composite.md)).

### High-Level Processing Pipeline

```text
┌─────────────────────────────────────────────────────────────┐
│                    API / Ingestion Layer                    │
│      (Inbound REST Adapters / Edge Ingestion Pipelines)      │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                      Planning Engine                        │
│         (Batching, Locker Allocation, Route Slices)         │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                      Execution Layer                        │
│       (Use Case Handlers, Queues, Backpressure, Workers)    │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                    Domain Model & State                     │
│         (Parcel Aggregates, Lockers, Courier Allocations)   │
└─────────────────────────────────────────────────────────────┘
```

The system operates initially inside a single JVM. This allows studying distributed systems behavior (queues, thread pools, retries, serialization overhead, saturation knees) with fast feedback cycles before introducing multi-process networking or Kubernetes overhead.

### Execution Units & Leaf Modules Taxonomy

Per [ADR-0005](docs/architecture/adr/ADR-0005_Module_Taxonomy_System_Execution_Units_Leaf_Modules.md), the codebase is partitioned into three structural tiers:

1. **System Root (`logistics-core-system`)**: The top-level composite build aggregating all sub-builds via `includeBuild(...)`.
2. **Execution Units (Feature Aggregates)**: E.g., `domains/shipping/shipment`, `domains/shipping/edge-ingestion`. These represent cohesive business subdomains that can be independently built, tested, and potentially extracted into standalone microservices.
3. **Technical Leaf Modules**: Internal building blocks within an execution unit:
   - `*-domain`: Pure domain model, aggregates, entities, value objects, domain events, domain-level repository interfaces. **No dependencies on Spring or JPA.**
   - `*-application`: Use cases, commands, queries, handler interfaces, and shared orchestration logic.
   - `*-adapters`: Inbound controllers (REST/Web), outbound persistence (JPA/in-memory), outbound messaging, and external system gateways.
   - `*-service`: Spring Boot application runtime, dependency injection, and configuration.

### Vertical Slice & Application Layer Rules

Per [ADR-0001](docs/architecture/adr/ADR-0001_Use_Case_Centric_Vertical_Slice_Architecture.md), [ADR-0002](docs/architecture/adr/ADR-0002_Module_Boundaries_Application_Layer.md), and [ADR-0003](docs/architecture/adr/ADR-0003_local_Application_Service_Orchestrating_Multiple_Domain_Services.md):

- **Handlers are the sole entry points** to the application layer.
- **A handler represents exactly one use case** and contains no domain business logic.
- **Handlers never invoke other handlers.** Composition occurs via domain models or shared application coordinators.
- **Business logic always lives in the Domain Layer** (aggregates and domain services).

```text
domains/shipping/shipment/
├── adapters/
│   ├── id-codec/             # Base32 adapter implementation
│   ├── inbound-web/          # Spring REST controllers & request mappings
│   ├── outbound-jpa/         # Database persistence & Spring Data repositories
│   └── outbound-system/      # External service connectors
├── libs/
│   ├── application/          # Use case handlers (e.g. RegisterParcelHandler)
│   └── domain/               # Parcel, Locker, Courier, Status, Invariants
└── services/
    └── shipment-service/     # Spring Boot application entrypoint
```

### Cross-Cutting Platform (`platform-spring-boot`)

Per `platform-spring-boot/README.md`, all technical, non-business infrastructure is centralized:

- **Security & Auth**: Common `SecurityFilterChain`, JWT decoding/encoding (`DevJwtCfg`), and application extension points via `HttpAuthContributor`.
- **Web & HTTP**: Standardized RFC 7807 problem details (`Errors.kt`), correlation ID filters, CORS, and standard error handling.
- **Observability**: MDC logging filters, Micrometer metric registries, and trace propagation.
- **Invariant**: The platform provides *mechanisms*, not *business rules*. Domain models and use cases must never leak into the platform.

### Shared Libraries (`libs/public-id-codec`)

Implements RFC 4648 Base32 uppercase encoding/decoding without padding ([ADR-0010](docs/architecture/adr/ADR-0010_Public_Private_Identifier_Pattern.md), [ADR-0012](docs/architecture/adr/ADR-0012_Base32_Encoding_Identifiers.md), [ADR-0019](docs/architecture/adr/ADR-0019:_Centralized_PublicId_Library.md)):
- Maps internal 128-bit UUIDv7 identifiers to compact, URL-safe public strings (e.g., `par_AHT4K6RJX2Y66WKMKHE7NDZPDY`).
- Lossless, deterministic, and safe from dictionary scanning or internal database sequence leakage.

### Edge & IoT Ingestion Pipeline

The `domains/shipping/edge-ingestion` execution unit explores handling dual competing workloads:
- **Live Stream**: Continuous high-frequency scanner events arriving from connected devices.
- **Backlog Ingestion**: Massive multi-gigabyte batch event files uploaded when offline warehouse scanners reconnect.
- Explores bounded memory streaming pipelines, backpressure coordination, and preventing edge ingestion from starving the core delivery engine.

---

## 4. Architecture Decision Records (ADRs)

All significant technical and architectural choices are recorded in [docs/architecture/adr/](docs/architecture/adr/):

| ADR | Title | Status | Summary |
| :--- | :--- | :--- | :--- |
| [**ADR-0001**](docs/architecture/adr/ADR-0001_Use_Case_Centric_Vertical_Slice_Architecture.md) | Use Case–Centric Vertical Slice Architecture | Accepted | Enforces handlers as thin entry points; strictly forbids domain logic in handlers. |
| [**ADR-0002**](docs/architecture/adr/ADR-0002_Module_Boundaries_Application_Layer.md) | Module Boundaries for Application Layer | Accepted | Differentiates Use Case Leaf Modules from Shared Application Modules. |
| [**ADR-0003**](docs/architecture/adr/ADR-0003_local_Application_Service_Orchestrating_Multiple_Domain_Services.md) | Application Service Orchestrating Domain Services | Accepted | Governs cross-aggregate orchestration and synchronization. |
| [**ADR-0004**](docs/architecture/adr/ADR-0004_Monorepo_Structure_Based_on_Bounded_Contexts_with_Domain_First_Approach.md) | Monorepo Structure with Bounded Contexts | Accepted | Establishes `domains/<area>/<context>` layout with framework-free domain cores. |
| [**ADR-0005**](docs/architecture/adr/ADR-0005_Module_Taxonomy_System_Execution_Units_Leaf_Modules.md) | Module Taxonomy: System, Execution Units, Leaf Modules | Accepted | Defines three-tier hierarchy (System Root, Execution Unit Aggregates, Leaf Modules). |
| [**ADR-0006**](docs/architecture/adr/ADR-0006_local_Repository_Structure_Gradle_Composite.md) | Repository Structure & Gradle Composite Builds | Accepted | Uses `includeBuild(...)` for independent modular compilation and CI isolation. |
| [**ADR-0010**](docs/architecture/adr/ADR-0010_Public_Private_Identifier_Pattern.md) | Public / Private Identifier Pattern | Accepted | Internal UUIDv7 for DB performance vs external opaque IDs for public APIs. |
| [**ADR-0012**](docs/architecture/adr/ADR-0012_Base32_Encoding_Identifiers.md) | Base32 Encoding for External Identifiers | Accepted | Adopts RFC 4648 uppercase unpadded Base32 for external representation. |
| [**ADR-0016**](docs/architecture/adr/ADR-0016_Standardized_Handling_Time_Timezones_Timestamps.md) | Standardized Handling of Time & Timestamps | Accepted | Mandates UTC reference time, `Instant` internally, and ISO-8601 formatting. |
| [**ADR-0017**](docs/architecture/adr/ADR-0017_Time_Handling_Time_Zone_Strategy.md) | Time Handling & Time Zone Strategy | Accepted | Separates instant in time from local presentation timezones (`X-User-Timezone`). |
| [**ADR-0018**](docs/architecture/adr/ADR-0018_Logging_Strategy_for_High_Throughput.md) | Production Logging Strategy for High Throughput | Accepted | Enforces $O(1)$ lightweight INFO logs; forbids logging collections/DTOs on hot path. |
| [**ADR-0019**](docs/architecture/adr/ADR-0019:_Centralized_PublicId_Library.md) | Centralized PublicId Library | Accepted | Consolidates Base32 identifier encoding in `libs/public-id-codec`. |

---

## 5. Runtime Model, Backpressure & Saturation

### Runtime Control Points

The system exposes explicit tuning levers to control performance dynamics:

- **Queue Capacity**: Bounded buffer limits for incoming events and tasks.
- **Worker Concurrency**: Number of parallel workers per processing stage.
- **Processing Cost**: Artificial or realistic computational cost per unit of work.
- **Resource Limits**: Locker capacities and courier availability.
- **Retry Policy**: Maximum attempts, backoff intervals, and jitter randomization.
- **Admission Policy**: Buffer-and-wait vs load-shedding (fail-fast / HTTP 429).

### Backpressure & Saturation Dynamics

When arrival rate ($\lambda$) exceeds service capacity ($\mu$), unmanaged systems experience catastrophic queue growth and tail latency collapse:

```text
   Arrival Rate > Service Rate (λ > μ)
                   │
                   ▼
             Queue Growth
                   │
                   ▼
      Elevated Queueing Latency (W_q)
                   │
                   ▼
     p99 Latency Spikes (Tail Expansion)
                   │
                   ▼
     Timeouts & Client Retries
                   │
                   ▼
         Retry Storm Amplification
                   │
                   ▼
         Cascading Collapse
```

Using Little's Law ($L = \lambda W$) and queueing models, the platform experiments with:
- **Bounded Queues vs Rejection**: Measuring queueing latency vs immediate drop.
- **Load Shedding**: Rejecting excess work before it degrades in-flight processing.
- **Retry Budgets**: Limiting retry traffic to prevent self-inflicted DDoS.

### Failure Injection

The in-memory event bus and adapters support controlled failure modes without requiring external network chaos tools:
- **Delayed Delivery**: Simulates transient network lag and out-of-order event arrivals.
- **Duplicate Delivery**: Validates idempotency guards and optimistic locking checks.
- **Dropped Messages**: Evaluates delivery timeouts, reconciliation sweeps, and requeue flows.
- **Slow Consumers**: Induces backpressure upstream to test queue overflow policies.

### Time & Scheduling Model

Time is an active participant in system behavior:
- All timestamps internally represent absolute instants (`Instant`) in UTC ([ADR-0016](docs/architecture/adr/ADR-0016_Standardized_Handling_Time_Timezones_Timestamps.md)).
- Resource commitments are modeled explicitly:
  ```kotlin
  courier.busyUntil = now.plus(deliveryDuration)
  ```
- Generates resource contention, backlog waves, and SLA pressure predictably and reproducibly.

---

## 6. Performance Engineering & Benchmarking

### The 12 Performance Principles

Derived from [perf/k6/README.md](perf/k6/README.md), all load testing and profiling follow strict rules:

1. **Single Responsibility of Load**: Each test answers exactly one question.
2. **Explicit Regimes Principle**: Distinguish warm vs cold, cached vs uncached, GC pressure vs no pressure.
3. **Steady-State First, Transients Separate**: Warm up $\to$ stabilize $\to$ measure $\to$ cool down.
4. **Distribution, Not Point Estimates**: Evaluate $p50$, $p95$, $p99$, and variance—never rely solely on averages.
5. **Tail-Latency is a First-Class Citizen**: Optimize for the tail percentiles where SLO violations occur.
6. **Backpressure Integrity**: Test with explicit queue bounds, timeouts, and circuit breakers.
7. **Capacity Invariants**: Apply queueing laws (e.g. Little's Law) as sanity checks.
8. **Control the Controllables**: Fix JVM flags, heap size, CPU pinning, and workload parameters.
9. **Observability-Driven Benchmarking**: Every hypothesis must be verifiable via metrics or profiling.
10. **No Coordinated Omission**: Ensure load generators sustain target RPS even during server pauses.
11. **Reproducibility Budget**: Run repeated baselines; discard tests with high coefficient of variation.
12. **Change with Characterization, Not Hope**: Validate improvements against the full latency distribution.

### Active Research Hypotheses

| Hypothesis | Subject | Description |
| :--- | :--- | :--- |
| [**HYP-001**](docs/research/HYP-001-tail-latency-driven-gc-pressure/HYP-001-tail-latency-driven-allocation-rate-gc-pressure.md) | **Allocation Rate & GC Pressure** | Demonstrates that high allocation rates (DTO churn, object mapping) drive young GC pause clustering, which directly causes tail latency ($p95/p99$) spikes even under low CPU utilization. |
| [**HYP-002**](docs/research/HYP-002-security-filter-cpu/HYP-0001-security-filter-cpu.md) | **Security Filter CPU Overhead** | Investigates CPU cost, thread blocking, and latency overhead introduced by servlet security filter chains and cryptographic JWT verification under high RPS. |
| [**HYP-00X**](docs/research/HYP-00X-silent-exception-swallowing/HYP-001-Silent-Exception-Swallowing.md) | **Exception Handling & Flow Control** | Evaluates JVM stack-trace construction costs and the behavioral impact of swallowed exceptions on downstream latency. |

### Experiment Scenarios

- **Steady Load**: Measure baseline latency and allocation rates well below capacity.
- **Ramp to Saturation**: Incrementally increase load to pinpoint the throughput saturation knee.
- **Burst Load**: Deliver instantaneous load spikes to observe queue drain times and memory headroom.
- **Queue Policy**: Compare large buffered queues against bounded queues with immediate rejection.
- **Retry Storm**: Introduce downstream failures and quantify amplification factors.

---

## 7. Observability & Telemetry

Observability is engineered into the architecture from the bottom up:

```text
┌─────────────────────────────────────────────────────────────┐
│                    Application Signals                      │
│ Throughput (RPS) • p50/p95/p99 Latency • Queue Depth • WIP  │
│  Service Time • Event Lag • Retries • Rejections • Errors   │
├─────────────────────────────────────────────────────────────┤
│                        JVM Signals                          │
│ Allocation Rate (MB/s) • Young/Old GC Pauses • Heap Churn   │
│    Safepoint Pauses • Thread States (WAITING/RUNNABLE)      │
├─────────────────────────────────────────────────────────────┤
│                     Operating System                        │
│    CPU Saturation • Context Switches • Socket Backlogs      │
│          TCP Retransmits • Connection Pool Queues           │
└─────────────────────────────────────────────────────────────┘
```

### Diagnostics Tooling

- **k6**: Load test execution engine with custom threshold scripts (`perf/k6/`).
- **Java Flight Recorder (JFR)**: Low-overhead JVM profiling recording CPU samples, memory allocations, and lock contention.
- **JDK Mission Control (JMC)**: Detailed offline analysis of `.jfr` recordings.
- **async-profiler (`asprof`)**: High-accuracy CPU and allocation flamegraphs.
- **Prometheus & Grafana**: Time-series metrics dashboards ([infra/compose/](infra/compose/)).
- **Portainer**: Lightweight container monitoring and management.

---

## 8. Repository Structure

```text
perf-lab/
├── clients/
│   └── web/                         # React + Vite + TypeScript dashboard
├── contracts/
│   └── http/openapi/                # OpenAPI specifications (e.g. ops-tracking.v1.yaml)
├── docs/
│   ├── architecture/
│   │   ├── adr/                     # Architecture Decision Records (ADR-0001 to ADR-0019)
│   │   └── diagrams/                # C1/C2 architectural diagrams
│   ├── overview/                    # High-level context & system summary
│   └── research/                    # Hypothesis-Driven Engineering lab docs & results
├── domains/
│   └── shipping/
│       ├── shipment/                # Execution unit: Parcel lifecycle & delivery
│       │   ├── adapters/            # Web REST, JPA persistence, system adapters
│       │   ├── libs/
│       │   │   ├── application/     # Use case handlers & orchestration
│       │   │   └── domain/          # Parcel aggregate, Locker, Courier, invariants
│       │   └── services/
│       │       └── shipment-service/# Spring Boot runtime application
│       └── edge-ingestion/          # Execution unit: High-volume scanner & IoT ingestion
├── infra/
│   └── compose/                     # Docker Compose (Postgres, Prometheus, Grafana)
├── libs/
│   └── public-id-codec/             # Shared RFC 4648 Base32 public identifier codec
├── perf/
│   └── k6/                          # Performance scenarios, baselines, and smoke tests
├── platform-spring-boot/            # Shared JVM platform (Security, Auth, Web, Errors)
├── tests/
│   └── api/                         # End-to-end API verification scripts
├── Makefile                         # Convenient workflow targets
├── build.gradle.kts                 # Composite root build script
└── settings.gradle.kts              # Root composite configuration (includeBuild)
```

---

## 9. Quickstart & Operational Guide

### Prerequisites

- **Java Development Kit (JDK)**: 21 or 24 (recommended: Eclipse Adoptium / Temurin)
- **Docker & Docker Compose**: For backing services (PostgreSQL, Prometheus, Grafana)
- **k6**: For running load testing scripts
- **Make**: For running test automation shortcuts

### Building the System

The project uses a Gradle Composite build. Build all modules from the root:

```bash
./gradlew clean buildAll
```

### Running Infrastructure

Start PostgreSQL, Prometheus, and Grafana via Docker Compose:

```bash
docker compose -f infra/compose/docker-compose.yml --profile core --profile obs up -d --build
```

- **Postgres**: `localhost:5432` (db: `app`, user: `app`, pass: `pass`)
- **Prometheus**: `http://localhost:9090`
- **Grafana**: `http://localhost:3000`

### Starting the Application

Launch the `shipment-service` with the `dev` profile enabled:

```bash
./gradlew :parcel:services:parcel-service:bootRun --args='--spring.profiles.active=dev'
```

Verify service liveness:

```bash
curl -v http://localhost:8080/api/ping
```

### Executing Tests & Benchmarks

Run the API test suite:

```bash
make test-api-parcel AUTH_TOKEN="<DEV_JWT_TOKEN>" BASE_URL="http://localhost:8080"
```

Execute a k6 smoke test:

```bash
k6 run perf/k6/smoke.js
```

Execute an allocation-pressure baseline experiment:

```bash
k6 run perf/k6/EXP-001_HYP-001-tail-latency-driven-gc-pressure/exp0001_hyp0001_allocation_rate_baseline.js
```

### Profiling with async-profiler & JFR

Capture a 30-second CPU flamegraph using `asprof`:

```bash
asprof collect -e cpu -d 30 -o flamegraph --color=java -f flame.html <PID>
```

Open a captured Java Flight Recorder baseline in JDK Mission Control:

```bash
jmc --open baseline.jfr
```

---

## 10. What This Project Is Not

This project is deliberately **not** an attempt to construct a commercial parcel carrier platform. It explicitly avoids:

- Realistic geographic routing, map computations, and road networks
- Dozens of arbitrary business fields without behavioral consequences
- Sprawling microservice counts without concrete failure-mode motivations
- Infrastructure added solely for aesthetic architectural complexity

The objective remains focused: **build the leanest system capable of exhibiting production-scale distributed runtime behavior, and study that behavior through empirical performance engineering.**