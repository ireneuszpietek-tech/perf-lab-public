# `parcel` – module inside the `shipping` bounded context

The **`parcel`** module is part of the **`shipping` bounded context** in the  
`logistics-core-system` project (a modular monolith for experiments, R&D, and performance engineering).

It is responsible for the **business logic of an individual parcel** within shipping processes:
creation, validation, status transitions, basic business rules, and integration points with
other parts of the `shipping` BC.

---

## Domain context

- **System:** `logistics-core-system`
- **Bounded Context (BC):** `shipping`
- **Domain module:** `parcel`

The `parcel` module is treated as an **execution unit** and a potential **future microservice**,
but currently lives inside a **modular monolith**.

Core responsibilities include:

- representing a parcel as a **domain aggregate**
- basic rules:
  - creating a parcel for a shipping order
  - validating parameters (weight, dimensions, type, etc.)
  - managing status transitions (CREATED, REGISTERED, IN_TRANSIT, DELIVERED, RETURNED, …)
- emitting domain events (e.g. `ParcelCreated`, `ParcelDelivered`)
- exposing application-level APIs (ports) for other parts of the `shipping` BC

---

## Directory structure

Module location:

```text
domains/
  shipping/
    parcel/
      adapters/
      libs/
        application/
        domain/
      services/
Responsibility of each directory:

adapters/
Adapter layer (hexagonal / ports & adapters):

REST controllers and HTTP endpoints

persistence adapters (e.g. JPA repositories, in-memory repositories)

messaging adapters (if/when outbound events are added)

translation between the external world and libs/ + services/

Example (conceptually):

text
Copy code
adapters/
  rest/
  persistence/
  messaging/
libs/
Reusable library code of the parcel module – domain and application-level APIs
that should be usable from other modules and/or runtimes.

Current substructure:

text
Copy code
libs/
  application/  // application-level contracts / DTOs / facades
  domain/       // domain model (aggregates, value objects, domain events)
Suggested responsibilities:

libs/domain:

domain model (Parcel aggregate, value objects, enums)

domain events

domain-level interfaces (e.g. ParcelRepository, ParcelEventPublisher)

libs/application:

application-level contracts exposed to other modules (facades, DTOs, commands/queries)

interfaces representing use-cases at the application boundary

services/
Application services / orchestration layer – where use-cases are implemented:

application services orchestrating domain logic and adapters

command/query handlers (depending on style)

scenario-level logic (e.g. “create parcel and initialize tracking”)

Example:

text
Copy code
services/
  ParcelApplicationService.kt
  ParcelQueryService.kt
Package convention
Recommended base package:

kotlin
Copy code
package ip.logistics.shipping.parcel
Example package layout:

text
Copy code
ip.logistics.shipping.parcel.domain        // domain model, value objects, aggregates (libs/domain)
ip.logistics.shipping.parcel.application   // application-level contracts & services (libs/application, services/)
ip.logistics.shipping.parcel.adapters      // controllers, persistence, messaging (adapters/)
Depending on how strictly you separate layers, you may choose a more fine-grained structure:

text
Copy code
ip.logistics.shipping.parcel.domain.model
ip.logistics.shipping.parcel.domain.event
ip.logistics.shipping.parcel.application.service
ip.logistics.shipping.parcel.adapters.rest
ip.logistics.shipping.parcel.adapters.persistence
Integration with other modules
In the modular monolith, other shipping modules can use parcel via:

its domain-level contracts (libs/domain)

its application-level contracts (libs/application), depending on your chosen boundaries

Example Gradle dependency (if parcel is a dedicated Gradle project):

kotlin
Copy code
dependencies {
    implementation(project(":domains:shipping:parcel"))
}
Classes in libs/ and services/ should be designed so that:

they are stable candidates for later extraction into an independent service,

they do not leak infrastructure details from adapters/.

Entry points
When exploring the module, good starting points are:

Domain model (libs/domain):

Parcel aggregate

value objects (weight, dimensions, identifiers, etc.)

status enums and type hierarchies

Application services (services/):

main use-cases for working with parcels (create, update, status transitions, etc.)

Adapters (adapters/):

REST controllers (e.g. ParcelController)

mapping between HTTP payloads and domain/application objects

Testing
Suggested testing approach:

Domain tests (libs/domain):

unit tests of Parcel and domain services

Application tests (services/):

use-case / scenario tests at application-service level

Integration tests (adapters/):

REST + persistence + domain integration

Roadmap / potential evolution
Possible evolutions of the parcel module inside the shipping BC:

richer parcel lifecycle and exception handling (damaged, lost, returned workflows)

deeper integration with:

routing (route assignment for parcels)

tracking (parcel event timeline)

exposing a more stable public API (in libs/application) for other BCs

promoting parcel to a separate runtime (own app / microservice) without
breaking domain/application contracts

Status
The parcel module is part of the modular monolith logistics-core-system,
inside the shipping bounded context. It acts as an execution unit responsible
for the lifecycle and business rules of individual parcels within shipping flows.