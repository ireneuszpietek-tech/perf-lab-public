package ip.logistics.platform.security

import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration(proxyBeanMethods = false)
class SecurityCfg(
    private val contributors: ObjectProvider<HttpAuthContributor>,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        //todo
        println(">>> LOADING SecurityFilterChain from platform/SecurityCfg <<<")

        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

            .authorizeHttpRequests { auth ->
                // np. token dev / actuator bez auth
                auth
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    .requestMatchers("/dev/token").permitAll()
                    .anyRequest().authenticated()
            }

            // switch on JWT as auth mechanism:
            .oauth2ResourceServer { it.jwt {} }

            // *** to protect redirect on /login ***
            .formLogin { it.disable() }
            .httpBasic { it.disable() }

        return http.build()
    }
}
