package ip.logistics.shipping.parcel.application.command

import ip.logistics.parcel.domain.model.ShipmentStatus
import ip.logistics.parcel.domain.model.TrackingCode

data class CreateShipmentResult(
    val trackingCode: TrackingCode,
    val status: ShipmentStatus
)