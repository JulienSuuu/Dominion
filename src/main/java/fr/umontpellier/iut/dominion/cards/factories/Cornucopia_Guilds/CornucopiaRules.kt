package fr.umontpellier.iut.dominion.cards.factories.Cornucopia_Guilds

import fr.umontpellier.iut.dominion.Button
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.game.Game
import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.Skills.draw
import fr.umontpellier.iut.dominion.Player.Skills.moveTo
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.Events.OnGainEvent
import fr.umontpellier.iut.dominion.cards.builders.branchDecision
import fr.umontpellier.iut.dominion.cards.component.BiEffect
import fr.umontpellier.iut.dominion.cards.component.InteractionRequest
import fr.umontpellier.iut.dominion.cards.component.chooseCardFromList
import fr.umontpellier.iut.dominion.cards.component.chooseWhatToDo
import fr.umontpellier.iut.dominion.cards.gainFromSupply
import fr.umontpellier.iut.dominion.cards.gainMultiplyCardFromSupply

class CornucopiaRules(private val game: Game) {
    fun footpadPassive(event: OnGainEvent) {
        if (game.isActionPhase) {
            event.player.draw()
            game.log("Règle Footpad : +1 Carte piochée.")
        }
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
