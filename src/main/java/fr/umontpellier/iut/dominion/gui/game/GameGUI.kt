package fr.umontpellier.iut.dominion.gui.game

import com.fasterxml.jackson.databind.JsonNode
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.client.Client
import fr.umontpellier.iut.dominion.game.Game
import fr.umontpellier.iut.dominion.game.GameInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.util.concurrent.LinkedBlockingQueue

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
open class GameGUI(
    override val uiStateService: UiStateService,
    override val context: ApplicationContext
) : Game(
    context,
    uiStateService
) {

    private val inputChannel = Channel<JsonNode>(Channel.UNLIMITED)
    private val outputChannel = Channel<Pair<String, Client?>>(Channel.UNLIMITED)
    private val guiScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun init(
        playerNames: List<Player>,
        kingdomPiles: List<Card>,
        events : List<Card>,
        extras: Map<String, Array<String>>?,
        instance: GameInstance
    ) {
        super.init(playerNames, kingdomPiles, events, extras, instance)

        guiScope.launch {
            for ((message, session) in outputChannel) {
                if (session != null) {
                    instance.sendToSession(session, message)
                } else {
                    instance.updateGameState(message)
                }
            }
        }
    }


    override fun sendToUI(message: String, session: Client?) {
        outputChannel.trySend(Pair(message, session))
    }

    override fun sendToUI(message: String) {
        outputChannel.trySend(Pair(message, null))
    }

    override fun endGame(message : String){ instance.endGame(message) }

    override suspend fun readLine(): JsonNode? {
        return try {
            inputChannel.receive()
        } catch (e: Exception) {
            null
        }
    }

    fun addInput(message: JsonNode?) {
        if (message != null) { inputChannel.trySend(message) }
    }

    fun closeGame() {
        inputChannel.close()
        outputChannel.close()
    }
}