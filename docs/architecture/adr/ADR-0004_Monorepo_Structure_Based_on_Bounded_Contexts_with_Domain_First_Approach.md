# ADR-0001: Monorepo Structure Based on Bounded Contexts with a Domain-First Approach

- Status: ACCEPTED  
- Date: 2025-11-15  
- Author: IP

## Context

We are building an R&D repository (`perf-lab`) following Domain-Driven Design (DDD) and Hexagonal Architecture.  
Our goals:

- isolate **domain logic** from frameworks and infrastructure,
- support easy experimentation with multiple adapters (web, JPA, external systems),
- maintain a clear, scalable structure for many **bounded contexts** inside a single monorepo.

Example bounded context: `shipping/parcel`:

```text
perf-lab
 └─ domains
    └─ shipping
       └─ parcel
          ├─ adapters
          │   ├─ inbound-web
          │   ├─ outbound-jpa
          │   └─ outbound-system
          ├─ libs
          │   ├─ application
          │   └─ domain
          └─ services
              └─ parcel-service
              
We need to document why this structure exists and the architectural rules behind it.
```

Decision
We adopt the following architectural principles:

1. Bounded Context as the Primary Logical Module
Each domain area lives under domains/, for example:

domains/shipping/parcel

future: domains/billing/invoice, domains/customer/profile, etc.

Each folder represents a fully self-contained bounded context with its own domain, application layer, and adapters.

2. Domain-First
Inside every bounded context, the domain (libs/domain) is the source of truth.

The domain has no dependencies on frameworks (Spring, JPA, etc.).

It contains:
aggregates, entities, value objects,
business rules and policies,
domain events.

3. Application Layer as Use-Case Orchestration
libs/application contains:
use cases,
inbound ports,
outbound ports,
transactional/application logic (framework-agnostic).
application depends on domain, but not the other way around.

4. Adapters as Port Implementations
adapters includes the Hexagonal adapters:
inbound-web – REST controllers / APIs,
outbound-jpa – persistence / ORM mappings,
outbound-system – external integrations, messaging, APIs.
Adapters depend on technologies and implement outbound ports from application.

5. Service Layer as the Runtime Entry Point
services/parcel-service contains:
the actual Spring Boot app,
DI configuration,
infrastructure setup.
This module composes domain, application, and any required adapters.

6. Monorepo as an Aggregate of Contexts
The monorepo structure allows:
logical and build-level isolation of bounded contexts,
shared tooling (build-logic, contracts, clients),
running each bounded context’s service independently.

Rationale
Clear Mapping to DDD/Hex Patterns
The structure makes responsibilities explicit:
“Where is the domain logic?” → libs/domain
“Where is the use case?” → libs/application
“Where is the web entry point?” → adapters/inbound-web
“Where is the DB integration?” → adapters/outbound-jpa

Clean Separation of Business Logic
Domain logic remains testable without Spring or databases.
Technology changes (e.g., switching from JPA to raw SQL or event sourcing) do not affect the domain.
Easy Scaling to Many Bounded Contexts
The structure is template-friendly: new bounded contexts can reuse the same layout.
Changes inside one context do not affect others.
Designed for R&D Experimentation
Adapters can be swapped or duplicated for experiments.

Domain and application layers stay stable even when infrastructure changes constantly.

Consequences
Positive
Highly maintainable domain logic – clean, focused, and easy to test.

Clear separation of concerns – each folder has a well-defined purpose.

Parallel development is easy – teams can work on different contexts independently.

Production-ready foundation if the R&D initiative evolves into a real product.

Negative
Higher learning curve for developers used to simple controller-service-repository Spring Boot structures.

More files and directories even for small features.

Requires discipline to prevent domain leakage into adapters or technology leaking into the domain.

Alternatives Considered
1. Classic Spring Boot Structure (controller-service-repository)
Pros: easier onboarding, fewer modules, quick prototyping.

Cons: harder to scale, domain gets tangled with framework details, leads to a “big ball of mud.”

2. One Repository per Bounded Context
Pros: maximum isolation.

Cons: harder collaboration in R&D, duplicated contracts/clients, more CI/CD overhead.

We chose the current structure as the best balance between DDD/Hex purity and practical monorepo-driven R&D.

Guidance for Future Development
New bounded contexts must follow the same structure under domains/<area>/<context>.

New features should:

start with domain changes,

then update the application layer,

and finally adjust or create adapters.

Any unusual architectural choice must be described as a separate ADR (e.g., ADR-0002-event-sourcing-for-parcel.md).

