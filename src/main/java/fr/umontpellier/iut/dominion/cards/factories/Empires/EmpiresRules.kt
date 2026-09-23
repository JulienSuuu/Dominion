package fr.umontpellier.iut.dominion.cards.factories.Empires

import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.game.Game
import fr.umontpellier.iut.dominion.game.rules.GameComponent
import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Supply.SupplyPile
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.Events.Event
import fr.umontpellier.iut.dominion.cards.Events.OnGainEvent
import fr.umontpellier.iut.dominion.cards.builders.branchDecision
import fr.umontpellier.iut.dominion.cards.builders.filterNotNull
import fr.umontpellier.iut.dominion.cards.builders.match
import fr.umontpellier.iut.dominion.cards.component.BiEffect
import fr.umontpellier.iut.dominion.cards.component.gainSpecificCardFromSupply
import fr.umontpellier.iut.dominion.cards.component.increment
import fr.umontpellier.iut.dominion.cards.component.lookingAt
import fr.umontpellier.iut.dominion.cards.factories.Empires.EmpiresFactoryKt.takeAllVictoryPoint
import fr.umontpellier.iut.dominion.cards.gainFromSupply
import fr.umontpellier.iut.dominion.game.rules.EnchantressRule
import fr.umontpellier.iut.dominion.game.rules.IEmpireRules
import kotlinx.coroutines.flow.update
import kotlin.reflect.KClass

class EmpiresRules(val game: Game)  : GameComponent {
    var obeliskTarget = ""
    val rules = mutableMapOf<KClass<out IEmpireRules>, IEmpireRules>()
    inline fun <reified T : IEmpireRules> getRule() = rules[T::class] as? T
    inline fun <reified T : IEmpireRules> addRule(rule : T) {rules[T::class] = rule}
    init {
        if(game.hasCard("Enchantress")) addRule(EnchantressRule(game))
    }

    fun taxPassive(event : OnGainEvent){
        if(event.player.isInBuyPhase){
            val card = event.card ?: return
            val player = event.player

            player.incrementByAction(Item.DEBT){
                card.takePointFrom(SupplyPile.DEBTTAX)
            }
        }
    }

    fun toJson(): String {
        val enchantressJson = getRule<EnchantressRule>()?.let { "${it.toJson()}," } ?: ""

        return """
        {
          $enchantressJson
          "obeliskTarget": "$obeliskTarget"
        }
    """.trimIndent()
    }
}

var Game.obeliskTarget get() = getRule<EmpiresRules>()?.obeliskTarget ?: ""
    set(value) {getRule<EmpiresRules>()?.obeliskTarget = value}


val runRocks = BiEffect.empty<Event, Card>()
    .lookingAt { event, _ -> event.player.isInBuyPhase }
    .branchDecision {
        on {data} then {event, _, _ -> event.player.gainFromSupply("Silver", Destination.PlayerZone.Draw)}
        on {!data} then {event, _, _ -> event.player.gainFromSupply("Silver", Destination.PlayerZone.Hand)}
    }
    .end()

val runCrumblingCastle = BiEffect.empty<Event, Card>()
    .increment(Item.VICTORY_TOKEN)
    .gainSpecificCardFromSupply(cardName = "Silver")

fun runAqueduc(scope : Card) = BiEffect.empty<Player, Event>()
    .lookingAt { _, event -> event.card }.filterNotNull{it.hasType(CardType.TREASURE) || it.hasType(CardType.VICTORY)}
    .match {
        on {data.hasType(CardType.TREASURE)} then {_, _, card -> scope.addPointToFrom(SupplyPile.VICTORYTOKEN, 1, card) }
        on{data.hasType(CardType.VICTORY)} then {player, _, _ -> player.takeAllVictoryPoint(scope) }
    }
    .end()

fun runDefiledShrine(scope : Card) = BiEffect.empty<Player, Event>()
    .lookingAt { _, event -> event.card }.filterNotNull{it.hasType(CardType.ACTION) || it.hasType(CardType.CURSE)}
    .match {
        on {data.hasType(CardType.ACTION)} then {_, _, card -> scope.addPointToFrom(SupplyPile.VICTORYTOKEN, 1, card) }
        on {data.hasType(CardType.CURSE) && player.isInBuyPhase} then {player, _, _ -> player.takeAllVictoryPoint(scope) }
    }
    .end()