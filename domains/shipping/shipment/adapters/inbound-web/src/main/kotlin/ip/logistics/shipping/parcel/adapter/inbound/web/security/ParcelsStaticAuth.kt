package ip.logistics.shipping.parcel.adapter.inbound.web.security

import ip.logistics.platform.security.HttpAuthContributor
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer

@Order(0)
@Configuration(proxyBeanMethods = false)
class ParcelsStaticAuth : HttpAuthContributor {
    override fun contribute(
        auth: AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry,
    ) {
        auth.requestMatchers(HttpMethod.GET, "/api/parcels/**")
            .hasAuthority("SCOPE_read:parcel")
        auth.requestMatchers(HttpMethod.POST, "/api/parcels")
            .hasAuthority("SCOPE_create:parcel")
    }
}