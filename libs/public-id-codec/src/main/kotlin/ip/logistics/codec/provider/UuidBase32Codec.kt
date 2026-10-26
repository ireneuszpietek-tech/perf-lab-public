package ip.logistics.codec.provider

import mu.KotlinLogging
import org.apache.commons.codec.binary.Base32
import java.nio.ByteBuffer
import java.util.UUID

private val log = KotlinLogging.logger {}

class UuidBase32Codec() {
    fun encode(uuid: UUID): String {
        log.debug("Encoding uuid: [{}]", uuid)

        val bytes = ByteBuffer.allocate(16)
            .putLong(uuid.mostSignificantBits)
            .putLong(uuid.leastSignificantBits)
            .array()

        val encoded = Base32().encodeAsString(bytes).replace("=", "")

        log.debug("Encoded to base32: [{}]", encoded)
        return encoded
    }

    fun decode(base32Str: String): UUID {
        log.debug("Decoding base32: [{}]", base32Str)
        val bytes = Base32().decode(base32Str)
        require(bytes.size == 16) { "Invalid Base32 UUID payload" }
        val buffer = ByteBuffer.wrap(bytes)
        val decoded = UUID(buffer.long, buffer.long)
        log.debug("Decoded to uuid: [{}]", decoded)
        return decoded
    }

}