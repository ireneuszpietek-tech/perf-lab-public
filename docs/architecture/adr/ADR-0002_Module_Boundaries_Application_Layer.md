ADR-00W: Module Boundaries for Application Layer (Handlers, Shared Components, Workflow Engines)

Status: Accepted
Date: 2025-01-XX
Related: ADR-00Y (Module Taxonomy), ADR-003 (Orchestration), ADR-00Z (Vertical Slice Architecture)

Context

The Application Layer must support:

clear execution unit boundaries,

vertical slicing by use case,

shared orchestration logic,

modular monolith constraints,

domain-first semantics.

While Domain Layer boundaries are well defined, Application Layer boundaries are currently implicit, leading to:

inconsistent placement of shared logic,

mixing technical workflows with use case responsibilities,

unclear distinction between handlers, application helpers, and domain services,

duplicated use-case structure across modules,

unnecessary coupling between handlers.

To maintain architectural clarity and module independence, Application Layer boundaries require explicit definition.

Decision
1. Application Layer consists of two types of modules

Use Case Modules (Leaf Modules)

Correspond to execution units (ADR-00Y).

Include:

command/query,

handler,

local DTOs/validators,

slice-specific dependencies.

Must not contain shared logic.

Shared Application Modules

Contain reusable application-level components:

workflow coordinators,

helper services,

synchronous orchestrators,

task runners,

application-level policies.

May be used by multiple use case modules within the same bounded context.

2. Handler boundaries

A handler represents exactly one use case.

It is a thin orchestration layer, delegating core logic to:

aggregates,

domain services,

or shared application components.

Handlers may depend on:

outbound ports,

domain model,

shared application modules,

repositories (ports).

Handlers must not:

contain business logic,

call other handlers,

contain workflow engines.

3. Shared Application Components

Shared logic must be modeled as:

application helpers (sync),

workflow engines (sync),

process managers/sagas (async),

import/coordinator services for multi-step flows.

They address concerns that are:

multi-step but synchronous,

technical or orchestration-based,

required by multiple use cases.

They must remain free of domain business logic.

4. Directory/Module Structure

Inside a bounded context:

application/
  usecaseA/
    UsecaseACommand.kt
    UsecaseAHandler.kt
  usecaseB/
    UsecaseBCommand.kt
    UsecaseBHandler.kt
  usecaseC/
    UsecaseCCommand.kt
    UsecaseCHandler.kt
  shared/
    WorkflowEngine.kt
    ImportCoordinator.kt
    ApplicationPolicies.kt


Domain Layer remains separate:

domain/
  aggregates/
  valueobjects/
  domainservices/
  events/
  ports/


Infrastructure via adapters:

infrastructure/
  adapters/
  repositories/
  gateways/

Consequences
Positive

Clear structural separation inside Application Layer.

Consistent reuse of shared logic without polluting use case modules.

Prevention of “mega application services.”

Cleaner vertical slices and easier testing.

Stronger alignment with execution-unit taxonomy (ADR-00Y).

Negative

Slight increase in number of modules/subpackages.

Requires architectural discipline to keep shared modules small and focused.

Migration Strategy

New use cases must follow this structure immediately.

Legacy modules can be gradually migrated by extracting shared logic into application/shared and reducing handler size.

Domain logic should be relocated from Application Layer where applicable.