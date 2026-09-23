package fr.umontpellier.iut.dominion.Supply

import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.component.Price
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object EmptySupply : ReadableSupplyPile {
    override val pileName: String = "EmptySupply"
    override val supplyName: String get() = pileName
    override val cards: StateFlow<List<Card>> = MutableStateFlow(emptyList())
    override val isEmpty: Boolean = true
    override val isNotEmpty: Boolean = false
    override val size: Int = 0;
    override val topCard: Card? = null
    override val cost: Price = Price.darkAges(0)
    override fun verifyName(name: String): Boolean = false

    override fun hasType(type: CardType): Boolean = false

    override fun contains(card: Card): Boolean = false

    override val name: String = "EmptySupply"
}