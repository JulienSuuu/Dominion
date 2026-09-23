package fr.umontpellier.iut.dominion.gui.game


import fr.umontpellier.iut.dominion.Client.ClientView
import fr.umontpellier.iut.dominion.service.GameManager
import kotlinx.coroutines.flow.update
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/game")
class GameOverService(
    private val gameManager: GameManager
) {


    @PostMapping("/stop/{gameId}")
    fun stopGame(@PathVariable gameId: String): ResponseEntity<Map<String, String>> {
        val gameInstance = gameManager.getActiveGame(gameId)
            ?: return ResponseEntity.notFound().build()

        gameInstance.clients.forEach { client ->
            client.currentGameId = null
        }

        gameManager.endGame(gameInstance)

        println(" [REST API] La partie $gameId a été arrêtée et supprimée de activeGames.")

        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "message" to "La partie $gameId a bien été supprimée."
        ))
    }

    @PostMapping("/leaving/{gameId}")
    fun leaving(
        @PathVariable gameId: String,
        @RequestParam playerId: String
    ): ResponseEntity<Map<String, String>> {

        val gameInstance = gameManager.getActiveGame(gameId)
            ?: return ResponseEntity.notFound().build()

        val client = gameInstance.clients.find { it.id == playerId }
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Client non trouvé dans cette partie"))

        client.currentGameId = null
        gameInstance.clients.remove(client)
        client.view.update { ClientView.Home }

        println(" [REST API] Le joueur ${client.name} ($playerId) a quitté la partie $gameId.")

        if (gameInstance.clients.isEmpty()) {
            gameManager.endGame(gameInstance)
            println(" [REST API] La partie $gameId était vide et a été supprimée.")
        }

        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "message" to "Le joueur $playerId a bien quitté la partie $gameId."
        ))
    }
}