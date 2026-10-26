package ip.logistics.shipping.parcel.application.port

import ip.logistics.parcel.domain.model.DomainEvent

interface EventBus {
    fun publish(event: DomainEvent)
    fun subscribe(handler: (DomainEvent) -> Unit)
}