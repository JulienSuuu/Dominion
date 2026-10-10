package fr.umontpellier.iut.dominion.infrastructure.network.websocket.interceptor

import fr.umontpellier.iut.dominion.application.service.JwtService
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

class JwtWebSocketInterceptor(
    private val jwtService: JwtService
) : HandshakeInterceptor {

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        if (request is ServletServerHttpRequest) {
            val servletRequest = request.servletRequest
            val jwtCookie = servletRequest.cookies?.find { it.name == "jwt" }

            if (jwtCookie != null) {
                val token = jwtCookie.value

                if (jwtService.isTokenValid(token)) {
                    val playerId = jwtService.extractPlayerId(token)

                    attributes["PLAYER_ID"] = playerId
                    attributes["playerId"] = playerId

                    val pseudo = jwtService.extractPseudo(token)
                    attributes["playerName"] = pseudo ?: playerId

                    return true
                }
            }
        }
        return false
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {}
}