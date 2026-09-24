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

        // 3. Regrouper les cartes par coût (ex: { 3 -> [Carte1, Carte2, Carte3], 2 -> [Carte4] })
        val costGroups = cards.groupBy { it.costValue }

        // 4. Calculer la fréquence de chaque coût (ex: [3, 1, 1] pour un brelan)
        val costCounts = costGroups.mapValues { it.value.size }.values.sortedDescending()

        val isFlush = isFlush(cards)       // Au moins un type partagé
        val isStraight = isStraight(costs) // (ex: 2, 3, 4, 5, 6)

        val (type, scoringCards) = when {

            // les 5 cartes font partie du motif
            isStraight && isFlush -> PokerHand.STRAIGHT_FLUSH to cards
            costCounts == listOf(3, 2) -> PokerHand.FULL_HOUSE to cards
            isFlush -> PokerHand.FLUSH to cards
            isStraight -> PokerHand.STRAIGHT to cards

            // --- uniquement les 4 cartes du même coût ---
            costCounts == listOf(4, 1) -> {
                PokerHand.FOUR_OF_A_KIND to costGroups.values.first { it.size == 4 }
            }

            // --- uniquement les 3 cartes du même coût ---
            costCounts == listOf(3, 1, 1) -> {
                PokerHand.THREE_OF_A_KIND to costGroups.values.first { it.size == 3 }
            }

            // --- les 2 cartes de chaque paire ---
            costCounts == listOf(2, 2, 1) -> {
                PokerHand.TWO_PAIR to costGroups.values.filter { it.size == 2 }.flatten()
            }

            // --- les 2 cartes de la paire ---
            costCounts.firstOrNull() == 2 -> {
                PokerHand.PAIR to costGroups.values.first { it.size == 2 }
            }

            // --- retient la carte la plus chère ---
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