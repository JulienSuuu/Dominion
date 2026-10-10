package fr.umontpellier.iut.dominion.domain.components.game

import fr.umontpellier.iut.dominion.domain.model.core.Item
import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.game.Game
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class CoffersRule(val game: Game) : ResourceRule {

    override val targetItem = Item.COFFER
    private val maxCoffers = 150

    val coffers: StateFlow<Int> = combine(
        game.players.mapNotNull { it.getPropertyOf(Item.COFFER) }
    ) { values ->
        (maxCoffers - values.sum()).coerceAtLeast(0)
    }.stateIn(
        scope = game.gameScope,
        started = SharingStarted.Eagerly,
        initialValue = maxCoffers
    )

    override fun grant(player: Player, qty: Int): Int {
        val currentSupply = coffers.value
        val actualGranted = minOf(qty, currentSupply)

        if (actualGranted > 0) {
            player.directIncrement(Item.COFFER, qty)
        }

        return actualGranted
    }
}