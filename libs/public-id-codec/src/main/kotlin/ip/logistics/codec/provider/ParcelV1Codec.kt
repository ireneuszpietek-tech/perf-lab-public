package ip.logistics.codec.provider

import ip.logistics.codec.DecodedPublicId
import ip.logistics.codec.PublicIdType
import ip.logistics.codec.Version
import java.util.UUID

class ParcelV1Codec {
    private val prefix = "parcel"
    private val version = "1"

    fun encode(internalId: UUID): String {
        val payload = UuidBase32Codec().encode(internalId)
        return "$prefix-$version-$payload"
    }

    fun decode(externalId: String): DecodedPublicId {
        val parts = externalId.split('-', limit = 3)

        //validation
        require(parts.size == 3) { "Invalid public id: $externalId" }
        val (resource, ver, payload) = parts
        require(resource == prefix) { "Unexpected resource: $resource" }
        require(ver == version) { "Unsupported version: $ver" }

        return DecodedPublicId(
            PublicIdType.TRACKING,
            1,
            UuidBase32Codec().decode(payload)
        )
    }

}