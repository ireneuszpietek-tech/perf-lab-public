package ip.logistics.shipping.parcel.adapter.inbound.web.mapper

//import ip.logistics.shipping.parcel.adapter.inbound.web.dto.CreateShipmentRequestDto
//import ip.logistics.shipping.parcel.adapter.inbound.web.dto.ParcelResponseDto
//import ip.logistics.shipping.parcel.application.command.CreateShipmentCommand
//import ip.logistics.parcel.domain.model.Shipment
//import ip.logistics.parcel.domain.model.ParcelStatus
//import kotlin.String
//
//extension function
//internal fun Shipment.toParcelResponseDto(
//    publicId: String,
//    destinationLockerId: String,
//    label: String,
//
//    ) =
//    ParcelResponseDto(
//        trackingCode = publicId,
//        destinationLockerId = destinationLockerId,
//        label = label,
//        status = status.toExternal() // izolacja kontraktu
//    )
//
////extension function
//internal fun CreateShipmentRequestDto.toRegisterParcelCommand() = CreateShipmentCommand(destinationLockerId, label)
//
////extension function
//fun ParcelStatus.toExternal(): String = when (this) {
//    ParcelStatus.CREATED -> "CREATED"
//    ParcelStatus.IN_TRANSIT -> "IN_TRANSIT"
//    ParcelStatus.DELIVERED -> "DELIVERED"
//}
