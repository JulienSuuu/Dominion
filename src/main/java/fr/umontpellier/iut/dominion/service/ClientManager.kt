package fr.umontpellier.iut.dominion.service

import fr.umontpellier.iut.dominion.client.Client
import fr.umontpellier.iut.dominion.game.GameInstance
import fr.umontpellier.iut.dominion.gui.UserService
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import org.springframework.web.socket.WebSocketSession
import java.net.URLDecoder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.text.isNotBlank
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Service
class ClientManager(
    private val userService: UserService,
    private val gameManager: GameManager,
    private val serverScope : CoroutineScope
) {

    data class UserIdentity(val id : String, val name : String)
    data class ClientStatement(val client : Client, val gameInstance: GameInstance?)
    data class ClientDisconnected(val client : Client?, val gameInstance: GameInstance?)

    val activesClient = ConcurrentHashMap<String, Client>()

    fun getClient(clientId: String) = activesClient[clientId]
    fun addActiveClient(client: Client) { activesClient[client.id] = client }

    fun updateClientWhenDisconnected(session : WebSocketSession) : ClientDisconnected{
        val clientId = session.attributes["playerId"] as? String ?: return ClientDisconnected(null, null)
        val client = activesClient[clientId] ?: return ClientDisconnected(null, null)
        return ClientDisconnected(client, gameManager.getGameByClient(client))
    }

    @PreDestroy
    fun onShutdown() { activesClient.forEach { (_, client) -> userService.saveStatsAsync(client.id, client.stats.toMutableMap()) } }


    fun onClientConnect(client : Client){ userService.loadStatsForClient(client) }
    fun onClientDisconnect(client: Client){ userService.saveStatsAsync(client.id, client.stats.toMap()) }

    fun addClient(session: WebSocketSession) {
        val (playerId, playerName) = extractUserIdentity(session)
        val (client, ongoingGame) = extractClientStatement(playerId, playerName, session)
        if (ongoingGame != null) { updateUserViewOnConnexion(client, ongoingGame) }
        else { client.send("{\"view\": \"MAIN_MENU\", \"my_local_id\": \"$playerId\"}") }
    }

    private fun purgeClient(client: Client) {
        client.disconnectJob?.cancel()
        activesClient.remove(client.id)?.let {
            onClientDisconnect(it)
            it.close()
        }
    }

    fun clientDisconnect(clientId: String) {
        activesClient[clientId]?.let { client ->
            println("Déconnexion volontaire du client : ${client.name}")
            gameManager.updateGameOnDisconnexion(client)
            purgeClient(client)
        }
        println("Clients actifs restants : ${activesClient.mapValues { it.value.name }}")
    }

    fun removeClient(session: WebSocketSession) {
        val (client, gameInstance) = updateClientWhenDisconnected(session) ?: return
        if (client == null || gameInstance == null) return

        println("Déconnexion temporaire du client : ${client.name}")
        client.session = null

        client.disconnectJob?.cancel()

        client.disconnectJob = serverScope.launch {
            delay(5.minutes)

            if (!client.isOpen) {
                println("Délai de grâce expiré pour ${client.name}. Suppression définitive.")
                purgeClient(client)
                checkGameAbandoned(gameInstance)
            }
        }
    }

    private fun checkGameAbandoned(gameInstance: GameInstance) {
        if (gameInstance.clients.none { it.isOpen }) {
            println("Partie ${gameInstance.gameId} abandonnée. Nettoyage...")
            gameManager.endGame(gameInstance)
        }
    }



    fun extractUserIdentity(session : WebSocketSession): UserIdentity {
        var playerId = session.attributes["playerId"] as? String
        var playerName = session.attributes["playerName"] as? String

        if (playerId == null) {
            val queryPairs = session.uri?.query?.split("&")?.associate { pair ->
                val split = pair.split("=")
                val key = URLDecoder.decode(split[0], "UTF-8")
                val value = if (split.size > 1) URLDecoder.decode(split[1], "UTF-8") else ""
                key to value
            } ?: emptyMap()

            playerId = queryPairs["playerId"]?.takeIf { it.isNotBlank() }
                ?: "player_${UUID.randomUUID().toString().substring(0, 8)}"
            playerName = queryPairs["playerName"] ?: "Anonyme"
        } else if (playerName == null) {
            playerName = "player_${playerId.take(4)}"
        }

        return UserIdentity(playerId, playerName)
    }

    fun extractClientStatement(id : String, name : String, session : WebSocketSession) : ClientStatement {
        val existingClient = getClient(id)
        val client = if (existingClient != null){
            println("[RECONNEXION WS] Mise à jour de la session pour $name ($id)")
            existingClient.name = name

            existingClient.updateSession(session)
            existingClient
        } else {
            val newClient = Client(id, session, serverScope).apply { this.name = name; updateSession(session) }
            addActiveClient(newClient)
            onClientConnect(newClient)
            newClient
        }

        val ongoingGame = gameManager.getGameByClient(client)

        return ClientStatement(client, ongoingGame)
    }

    fun updateUserViewOnConnexion(client : Client, ongoingGame : GameInstance){
        client.updateGame(ongoingGame.gameId)

        synchronized(ongoingGame.clients) {
            ongoingGame.clients.removeIf { oldClient -> oldClient.id == client.id && oldClient != client }
            if (!ongoingGame.clients.contains(client)) ongoingGame.clients.add(client)
        }

        try { ongoingGame.gameGui.players.find { it.client.id == client.id && it.client != client }?.client = client } catch (e: Exception){
            println("[RECONNEXION WARNING] Erreur moteur de jeu : ${e.message}")
        }

        when{
            !ongoingGame.gameIsFinished -> {
                ongoingGame.lastPlayerJsonStates[client.id]
                    ?.takeIf { !it.isEmpty }
                    ?.let { client.send(it.toString()) }
                    ?: run { client.send(ongoingGame.gameState.toString()) }
            }
            else -> client.send(ongoingGame.gameState.toString())
        }

    }


    fun requireClient(session: WebSocketSession): Client {
        val id = session.attributes["playerId"] as? String
            ?: throw IllegalStateException("Session non identifiée")
        return activesClient[id]
            ?: throw IllegalArgumentException("Aucun client actif trouvé pour l'ID : $id")
    }



}