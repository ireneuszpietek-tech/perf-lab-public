ADR-00Z: Use Case–Centric Vertical Slice Architecture and Role of Application Layer Components

Status: Accepted
Date: 2025-01-XX
Supersedes / Related: ADR-003, ADR-00Y, ADR-003, ADR-0001
Context: Bounded Context Architecture, Execution Units, Hexagonal Architecture, Modular Monolith

cd 
A handler is the only Application Layer entrypoint.

A handler contains no business logic.

Responsibilities:

load aggregates,

orchestrate domain operations,

call outbound ports,

persist state,

publish domain/application events.

3. Shared logic in Application Layer is extracted into application components (helpers/coordinators)

Application helper classes encapsulate reusable orchestration logic.

These are not use cases and must not be called “application services.”

They may be shared across multiple use case handlers within the same bounded context.

They may coordinate multiple domain services or port calls.

4. Business logic (“meat”) always lives in the Domain Layer

Aggregates model invariants and core rules.

Domain services encapsulate pure domain logic that spans aggregates.

No domain rules may be placed in handlers or shared application components.

5. Handlers never call other handlers

Composition of logic must go through:

the domain model,

application helpers,

or orchestrators/process managers (if asynchronous).

6. Application Layer structure uses vertical slices + application/shared

Example:

application/
importx/
ImportXCommand.kt
ImportXHandler.kt
importy/
ImportYCommand.kt
ImportYHandler.kt
importall/
ImportAllCommand.kt
ImportAllHandler.kt
shared/
ImportEngine.kt

7. Vertical slice boundaries follow the naming and taxonomy rules of execution units and leaf modules (ADR-00Y)

Each slice corresponds to one leaf module.

Shared application components remain in shared submodules.

Consequences
Positive

Clear separation of responsibilities in Application Layer.

Strong alignment with execution-unit taxonomy.

Reduced duplication between related use cases.

Thinner handlers, richer domain models.

Easier onboarding and reasoning about system structure.

Prevention of cyclic dependencies between use cases.

Negative / Trade-offs

More packages/modules inside a bounded context.

Risk of creating overly complex shared components if not monitored.

Status and Migration

This ADR applies to all new bounded contexts and use cases.
Existing contexts may migrate incrementally as they evolve.