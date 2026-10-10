package fr.umontpellier.iut.dominion.infrastructure.network.websocket

import fr.umontpellier.iut.dominion.application.service.JwtService
import fr.umontpellier.iut.dominion.infrastructure.network.websocket.interceptor.JwtWebSocketInterceptor
import fr.umontpellier.iut.dominion.application.service.ServerManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
open class WebSocketConfig(
    private val jwtService: JwtService,
    private val serverManager: ServerManager,
    @Value("\${cors.allowed-origins}") private val allowedOrigins: String
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        val origins = allowedOrigins.split(",").toTypedArray()
        registry.addHandler(DominionWebSocketHandler(serverManager), "/ws")
            .addInterceptors(JwtWebSocketInterceptor(jwtService))
            .setAllowedOriginPatterns(*origins)
    }
}