package ip.logistics.shipping.parcel.adapter.outbound.jpa

import ip.logistics.parcel.domain.model.Shipment
import ip.logistics.parcel.domain.model.ShipmentId
import ip.logistics.shipping.parcel.application.port.outbound.ShipmentRepository
import ip.logistics.shipping.parcel.application.shared.ParcelIdDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class InMemoryShipmentRepository : ShipmentRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    val storage = ConcurrentHashMap<UUID, Shipment>()

    override fun get(id: ShipmentId): Shipment? {
        log.debug("Get by id: [{}]", id)
        return storage[id.value]
    }

    //todo: what we should return
    override fun save(shipment: Shipment) {
        storage[shipment.id.value] = shipment
        log.info("#Saved Parcel with id: [{}]", shipment.id)

//        Thread.sleep(10000)
//        if(storage.size>100){
//            throw IllegalStateException("Storage to large")
//        }

        log.info("#Storage has {} rows", storage.size)
    }
}