package fr.umontpellier.iut.dominion.game.rules

import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Supply.SupplyPile
import fr.umontpellier.iut.dominion.game.Game

interface GameComponent

interface TurnEndListener: GameComponent {
    suspend fun onTurnEnded(game : Game)
}

interface SupplyContributor : GameComponent {
    fun getSupplyPiles(): List<SupplyPile>
}

interface ResourceRule : GameComponent {
    val targetItem: Item
    fun grant(player: Player, qty: Int): Int
}

interface ResourceInterceptor : GameComponent {
    fun modify(player: Player, item: Item, amount: Int): Int
}



