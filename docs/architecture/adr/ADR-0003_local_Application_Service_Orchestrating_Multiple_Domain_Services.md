# ADR-003: Application Service Orchestrating Multiple Domain Services

**Status:** Accepted  
**Date:** 2025-11-02  
**Author:** IP  
**Related:**  
- ADR-001: Opaque Identifier Pattern  
- ADR-002: Use Base32 Encoding for External Identifiers  
**Supersedes:** none  

---

## Context
As the domain model of the Shipping bounded context evolves, certain use cases (e.g. parcel creation, shipping workflow, label generation, event publication) require coordination across multiple **Domain Services** and **Aggregates**.
Direct orchestration from REST controllers or messaging adapters leads to:

- duplicated transactional logic,  
- tight coupling between infrastructure and domain logic,  
- difficulty in testing orchestration flows without HTTP or persistence layers.

We need a clear architectural boundary between:
- **Application layer (use cases, orchestration)** — defining what the system *does*,  
- **Domain layer (business logic, invariants)** — defining *how it behaves*.

This decision clarifies the role of **Application Services** in our hexagonal / DDD-based architecture.

---

## Decision
We will introduce explicit **Application Services** (a.k.a. *Use Case Services*) as the entry points into the domain layer.

Each Application Service:
1. Implements a specific **use case** — e.g. `CreateParcelUseCase`, `ShipParcelUseCase`, `GetParcelUseCase`.  
2. **Orchestrates multiple Domain Services and Aggregates** to achieve the business goal.  
3. Exposes a stable **input model** (command or query) and returns an **output DTO** or domain object.  
4. Runs within a single transactional boundary.  
5. Does **not** contain business rules itself — those remain in the domain model.  
6. Can be invoked from multiple adapters (REST, messaging, CLI, etc.) without code duplication.

### Example (Kotlin)

```kotlin
@Service
class ParcelService(
    private val parcelRepository: ParcelRepository,
    private val pricingService: PricingDomainService,
    private val trackingService: TrackingDomainService,
    private val eventPublisher: DomainEventPublisher
) : CreateParcelUseCase, GetParcelUseCase {

    @Transactional
    override fun create(cmd: CreateParcelCommand): ParcelId {
        val price = pricingService.calculatePrice(cmd.from, cmd.to)
        val parcel = Parcel.create(cmd.from, cmd.to, price)
        parcelRepository.save(parcel)
        eventPublisher.publish(parcel.domainEvents)
        return parcel.id
    }

    override fun get(id: ParcelId): Parcel =
        parcelRepository.findByIdOrThrow(id)
}
```

The REST adapter or message handler delegates directly to the Application Service:

```kotlin
@RestController
@RequestMapping("/api/shipping/v1/parcels")
class ParcelController(
    private val createParcelUseCase: CreateParcelUseCase,
    private val getParcelUseCase: GetParcelUseCase
) {
    @PostMapping
    fun create(@RequestBody req: CreateParcelRequestDto): ResponseEntity<ParcelStatusResponseDto> {
        val id = createParcelUseCase.create(req.toCommand())
        return ResponseEntity.created(URI.create("/api/shipping/v1/parcels/${id}"))
            .body(ParcelStatusResponseDto(id))
    }
}
```

### Consequences

✅ Pros
- Separation of Concerns — Adapters (REST, messaging) remain thin, focusing only on I/O.
- Testability — Use cases can be tested independently from controllers or persistence.
- Explicit Transaction Boundaries — Each Application Service defines its own transactional scope.
- Reusability — The same service can be reused for REST, GraphQL, or Kafka event flows.
- Alignment with Hexagonal Architecture — Application layer forms the boundary between infrastructure and domain.

⚠️ Cons
- Slightly more boilerplate (interfaces and classes per use case).
- Risk of “anemic” application services if domain logic leaks upward.
- Requires discipline to keep orchestration out of controllers and domain logic out of services.

### Alternatives

Option	Pros	Cons
Direct orchestration in controllers	Simpler for small CRUD apps	Violates hexagonal layering, hard to test, duplicates logic
Domain Services handle orchestration	Pure domain-driven logic	Forces domain to know about transactions and infrastructure
Single monolithic Application Service	Centralized control	Breaks modularity, poor scalability of team ownership

Consequences for Hexagonal Design
Application Services sit inside the hexagon (core),
Domain and Repository ports are driven from them.

Adapters (e.g. REST controllers) are outside the hexagon,
invoking Application Services through ports.

Each port in app.port.in is an interface (e.g. CreateParcelUseCase),
implemented in app.command or app.query packages.

### Review
Revisit this decision if:
- the number of use cases grows excessively and code reuse becomes difficult,
- we move toward CQRS with separate command/query pipelines,
- or the orchestration moves into a workflow engine (e.g. Camunda, Temporal).

References
- Domain-Driven Design: Tackling Complexity in the Heart of Software — Eric Evans
- Implementing Domain-Driven Design — Vaughn Vernon
- Hexagonal Architecture — Alistair Cockburn
- ADR-001: Opaque Identifier Pattern
- ADR-002: Use Base32 Encoding for External Identifiers