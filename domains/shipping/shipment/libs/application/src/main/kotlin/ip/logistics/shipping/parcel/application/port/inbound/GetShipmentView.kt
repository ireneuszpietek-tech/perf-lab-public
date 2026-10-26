package ip.logistics.shipping.parcel.application.port.inbound

import ip.logistics.parcel.domain.model.TrackingCode
import ip.logistics.shipping.parcel.application.query.ShipmentView

interface GetShipmentView {
    fun handle(trackingCode: TrackingCode): ShipmentView?
}