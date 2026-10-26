package ip.logistics.shipping.parcel.application.command

import ip.logistics.shipping.parcel.application.shared.ParcelIdDto
import java.util.UUID

data class RegisterParcelResult(val parcelIdDto: ParcelIdDto)
