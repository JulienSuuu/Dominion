package fr.umontpellier.iut.dominion.Player.Tokens

import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.Skills.draw
import fr.umontpellier.iut.dominion.Player.Skills.trash
import fr.umontpellier.iut.dominion.cards.Events.Event

object TokenEffect {

    private val effects: Map<Token.OnPile, suspend (Event, Player) -> Unit> = mapOf(
        Token.OnPile.OneMoneyToken to { _, player -> applyPlusOne(player, Item.MONEY) },
        Token.OnPile.OneActionToken to { _, player -> applyPlusOne(player, Item.ACTION) },
        Token.OnPile.OneBuyToken to { _, player -> applyPlusOne(player, Item.BUY) },
        Token.OnPile.OneCardToken to { _, player -> player.draw() },
        Token.OnPile.TrashingToken to ::applyTrashingEffect
    )

    private fun applyPlusOne(player: Player, item: Item) {
        player.increment(item, 1)
    }

    private suspend fun applyTrashingEffect(event: Event, player: Player) {
        player.chooseCardFromHand("you may trash a card from your hand [trashing token]", true)?.let { card ->
            player.trash(card)
        }
    }

    suspend fun execute(token: Token.OnPile, event: Event, player: Player) {
        effects[token]?.invoke(event, player)
    }

    val tokens: Set<Token.OnPile>
        get() = effects.keys
}