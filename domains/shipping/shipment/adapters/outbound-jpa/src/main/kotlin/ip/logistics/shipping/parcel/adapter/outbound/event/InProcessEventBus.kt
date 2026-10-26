package ip.logistics.shipping.parcel.adapter.outbound.event

import ip.logistics.parcel.domain.model.DomainEvent
import ip.logistics.shipping.parcel.application.port.EventBus
import org.springframework.stereotype.Service
import java.util.concurrent.CopyOnWriteArrayList


@Service
class InProcessEventBus : EventBus {
    private val handlers = CopyOnWriteArrayList<(DomainEvent) -> Unit>()
    override fun publish(event: DomainEvent) {
        handlers.forEach { it(event) }
    }

    override fun subscribe(handler: (DomainEvent) -> Unit) {
        handlers.add(handler)
    }
}