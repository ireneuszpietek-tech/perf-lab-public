package ip.logistics.codec

import ip.logistics.codec.provider.ParcelV1Codec
import java.util.UUID

class PublicIdServiceImpl : PublicIdService {
    override fun encode(
        uuid: UUID,
        type: PublicIdType,
        version: Version
    ): String {
        if(type == PublicIdType.TRACKING && version == Version.Current) {
            return ParcelV1Codec().encode(uuid)
        }
        return "" // todo throw ex
    }

    override fun decode(raw: String): DecodedPublicId {
        return ParcelV1Codec().decode(raw)
    }

    override fun decode(raw: String, expectedType: PublicIdType): UUID {
        return ParcelV1Codec().decode(raw).uuid
    }
}