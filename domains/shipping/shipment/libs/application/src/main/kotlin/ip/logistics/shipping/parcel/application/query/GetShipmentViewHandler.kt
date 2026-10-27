package ip.logistics.shipping.parcel.application.query

import ip.logistics.parcel.domain.model.TrackingCode
import ip.logistics.shipping.parcel.application.port.inbound.GetShipmentView
import ip.logistics.shipping.parcel.application.port.outbound.ParcelPublicIdCodec
import ip.logistics.shipping.parcel.application.port.outbound.ShipmentRepository
import java.time.Instant

data class ShipmentView(
    val trackingCode: TrackingCode,
    val status: String,
    val fromLocker: String,
    val toLocker: String,
    val weightGrams: Int,
    val size: String,
    val lastUpdatedAt: Instant
)

class GetShipmentViewHandler(
    private val parcelPublicIdCodec: ParcelPublicIdCodec,
    private val shipmentRepository: ShipmentRepository
) : GetShipmentView {
    override fun handle(trackingCode: TrackingCode): ShipmentView? {
        val id = parcelPublicIdCodec.decode(trackingCode)
        val shipment = shipmentRepository.get(id) ?: return null

        return ShipmentView(
            trackingCode = trackingCode,
            status = shipment.status().name,
            fromLocker = shipment.fromLocker,
            toLocker = shipment.toLocker,
            weightGrams = shipment.spec.weightGrams,
            size = shipment.spec.size,
            lastUpdatedAt = shipment.updatedAt()
        )
    }
}
