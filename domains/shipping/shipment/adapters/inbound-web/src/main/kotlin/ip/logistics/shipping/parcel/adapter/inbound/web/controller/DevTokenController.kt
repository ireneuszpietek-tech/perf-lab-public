package ip.logistics.shipping.parcel.adapter.inbound.web.controller

import org.springframework.context.annotation.Profile
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@Profile("dev")
class DevTokenController(private val encoder: JwtEncoder) {

    @GetMapping("/dev/token")
    fun token(
        @RequestParam("sub") sub: String = "user1",
        @RequestParam("scope", required = false) scope: String?
    ): Map<String, String> {
        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            .subject(sub)
            .id(UUID.randomUUID().toString())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(13600))
            .claim("scope", scope ?: "")
            .build()

        val headers = JwsHeader
            .with(MacAlgorithm.HS256)
            .header("kid", "dev-hs256") //this dev-hs256 is for test only
            .build()

        val jwt = encoder.encode(
            JwtEncoderParameters.from(headers, claims)
        )
        return mapOf("access_token" to jwt.tokenValue)
    }
}