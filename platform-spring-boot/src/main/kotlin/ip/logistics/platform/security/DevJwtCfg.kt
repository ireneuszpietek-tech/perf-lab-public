package ip.logistics.platform.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.OctetSequenceKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import javax.crypto.spec.SecretKeySpec

@Profile("dev")
@Configuration
class DevJwtCfg {

    private val DEV_HS256_SECRET_ASCII = "0123456789abcdef0123456789abcdef" // 32 chars = 32 bytes
    private val secretBytes = DEV_HS256_SECRET_ASCII.toByteArray(Charsets.UTF_8)
    private val secretKey = SecretKeySpec(secretBytes, "HmacSHA256")

    @Bean
    fun jwtEncoder(): JwtEncoder {
        val jwk = OctetSequenceKey.Builder(secretKey)
            .algorithm(JWSAlgorithm.HS256)
            .keyID("dev-hs256")
            .build()
        val jwkSet = JWKSet(jwk)
        val jwkSource = ImmutableJWKSet<SecurityContext>(jwkSet)
        return NimbusJwtEncoder(jwkSource)
    }

    @Bean
    fun jwtDecoder(): JwtDecoder =
        NimbusJwtDecoder
            .withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build()

}