package fr.umontpellier.iut.dominion.cards.Bonus

import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.Skills.draw
import fr.umontpellier.iut.dominion.cards.Card
import java.util.EnumMap

data class MutableBonus(
    val items : Map<Item, (Player, Card) -> Int> = EnumMap(Item::class.java),
    val cardsToDraw : (Player, Card) -> Int = { player, card -> 0}
) : DominionBonus {
    override val logs = mutableListOf<String>()
    override fun apply(player: Player, card: Card) {
        logs.clear()
        val toDraw = cardsToDraw(player, card)
        if(toDraw > 0){
            player.draw(toDraw)
            logs.add("+$toDraw Card${if(toDraw > 1) "s" else ""}")
        }
        items.forEach { (item, action) ->
            val gain = action(player, card)
            if(gain > 0){
                player.increment(item, gain)
                logs.add("+$gain ${item.name.lowercase()}")
            }
        }
    }

    fun with(item : Item, action: (Player, Card) -> Int): MutableBonus {
        val newItems = EnumMap(items).apply { put(item, action) }
        return this.copy(items = newItems)
    }

    fun draw(action: (Player, Card) -> Int): MutableBonus {
        return this.copy(cardsToDraw = action)
    }

}