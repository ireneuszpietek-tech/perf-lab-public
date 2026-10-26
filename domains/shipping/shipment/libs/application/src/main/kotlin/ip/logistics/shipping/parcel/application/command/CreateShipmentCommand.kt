package ip.logistics.shipping.parcel.application.command

//application input model,
data class CreateShipmentCommand(
    val fromLocker: String,
    val toLocker: String,
    val weightGrams: Int,
    val size: String,
    val clientOperationId: String? = null // optional for POST idempotency
)