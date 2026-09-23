package fr.umontpellier.iut.dominion.Supply

import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Supply.Event.CardChangeEvent
import fr.umontpellier.iut.dominion.addListListener
import fr.umontpellier.iut.dominion.bind
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.component.Price
import fr.umontpellier.iut.dominion.unbind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.function.Predicate
import kotlin.math.max

abstract class AbstractSupplyPile(
    override val pileName: String,
    initialCards: List<Card> = emptyList(),
    protected val scope: CoroutineScope? = null
) : SupplyPile {

    override var supplyType = SupplyType.NORMAL
    protected val _cards = MutableStateFlow<List<Card>>(emptyList())
    override val cards: StateFlow<List<Card>> get() = _cards

    protected val _nameProperty = MutableStateFlow(pileName)
    override val nameProperty: StateFlow<String> get() = _nameProperty

    override val supplyName: String get() = topCard?.name ?: pileName
    override val name: String get() = supplyName

    override var cursed: Int = 0
    override var token: Int = 0

    val flows = mutableMapOf<String, MutableStateFlow<Int>>()
    val flags = mutableMapOf<String, MutableStateFlow<Boolean>>()

    protected var available: Predicate<Player>? = null
    protected val types = mutableSetOf<CardType>()
    protected var copy = mutableMapOf<String, Any>()

    override var onCardChange: ((CardChangeEvent) -> Unit)? = null

    init {
        initialCards.forEach { card -> card.moveTo(_cards, Destination.Supply); card.supply = this }

        if (scope != null) {
            _cards.addListListener(scope) { change ->
                var hasChanged = false

                if (change.wasRemoved()) {
                    hasChanged = true
                    change.removed.forEach { c ->
                        c.price.coinsProperty.unbind()
                        c.price.debtProperty.unbind()
                    }
                    sendRemoveToUi(change.removed)
                }

                if (change.wasAdded()) {
                    hasChanged = true
                    sendAddToUi(change.added)
                }

                if (hasChanged) update(scope)
            }
        }

        getFlag("inGame").value = true
    }

    override val isEmpty: Boolean get() = _cards.value.isEmpty()
    override val isNotEmpty: Boolean get() = !isEmpty
    override val size: Int get() = _cards.value.size
    override val topCard: Card? get() = _cards.value.lastOrNull()

    override val cost: Price get() = topCard?.price ?: Price.classic(0)
    override val costValue: Int get() = max(cost.coinsProperty.value, 0)

    override fun popCard(): Card? {
        return _cards.value.lastOrNull()
    }

    override fun verifyName(name: String): Boolean = this.pileName == name || this.supplyName == name

    override fun hasType(type: CardType): Boolean {
        if (types.isEmpty()) setType()
        return types.contains(type)
    }

    protected open fun setType() {
        topCard?.let { types.addAll(it.types) }
    }

    override fun shuffle() {
        _cards.value = _cards.value.shuffled()
    }

    override fun take(numberOfCopies: Int): MutableList<Card> {
        val taken = _cards.value.takeLast(numberOfCopies).toMutableList()
        return taken
    }

    override fun sortWith(comparator: Comparator<Card>): SupplyPile {
        _cards.value = _cards.value.sortedWith(comparator)
        return this
    }

    override fun contains(card: Card) = card in cards.value

    open override fun update(scope : CoroutineScope) {

        val pileCost = priceProperty()

        if (isEmpty) return

        popCard()?.let { card ->

            val cardCost = card.price

            cardCost.coinsProperty.unbind()

            cardCost.debtProperty.unbind()

            cardCost.coinsProperty.bind(scope, pileCost)
            cardCost.debtProperty.bind(scope, debtProperty())

            copy = HashMap(card.properties)
        }

    }

    override fun replace(card: Card) {
        card.clear()
        copy.forEach { (key, value) -> card.set(key, value) }
        card.moveTo(_cards, Destination.Supply)
    }

    override fun updateFlag(resource: String, value : Boolean) { getFlag(resource).update{value} }


    override fun clearResource(resource : String) {flows[resource]?.update { 0 }}
    override fun useResource(resource: String, take: Int) {flows[resource]?.update{ it - take}}
    override fun updateResource(resource: String, value: Int) { getResource(resource).update { it + value } }

    override fun isFlagSet(nameProperty: String): Boolean { return flags[nameProperty]?.value ?: false }

    private fun sendAddToUi(addedCards: List<Card>) {
        if (addedCards.isEmpty()) return
        onCardChange?.invoke(CardChangeEvent("ADD", supplyName, size, addedCards.map { it.name }, topCard))
    }

    private fun sendRemoveToUi(removedCards: List<Card>) {
        if (removedCards.isEmpty()) return
        onCardChange?.invoke(CardChangeEvent("REMOVE", supplyName, size, removedCards.map { it.name }, topCard))
    }

    override fun getFlag(nameProperty: String): MutableStateFlow<Boolean> = flags.getOrPut(nameProperty) { MutableStateFlow(false) }
    override fun getResource(nameProperty: String): MutableStateFlow<Int> = flows.getOrPut(nameProperty) { MutableStateFlow(0) }
}