package fr.umontpellier.iut.dominion.Player.PlayerComponent

import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.cards.Card
import kotlinx.serialization.Serializable


data class PlayerState(
    val turnPhase : PlayerTurnPhase = PlayerTurnPhase.Waiting,
    val interaction: PlayerInteractionState? = null,
    val inTurnState: PlayerInTurnState? = null
){
    fun changeTurnState(turnState: PlayerTurnPhase) = copy(turnPhase = turnState)
    fun changeInteractionState(interaction: PlayerInteractionState?) = copy(interaction = interaction)
    fun changeInTurnState(inTurnState: PlayerInTurnState?) = copy(inTurnState = inTurnState)

    fun reset() = PlayerState()

    val isCleaningUp: Boolean
        get() = turnPhase == PlayerTurnPhase.CleanupPhase

    val isIgnoringStats: Boolean
        get() = turnPhase == PlayerTurnPhase.CleanupPhase || turnPhase == PlayerTurnPhase.StartTurn

    val inBuyPhase get() = turnPhase == PlayerTurnPhase.BuyPhase || turnPhase == PlayerTurnPhase.StartBuyPhase
}

/**
 * Phased du tour d'un joueur
 */
sealed interface PlayerTurnPhase {
    data object Waiting : PlayerTurnPhase
    data object StartTurn : PlayerTurnPhase
    data object StartNightPhase : PlayerTurnPhase
    data object ActionPhase : PlayerTurnPhase
    data object BuyPhase : PlayerTurnPhase
    data object StartBuyPhase : PlayerTurnPhase
    data object CleanupPhase : PlayerTurnPhase
    data object CleanupGeneral : PlayerTurnPhase
}
/**
 * État des interactions du joueur (porte les données contextuelles)
 */
sealed interface PlayerInteractionState {
    data class WaitingForChoice(
        val prompt : String,
        val choices : List<String>
    ) : PlayerInteractionState

    data class MustDiscard(val count: Int) : PlayerInteractionState
    data class MustTrash(val count: Int, val maxCost: Int? = null) : PlayerInteractionState
    data class MustGain(val maxCost: Int, val destination: Destination = Destination.PlayerZone.Discard) : PlayerInteractionState
}
/**
 * État global du joueur
 */
sealed interface PlayerInTurnState {
    data class PlayingCard(val card : Card) : PlayerInTurnState
    data class BuyingCard(val card : Card) : PlayerInTurnState
    data class Choosing(val interaction: PlayerInteractionState) : PlayerInTurnState

    data object ActionPhase : PlayerInTurnState
    data object BuyingPhase : PlayerInTurnState
}

