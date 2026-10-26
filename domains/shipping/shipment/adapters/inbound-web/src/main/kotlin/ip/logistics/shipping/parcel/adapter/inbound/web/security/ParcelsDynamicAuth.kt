package ip.logistics.shipping.parcel.adapter.inbound.web.security

import ip.logistics.platform.security.HttpAuthContributor
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer

@Order(1)
@Configuration(proxyBeanMethods = false)
class ParcelsDynamicAuth(
    private val perms: PermissionsProvider
) : HttpAuthContributor {
    override fun contribute(
        auth: AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry,
    ) {
        auth.requestMatchers("/api/parcels/**")
            .access { authSupplier, ctx ->
                val req = ctx.request

                val required = perms.requiredScope(
                    HttpMethod.valueOf(req.method),
                    req.requestURI
                )

                val granted = if (required == null) {
                    true
                } else {
                    val authentication = authSupplier.get()
                    authentication != null &&
                            authentication.authorities.any { it.authority == "SCOPE_$required" }
                }

                AuthorizationDecision(granted)
            }
    }
}