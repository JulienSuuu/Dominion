package fr.umontpellier.iut.dominion.Player.PlayerComponent

import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.Skills.chooseOrder
import fr.umontpellier.iut.dominion.cards.Events.PokerEvent
import fr.umontpellier.iut.dominion.cards.component.PokerHandReactionComponent
import fr.umontpellier.iut.dominion.cards.factories.Futaba.PokerHandEvaluator

class PokerComponent(val self : Player) : PlayerComponent {

    suspend fun checkPokerReaction(){
        if (self.state.value.turnPhase != PlayerTurnPhase.ActionPhase) return

        val currentHand = self.getList(Destination.PlayerZone.Hand)
        val hasReactionCard = currentHand.any { it.hasComponent<PokerHandReactionComponent>() }

        if (!hasReactionCard || currentHand.size < 5) return

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
}

suspend fun Player.checkPokerReaction(){getComponent<PokerComponent>()?.checkPokerReaction()}