package fr.umontpellier.iut.dominion.client

import fr.umontpellier.iut.dominion.Client.ClientConnectionState
import fr.umontpellier.iut.dominion.Client.ClientGameUIState
import fr.umontpellier.iut.dominion.Client.ClientView
import fr.umontpellier.iut.dominion.gui.authentification.StatValue
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class Client(
    var id: String = UUID.randomUUID().toString(),
    @Volatile var session: WebSocketSession?,
    serverScope: CoroutineScope
) : AutoCloseable {

    var name: String = "PLAYER_${id.take(3)}"
    var ready : Boolean = false
    var currentGameId: String? = null

    val state = MutableStateFlow<ClientConnectionState?>(null)
    val view = MutableStateFlow<ClientView?>(null)
    val uiState = MutableStateFlow<ClientGameUIState?>(null)

    var disconnectJob: Job? = null

    init {
        state.update { ClientConnectionState.Connecting(id) }
        view.update { ClientView.Home }
    }

    val scope: CoroutineScope = CoroutineScope(
        SupervisorJob(serverScope.coroutineContext[Job]) + Dispatchers.Default
    )

    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    val stats = ConcurrentHashMap<StatKey, StatValue>()

    val isOpen: Boolean get() = session?.isOpen == true


    fun updateSession(newSession: WebSocketSession) {
        this.session = newSession
        newSession.attributes["playerId"] = this.id
        newSession.attributes["playerName"] = this.name
        this.currentGameId?.let { newSession.attributes["GAME_ID"] = it }
    }

    fun updateGame(gameId: String) {
        this.currentGameId = gameId
        session?.attributes["GAME_ID"] = gameId
    }


    fun updateStats(savedStats: Map<StatKey, StatValue>) { stats.putAll(savedStats) }

    /**
     * Récupère la stat existante (ou l'initialise) et incrémente sa valeur globale.
     * @return La nouvelle valeur globale totale.
     */
    fun getOrPut(key: StatKey, valueToAdd: Int): Int {
        val stat = stats.computeIfAbsent(key) { StatValue() }

        synchronized(stat) {
            stat.value += valueToAdd
            return stat.value
        }
    }

    fun recordHoverDetail(key: StatKey, detailLabel: String, countToAdd: Int = 1) {
        val stat = stats.computeIfAbsent(key) { StatValue() }

        synchronized(stat) {
            stat.value += countToAdd

            val currentCount = stat.hoverDetailsMap.getOrDefault(detailLabel, 0)
            stat.hoverDetailsMap[detailLabel] = currentCount + countToAdd
        }
    }

    fun send(message: String) {
        try {
            val currentSession = session
            if (currentSession != null && currentSession.isOpen) {
                synchronized(currentSession) {
                    currentSession.sendMessage(TextMessage(message))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun close() {
        try {
            println("Déconnexion : $id")
            scope.cancel()
            if (isOpen) {
                state.update { ClientConnectionState.Disconnected }
            }
            session = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}