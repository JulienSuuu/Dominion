package fr.umontpellier.iut.dominion.cards.factories.Futaba

import fr.umontpellier.iut.dominion.cards.Card

object PokerHandEvaluator {

    /**
     * Évalue un lot de 5 cartes pour déterminer la meilleure main de poker
     * et isoler les cartes qui composent le motif.
     */
    fun evaluate(cards: List<Card>): EvaluatedPokerHand {
        require(cards.size == 5) { "Une main nécessite exactement 5 cartes." }

        val costs = cards.map { it.costValue }.sorted()

        // 1. Groupement par coût global
        val costGroups = cards.groupBy { it.costValue }
        val costCounts = costGroups.mapValues { it.value.size }.values.sortedDescending()

        // 2. Groupement par coût de cartes DISTINCTES
        val distinctCostGroups = cards.distinctBy { it.name }.groupBy { it.costValue }
        val distinctCostCounts = distinctCostGroups.mapValues { it.value.size }.values.sortedDescending()

        // 3. Groupement par nom exact
        val nameGroups = cards.groupBy { it.name }
        val nameCounts = nameGroups.mapValues { it.value.size }.values.sortedDescending()

        val isFlush = isFlush(cards)
        val isStraight = isStraight(costs)

        val (type, scoringCards) = when {
            isStraight && isFlush -> PokerHand.STRAIGHT_FLUSH to cards
            costCounts == listOf(3, 2) -> PokerHand.FULL_HOUSE to cards
            isFlush -> PokerHand.FLUSH to cards
            isStraight -> PokerHand.STRAIGHT to cards

            // Carré : 4 cartes de même coût mais de noms différents
            distinctCostCounts == listOf(4, 1) -> {
                PokerHand.FOUR_OF_A_KIND to distinctCostGroups.values.first { it.size == 4 }
            }

            // Brelan : 3 cartes de même coût mais de noms différents
            distinctCostCounts == listOf(3, 1, 1) -> {
                PokerHand.THREE_OF_A_KIND to distinctCostGroups.values.first { it.size == 3 }
            }

            // Double Paire : 2 paires de cartes identiques (ex: 2x Cuivre + 2x Village)
            nameCounts == listOf(2, 2, 1) -> {
                PokerHand.TWO_PAIR to nameGroups.values.filter { it.size == 2 }.flatten()
            }

            // Paire : 2 exemplaires de la même carte
            nameCounts.firstOrNull() == 2 -> {
                PokerHand.PAIR to nameGroups.values.first { it.size == 2 }
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
        val has5DistinctCards = cards.distinctBy { it.name }.size == 5
        if (!has5DistinctCards) return false

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