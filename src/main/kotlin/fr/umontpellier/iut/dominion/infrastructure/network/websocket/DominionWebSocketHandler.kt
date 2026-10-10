package fr.umontpellier.iut.dominion.infrastructure.network.websocket

import fr.umontpellier.iut.dominion.application.service.ServerManager
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

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