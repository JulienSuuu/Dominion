package fr.umontpellier.iut.dominion.cards.factories.Futaba

import fr.umontpellier.iut.dominion.cards.Card

enum class PokerHand(val rank: Int, val displayName: String) {
    HIGH_CARD(1, "High Card"),
    PAIR(2, "Pair"),
    TWO_PAIR(3, "Two Pair"),
    THREE_OF_A_KIND(4, "Three of a Kind"),
    STRAIGHT(5, "Straight"),
    FLUSH(6, "Flush"),
    FULL_HOUSE(7, "Full House"),
    FOUR_OF_A_KIND(8, "Four of a Kind"),
    STRAIGHT_FLUSH(9, "Straight Flush")
}
data class EvaluatedPokerHand(
    val type: PokerHand,
    val fiveCardHand: List<Card>,
    val scoringCards: List<Card>
)