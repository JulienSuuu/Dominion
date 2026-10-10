package fr.umontpellier.iut.dominion.domain.dsl.card.extensions

import fr.umontpellier.iut.dominion.domain.model.ui.Button
import fr.umontpellier.iut.dominion.domain.model.location.Destination
import fr.umontpellier.iut.dominion.domain.model.core.Item
import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.player.skills.discard
import fr.umontpellier.iut.dominion.domain.model.player.skills.discardFromHand
import fr.umontpellier.iut.dominion.domain.model.player.skills.discardList
import fr.umontpellier.iut.dominion.domain.model.player.skills.moveTo
import fr.umontpellier.iut.dominion.domain.model.player.skills.trash
import fr.umontpellier.iut.dominion.domain.components.card.bonus.Bonus
import fr.umontpellier.iut.dominion.domain.model.card.Card
import fr.umontpellier.iut.dominion.domain.dsl.card.builders.branchDecision
import fr.umontpellier.iut.dominion.domain.components.card.effect.BiEffect
import fr.umontpellier.iut.dominion.domain.dsl.card.core.InteractionRequest
import fr.umontpellier.iut.dominion.domain.components.card.effect.then
import fr.umontpellier.iut.dominion.domain.setup.card.factories.ACTION
import fr.umontpellier.iut.dominion.domain.utils.gainFromSupply
import fr.umontpellier.iut.dominion.domain.utils.getTopCards
import fr.umontpellier.iut.dominion.domain.utils.plusAssign
import fr.umontpellier.iut.dominion.domain.utils.triggerEffect
import fr.umontpellier.iut.dominion.domain.dsl.card.actions.attackOthers
import fr.umontpellier.iut.dominion.domain.dsl.card.actions.draw
import fr.umontpellier.iut.dominion.domain.dsl.card.actions.revealList
import fr.umontpellier.iut.dominion.domain.dsl.card.builders.chooseWhatToDo
import fr.umontpellier.iut.dominion.domain.dsl.card.builders.filter
import fr.umontpellier.iut.dominion.domain.dsl.card.builders.lookingAt

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
                    on {data.isBetween(3, 6)} then { player, _, card -> player.trash(card)}
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