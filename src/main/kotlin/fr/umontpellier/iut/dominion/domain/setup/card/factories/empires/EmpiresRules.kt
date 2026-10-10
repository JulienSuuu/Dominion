package fr.umontpellier.iut.dominion.domain.setup.card.factories.empires

import fr.umontpellier.iut.dominion.domain.model.card.CardType
import fr.umontpellier.iut.dominion.domain.model.location.Destination
import fr.umontpellier.iut.dominion.domain.model.game.Game
import fr.umontpellier.iut.dominion.domain.model.core.Item
import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.supply.SupplyPile
import fr.umontpellier.iut.dominion.domain.model.card.Card
import fr.umontpellier.iut.dominion.domain.model.event.Event
import fr.umontpellier.iut.dominion.domain.model.event.OnGainEvent
import fr.umontpellier.iut.dominion.domain.dsl.card.builders.branchDecision
import fr.umontpellier.iut.dominion.domain.dsl.card.builders.match
import fr.umontpellier.iut.dominion.domain.components.card.effect.BiEffect
import fr.umontpellier.iut.dominion.domain.components.game.EnchantressRule
import fr.umontpellier.iut.dominion.domain.setup.card.factories.empires.EmpiresFactoryKt.takeAllVictoryPoint
import fr.umontpellier.iut.dominion.domain.utils.gainFromSupply
import fr.umontpellier.iut.dominion.domain.dsl.card.actions.gainSpecificCardFromSupply
import fr.umontpellier.iut.dominion.domain.dsl.card.actions.increment
import fr.umontpellier.iut.dominion.domain.dsl.card.builders.lookingAt
import fr.umontpellier.iut.dominion.domain.dsl.card.extensions.filterNotNull
import fr.umontpellier.iut.dominion.domain.components.game.IEmpireRules
import fr.umontpellier.iut.dominion.domain.components.game.TurnEndListener
import kotlin.reflect.KClass

class EmpiresRules(val game: Game)  : TurnEndListener {
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

    override suspend fun onTurnEnded(game: Game) { getRule<EnchantressRule>()?.resetTurnTracker() }
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