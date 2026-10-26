package ip.logistics.shipping.parcel.adapter.inbound.web.dto

data class ParcelResponseDto(
    val trackingCode: String,          // publiczne ID (zakodowane)
    val destinationLockerId: String,
    val label: String,
    val status: String
)

