package ip.logistics.shipping.parcel.adapter.inbound.web.dto

/**
 * layer HTTP (API). contract JSON without domain logic
 * Opaque Identifier Pattern(Internal/External ID Separation, Dual Identifier Model).
 * internal_id: technical, local, fast, often UUID/timeuuid/bigint,
 * external_id: global or public, stable, unique across the entire system, often opaque.
 *
 * Classified under category:
 * 📘 Data Modeling / API Design / Multi-Tenant Architecture Patterns.
 * see ADR-001: internal-external-id-pattern.
 */
data class CreateShipmentRequestDto(
    val fromLocker: String,
    val toLocker: String,
    val weightGrams: Int,
    val size: String,
    val clientOperationId: String?
)
