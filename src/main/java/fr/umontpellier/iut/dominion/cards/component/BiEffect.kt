package fr.umontpellier.iut.dominion.cards.component

import fr.umontpellier.iut.dominion.Button
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Interface.Logger
import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.Skills.discardAll
import fr.umontpellier.iut.dominion.Player.Skills.draw
import fr.umontpellier.iut.dominion.Player.Skills.moveTo
import fr.umontpellier.iut.dominion.Player.Skills.reveals
import fr.umontpellier.iut.dominion.Player.Skills.trash
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.Description.GainDescription
import fr.umontpellier.iut.dominion.cards.Description.InstructionDescription
import fr.umontpellier.iut.dominion.cards.builders.ChooseBuilder
import fr.umontpellier.iut.dominion.cards.builders.Context
import fr.umontpellier.iut.dominion.cards.builders.ContextBuilder
import fr.umontpellier.iut.dominion.cards.builders.GainChoice
import fr.umontpellier.iut.dominion.cards.builders.TargetedBuilder
import fr.umontpellier.iut.dominion.cards.builders.attack
import fr.umontpellier.iut.dominion.cards.builders.filterNotNull
import fr.umontpellier.iut.dominion.cards.gainFromSupply


fun interface BiEffect<in U : Logger, in V> : CardComponent, suspend (U, V) -> Unit {
    override suspend fun invoke(u: U, v: V)
    companion object {
        fun <U : Logger, V> empty() : BiEffect<U, V> {
            return BiEffect{_, _ -> }
        }
    }
}


fun <T, U : Logger, V> empty() : BiEffect<U, V> where T : CardComponent, T : BiEffect<U, V> {
    return BiEffect{ _, _ -> }
}
/**
 * Enchaîne une action après l'effet actuel (Alias de then)
 */
infix fun <U : Logger, V, T : BiEffect<U, V>> T.first(after: suspend (U, V) -> Unit): BiEffect<U, V> = then(after)

/**
 * Enchaîne une action après l'effet actuel.
 */
