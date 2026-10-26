package ip.logistics.shipping.parcel.adapter.share

import com.fasterxml.uuid.Generators
import ip.logistics.shipping.parcel.application.port.outbound.InternalParcelIdGenerator
import ip.logistics.parcel.domain.model.ShipmentId
import org.springframework.stereotype.Component

@Component
class RandomUuidInternalParcelIdGenerator : InternalParcelIdGenerator {

    private val gen = Generators.randomBasedGenerator()
    override fun newId(): ShipmentId = ShipmentId(gen.generate())
}