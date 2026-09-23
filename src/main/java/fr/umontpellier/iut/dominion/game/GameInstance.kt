package fr.umontpellier.iut.dominion.game

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import fr.umontpellier.iut.dominion.Client.ClientConnectionState
import fr.umontpellier.iut.dominion.Client.ClientView
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.cards.factories.FactorySupplyPile
import fr.umontpellier.iut.dominion.client.Client
import fr.umontpellier.iut.dominion.gui.game.GameGUI
import fr.umontpellier.iut.dominion.service.UtilsService.Companion.parseJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.springframework.beans.factory.getBean
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.collections.component1
import kotlin.collections.component2

class GameInstance(val gameId: String, val gameGui: GameGUI) {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mapper = ObjectMapper()
    val lastPlayerJsonStates = ConcurrentHashMap<String, ObjectNode>()
    val clients = CopyOnWriteArrayList<Client>()
    val currentSelected = ArrayList<String>()
    val currentEventSelected = ArrayList<String>()
    val setupChoices = HashMap<String, Array<String>>()
    val setupTasks = ArrayList<String>()
    var presetExtraCard: String? = null
    var ownerPlayerId: String? = null

    var gameIsFinished: Boolean = false




    @Volatile
    var gameState: ObjectNode = mapper.createObjectNode()

    fun arriveToLobby(){
        val available = FactorySupplyPile.availableCardsByExtension
        val availableEvents = FactorySupplyPile.availableEvent
        val presets = FactorySupplyPile.getPreSets()
        val availableJson = formatMapToJson(available)
        val availableEventJson = formatMapToJson(availableEvents)
        val presetsJson = formatNestedMapToJson(presets)

        val newGameStat = """
            "gameId":"$gameId",
            "view":"LOBBY",
            "availableCards":$availableJson,
            "availableEvents":$availableEventJson,
            "presets":$presetsJson,
        """.trimIndent()

        updateHubState(newGameStat)
    }


    fun updateHubState(message : String = "") {
        val selected = ArrayList(currentSelected)
        presetExtraCard?.let { selected.add(it) }
        val selectedJson = selected.joinToString(",", "[", "]") { s -> "\"$s\"" }
        val selectedEventJson = currentEventSelected.joinToString(",", "[", "]") { s -> "\"$s\"" }

        val clientJson = lobbyUserToJson()
        val canStart = canStartGame()

        val newGameState = """
            {
            $message
            "selectedCards":$selectedJson,
            "selectedEvents" : $selectedEventJson,
            "players":$clientJson,
            "ownerId":"$ownerPlayerId",
            "canStart":$canStart
            }
        """.trimIndent().replace("\n", "")

        updateGameState(newGameState)
    }

    fun updateAfterClear(){
        val canStart = canStartGame()

        val newGameState = """
            {
            "selectedCards":[],
            "selectedEvents":[],
            "canStart":$canStart
            }
        """.trimIndent()

        updateGameState(newGameState)
    }

    fun updateCardWasChosen(){
        val canStart = canStartGame()

        val selectedJson = currentSelected.joinToString(",", "[", "]") { s -> "\"$s\"" }
        val selectedEventJson = currentEventSelected.joinToString(",", "[", "]") { s -> "\"$s\"" }

        val newGameState = """
            {
            "selectedCards":$selectedJson,
            "selectedEvents" : $selectedEventJson,
            "canStart":$canStart
            }
        """.trimIndent()

        updateGameState(newGameState)
    }


    private fun lobbyUserToJson() = clients.joinToString(",", "[", "]") { p ->
        """     
            {
                "id": "${p.id}",
                "pseudo": "${p.name}",
                "isReady": ${p.ready}
            }
            """.trimIndent()
    }

    private fun canStartGame(): Boolean{
        val allReady = clients.isNotEmpty() && clients.all { it.ready }
        return  currentSelected.size >= 10 && clients.size >= 2 && allReady
    }

    fun updateLobbyUser(){
        val clientJson = lobbyUserToJson()
        val canStart = canStartGame()

        val newGameState = """
            {
             "players": $clientJson,
             "canStart": $canStart
             }
        """.trimIndent()


        updateGameState(newGameState)
    }

