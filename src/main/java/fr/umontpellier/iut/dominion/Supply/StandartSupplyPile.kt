package fr.umontpellier.iut.dominion.Supply

import fr.umontpellier.iut.dominion.cards.Card
import kotlinx.coroutines.CoroutineScope

class StandardSupplyPile(
    cardSupplier: () -> Card,
    numberOfCopies: Int,
    scope: CoroutineScope? = null
) : AbstractSupplyPile(
    pileName = if (numberOfCopies > 0) cardSupplier().name else "Empty",
    initialCards = List(numberOfCopies) { cardSupplier() },
    scope = scope
)