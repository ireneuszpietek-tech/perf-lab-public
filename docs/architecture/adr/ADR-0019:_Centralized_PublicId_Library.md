ADR: Centralized PublicId ↔ UUID Mapping in a Closed Edge Library

Status: Accepted
Date: 2026-02-09
Context / Problem
We expose multiple external/public identifiers (e.g., TrackingCode, CourierCode, locker codes) while the system uses internal canonical IDs (UUID). The mapping is security-sensitive: a single defect can cause cross-customer data exposure (e.g., returning another customer’s parcel).
We also need controlled evolution of ID formats (e.g., BC-1 → BC-2) without breaking existing clients.

Previous approach: a generic “mini-framework” where the shared jar provided base interfaces/abstractions and adapters supplied codec/provider implementations. Over time this increased complexity (provider ordering, additional mappers/steps, configuration drift) and moved critical decisions into adapters.

Decision
Implement a closed, centralized mapping library (jar) that owns:

parsing and validation of public IDs,

scheme/type recognition (e.g., TRACKING vs COURIER),

version routing/dispatch (e.g., BC-1, BC-2),

deterministic encode/decode rules and error semantics.

Adapters (edge) must not provide providers/codecs or routing rules. Adapters only:

call PublicIdService.decode(raw, expectedType) and map errors to protocol responses,

call PublicIdService.encode(uuid, type, version) to emit external IDs,

wrap the resulting UUID into internal types (e.g., ParcelId(UUID)).

Consequences
✅ Pros

Single source of truth for ID mapping; minimal regression surface.

Adapters cannot “accidentally” change mapping logic.

Controlled format evolution via SemVer and internal version registry.

Contract testing is centralized (golden vectors + round-trip/property tests).

Cleaner hex boundary: domain/application remain unaware of public ID formats.

⚠️ Cons

Adding a new scheme/version requires releasing a new jar version.

Less runtime pluggability vs provider/SPI approach.

Alternatives Considered

Provider/SPI-based codecs supplied by adapters

Rejected: pushes critical selection/configuration into adapters, increases drift and regression risk, scales poorly with multiple types/versions.

Implement mapping inside application/domain

Rejected: leaks infrastructure concerns (formats/versions) into core layers and complicates use-cases.

Implementation Notes

Provide a single high-level API (PublicIdService) with decode(..., expectedType) and encode(...).

Maintain an internal closed registry keyed by (type, version); no adapter-supplied providers.

Enforce strict parsing (no silent fallbacks).

Use contract tests: golden test vectors + round-trip property tests + negative cases.