package fr.umontpellier.iut.dominion.domain.dsl.card.actions

import fr.umontpellier.iut.dominion.domain.model.location.Destination
import fr.umontpellier.iut.dominion.domain.common.Logger
import fr.umontpellier.iut.dominion.domain.model.core.Item
import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.player.skills.discardAll
import fr.umontpellier.iut.dominion.domain.model.player.skills.draw
import fr.umontpellier.iut.dominion.domain.model.player.skills.reveals
import fr.umontpellier.iut.dominion.domain.model.card.Card
import fr.umontpellier.iut.dominion.domain.components.card.description.GainDescription
import fr.umontpellier.iut.dominion.domain.components.card.description.InstructionDescription
import fr.umontpellier.iut.dominion.domain.components.card.effect.BiEffect
import fr.umontpellier.iut.dominion.domain.utils.gainFromSupply
import fr.umontpellier.iut.dominion.domain.components.card.effect.then
import fr.umontpellier.iut.dominion.domain.dsl.card.builders.ChooseBuilder
import fr.umontpellier.iut.dominion.domain.dsl.card.builders.ContextBuilder
import fr.umontpellier.iut.dominion.domain.dsl.card.builders.lookingAt
import fr.umontpellier.iut.dominion.domain.dsl.card.core.GainChoice
import fr.umontpellier.iut.dominion.domain.dsl.card.core.PipelineState
import fr.umontpellier.iut.dominion.domain.dsl.card.extensions.filterNotNull

fun <T : BiEffect<U, V>, U : Logger, V, X> ContextBuilder<T, U, V, X>.draw(
    number: Int = 1
): ContextBuilder<T, U, V, X> = thenWith { u, _ ->
    u.toPlayer().draw(number)
}

fun <T : BiEffect<U, V>, U : Logger, V, X> ContextBuilder<T, U, V, X>.increment(
    item: Item,
    number: Int = 1
): ContextBuilder<T, U, V, X> = thenWith { u, _ ->
    u.toPlayer().increment(item, number)
}

fun <T : BiEffect<U, V>, U : Logger, V> ContextBuilder<T, U, V, Card>.revealCard(): ContextBuilder<T, U, V, Card> =
    thenWith { u, card -> u.toPlayer().reveals(card) }

fun <T : BiEffect<U, V>, U : Logger, V, X : List<Card>> ContextBuilder<T, U, V, X>.revealList(): ContextBuilder<T, U, V, X> =
    thenWith { u, cards -> u.toPlayer().reveals(cards) }


private suspend fun executeGainFromSupply(
    player: Player,
    card: Card,
    instruction: GainChoice.() -> String,
    filter: GainChoice.(Card) -> Boolean,
    destination: Destination.PlayerZone,
    canPass: Boolean
) {
    val choice = GainChoice(player, card)
    player.gainFromSupply(choice.instruction(), { choice.filter(it) }, destination, canPass)
}

fun <T : BiEffect<U, V>, U : Logger, V, X, D> ContextBuilder<T, U, V, PipelineState<X, Card, D>>.gainFromSupplyWithPipe(
    target: PipelineState<X, Card, D>.(U, V) -> Player = { u, _ -> u.toPlayer() },
    pipeLineCard: PipelineState<X, Card, D>.(U, V) -> Card = { _, _ -> current },
    instruction: GainChoice.() -> String,
    filter: GainChoice.(Card) -> Boolean = { true },
    destination: Destination.PlayerZone = Destination.PlayerZone.Discard,
    canPass: Boolean = false,
): ContextBuilder<T, U, V, PipelineState<X, Card, D>> = thenDo { u, v, state ->
    executeGainFromSupply(
        player = state.target(u, v),
        card = state.pipeLineCard(u, v),
        instruction = instruction,
        filter = filter,
        destination = destination,
        canPass = canPass
    )
}

fun <T : BiEffect<U, Card>, U : Logger, X> ContextBuilder<T, U, Card, X>.gainFromSupply(
    target: X.(U, Card) -> Player = { u, _ -> u.toPlayer() },
    pipeLineCard: X.(U, Card) -> Card = { _, v -> v },
    instruction: GainChoice.() -> String,
    filter: GainChoice.(Card) -> Boolean = { true },
    destination: Destination.PlayerZone = Destination.PlayerZone.Discard,
    canPass: Boolean = false,
): ContextBuilder<T, U, Card, X> = thenDo { u, v, data ->
    executeGainFromSupply(
        player = data.target(u, v),
        card = data.pipeLineCard(u, v),
        instruction = instruction,
        filter = filter,
        destination = destination,
        canPass = canPass
    )
}

