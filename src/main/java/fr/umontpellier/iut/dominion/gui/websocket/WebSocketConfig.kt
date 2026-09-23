package fr.umontpellier.iut.dominion.gui.websocket

import fr.umontpellier.iut.dominion.gui.authentification.JwtService
import fr.umontpellier.iut.dominion.gui.authentification.JwtWebSocketInterceptor
import fr.umontpellier.iut.dominion.service.ServerManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.handler.TextWebSocketHandler

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

class DominionWebSocketHandler(
    private val serverManager: ServerManager
) : TextWebSocketHandler() {

    override fun afterConnectionEstablished(session: WebSocketSession) {
        serverManager.addClient(session)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        serverManager.addInput(session, message.payload)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
         serverManager.removeClient(session)
    }
}