package fr.umontpellier.iut.dominion.cards.factories.Futaba

import fr.umontpellier.iut.dominion.cards.Card

object PokerHandEvaluator {

    fun evaluate(cards: List<Card>): EvaluatedPokerHand {
        require(cards.size == 5) { "Une main nécessite exactement 5 cartes." }

        val costs = cards.map { it.costValue }.sorted()
        val costGroups = cards.groupBy { it.costValue }
        val costCounts = costGroups.mapValues { it.value.size }.values.sortedDescending()

        val isFlush = isFlush(cards)
        val isStraight = isStraight(costs)

        val (type, scoringCards) = when {
            isStraight && isFlush -> PokerHand.STRAIGHT_FLUSH to cards
            costCounts == listOf(3, 2) -> PokerHand.FULL_HOUSE to cards
            isFlush -> PokerHand.FLUSH to cards
            isStraight -> PokerHand.STRAIGHT to cards

            costCounts == listOf(4, 1) -> {
                PokerHand.FOUR_OF_A_KIND to costGroups.values.first { it.size == 4 }
            }

            costCounts == listOf(3, 1, 1) -> {
                PokerHand.THREE_OF_A_KIND to costGroups.values.first { it.size == 3 }
            }

            costCounts == listOf(2, 2, 1) -> {
                PokerHand.TWO_PAIR to costGroups.values.filter { it.size == 2 }.flatten()
            }

            costCounts.firstOrNull() == 2 -> {
                PokerHand.PAIR to costGroups.values.first { it.size == 2 }
            }

            else -> PokerHand.HIGH_CARD to listOf(cards.maxBy { it.costValue })
        }

        return EvaluatedPokerHand(
            type = type,
            fiveCardHand = cards,
            scoringCards = scoringCards
        )
    }


    fun bestHand(hand: List<Card>): EvaluatedPokerHand? {
        if (hand.size < 5) return null
        return hand.combinations(5)
            .map { evaluate(it) }
            .maxByOrNull { it.type.rank }
    }

    private fun isFlush(cards: List<Card>): Boolean {
        return cards
            .map { it.types.toSet() }
            .reduce { acc, types -> acc.intersect(types) }
            .isNotEmpty()
    }

    private fun isStraight(sortedCosts: List<Int>): Boolean {
        return sortedCosts.distinct().size == 5 && (sortedCosts.last() - sortedCosts.first() == 4)
    }
}

fun <T> List<T>.combinations(k: Int): List<List<T>> {
    if (k == 0) return listOf(emptyList())
    if (isEmpty()) return emptyList()
    val head = first()
    val tail = drop(1)
    return tail.combinations(k - 1).map { listOf(head) + it } + tail.combinations(k)
}