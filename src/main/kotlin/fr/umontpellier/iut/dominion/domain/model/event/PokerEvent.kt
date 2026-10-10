package fr.umontpellier.iut.dominion.domain.model.event

import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.card.Card
import fr.umontpellier.iut.dominion.domain.setup.card.factories.futaba.EvaluatedPokerHand
import fr.umontpellier.iut.dominion.domain.setup.card.factories.futaba.PokerHand

class PokerEvent(player: Player, val evaluatedPokerHand: EvaluatedPokerHand) : Event(player) {
    override fun isPokerHand(type: PokerHand): Boolean { return evaluatedPokerHand.type == type }
    override fun getEvaluatedHand(): List<Card> { return evaluatedPokerHand.scoringCards }
}