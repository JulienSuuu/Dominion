package fr.umontpellier.iut.dominion.domain.dsl.card.extensions

import fr.umontpellier.iut.dominion.domain.model.ui.Button
import fr.umontpellier.iut.dominion.domain.model.location.Destination
import fr.umontpellier.iut.dominion.domain.model.core.Item
import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.player.skills.draw
import fr.umontpellier.iut.dominion.domain.model.player.skills.moveTo
import fr.umontpellier.iut.dominion.domain.model.card.Card
import fr.umontpellier.iut.dominion.domain.model.event.OnGainEvent
import fr.umontpellier.iut.dominion.domain.dsl.card.builders.branchDecision
import fr.umontpellier.iut.dominion.domain.components.card.effect.BiEffect
import fr.umontpellier.iut.dominion.domain.dsl.card.core.InteractionRequest
import fr.umontpellier.iut.dominion.domain.utils.gainFromSupply
import fr.umontpellier.iut.dominion.domain.utils.gainMultiplyCardFromSupply
import fr.umontpellier.iut.dominion.domain.dsl.card.builders.chooseCardFromList
import fr.umontpellier.iut.dominion.domain.dsl.card.builders.chooseWhatToDo

fun footpadPassive(event: OnGainEvent) {
    if (event.player.game.isActionPhase) {
        event.player.draw()
        event.player.game.log("Règle Footpad : +1 Carte piochée.")
    }
}

internal suspend fun runButcherDecision(player : Player, coins : Int) {
    val effect = BiEffect.empty<Player, Int>()
        .chooseWhatToDo { player, coin -> InteractionRequest(
            instruction = "$player, gain a card ${if(player.coffer > 0) "or increment choices by spending coffers (${player.coffer})" else ""}",
            cards = player.game.availableSupplyCard.filter { it isAtMost coin },
            chooseFilter = {true},
            buttons = if(player.coffer > 0) listOf(Button("Lvl up shop", "l")) else emptyList(),
        ) }
        .branchDecision {
            on {"l" == choice } then { player, coin, _ ->
                player.decrement(Item.COFFER)
                runButcherDecision(player, coin+1 )
            }
            on {true} then {player, _, _ -> player.gainFromSupply(this)}
        }
        .endParent()


    effect(player, coins)
}

internal tailrec suspend fun  runHeraldChoose(player : Player, overPaid : Int) {

    if(overPaid == 0)return

    val effect = BiEffect.empty<Player, Int>()
        .chooseCardFromList{player, i -> InteractionRequest(
            instruction = "$player, put again $i card in your deck from your discard",
            cards = player.getList(Destination.PlayerZone.Discard)
        ) }
        .thenWith { player, card ->
            player.moveTo(card, Destination.PlayerZone.Draw)
        }
        .end()

    effect(player, overPaid)
    runHeraldChoose(player, overPaid-1)
}

internal tailrec suspend fun runCourserChoices(
    player : Player,
    self : Card,
    buttons : MutableList<Button> = mutableListOf<Button>().apply {
        add(Button("+2 Card", "card"))
        add(Button("+2 Action", "action"))
        add(Button("+2 Money", "money"))
        add(Button("+4 silver", "silver")) },
    numberToCheck : Int = 2 )
{
    if(numberToCheck == 0) return

    val effect = BiEffect.empty<Player, Card>()
        .chooseWhatToDo { player, card -> InteractionRequest(
            instruction = "$player, Choose $numberToCheck different options",
            cards = listOf(card),
            buttons = buttons
        ) }
        .branch(
            "card" to {player, _, _ -> player.draw(2)},
            "action" to {player, _, _ -> player.increment(Item.ACTION, 2)},
            "buy" to {player, _, _ -> player.decrement(Item.BUY, 2)},
            "silver" to {player, _, _ -> player.gainMultiplyCardFromSupply("Silver", Destination.PlayerZone.Discard, 4)}
        )
        .thenDo { string -> buttons.removeIf { b -> b.value==string } }
        .log { player, string -> "$player chooses $string" }
        .end()

    effect(player, self)
    runCourserChoices(player, self, buttons, numberToCheck-1)
}
