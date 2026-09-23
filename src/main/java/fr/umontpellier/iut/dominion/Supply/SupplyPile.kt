package fr.umontpellier.iut.dominion.Supply

import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Interface.IDominionObject
import fr.umontpellier.iut.dominion.Supply.Event.CardChangeEvent
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.component.Price
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

interface SupplyPile : ReadableSupplyPile {
    companion object {
        const val VICTORYTOKEN = "VICTORY_TOKEN"
        const val DEBTTAX = "DEBT_TAX"
    }

    val nameProperty : StateFlow<String>
    val costValue: Int

    var cursed: Int
    var token: Int
    var supplyType: SupplyType
    var onCardChange: ((CardChangeEvent) -> Unit)?


    fun priceProperty() = cost.coinsProperty
    fun debtProperty() = cost.debtProperty

    fun update(scope: CoroutineScope)

    fun popCard(): Card?

    fun shuffle()
    fun forEach(consumer: (Card) -> Unit) = cards.value.forEach(consumer)
    fun take(numberOfCopies: Int): MutableList<Card>
    fun sortWith(comparator: Comparator<Card>): SupplyPile
    fun replace(card: Card)


    fun last() =  cards.value.last()

    fun getFlag(nameProperty: String): StateFlow<Boolean>
    fun isFlagSet(nameProperty: String): Boolean

    fun getResource(nameProperty: String): StateFlow<Int>

    fun updateResource(resource: String, value: Int)
    fun clearResource(resource : String)
    fun useResource(resource: String, take : Int)


    fun updateFlag(resource: String, value: Boolean)

}