infix fun <U : Logger, V, T : BiEffect<U, V>> T.then(after: suspend (U, V) -> Unit): BiEffect<U, V> {
    return BiEffect { u, v ->
        this(u, v)
        after(u, v)
    }
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



infix fun < U : Logger, V, T : BiEffect<U, V>> T.then(after: suspend U.() -> Unit): BiEffect<U, V> {
    return then { u, _ -> u.after() }
}

/**
 * Répète l'effet actuel un nombre fixe de fois.
 */
infix fun <U : Logger, V, T : BiEffect<U, V>> T.repeat(times: Int): BiEffect<U, V> {
    return BiEffect { u, v ->
        repeat(times) { this(u, v) }
    }
}

/**
 * Conditionne l'exécution de l'effet actuel par un prédicat.
 */
infix fun <U : Logger, V, T : BiEffect<U, V>> T.whenCondition(check: (U, V) -> Boolean): BiEffect<U, V> {
    return BiEffect { u, v ->
        if (check(u, v)) {
            this(u, v)
        }
    }
}

/**
 * Insère une action avant l'effet actuel.
 */
infix fun <U : Logger, V, T : BiEffect<U, V>> T.compose(before: (U, V) -> Unit): BiEffect<U, V> {
    return BiEffect { u, v ->
        before(u, v)
        this(u, v)
    }
}

/**
 * Répète l'effet tant qu'une condition est remplie ou que le nombre maximum est atteint.
 */
fun <U : Logger, V, T : BiEffect<U, V>> T.repeatWhile(check: (U, V) -> Boolean, times: Int): BiEffect<U, V> {
    return BiEffect { u, v ->
        for (i in 0 until times) {
            if (!check(u, v)) break
            this(u, v)
        }
    }
}

fun <U : Logger, V, T : BiEffect<U, V>> T.filter(predicate: suspend (U, V) -> Boolean): ContextBuilder<T, U, V, Boolean> {
    return  this.lookingAt { u, v -> true }.filter { u, v, _ -> predicate(u, v) }
}

/**
 * Bascule vers un ContextBuilder en extrayant une donnée spécifique.
 */
infix fun <U : Logger, V, T, X> T.lookingAt(extractor: suspend (U, V) -> X): ContextBuilder<T, U, V, X> where T : BiEffect<U, V>{
    return ContextBuilder(this) {
        val value = extractor(right, left)
        Context(right, left, value)
    }
}

fun <U : Logger, V, T : BiEffect<U, V>> T.choose(): TargetedBuilder<T, U, V, Boolean> {
    return lookingAt { _, _ -> true }.choose()
}

infix fun <U : Logger, V, T : BiEffect<U, V>> T.choose(target: (U, V) -> Logger): TargetedBuilder<T, U, V, Boolean> {
    return lookingAt { _, _ -> true }.choose(target)
}

fun <U : Logger,V, T : BiEffect<U, V>> T.chooseWhatToDo(target: (U, V) -> Logger = {u,_ -> u}, interactionRequest: InteractionRequest<Unit>): ChooseBuilder<T, U, V, Unit, String, Unit> {
    return lookingAt { _, _ -> }.chooseWhatToDo({u, v -> target(u, v)}){u, v -> interactionRequest}
}

fun <U : Logger,V, T : BiEffect<U, V>> T.chooseWhatToDo(target: (U, V) -> Logger = {u,_ -> u}, interactionRequest: (U, V) -> InteractionRequest<Unit>): ChooseBuilder<T, U, V, Unit, String, Unit> {
    return lookingAt { _, _ -> }.chooseWhatToDo({u, v -> target(u, v)}){u, v -> interactionRequest(u, v)}
}



fun <U : Logger, V, T : BiEffect<U, V>, R> T.map(
    transformation: (U, V) -> R
): ContextBuilder<T, U, V, R> {
    return this.lookingAt { u, v -> transformation(u, v) }
}


fun<U : Logger, V, T : BiEffect<U, V>> T.chooseCardFromHand(target : (U, V) -> Logger = {u,_ -> u}, interactionRequest: InteractionRequest<Unit>): ChooseBuilder<T, U, V, Unit, Card, Unit> {
    return lookingAt { _, _ -> }.chooseCardFromHand({u, v -> target(u, v)}){u, v -> interactionRequest}
}

fun <U : Logger,V, T : BiEffect<U, V>> T.chooseCardFromHand(target: (U, V) -> Logger = {u,_ -> u}, interactionRequest: (U, V) -> InteractionRequest<Unit>): ChooseBuilder<T, U, V, Unit, Card, Unit> {
    return lookingAt { _, _ -> }.chooseCardFromHand ({u, v -> target(u, v)}){u, v -> interactionRequest(u, v)}
}

fun <U : Logger,V, T : BiEffect<U, V>> T.trashCardFromHand(
    target: (U, V) -> Logger = {u,_ -> u},
    canPass : Boolean = false,
    filter : (Card) -> Boolean = {true},
    extraInstruction : String = "",
    interactionRequest: (U, V) -> InteractionRequest<Unit> = { u, _ -> InteractionRequest(
        instruction = "${u.toPlayer()},${if (canPass) " you may" else ""} trash a $extraInstruction card from your hand",
        canPass = canPass,
        filter = filter
    ) }
): ChooseBuilder<T, U, V, Unit, Card, Unit>
{
    return lookingAt { _, _ ->  }.chooseCardFromHand ({u, v -> target(u, v)}){u, v -> interactionRequest(u, v)}.filter{u, card -> u.toPlayer().trash(card)}
}

fun <U : Logger,V, T : BiEffect<U, V>> T.chooseCardFromList(target: (U, V) -> Logger = {u,_ -> u}, interactionRequest: (U, V) -> InteractionRequest<Unit>): ChooseBuilder<T, U, V, Unit, Card, Unit> {
    return lookingAt { _, _ -> }.chooseCardFromList ({u, v -> target(u, v)}){u, v -> interactionRequest(u, v)}
}

fun<U : Logger, V, T : BiEffect<U, V>> T.chooseCardFromList(target : (U, V) -> Logger = {u, _ -> u}, interactionRequest: InteractionRequest<Unit>): ChooseBuilder<T, U, V, Unit, Card, Unit> {
    return lookingAt { _, _ -> }.chooseCardFromList({u, v -> target(u, v)} ){u, v -> interactionRequest}
}

fun<U : Logger, V, T : BiEffect<U, V>> T.chooseCardFromSupply(target : (U, V) -> Logger = {u, _ -> u}, interactionRequest: (U, V) -> InteractionRequest<Unit>): ChooseBuilder<T, U, V, Unit, Card, Unit> {
    return lookingAt { _, _ -> }.chooseCardFromSupply({ u, v -> target(u, v)} ){ u, v -> interactionRequest(u, v)}
}



fun BiEffect<Player, Card>.attack(playerLogic : suspend (attacker:Player, opponent:Player, card:Card) -> Unit) : ContextBuilder<BiEffect<Player, Card>, Player, Card, Boolean>{
    return this.lookingAt { _, _-> true }.attack{player, opponent, card -> playerLogic(player, opponent, card)}
}

fun <U : Logger> BiEffect<U, Card>.attackOthers(playerLogic: suspend (attacker: Player, opponent: Player, card : Card) -> Unit) : BiEffect<U, Card>{
    return this.then{u, card ->
        u.toPlayer().game.processAttack(u.toPlayer(), card){vi -> playerLogic(u.toPlayer(), vi, card)}
    }
}



class LoopCounter(var current: Int = 0, val max : Int = 0){
    val remaining get() = max - current
}

fun <U : Logger, V, X> BiEffect<U, V>.loop(
    number: Int,
    block: BiEffect<U, V>.(LoopCounter) -> ContextBuilder<BiEffect<U, V>, U, V, X>
): BiEffect<U, V> {
    return this.then { u, v ->
        val counter = LoopCounter(max = number)
        val effect = BiEffect.empty<U, V>().block(counter)
        var currentData: Any? = Unit

        for(iterator in 0 until number) {
            val ctx = effect.function(Context(u, v, currentData))
            if(ctx.data == null) break
            currentData = ctx.data
            counter.current++
        }
    }
}




fun <U : Logger, V> BiEffect<U, V>.benefit(playerLogic: suspend (Player) -> Unit) : BiEffect<U, V> =
    this.then { u, v -> u.toPlayer().game.processBenefit(u.toPlayer(), playerLogic ) }

fun <U : Logger, V> BiEffect<U, V>.globalEffect(playerLogic: suspend (Player, V, Player) -> Unit) : BiEffect<U, V> {
    return this.then { u, v -> u.toPlayer().game.processGlobalEffect(u.toPlayer(), {z -> playerLogic(u.toPlayer(), v, z) } ) }
}

fun <U : Logger> BiEffect<U, Card>.processHandDown(dest : Destination.PlayerZone = Destination.PlayerZone.Discard, toReach : Int = 1, mayDiscard : Boolean = false) : BiEffect<U, Card>{
    return this.then { u, v -> u.toPlayer().game.processHandDown(u.toPlayer(), v, dest, toReach, mayDiscard) }
}

/**
 * Variante de lookingAt extrayant une Pair de données native en Kotlin.
 */
infix fun <U : Logger, V, T : BiEffect<U, V>, X, Y> T.lookingAtPair(extractor: suspend (U, V) -> Pair<X, Y>): ContextBuilder<T, U, V, Pair<X, Y>> {
    return ContextBuilder(this) {
        val pair = extractor(right, left)
        Context(right, left, pair)
    }
}


fun BiEffect<Player, Card>.spyEffect(
    target: (Player, Card) -> Player
): ContextBuilder<BiEffect<Player, Card>, Player, Card, String> {
    return this
        .lookingAt { player, card -> target(player, card).getCardFromDeck() }
        .filterNotNull()
        .chooseWhatToDo { _, _ -> InteractionRequest(
            instruction = "Do you want to discard ?",
            cards = listOf(this),
            buttons = Button.yesOrNo,
            canPass = true
        ) }.filter { "y" == it }
        .thenDo { player, card, chosen ->
            target(player, card).moveTo(chosen, Destination.PlayerZone.Discard)

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

