package fr.umontpellier.iut.dominion.Supply

import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Interface.IDominionObject
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.component.Price
import kotlinx.coroutines.flow.StateFlow

interface ReadableSupplyPile : IDominionObject {
    val pileName: String
    val supplyName: String
    val cards: StateFlow<List<Card>>
    val isEmpty: Boolean
    val isNotEmpty: Boolean
    val size: Int
    val topCard: Card?
    val cost: Price
    fun verifyName(name: String): Boolean
    fun hasType(type: CardType): Boolean
    fun contains(card: Card): Boolean
}