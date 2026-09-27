package fr.umontpellier.iut.dominion.game.rules

import fr.umontpellier.iut.dominion.Supply.SupplyPile

interface GameComponent {
    fun getAllSupply() : List<SupplyPile> = emptyList()
}