# ADR-0010: Public / Private Identifier Pattern (Internal / External ID Separation)

**Status:** Accepted  
**Date:** 2025-10-11  
**Author:** IP
---

## Context
We are designing a system based on **Domain-Driven Design (DDD)** and **CQRS** principles.  
Each aggregate requires a stable identifier that serves two distinct purposes:

- an **internal (private)** identifier used inside the domain, repositories, and databases,
- and a **public (external)** identifier exposed in APIs, integration events, or URLs.

Using the same representation for internal and externally exposed identifiers couples internal identifier choices to public contracts,
making evolution and refactoring risky.
---

## Decision
Adopt the **Public / Private Identifier Pattern**  
(also known as *Opaque Identifier Pattern* or *Dual-Identifier Model*).

### Internal (Private) ID
- Type: UUIDv7 (or time-based UUID)
- Used within the domain model and database as the aggregate key
- Invisible to external systems
- Example: `ParcelId(UUID)`

### External (Public) ID
- Type: opaque string (e.g., Base32-encoded UUID)
- Exposed in REST APIs and integration events
- Encodes or maps deterministically to internal ID
- Example: `parcel-1-AHT4K6RJX2Y66WKMKHE7NDZPDY`

This pattern decouples **internal identifiers** from **public contracts**, enabling safer evolution and refactoring.

---

## Example

```kotlin
@JvmInline
value class ParcelId(val value: UUID) {
    companion object { 
        fun new() = ParcelId(UuidV7Generator.generate())
    }
}
Public representation (external ID) is derived using a codec:
val parcelPublicId = parcelId.toPublicId(parcelPublicIdCodec)
```

## Consequences
✅ Pros
* Stable public contracts (URLs, API payloads)
* Avoids exposing the raw internal UUID representation in public contracts.
* Flexibility in changing ID generators or formats
* Enables safe migration and data sharding

⚠️ Cons
* Slightly more complex model (two identifiers)
* Requires additional column or mapping in persistence
* Needs consistent encode/decode layer

**Alternatives**
```
Option	                        Pros	                Cons
Single UUID for everything      Simpler to implement    Leaks internal structure, brittle public API
Semantic / human-readable IDs   Easy for users          Not opaque, risk of conflicts, poor for security
HMAC / irreversible hash        Fully opaque            Not reversible, requires lookup table
```

### Review
Revisit after 3–6 months or when:
- introducing multi-region data replication,
- exposing financial or regulated data,
- or implementing non-reversible public ID schemes (e.g. ULID, Base58, or HMAC-based).

### References
- Opaque Identifier Pattern — APIs You Won’t Hate, 2021
- RFC 4122 — UUID Specification
- ADR-002: Use Base32 Encoding for External Identifiers
