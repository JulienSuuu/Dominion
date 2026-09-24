package fr.umontpellier.iut.dominion.gui

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
open class SecurityConfig(
    @Value("\${cors.allowed-origins}")
    private val allowedOrigins: String,
    private val jwtCookieFilter: JwtCookieFilter
) {

    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .headers { headers ->
                headers.frameOptions { it.sameOrigin() }
            }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/auth/**", "/ws/**", "/h2-console/**").permitAll()
                    .requestMatchers("/api/themes").permitAll()
                    .requestMatchers("/api/themes/**", "/ws/stats/**").authenticated()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtCookieFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    open fun corsConfigurationSource(): CorsConfigurationSource {
        val origins = allowedOrigins.split(",").map { it.trim() }

        val configuration = CorsConfiguration().apply {
            allowedOriginPatterns = origins
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}