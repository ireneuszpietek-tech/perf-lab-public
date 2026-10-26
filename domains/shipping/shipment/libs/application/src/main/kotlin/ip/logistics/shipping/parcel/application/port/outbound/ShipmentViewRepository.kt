package ip.logistics.shipping.parcel.application.port.outbound

import ip.logistics.parcel.domain.model.ShipmentId
import ip.logistics.shipping.parcel.application.query.ShipmentView

interface ShipmentViewRepository {
    fun get(id: ShipmentId): ShipmentView?
    fun upsert(id: ShipmentId, view: ShipmentView)
}