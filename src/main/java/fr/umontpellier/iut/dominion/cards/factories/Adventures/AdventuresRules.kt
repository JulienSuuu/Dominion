package fr.umontpellier.iut.dominion.cards.factories.Adventures

import fr.umontpellier.iut.dominion.Button
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.Skills.discard
import fr.umontpellier.iut.dominion.Player.Skills.discardFromHand
import fr.umontpellier.iut.dominion.Player.Skills.discardList
import fr.umontpellier.iut.dominion.Player.Skills.moveTo
import fr.umontpellier.iut.dominion.Player.Skills.trash
import fr.umontpellier.iut.dominion.cards.Bonus.Bonus
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.builders.branchDecision
import fr.umontpellier.iut.dominion.cards.builders.filterNotNull
import fr.umontpellier.iut.dominion.cards.builders.listIsNotEmpty
import fr.umontpellier.iut.dominion.cards.builders.revealList
import fr.umontpellier.iut.dominion.cards.component.BiEffect
import fr.umontpellier.iut.dominion.cards.component.InteractionRequest
import fr.umontpellier.iut.dominion.cards.component.attackOthers
import fr.umontpellier.iut.dominion.cards.component.chooseWhatToDo
import fr.umontpellier.iut.dominion.cards.component.draw
import fr.umontpellier.iut.dominion.cards.component.filter
import fr.umontpellier.iut.dominion.cards.component.lookingAt
import fr.umontpellier.iut.dominion.cards.component.then
import fr.umontpellier.iut.dominion.cards.factories.ACTION
import fr.umontpellier.iut.dominion.cards.gainFromSupply
import fr.umontpellier.iut.dominion.cards.getTopCards
import fr.umontpellier.iut.dominion.cards.plusAssign
import fr.umontpellier.iut.dominion.cards.triggerEffect

internal val runAmulet =
    BiEffect.empty<Player, Card>()
        .chooseWhatToDo { player, card -> InteractionRequest(
            instruction = "$player, Choose: +1$ or trash a card from your or gain a silver",
            cards = listOf(card),
            buttons = listOf(Button("+1$", "m"), Button.Trash, Button("gain a silver", "s"))
        ) }
        .branch(
            "m" to {player, _, _ -> player.increment(Item.MONEY)},
            "t" to {player, _, _ -> player.trash()},
            "s" to {player, _, _ -> player.gainFromSupply("Silver")},
        )
        .end()



internal fun BiEffect<Player, Card>.runBridgeTroll(): BiEffect<Player, Card> =
    this.then { player, card ->
        player.triggerEffect(ACTION, card, Bonus.Buy)
        player.game.stat.reduction+=1
    }

internal suspend fun runGiantAttack(p : Player, card : Card, bonus : Bonus) {
    val effect = bonus.onPlay()
        .attackOthers { _, opponent, _ ->
            val opponentEffect = BiEffect.empty<Player, Unit>()
                .lookingAt { player, _ -> player.getCardFromDeck() }.filterNotNull()
                .branchDecision{
                    on {data.isBetween(3, 6)} then {player, _, card -> player.trash(card)}
                    otherwise { player, _, card -> player.discard(card); player.gainFromSupply("Curse") }
                }
                .otherwise { player, _ -> player.gainFromSupply("Curse")}
                .end()

            opponentEffect(opponent, Unit)
        }

    effect(p, card)
}

internal suspend fun runRazeReveals(player : Player, trashedCard : Card) {
    val effect = BiEffect.empty<Player, Card>()
        .filter { player, card -> player.trash(card) }
        .map { player, card, _ -> player.getTopCards(card.costValue) }
        .listIsNotEmpty()
        .revealList()
        .chooseCardFromList{player, _ -> InteractionRequest(
            instruction = "$player, put a card from your reveal deck into your hand",
            cards = this,
        ) }
        .thenDo { player, _, cards, chosen ->
            player.moveTo(chosen, Destination.PlayerZone.Hand)
            cards.remove(chosen)
            player.discardList(cards)
        }
        .end()

    effect(player, trashedCard)
}

internal fun BiEffect<Player, Card>.runDungeonDrawAndDiscardPhase() =
    this.draw(2).then { discardFromHand(2) }