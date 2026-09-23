package fr.umontpellier.iut.dominion.Supply

import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.component.Price
import kotlinx.coroutines.CoroutineScope

class MixedSupplyPile(
    pileName: String,
    suppliersWithCount: List<Pair<() -> Card, Int>>,
    scope: CoroutineScope? = null,
    private val template: Card? = null
) : AbstractSupplyPile(
    pileName = pileName,
    initialCards = suppliersWithCount.flatMap { (supplier, count) -> List(count) { supplier() } },
    scope = scope
) {

    private val namesCard: Set<String> = cards.value.map { it.name }.toSet()
    
    init {
        template?.supply = this

        updateNameProperty()
    }

    override fun setType() {
        template?.let { types.addAll(it.types) } ?: super.setType()
    }

    override fun update(scope: CoroutineScope) {
        super.update(scope)
        updateNameProperty()
    }

    private fun updateNameProperty() {
        _nameProperty.value = if (isEmpty) (template?.name ?: pileName) else (topCard?.name ?: pileName)
    }

    override val costValue: Int
        get() = if (isEmpty) 0 else maxOf(topCard?.costValue ?: 0, 0)

    override val cost: Price
        get() = if (isEmpty) (template?.price ?: Price.classic(0)) else (topCard?.price ?: Price.classic(0))

    override fun verifyName(name: String): Boolean {
        return name == pileName || namesCard.contains(name) || template?.name == name
    }

    override val supplyName: String
        get() = if (isEmpty) (template?.name ?: pileName) else (topCard?.name ?: pileName)
}