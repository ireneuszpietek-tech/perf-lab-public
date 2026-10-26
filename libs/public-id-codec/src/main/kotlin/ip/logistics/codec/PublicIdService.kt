package ip.logistics.codec

import java.util.UUID

/**
 * Public identifier <-> internal UUID mapping is treated as a critical,
 * security-sensitive capability. A single defect may lead to data leakage
 * (e.g. exposing another customer's parcel).
 *
 * Because of that:
 * - mapping logic is centralized in this library,
 * - adapters are NOT allowed to provide codecs, providers or routing rules,
 * - version recognition and dispatch are internal responsibilities,
 * - decode/encode must be deterministic and fully covered by contract tests.
 *
 * Design driver is safety and change control, not pluggability.
 * Introducing SPI/provider patterns here would distribute responsibility
 * across adapters and significantly increase regression risk.
 *
 * In short:
 * one entry point, closed registry, strict ownership, SemVer evolution.
 */

enum class PublicIdType {
    TRACKING,
    COURIER
}

sealed interface Version {
    data object Current : Version
    data class Exact(val value: Int) : Version
}

data class DecodedPublicId(
    val type: PublicIdType,
    val version: Int,
    val uuid: UUID
)

sealed class DecodeException(message: String) : RuntimeException(message) {
    class InvalidFormat(raw: String) :
        DecodeException("Invalid public id format: $raw")

    class UnsupportedType(raw: String, expected: PublicIdType) :
        DecodeException("Unsupported public id type for $raw. Expected: $expected")
}

interface PublicIdService {

    /**
     * Internal -> External
     */
    fun encode(
        uuid: UUID,
        type: PublicIdType,
        version: Version = Version.Current
    ): String

    /**
     * External -> Internal (autodetect)
     */
    fun decode(raw: String): DecodedPublicId

    /**
     * External -> Internal with type safety for endpoint
     */
    fun decode(raw: String, expectedType: PublicIdType): UUID
}
