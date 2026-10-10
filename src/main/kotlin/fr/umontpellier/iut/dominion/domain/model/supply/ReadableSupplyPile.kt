package fr.umontpellier.iut.dominion.domain.model.supply

import fr.umontpellier.iut.dominion.domain.model.card.CardType
import fr.umontpellier.iut.dominion.domain.common.IDominionObject
import fr.umontpellier.iut.dominion.domain.model.card.Card
import fr.umontpellier.iut.dominion.domain.model.card.price.Price
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