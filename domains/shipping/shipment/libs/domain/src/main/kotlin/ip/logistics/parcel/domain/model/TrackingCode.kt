package ip.logistics.parcel.domain.model

/**
 * Versioned public tracking identifier exposed through external APIs.
 */
@JvmInline
value class TrackingCode(val value: String) {
    init {
        require(value.isNotBlank())
    }
}