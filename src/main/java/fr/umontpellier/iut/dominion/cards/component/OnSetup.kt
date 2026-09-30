package fr.umontpellier.iut.dominion.cards.component

import fr.umontpellier.iut.dominion.Supply.SupplyPile
import fr.umontpellier.iut.dominion.game.Game

class OnSetup(val block : Game.(MutableList<SupplyPile>) -> Unit): CardComponent {
    var neededPlayer = false
    fun execute(game : Game, allSupplyPile : MutableList<SupplyPile>) { game.block(allSupplyPile) }
}