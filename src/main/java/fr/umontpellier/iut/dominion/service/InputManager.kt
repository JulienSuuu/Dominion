package fr.umontpellier.iut.dominion.service

import com.fasterxml.jackson.databind.JsonNode
import fr.umontpellier.iut.dominion.client.Client
import fr.umontpellier.iut.dominion.game.GameInstance
import fr.umontpellier.iut.dominion.service.UtilsService.Companion.toArray
import fr.umontpellier.iut.dominion.service.UtilsService.Companion.toNameField

import org.springframework.stereotype.Service
import org.springframework.web.socket.WebSocketSession

@Service
class InputManager(
    private val gameManager: GameManager,
    private val clientManager: ClientManager
) {
    fun addInput(session: WebSocketSession, message: String) {
        try {
            val client = try {clientManager.requireClient(session)} catch (e: Exception) {println(e.message); null} ?: return
            val jsonNode = try { UtilsService.parseJson(message) } catch (e: Exception) { null }

            if (jsonNode != null && jsonNode.isObject) {
                val action = jsonNode.get("action")?.asText() ?: ""

                when (action) {
                    "CREATE_GAME" -> handleCreateGame(client, jsonNode)
                    "JOIN_GAME" -> handleJoinGame(client, jsonNode)
                    else -> {
                        val gameId = client.currentGameId ?: return
                        val gameInstance = gameManager.getActiveGame(gameId) ?: return

                        handleGameLobbyOrInput(client, gameInstance, jsonNode)
                    }
                }
            }
        } catch (e: Exception) {
            println("Erreur traitement message : ${e.message}")
        }
    }


    private fun handleCreateGame(client: Client, jsonNode: JsonNode) {gameManager.handleCreateGame(client, jsonNode) }
    private fun handleJoinGame(client: Client, jsonNode: JsonNode) { gameManager.handleJoinGame(client, jsonNode) }



    private fun handleGameLobbyOrInput(client: Client, gameInstance: GameInstance, json : JsonNode) {
        val action = json.get("action")?.asText() ?: ""

        when (action) {
            "READY" -> gameInstance.handleReady(client)
            "START_GAME" -> gameInstance.handleStartGame(client)
            "CLEAR_CARDS" -> gameInstance.handleClearCards()
            "CHOOSE_PRESET" -> gameInstance.handlePresetCardWasChosen(json)
            "CHOOSE_CARD" -> gameInstance.handleCardWasSelected(json)
            "CHOOSE_EVENT" -> gameInstance.handleEventWasSelected(json)
            "CONFIRM_FERRYMAN" -> gameInstance.handleFerrymanChoice(json)
            "CONFIRM_YOUNG_WITCH" -> gameInstance.handleYoungWitchChoice(json)
            else -> gameInstance.gameGui.addInput(json)
        }
    }

    private fun GameInstance.handleReady(client : Client) {
        client.ready = true
        updateLobbyUser()
    }



    private fun GameInstance.handleClearCards(){
        currentSelected.clear()
        currentEventSelected.clear()
        presetExtraCard = null
        updateAfterClear()
    }

    private fun GameInstance.handlePresetCardWasChosen(json : JsonNode){
        val cards = json.toArray("preset") ?: return

        currentSelected.clear()
        presetExtraCard = null

        cards.forEachIndexed { i, card ->
            val cardName = card.asText().trim()
            if(i < 10) { currentSelected.add(cardName) }
            else presetExtraCard = card.asText()
        }

        updateCardWasChosen()
    }

    private fun GameInstance.handleCardWasSelected(json : JsonNode){
        val cardName = json.toNameField("card") ?: return

        if (currentSelected.contains(cardName)) currentSelected.remove(cardName)
        else if (currentSelected.size < 10) currentSelected.add(cardName)

        updateCardWasChosen()
    }

    private fun GameInstance.handleEventWasSelected(json : JsonNode){
        val eventName = json.toNameField("card") ?: return

        if (currentEventSelected.contains(eventName)) currentEventSelected.remove(eventName)
        else if (currentEventSelected.size < 2) currentEventSelected.add(eventName)

        updateCardWasChosen()
    }

    private fun GameInstance.handleStartGame(client : Client){
        if(ownerPlayerId == client.id &&
            currentSelected.size >= 10 &&
            clients.size >= 2) {
            prepareSetupTasks()
        }
    }

    private fun GameInstance.handleFerrymanChoice(json : JsonNode){
        val cardName = json.toNameField("card") ?: return
        setupChoices["Ferryman"] = arrayOf(cardName)
        if(cardName == "Young Witch") setupTasks.add(0, "YOUNG_WITCH")
        processNextSetupTask()
    }

    private fun GameInstance.handleYoungWitchChoice(json : JsonNode){
        val cardName = json.toNameField("card") ?: return
        setupChoices["Banes"] = arrayOf(cardName)
        processNextSetupTask()
    }

}