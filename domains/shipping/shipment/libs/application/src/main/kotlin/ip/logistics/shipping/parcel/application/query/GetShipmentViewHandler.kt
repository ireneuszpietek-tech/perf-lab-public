package ip.logistics.shipping.parcel.application.query

import ip.logistics.parcel.domain.model.TrackingCode
import ip.logistics.shipping.parcel.application.port.inbound.GetShipmentView
import ip.logistics.shipping.parcel.application.port.outbound.ParcelPublicIdCodec
import ip.logistics.shipping.parcel.application.port.outbound.ShipmentViewRepository
import mu.KotlinLogging
import java.time.Instant

private val log = KotlinLogging.logger {}

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
    private val viewRepo: ShipmentViewRepository
) : GetShipmentView {
    override fun handle(trackingCode: TrackingCode): ShipmentView? {
        val id = parcelPublicIdCodec.decode(trackingCode) ?: return null
        val view = viewRepo.get(id) ?: return null
        return view.copy(trackingCode = trackingCode)
    }
}