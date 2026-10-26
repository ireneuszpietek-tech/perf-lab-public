package ip.logistics.parcel.domain.model

import java.time.Instant
import java.util.UUID

sealed interface DomainEvent { val eventId: UUID; val occurredAt: Instant }

data class ShipmentCreated(
    val shipmentId: ShipmentId,
    override val occurredAt: Instant,
    override val eventId: UUID = UUID.randomUUID()
) : DomainEvent