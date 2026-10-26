package ip.logistics.shipping.parcel.adapter.outbound.jpa

import ip.logistics.parcel.domain.model.ShipmentId
import ip.logistics.shipping.parcel.application.port.outbound.ShipmentViewRepository
import ip.logistics.shipping.parcel.application.query.ShipmentView
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class InMemoryShipmentViewRepository : ShipmentViewRepository {
    private val map = ConcurrentHashMap<UUID, ShipmentView>()
    override fun get(id: ShipmentId): ShipmentView? = map[id.value]
    override fun upsert(id: ShipmentId, view: ShipmentView) {
        map[id.value] = view
    }
}