    fun updateGameState(message: String) {
        try {
            val incomingJson = mapper.readTree(message) as? ObjectNode

            if (incomingJson != null) {
                if (incomingJson.has("logChange")) computeLogChange(incomingJson)

                if (incomingJson.has("itemChange")) computeItemChange(incomingJson)

                incomingJson.fieldNames().forEach { fieldName ->
                    if (fieldName != "logChange" && fieldName != "itemChange") {
                        gameState.set<JsonNode>(fieldName, incomingJson.get(fieldName))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        broadcast(message)
    }

    private fun computeLogChange(json : ObjectNode){
        val newLog = json.get("logChange")

        val logsArray = if (gameState.has("logChange") && gameState.get("logChange").isArray) {
            gameState.get("logChange") as ArrayNode
        } else {
            gameState.putArray("logChange")
        }

        if (newLog.isArray) {
            logsArray.addAll(newLog as ArrayNode)
        } else {
            logsArray.add(newLog)
        }
    }

    private fun computeItemChange(json: ObjectNode) {
        val itemChanges = json.get("itemChange") ?: return
        val changesArray = if (itemChanges.isArray) itemChanges else listOf(itemChanges)

        for (change in changesArray) {
            val targetPlayerId = change.get("id")?.asText()
            val rawItem = change.get("item")?.asText()?.lowercase()
            val newValue = change.get("new")?.asInt()

            if (targetPlayerId != null && rawItem != null && newValue != null) {
                val resourceKey = when (rawItem) {
                    "coins" -> "money"
                    "victory_token", "vt" -> "vt"
                    else -> rawItem
                }

                lastPlayerJsonStates.forEach { (cachedPlayerId, cachedPlayerState) ->
                    val game = cachedPlayerState.get("game") as? ObjectNode
                    if (game != null) {

                        val client = game.get("client") as? ObjectNode
                        val clientId = client?.get("client")?.asText() ?: client?.get("id")?.asText()

                        if (client != null && clientId == targetPlayerId) {
                            client.put(resourceKey, newValue)
                        }

                        val players = game.get("players") as? ArrayNode
                        if (players != null) {
                            for (player in players) {
                                if (player is ObjectNode) {
                                    val pId = player.get("playerId")?.asText()
                                        ?: player.get("client")?.asText()
                                        ?: player.get("id")?.asText()

                                    if (pId == targetPlayerId) {
                                        player.put(resourceKey, newValue)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun broadcast(message: String) {
        for (client in clients) {
            try {
                client.send(message)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Envoie un message personnalisé à une session spécifique (Unicast)
     */
    fun sendToSession(session: Client, message: String) {
        try {
            if (session.isOpen) {
                val id = session.id
                saveUserState(id, message)
                session.send(message)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun saveUserState(id : String, state : String ) {
        val json = try {parseJson(state) as? ObjectNode} catch (e :Exception){e.message; null} ?: return
        val currentState = lastPlayerJsonStates.computeIfAbsent(id) { mapper.createObjectNode() }
        mergeJsonNodes(currentState, json)
    }

    private fun mergeJsonNodes(main: ObjectNode, source: ObjectNode) {
        source.fieldNames().forEach { fieldName ->
            val sourceValue = source.get(fieldName)
            val mainValue = main.get(fieldName)

            if (mainValue is ObjectNode && sourceValue is ObjectNode) {
                mergeJsonNodes(mainValue, sourceValue)
            } else if (fieldName == "players" && mainValue is ArrayNode && sourceValue is ArrayNode) {
                mergePlayersArray(mainValue, sourceValue)
            } else {
                main.set<JsonNode>(fieldName, sourceValue)
            }
        }
    }

    private fun mergePlayersArray(mainPlayers: ArrayNode, sourcePlayers: ArrayNode) {
        for (sourcePlayer in sourcePlayers) {
            if (sourcePlayer is ObjectNode) {
                val sourceId = sourcePlayer.get("playerId")?.asText()
                    ?: sourcePlayer.get("client")?.asText()
                    ?: sourcePlayer.get("id")?.asText()

                if (sourceId != null) {
                    val existingPlayer = mainPlayers.firstOrNull { p ->
                        p is ObjectNode && (
                                p.get("playerId")?.asText() == sourceId ||
                                        p.get("client")?.asText() == sourceId ||
                                        p.get("id")?.asText() == sourceId
                                )
                    } as? ObjectNode

                    if (existingPlayer != null) {
                        mergeJsonNodes(existingPlayer, sourcePlayer)
                    } else {
                        mainPlayers.add(sourcePlayer)
                    }
                }
            }
        }
    }

    fun prepareSetupTasks() {
        setupTasks.clear()
        setupChoices.clear()

        if (currentSelected.contains("Ferryman") && presetExtraCard != null) {
            setupChoices["Ferryman"] = arrayOf(presetExtraCard!!)
        } else if (currentSelected.contains("Ferryman")) {
            setupTasks.add("FERRYMAN")
        }

        if (currentSelected.contains("Young Witch") && presetExtraCard != null) {
            setupChoices["Banes"] = arrayOf(presetExtraCard!!)
        } else if (currentSelected.contains("Young Witch")) {
            setupTasks.add("YOUNG_WITCH")
        }

        processNextSetupTask()
    }

    fun processNextSetupTask() {
        val currentTask = setupTasks.removeFirstOrNull() ?: run {
            launchGame()
            return
        }

        if (currentTask == "FERRYMAN") {
            sendChoiceRequest("FERRYMAN_CHOICE", FactorySupplyPile.getFerrymanOptions(currentSelected))
        } else if (currentTask == "YOUNG_WITCH") {
            sendChoiceRequest("YOUNG_WITCH_CHOICE", FactorySupplyPile.getYoungWitchOptions(currentSelected))
        }
    }

    private fun sendChoiceRequest(viewName: String, options: Map<String, List<String>>) {
        val optionsJson = options.entries.joinToString(",", "{", "}") { entry ->
            val cards = entry.value.joinToString(",", "[", "]") { card -> "\"$card\"" }
            "\"${entry.key}\":$cards"
        }
        val msg = "{\"gameId\":\"$gameId\", \"view\":\"$viewName\", \"options\":$optionsJson}"
        updateGameState(msg)
    }

    fun removeClient(client: Client): Boolean {
        synchronized(clients) {
            gameGui.players.find { it.client.id == client.id }?.client?.state?.let { it.update { ClientConnectionState.Disconnected } }
            updateHubState()
            return clients.all { it.state.value == ClientConnectionState.Disconnected }
        }
    }

    private fun launchGame() {
        println("[$gameId] Lancement du jeu...")
        val kingdomCards = currentSelected.take(10).toTypedArray()
        val kingdom = kingdomCards.mapNotNull { FactorySupplyPile.createCard(it) }
        val events = currentEventSelected.mapNotNull { FactorySupplyPile.createCard(it) }

        val players = clients.mapNotNull { client ->
            client?.ready = false
            if (client != null) {
                gameGui.context.getBean<Player>().apply {
                    self = this
                    this.name = client.name
                    this.client = client
                }
            } else null
        }

        gameGui.init(players, kingdom, events,  HashMap(setupChoices), this)


        players.forEach {
            it.client.view.update { ClientView.Game(gameId) }
            sendToSession(it.client,"{\"gameId\":\"$gameId\", \"view\":\"GAME\"}" )
        }

        scope.launch { gameGui.run() }
    }

    fun endGame(message: String) {
        gameIsFinished = true

        try {
            val incomingJson = mapper.readTree(message)
            val newGameState = mapper.createObjectNode()
            incomingJson.fieldNames().forEach {
                newGameState.set<JsonNode>(it, incomingJson.get(it))
            }

            gameState = newGameState
        }catch (err: Exception){ }


        broadcast(gameState.toString())
    }

    private fun formatMapToJson(map: Map<String, List<String>>) = map.entries.joinToString(",", "{", "}") { e ->
        "\"${e.key}\":[${e.value.joinToString(",") { "\"$it\"" }}]"
    }

    private fun formatNestedMapToJson(map: Map<String, Map<String, List<String>>>) = map.entries.joinToString(",", "{", "}") { exp ->
        val setsJson = exp.value.entries.joinToString(",", "{", "}") { set ->
            "\"${set.key}\":[${set.value.joinToString(",") { "\"$it\"" }}]"
        }
        "\"${exp.key}\":$setsJson"
    }
}