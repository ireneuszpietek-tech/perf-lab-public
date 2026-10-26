package ip.logistics.shipping.shipment.service.confg

import ip.logistics.codec.PublicIdService
import ip.logistics.codec.PublicIdServiceImpl
import ip.logistics.shipping.parcel.adapter.idcodec.ParcelPublicIdBase32Codec
import ip.logistics.shipping.parcel.application.command.CreateShipmentHandler
import ip.logistics.shipping.parcel.application.port.EventBus
import ip.logistics.shipping.parcel.application.port.inbound.CreateShipment
import ip.logistics.shipping.parcel.application.port.inbound.GetShipmentView
import ip.logistics.shipping.parcel.application.port.outbound.ParcelPublicIdCodec
import ip.logistics.shipping.parcel.application.port.outbound.ShipmentRepository
import ip.logistics.shipping.parcel.application.port.outbound.ShipmentViewRepository
import ip.logistics.shipping.parcel.application.query.GetShipmentViewHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class ParcelConfig {

    @Bean
    fun clock(): Clock =
        Clock.systemUTC()

    @Bean
    fun publicIdService(): PublicIdService =
        PublicIdServiceImpl()

    @Bean
    fun parcelPublicIdCodec(
        publicIdService: PublicIdService
    ): ParcelPublicIdCodec =
        ParcelPublicIdBase32Codec(publicIdService)


    @Bean
    fun createShipment(
        shipmentRepository: ShipmentRepository,
        codec: ParcelPublicIdCodec,
        eventBus: EventBus,
        clock: Clock,
    ): CreateShipment =
        CreateShipmentHandler(
            shipmentRepository,
            codec,
            eventBus,
            clock,
        )

    @Bean
    fun getShipmentView(
        shipmentViewRepository: ShipmentViewRepository,
        codec: ParcelPublicIdCodec,
    ): GetShipmentView =
        GetShipmentViewHandler(
            codec,
            shipmentViewRepository,
        )
}