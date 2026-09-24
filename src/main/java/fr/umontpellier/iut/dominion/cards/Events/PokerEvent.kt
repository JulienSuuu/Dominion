package fr.umontpellier.iut.dominion.cards.Events

import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.factories.Futaba.EvaluatedPokerHand
import fr.umontpellier.iut.dominion.cards.factories.Futaba.PokerHand

class PokerEvent(player: Player, val evaluatedPokerHand: EvaluatedPokerHand) : Event(player) {
    override fun isPokerHand(type: PokerHand): Boolean { return evaluatedPokerHand.type == type }
    override fun getEvaluatedHand(): List<Card> { return evaluatedPokerHand.scoringCards }



}