fun <T : BiEffect<U, Card>, U : Logger, X> ContextBuilder<T, U, Card, X>.gainFromSupply(
    target: X.(U, Card) -> Player = { u, _ -> u.toPlayer() },
    pipeLineCard: X.(U, Card) -> Card = { _, v -> v },
    gainChoice: (Card) -> GainDescription? = { it.getDescription<InstructionDescription>()?.getTop<GainDescription>() },
    filter: Card.(Card, Int) -> Boolean = { card, cost -> card.isAtMost(cost) },
): ContextBuilder<T, U, Card, X> = thenDo { u, v, data ->
    val player = data.target(u, v)
    val referenceCard = data.pipeLineCard(u, v)

    val gainDesc = gainChoice(v)
    val instructionText = gainDesc?.let { "$player, ${it.text}" } ?: ""
    val dest = gainDesc?.destination ?: Destination.PlayerZone.Discard
    val optional = gainDesc?.optional ?: false

    val cardPredicate: (Card) -> Boolean = gainDesc?.applyFilter { cardToGain, maxCost ->
        referenceCard.filter(cardToGain, maxCost)
    } ?: { true }

    player.gainFromSupply(instructionText, cardPredicate, dest, optional)
}

fun <T : BiEffect<U, V>, U : Logger, V, X : Any, D : Any> ChooseBuilder<T, U, V, X, Card, D>.gainFromSupply(
    player: (U, V) -> Player = { u, _ -> u.toPlayer() },
    instruction: GainChoice.() -> String,
    filter: GainChoice.(Card) -> Boolean = { true },
    destination: Destination.PlayerZone = Destination.PlayerZone.Discard,
    canPass: Boolean = false
): ChooseBuilder<T, U, V, X, Card, D> {
    val next = result.gainFromSupplyWithPipe(
        target = { u, v -> player(u, v) },
        pipeLineCard = { _, _ -> current },
        instruction = instruction,
        filter = filter,
        destination = destination,
        canPass = canPass
    )
    return ChooseBuilder(parent, next, targetPicker)
}

fun <U : Logger, T : BiEffect<U, Card>> T.gainFromSupply(
    instruction: GainChoice.() -> String,
    filter: GainChoice.(Card) -> Boolean = { true },
    destination: Destination.PlayerZone = Destination.PlayerZone.Discard,
    canPass : Boolean = false
) : BiEffect<U, Card> {
    return this.then{ u, card ->
        val c = GainChoice(u.toPlayer(), card)
        u.toPlayer().gainFromSupply(c.instruction(), {c.filter(it)}, destination, canPass)
    }
}

fun <U : Logger, T : BiEffect<U, Card>> T.gainFromSupply(
    gainChoice : (Card) -> GainDescription? = {it.getDescription<InstructionDescription>()?.getTop<GainDescription>()},
    filter : GainDescription.() -> ((Card) -> Boolean) = {applyFilter()}
) : BiEffect<U, Card> {
    return this.then { u, card ->
        val gainDesc = gainChoice(card)
        val instruction =  gainDesc?.let { "${u.toPlayer()}, ${it.text}" } ?: ""
        val dest = gainDesc?.destination ?: Destination.PlayerZone.Discard
        val optional = gainDesc?.optional ?: false
        u.toPlayer().gainFromSupply(instruction, gainDesc?.filter() ?: {true}, dest, optional )
    }
}

fun <U : Logger, T : BiEffect<U, Card>> T.gainFromSupplyAndWith(
    instruction: GainChoice.() -> String,
    filter: GainChoice.(Card) -> Boolean = { true },
    destination: Destination.PlayerZone = Destination.PlayerZone.Discard,
    canPass : Boolean = false
) : ContextBuilder<T, U, Card, Card?> {
    return this.lookingAt { u, card ->
        val c = GainChoice(u.toPlayer(), card)
        u.toPlayer().gainFromSupply(c.instruction(), {c.filter(it)}, destination, canPass)
    }
}

fun <U : Logger, V> BiEffect<U, V>.draw(number: Int) : BiEffect<U, V>{
    return this.then {  toPlayer().draw(number) }
}

fun <U : Logger, V> BiEffect<U, V>.gainSpecificCardFromSupply(
    target : (U, V) -> Player = {u, _ -> u.toPlayer() },
    cardName : String,
    destination: Destination.PlayerZone = Destination.PlayerZone.Discard,
    silent : Boolean = false
) : BiEffect<U, V>{
    return this.then{u, v ->
        target(u, v).gainFromSupply(cardName, destination, silent)
    }
}

fun <U : Logger, V> BiEffect<U, V>.gainSpecificCardFromSupplyAndDo(
    target : (U, V) -> Player = {u, _ -> u.toPlayer() },
    cardName : String,
    destination: Destination.PlayerZone = Destination.PlayerZone.Discard,
    silent : Boolean = false
) : ContextBuilder<BiEffect<U, V>, U, V, Card> {
    return this.lookingAt{u, v -> target(u, v).gainFromSupply(cardName, destination, silent) }.filterNotNull()
}

fun <V> BiEffect<Logger, V>.discardAll(dest : Destination.PlayerZone ): BiEffect<Logger, V>{
    return this.then { toPlayer().discardAll(dest) }
}

fun <U : Logger, V> BiEffect<U, V>.increment(item : Item, number: Int = 1): BiEffect<U, V>{
    return this.then { toPlayer().increment(item, number) }
}

fun<U : Logger, V> BiEffect<U, V>.reveal( list : Player.() -> List<Card> ): BiEffect<U, V>{
    return this.then {
        val player = toPlayer()
        player.reveals(player.list())
    }
}