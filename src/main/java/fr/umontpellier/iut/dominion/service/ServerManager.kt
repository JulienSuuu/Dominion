package fr.umontpellier.iut.dominion.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.stereotype.Service
import org.springframework.web.socket.WebSocketSession

@Service
class ServerManager(
    private val clientManager: ClientManager,
    private val inputManager: InputManager,
    private val serverScope : CoroutineScope
) {
    fun addInput(session: WebSocketSession, input: String) = inputManager.addInput(session, input)
    fun addClient(session: WebSocketSession) = clientManager.addClient(session)
    fun removeClient(session: WebSocketSession) = clientManager.removeClient(session)
}