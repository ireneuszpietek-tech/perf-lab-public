package ip.logistics.shipping.parcel.application.port.inbound

import ip.logistics.shipping.parcel.application.command.CreateShipmentCommand
import ip.logistics.shipping.parcel.application.command.CreateShipmentResult

interface CreateShipment {
    fun handle(cmd: CreateShipmentCommand): CreateShipmentResult
}

