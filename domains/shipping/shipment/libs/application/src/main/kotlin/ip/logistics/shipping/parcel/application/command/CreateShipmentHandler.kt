package ip.logistics.shipping.parcel.application.command

import ip.logistics.parcel.domain.model.ParcelSpec
import ip.logistics.parcel.domain.model.Shipment
import ip.logistics.parcel.domain.model.ShipmentId
import ip.logistics.shipping.parcel.application.port.EventBus
import ip.logistics.shipping.parcel.application.port.inbound.CreateShipment
import ip.logistics.shipping.parcel.application.port.outbound.ParcelPublicIdCodec
import ip.logistics.shipping.parcel.application.port.outbound.ShipmentRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID

class CreateShipmentHandler(
    private val repo: ShipmentRepository,
    private val parcelPublicIdCodec: ParcelPublicIdCodec, //todo
    private val bus: EventBus,
    private val clock: Clock,
//    private val idempotency: IdempotencyStore? = null
) : CreateShipment {
    override fun handle(cmd: CreateShipmentCommand): CreateShipmentResult {
        // (opcjonalnie) idempotencja POST
//        cmd.clientOperationId?.let { opId ->
//            val existingId = idempotency?.findShipmentId(opId)
//            if (existingId != null) {
//                val existing = repo.get(existingId) ?: error("Idempotency points to missing shipment")
//                return CreateShipmentResult(codec.encode(existing.id), existing.status())
//            }
//        }

        val id = ShipmentId(UUID.randomUUID())
        val now = Instant.now(clock)

        val (shipment, events) = Shipment.create(
            id = id,
            fromLocker = cmd.fromLocker,
            toLocker = cmd.toLocker,
            spec = ParcelSpec(cmd.weightGrams, cmd.size),
            now = now
        )

        repo.save(shipment)
//        cmd.clientOperationId?.let { idempotency?.remember(it, id) }

        events.forEach(bus::publish)

        return CreateShipmentResult(
            trackingCode = parcelPublicIdCodec.encode(id),
            status = shipment.status()
        )
    }
}