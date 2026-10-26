package ip.logistics.shipping.parcel.adapter.idcodec

import ip.logistics.codec.PublicIdService
import ip.logistics.parcel.domain.model.TrackingCode

import ip.logistics.codec.PublicIdType
import ip.logistics.codec.Version
import ip.logistics.parcel.domain.model.ShipmentId
import ip.logistics.shipping.parcel.application.port.outbound.ParcelPublicIdCodec

/**
 * Encodes internal [ShipmentId] values into versioned public [TrackingCode]s
 * and decodes them back.
 *
 * The public format uses Base32 with a resource-type prefix and format version.
 * See ADR-012 for the encoding decision and ADR-010 for public/private ID separation.
 */
class ParcelPublicIdBase32Codec(
    val publicIdService: PublicIdService

) : ParcelPublicIdCodec {
    override fun encode(internalId: ShipmentId): TrackingCode {
        val encoded = publicIdService.encode(internalId.value, PublicIdType.TRACKING, Version.Current)
        return TrackingCode(encoded)
    }

    override fun decode(trackingCode: TrackingCode): ShipmentId {
        val decoded = publicIdService.decode(trackingCode.value, PublicIdType.TRACKING)
        return ShipmentId(decoded)
    }

}