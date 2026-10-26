package ip.logistics.shipping.shipment.service.command

import ip.logistics.shipping.parcel.adapter.idcodec.ParcelPublicIdBase32Codec
import ip.logistics.shipping.parcel.application.port.outbound.ParcelPublicIdCodec
import ip.logistics.shipping.parcel.application.query.GetShipmentViewHandler
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.time.ZoneId

@SpringBootApplication(
    scanBasePackages = [
        "ip.logistics.shipping.parcel",
        "ip.logistics.platform.security",
        "ip.logistics.shipping.shipment.service.confg"
    ]
)
class ShipmentServiceApplication

fun main(args: Array<String>) {
    if (!ZoneId.systemDefault().equals(ZoneId.of("UTC"))) {
        throw IllegalStateException("System timezone must be UTC");
    }


    println(
        ParcelPublicIdCodec::class.java.protectionDomain.codeSource.location
    )

    println(
        GetShipmentViewHandler::class.java.protectionDomain.codeSource.location
    )

    println(
        ParcelPublicIdBase32Codec::class.java.protectionDomain.codeSource.location
    )

    runApplication<ShipmentServiceApplication>(*args)
}