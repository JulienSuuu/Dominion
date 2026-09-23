package fr.umontpellier.iut.dominion.service

import com.fasterxml.jackson.databind.JsonNode
import fr.umontpellier.iut.dominion.Client.ClientConnectionState
import fr.umontpellier.iut.dominion.cards.component.Price
import fr.umontpellier.iut.dominion.cards.factories.checkDuration
import fr.umontpellier.iut.dominion.client.Client
import fr.umontpellier.iut.dominion.game.GameInstance
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.update
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.containsKey
import kotlin.collections.remove
import kotlin.coroutines.cancellation.CancellationException
import kotlin.text.get

@Service
class GameManager(
    private val gameService: GameService
) {
    private val activeGames = ConcurrentHashMap<String, GameInstance>()

    fun getActiveGame(gameId : String) = activeGames[gameId]
    fun getGameByClient(client : Client) : GameInstance? {
        return activeGames[client.currentGameId?: ""] ?: activeGames.values.firstOrNull{game -> game.clients.any { it.id == client.id } }
    }

    fun updateGameOnDisconnexion(client : Client){
        getGameByClient(client)?.let {game ->
            if(game.removeClient(client)){
                println("[GAME CLEANUP] Suppression de la partie ${game.gameId} (aucun joueur restant)")
                game.scope.cancel()
                activeGames.remove(game.gameId)
            }
            client.currentGameId = null
        }
    }

    private fun addGame(gameInstance: GameInstance) { activeGames[gameInstance.gameId] = gameInstance }

    fun endGame(gameInstance: GameInstance) {
        gameInstance.scope.cancel(cause = CancellationException("Game stopped by host or service"))
        activeGames.remove(gameInstance.gameId)
    }

    fun generateUniqueGameId(): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        var newId: String
        do {
            newId = (1..5).map { allowedChars.random() }.joinToString("")
        } while (activeGames.containsKey(newId))
        return newId
    }

    fun handleJoinGame(client: Client, jsonNode: JsonNode) {
        val gameId = jsonNode.get("gameId")?.asText()?.uppercase()?.trim() ?: ""
        val playerName = jsonNode.get("playerName")?.asText() ?: "Anonyme"
        val gameInstance = activeGames[gameId]

        if (gameInstance != null) {
            client.currentGameId = gameId
            client.name = playerName
            client.session?.attributes["GAME_ID"] = gameId

            synchronized(gameInstance) {
                gameInstance.clients.removeIf { oldClient -> oldClient.id == client.id || !oldClient.isOpen }
                gameInstance.clients.add(client)
            }
            println(" Joueur ${client.name} a rejoint la partie $gameId")
            gameInstance.arriveToLobby()
        } else {
            client.send("{\"error\": \"Partie introuvable\"}")
        }
    }


    fun handleCreateGame(client: Client, jsonNode: JsonNode) {
        val gameId = generateUniqueGameId()

        val playerName = jsonNode.get("playerName")?.asText() ?: "Anonyme"

        val newGameInstance = gameService.createGame(gameId)

        client.currentGameId = gameId
        client.name = playerName
        client.session?.attributes["GAME_ID"] = gameId

        synchronized(newGameInstance) {
            newGameInstance.clients.add(client)
            newGameInstance.ownerPlayerId = client.id
        }

        addGame(newGameInstance)
        println("Partie créée. ID généré : $gameId par ${client.name}")
        newGameInstance.arriveToLobby()
    }



}