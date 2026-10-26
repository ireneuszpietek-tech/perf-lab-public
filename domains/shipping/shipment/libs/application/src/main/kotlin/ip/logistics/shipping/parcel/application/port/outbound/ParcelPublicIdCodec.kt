package ip.logistics.shipping.parcel.application.port.outbound

import ip.logistics.parcel.domain.model.ShipmentId
import ip.logistics.parcel.domain.model.TrackingCode

interface ParcelPublicIdCodec {
    fun encode(internalId: ShipmentId): TrackingCode
    fun decode(trackingCode: TrackingCode): ShipmentId
}