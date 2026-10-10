package fr.umontpellier.iut.dominion.domain.components.card.setup

import fr.umontpellier.iut.dominion.domain.model.supply.SupplyPile
import fr.umontpellier.iut.dominion.domain.components.card.CardComponent
import fr.umontpellier.iut.dominion.domain.model.game.Game

class OnSetup(val block : Game.(MutableList<SupplyPile>) -> Unit): CardComponent {
    var neededPlayer = false
    fun execute(game : Game, allSupplyPile : MutableList<SupplyPile>) { game.block(allSupplyPile) }
}