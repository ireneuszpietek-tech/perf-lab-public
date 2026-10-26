# ADR-00X — Repository Structure and Use of Gradle Composite Builds

## Status
Accepted

## Context

The repository is a **modular monolith** organized as a monorepo. It contains multiple logical components, for example:

- **Domains / bounded contexts**, e.g. `domains/shipping/parcel`
- **Adapters**, e.g. `adapters/inbound-web`, `adapters/outbound-jpa`
- **Services / runtime applications**, e.g. `services/parcel-service`
- **Shared libraries**, e.g. `libs/public-id-codec`
- **Platform modules / technical infrastructure**

These components:

- belong to different bounded contexts,
- encapsulate different technical layers (domain, application, adapters, runtime),
- should remain:
    - buildable **independently**,
    - testable **in isolation**,
    - but still live in a single monorepo for a unified developer and CI experience.

We explicitly want to:

- keep **clear domain and execution-unit boundaries**,
- avoid a single huge Gradle multi-project build that couples everything together,
- avoid splitting everything into separate Git repositories (which would introduce versioning and operational overhead).

We need a structure that:

- maintains architectural boundaries (domains, execution units),
- supports modular monolith development,
- and remains convenient for local development and CI/CD.

## Decision

The repository is structured as a **Gradle composite build**, consisting of **multiple independent builds**, each with its own:

- `settings.gradle.kts`
- `build.gradle.kts`
- internal subprojects (e.g. domain, application, adapters, runtime)

At the top level, we introduce a **composite root build** (currently `logistics-core-system/`) which:

1. Applies only the minimal `base` plugin to obtain standard lifecycle tasks (`build`, `clean`).
2. Uses **`includeBuild(...)`** to assemble multiple independent builds into a single composite.
3. Provides aggregator tasks such as `buildAll` to orchestrate building and testing all components.
4. Serves as the unified entry point for developers and CI pipelines.

High-level directory structure (conceptual):

```text
logistics-core-system/             ← composite root build (system-level)
  settings.gradle.kts
  build.gradle.kts                 ← includes other builds via includeBuild(...)

  domains/
    shipping/
      parcel/                      ← independent Gradle build (execution unit aggregate)
        settings.gradle.kts
        build.gradle.kts
        parcel-domain/             ← technical leaf modules
        parcel-application/
        parcel-inbound-web/
        parcel-service/

  libs/
    public-id-codec/               ← independent Gradle build (shared library)
      settings.gradle.kts
      build.gradle.kts

  platform/                        ← independent Gradle build (platform / infra)
    settings.gradle.kts
    build.gradle.kts
    ...
Each independent Gradle build (e.g. domains/shipping/parcel, libs/public-id-codec, platform/) is free to define its own internal structure and subprojects, including:

execution-unit aggregates (e.g. a parcel aggregate project that re-exports its internal modules),

technical leaf modules (*-domain, *-application, *-inbound-web, *-service, etc.).

The composite root (logistics-core-system) does not define domain logic; it only coordinates builds.

Rationale
This setup provides several key benefits:

Clear architectural boundaries
Each domain / execution unit / library is an independent Gradle build, aligned with architectural boundaries.

Execution units (e.g. domains/shipping/parcel) can model their internal layers via separate subprojects (parcel-domain, parcel-application, parcel-service, etc.) and encapsulate them behind an aggregate if needed.

Independent development
Developers can run ./gradlew build inside any independent build (e.g. domains/shipping/parcel/) without touching the entire system.

This supports fast feedback cycles and local development focused on a single bounded context or feature.

Simplified ownership model
Teams (or future logical owners) can maintain their areas independently:

domains (e.g. shipping/parcel),

shared libraries (e.g. public-id-codec),

platform modules,

without being forced into a single global multi-project configuration.

Unified CI and tooling
The composite root provides a single entry point:

./gradlew build or a dedicated buildAll task builds and tests the whole system.

Tooling and plugin versions remain consistent across builds while preserving isolation.

Predictable configuration and scalability
Each independent build controls its own settings.gradle.kts and subprojects; the composite root only coordinates them via includeBuild(...).

New domains, execution units, or libraries can be added simply by introducing a new independent build and registering it in the composite root.

Considered Alternatives
1. A single large multi-project Gradle build
Rejected.

Blurs domain and execution-unit boundaries.

Increases configuration complexity.

Makes it easy to accidentally introduce cross-module coupling.

Slows down builds and complicates ownership as the system grows.

2. Separate Git repositories per domain/library
Rejected.

Adds operational overhead (multi-repo management, versioning, branching).

Makes cross-domain refactoring harder.

Complicates local development and CI orchestration.

3. Composite build inside a single monorepo (chosen)
Accepted.

Combines the benefits of:

strong architectural separation (independent builds),

and unified tooling/CI (one monorepo, one composite root).

Works naturally with a modular monolith:

system-level monorepo root,

domain-level / execution-unit builds (e.g. domains/shipping/parcel),

internal technical leaf modules inside each execution unit.

Consequences
Positive
Strong domain and execution-unit separation aligned with architecture.

Local, isolated builds enable faster feedback cycles and clearer ownership.

The composite root (logistics-core-system) simplifies building and testing the whole system from a single entry point.

Structure scales well as new domains, execution units, and shared libraries are added.

The model is compatible with future extraction of execution units into standalone microservices.

Negative
More complex Gradle topology (multiple independent builds, multiple settings.gradle.kts).

Developers must understand composite build mechanics and the distinction between:

system-level root,

execution-unit aggregates,

internal technical leaf modules.

Cross-build dependency management must be handled carefully (and documented) to avoid accidental coupling.