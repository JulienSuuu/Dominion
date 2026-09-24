package fr.umontpellier.iut.dominion.cards.factories.Futaba

import fr.umontpellier.iut.dominion.cards.Card

enum class PokerHand(val rank: Int, val displayName: String) {
    HIGH_CARD(1, "High Card"),
    PAIR(2, "Pair"),
    TWO_PAIR(3, "Double Pair"),
    THREE_OF_A_KIND(4, "Brelan"),
    STRAIGHT(5, "Straight"),
    FLUSH(6, "Flush"),
    FULL_HOUSE(7, "Full"),
    FOUR_OF_A_KIND(8, "Square"),
    STRAIGHT_FLUSH(9, "Quinte Flush")
}

data class EvaluatedPokerHand(
    val type: PokerHand,
    val fiveCardHand: List<Card>,
    val scoringCards: List<Card>
)