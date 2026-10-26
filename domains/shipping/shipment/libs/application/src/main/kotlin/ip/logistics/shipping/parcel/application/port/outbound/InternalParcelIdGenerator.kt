package ip.logistics.shipping.parcel.application.port.outbound

import ip.logistics.parcel.domain.model.ShipmentId

// Port na generowanie ID (out)
interface InternalParcelIdGenerator {
    fun newId(): ShipmentId
}