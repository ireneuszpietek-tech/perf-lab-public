# ADR-00Y — Module Taxonomy: System, Execution Units, Leaf Modules

## Status
Accepted

## Context

The repository is a **modular monolith** built as a Gradle composite (see ADR-00X).
We want the physical layout of modules to reflect architectural concepts such as:

- system-level scope,
- bounded contexts and execution units (features that can become microservices),
- internal technical layering (domain, application, adapters, runtime).

Without a clear taxonomy, all Gradle projects look “flat”, which makes it hard to
understand the architecture and navigate the codebase (especially for new contributors).

## Decision

We distinguish **three conceptual levels of modules**:

1. **System-level / monorepo root**

   - Example: `logistics-core-system` (this repository).
   - Represents the whole system and composite Gradle build.
   - Provides high-level tasks for CI (e.g. `buildAll`).

2. **Execution units / feature aggregates**

   - Example: `domains/shipping/parcel/`.
   - Represent a **feature-level execution unit** within a bounded context
     (something that could become a standalone microservice in the future).
   - Exposed as a single Gradle project, e.g. `:domains:shipping:parcel`.
   - The aggregate project contains no source code; it only aggregates
     internal leaf modules via `api(project(...))` dependencies.

3. **Technical leaf modules**

   - Example:
     - `:domains:shipping:parcel-domain`
     - `:domains:shipping:parcel-application`
     - `:domains:shipping:parcel-inbound-web`
     - `:domains:shipping:parcel-service`
   - These are **internal building blocks** of an execution unit:
     - `*-domain` — domain model, aggregates, value objects, domain services
     - `*-application` — application-level contracts / use cases
     - `*-inbound-*` / `*-outbound-*` — adapters
     - `*-service` / `*-app` — runtime / Spring Boot application with `main()`
   - Leaf modules are not meant to be used directly from other execution units.

### Naming convention

- Aggregate (execution unit) projects use a **short name without technical suffixes**, e.g.:
  - `:domains:shipping:parcel`

- Leaf projects use the aggregate name plus a suffix, e.g.:
  - `:domains:shipping:parcel-domain`
  - `:domains:shipping:parcel-application`
  - `:domains:shipping:parcel-inbound-web`
  - `:domains:shipping:parcel-service`

### Dependency rule

- Other execution units (and higher-level modules) **MUST depend only on**
  the aggregate project (e.g. `:domains:shipping:parcel`), and **MUST NOT depend**
  directly on its leaf modules (`parcel-domain`, `parcel-application`, ...).

This enforces encapsulation: the aggregate represents the **public API** of the
execution unit, while leaf modules form its internal structure.

## Rationale

- The taxonomy matches the architectural language: system → bounded context /
  execution unit → internal layers.
- Aggregate modules provide a clear “unit of ownership” and a future microservice
  boundary, without forcing microservices today.
- Leaf modules keep technical concerns separated (domain vs application vs adapters),
  while still being encapsulated behind an aggregate.
- The naming convention makes it immediately clear whether a project is:
  - a system root,
  - an execution unit,
  - or an internal technical module.

## Consequences

### Positive

- New contributors can quickly understand:
  - what is “the system”,
  - what are the feature-level execution units,
  - what are the internal technical layers.
- Execution units can be promoted to separate microservices with minimal refactoring.
- Dependencies remain aligned with architecture (no direct coupling to internal leaf modules).

### Negative

- Slightly more Gradle projects to manage.
- Developers need to understand the difference between aggregate and leaf modules.
