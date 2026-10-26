package ip.logistics.shipping.parcel.application.port.outbound

import ip.logistics.parcel.domain.model.Shipment
import ip.logistics.parcel.domain.model.ShipmentId
import ip.logistics.shipping.parcel.application.shared.ParcelIdDto

interface ShipmentRepository {
    fun save(shipment: Shipment)
    fun get(id: ShipmentId): Shipment?
}