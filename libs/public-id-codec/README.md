Versioned public ID encoding/decoding with explicit semantic evolution support, decoupling external identifiers from internal identity models.

*public-id-codec* is a small, focused library for encoding and decoding public-facing identifiers with an explicit separation between:
- internal identity (UUID / long / internal key)
- external representation (public, stable, opaque ID)

The primary goal is to support long-term evolution of ID semantics without breaking external contracts.

### Core Idea
Public IDs are treated as a versioned encoding, not as a raw identifier.

PublicId = encode(version + semantic payload)
This allows:
- multiple encoding strategies (v1, v2, …)
- semantic changes over time
- backward compatibility at decode time without leaking internal identifiers or coupling clients to internal models.

Example: Base32PublicIdCodec (v1)

Base32PublicIdCodec provides:
- deterministic encode(internalId)
- safe decode(publicId)
- URL-safe, case-insensitive Base32 representation
- fixed-length output suitable for APIs, logs, URLs, and customer-facing systems

This is intentionally not a reversible hash for security,
but an opaque, stable public identifier.
Versioning by Semantics, Not by Format
Future versions (v2, v3, …) are expected to change meaning, not just encoding:

Examples of semantic evolution:
different internal ID types (UUID → Snowflake / ULID)
sharding or routing hints
entity type prefixes
checksum / validation rules
migration away from legacy identifiers

The codec design assumes that:
old public IDs must remain decodable
new versions may carry different semantics
decoding logic is version-aware

Why This Exists
In real systems, public IDs often outlive:
databases
schemas
services
even entire architectures

This library isolates that concern and makes ID evolution:
explicit
testable
intentional

instead of accidental string parsing spread across the codebase.

What This Demonstrates
conscious API contract design
forward-compatible identifier strategy
separation of internal and external identity
engineering for change, not just for today’s format

Optional one-liner (CV / GitHub summary)