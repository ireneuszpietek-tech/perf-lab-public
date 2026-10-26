# ADR-012: Use Base32 Encoding for External Identifiers

- **Status:** Accepted
- **Date:** 2025-10-12
- **Author:** IP
- **Related:** [ADR-001]
- **Supersedes:** none


---

## Context
After introducing the **Opaque Identifier Pattern** in [ADR-001](ADR-001-opaque-identifier-pattern.md),
we need to define a **concrete representation format** for `external_id` values exposed via HTTP APIs and integration events.

Requirements for this format:
- **URL-safe** (no `+`, `/`, `=`)
- **Human-readable** (easy to copy, no ambiguous characters)
- **Reversible** (must allow conversion back to `internal_id`)
- **Stable & standardized** (preferably RFC-based)
- **Portable** across programming languages and databases

---

## Decision
We will use **Base32 encoding (RFC 4648)** to represent `external_id` values derived from internal `UUID` identifiers.

### Encoding
- Input: 128-bit `UUID` (typically UUIDv7)
- Encoding algorithm: Base32 (RFC 4648)
- **Uppercase alphabet** (`A–Z 2–7`)
- **No padding** (`=` removed)
- Example result:

UUID: 550e8400-e29b-41d4-a716-446655440000
Base32: AHT4K6RJX2Y66WKMKHE7NDZPDY

- Optional prefix to denote resource type, e.g.
`par_AHT4K6RJX2Y66WKMKHE7NDZPDY`

### Decoding
- The system must support decoding Base32 back to UUID for lookups:
- Reverse the Base32 encoding
- Parse the resulting bytes as UUID (MSB + LSB)
- The process must be **deterministic and lossless**.

### Implementation Example (Kotlin)
```kotlin
import java.nio.ByteBuffer
import java.util.UUID
import com.google.common.io.BaseEncoding

fun encodeBase32(uuid: UUID): String =
  BaseEncoding.base32().omitPadding().encode(
      ByteBuffer.allocate(16)
          .putLong(uuid.mostSignificantBits)
          .putLong(uuid.leastSignificantBits)
          .array()
  ).uppercase()

fun decodeBase32(s: String): UUID {
  val bytes = BaseEncoding.base32().omitPadding().decode(s.uppercase())
  val bb = ByteBuffer.wrap(bytes)
  return UUID(bb.long, bb.long)
}
```

### Alternatives
```
Option	    Pros                                Cons
Base62      Shorter (~22 chars), compact        Not standardized, case-sensitive, multiple variants
Base64URL   Very compact (22 chars), RFC 4648   Less readable, includes - and _
Hex (UUID)  Trivial to implement                Long (36 chars) and leaks structure
HMAC/Hash   Fully opaque, no reversibility      Requires lookup table for decoding
```

### Consequences
### Pros
- URL-safe and filesystem-safe
- Easy to copy and paste (no ambiguous characters)
- Reversible for backend systems
- Based on standard RFC 4648 (cross-language support)

### Cons
- Slightly longer (~26 chars vs 22 in Base62)
- Case-insensitive comparisons require normalization (uppercase)
- Minor runtime cost for encode/decode

### Review
Revisit this decision if:
- the encoded length becomes problematic (e.g. user-visible IDs),
- or if we adopt a new opaque ID scheme (e.g. ULID, Base58, or HMAC).

### References
- RFC 4648 - The Base16, Base32, and Base64 Data Encodings
- ADR-001: Opaque Identifier Pattern