package fr.umontpellier.iut.dominion.domain.components.game

import fr.umontpellier.iut.dominion.domain.model.core.Item
import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.supply.SupplyPile
import fr.umontpellier.iut.dominion.domain.model.game.Game

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



