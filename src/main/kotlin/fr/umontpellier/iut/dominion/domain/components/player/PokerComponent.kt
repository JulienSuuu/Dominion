package fr.umontpellier.iut.dominion.domain.components.player

import fr.umontpellier.iut.dominion.domain.model.location.Destination
import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.player.skills.chooseOrder
import fr.umontpellier.iut.dominion.domain.model.event.PokerEvent
import fr.umontpellier.iut.dominion.domain.components.card.trigger.PokerHandReactionComponent
import fr.umontpellier.iut.dominion.domain.setup.card.factories.futaba.PokerHandEvaluator

class PokerComponent(val self : Player) : PlayerComponent {

    suspend fun checkPokerReaction(){
        if (self.state.value.turnPhase != PlayerTurnPhase.ActionPhase) return

        val currentHand = self.getList(Destination.PlayerZone.Hand)
        val evaluatedHand = PokerHandEvaluator.bestHand(currentHand) ?: return

        self.chooseOrder<PokerHandReactionComponent>(
            instruction = "Choisissez une réaction Poker à activer (${evaluatedHand.type.displayName})",
            getter = { self.getList(Destination.PlayerZone.Hand) },
            canPass = true,
            event = PokerEvent(self, evaluatedHand)
        ) { chosenCard ->
            chosenCard.getComponent<PokerHandReactionComponent>()?.invoke(self, evaluatedHand)
        }
    }

    fun canEvaluatePokerHand() : Boolean {
        if (self.state.value.turnPhase != PlayerTurnPhase.ActionPhase) return false
        val currentHand = self.getList(Destination.PlayerZone.Hand)
        val hasReactionCard = currentHand.any { it.hasComponent<PokerHandReactionComponent>() }
        return hasReactionCard && currentHand.size >= 5
    }
}

suspend fun Player.doPoker(){getComponent<PokerComponent>()?.checkPokerReaction()}
fun Player.canEvaluatePokerHand() = getComponent<PokerComponent>()?.canEvaluatePokerHand() ?: false