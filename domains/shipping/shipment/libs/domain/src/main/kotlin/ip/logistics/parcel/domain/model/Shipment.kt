package ip.logistics.parcel.domain.model

import java.time.Instant

enum class ShipmentStatus { AWAITING_DROPOFF, CANCELLED, ACCEPTED, IN_TRANSIT }
data class ParcelSpec(val weightGrams: Int, val size: String)

class Shipment private constructor(
    val id: ShipmentId,
    val fromLocker: String,
    val toLocker: String,
    val spec: ParcelSpec,
    private var status: ShipmentStatus,
    private var updatedAt: Instant
) {
    fun status() = status
    fun updatedAt() = updatedAt

    companion object {
        fun create(
            id: ShipmentId,
            fromLocker: String,
            toLocker: String,
            spec: ParcelSpec,
            now: Instant
        ): Pair<Shipment, List<DomainEvent>> {
            val s = Shipment(id, fromLocker, toLocker, spec, ShipmentStatus.AWAITING_DROPOFF, now)
            return s to listOf(ShipmentCreated(id, now))
        }
    }
}