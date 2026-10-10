package fr.umontpellier.iut.dominion.domain.components.card.bonus

import fr.umontpellier.iut.dominion.domain.common.Logger
import fr.umontpellier.iut.dominion.domain.model.core.Item
import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.player.skills.draw
import fr.umontpellier.iut.dominion.domain.model.card.Card
import fr.umontpellier.iut.dominion.domain.components.card.effect.BiEffect
import fr.umontpellier.iut.dominion.domain.components.card.duration.DurationComponent
import fr.umontpellier.iut.dominion.domain.components.card.trigger.TriggerComponent
import fr.umontpellier.iut.dominion.domain.setup.card.factories.DURATION
import fr.umontpellier.iut.dominion.domain.setup.card.factories.EFFECT
import fr.umontpellier.iut.dominion.domain.utils.triggerEffect
import java.util.EnumMap

data class Bonus(
    val items: Map<Item, Int> = EnumMap(Item::class.java),
    val cardsToDraw: Int = 0
) : DominionBonus {

    override val logs: List<String> by lazy {
        buildList {
            if (cardsToDraw > 0) {
                add("+$cardsToDraw Card${if (cardsToDraw > 1) "s" else ""}")
            }
            items.forEach { (item, qty) ->
                if (qty > 0) add("+$qty ${item.name.lowercase()}")
            }
        }
    }
    
    /**
     * Ajoute ou remplace un élément (Item) avec sa quantité dans le bonus.
     * Utilise `copy()` pour retourner une nouvelle instance immuable.
     */
    fun with(item: Item, qty: Int = 1): Bonus {
        val newItems = EnumMap(items).apply { put(item, qty) }
        return this.copy(items = newItems)
    }

    /**
     * Modifie la quantité de cartes à piocher dans le bonus.
     */
    fun draw(qty: Int = 1): Bonus {
        return this.copy(cardsToDraw = qty)
    }

    fun <U : Logger> effect(): BiEffect<U, Card> = BiEffect{ logger, card -> logger.toPlayer().triggerEffect(EFFECT, card, this) }


    fun onDuration(): DurationComponent.Duration =
        DurationComponent.Duration{player, c -> player.triggerEffect(DURATION, c, this)}

    fun onBuy() : TriggerComponent.CheckItSelfBuy = TriggerComponent.CheckItSelfBuy{event, c -> event.player.triggerEffect(EFFECT, c, this)}
    fun onDiscard() : TriggerComponent.CheckItselfDiscarded = TriggerComponent.CheckItselfDiscarded {event, c -> event.player.triggerEffect(EFFECT, c, this)}


    override fun apply(player: Player, card : Card) {
        if(cardsToDraw > 0){ player.draw(cardsToDraw) }
        items.forEach { (item, i) -> if(i>0) player.increment(item, i) }
    }


    fun toDescription() : MutableMap<String, Int> {
        val mutableMap = mutableMapOf<String, Int>()
        mutableMap.putAll(items.map { (k, v) -> k.name.lowercase() to (v) })
        mutableMap["cards"] = (cardsToDraw)
        return mutableMap
    }



    companion object {

        /**
         * Crée un bonus vide.
         * Grâce aux valeurs par défaut du constructeur, on peut juste appeler Bonus().
         */
        @JvmStatic
        fun empty(): Bonus = Bonus()
        fun money(qty: Int = 1): Bonus = Bonus().with(Item.MONEY, qty)
        fun buy(qty: Int = 1): Bonus = Bonus().with(Item.BUY, qty)
        fun action(qty: Int = 1): Bonus = Bonus().with(Item.ACTION, qty)
        fun draw(qty: Int = 1): Bonus = Bonus().draw(qty)
        fun coffer(qty : Int = 1) = Bonus().with(Item.COFFER, qty)
        fun debt(qty: Int = 1) = Bonus().with(Item.DEBT, qty)
        fun victoryToken(qty: Int = 1) = Bonus().with(Item.VICTORY_TOKEN, qty)
        fun fish(qty: Int = 1) = Bonus().with(Item.FISH, qty)


        val Money = money()
        val Buy = buy()
        val Action = action()
        val draw = draw()
        val Coffer = coffer()
        val ActionAndDraw = action().draw()
        val enchantressEffect = ActionAndDraw
        val VictoryToken = victoryToken()
        val FISH = fish()


    }
}
