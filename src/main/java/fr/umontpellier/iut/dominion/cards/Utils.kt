@file:JvmName("CardUtil")

package fr.umontpellier.iut.dominion.cards


import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.Skills.gain
import fr.umontpellier.iut.dominion.Player.Skills.gainSilent
import fr.umontpellier.iut.dominion.Player.Skills.moveTo
import fr.umontpellier.iut.dominion.Player.Skills.reveals
import fr.umontpellier.iut.dominion.cards.Bonus.DominionBonus
import fr.umontpellier.iut.dominion.cards.factories.Nocturne.getAvailableNocturnePile
import fr.umontpellier.iut.dominion.cards.factories.Nocturne.getSpecificNightCard
import javafx.beans.property.IntegerProperty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.function.Consumer
import kotlin.math.min

fun Player.triggerEffect(effectName: String, card: Card, bonus: DominionBonus?) {
    if (bonus == null){
        log("Bonus is null")
        return
    }

    bonus.apply(self, card)

    if (bonus.logNotEmpty) {
        bonus.logs.forEach {
            log("   $it")
        }
    }
}

suspend fun Player.gainCardFromAsideSupply(namePile : String, instruction: String = "", filter: (Card) -> Boolean = {true}, dest: Destination.PlayerZone = Destination.PlayerZone.Discard, canPass: Boolean = false) : Card?{
    val available = game.getAvailableAsidePilesCard(namePile)
    val c = self.chooseCardFromList(instruction, available, canPass, filter)
    return gainIfPresent(c, dest)
}

suspend fun Player.gainCardFromNightSupply(namePile: String, instruction: String = "", filter: (Card) -> Boolean = { true }, dest: Destination.PlayerZone = Destination.PlayerZone.Discard, canPass: Boolean = false) : Card?{
    val available = game.getAvailableNocturnePile(namePile)
    val c = self.chooseCardFromList(instruction, available, canPass, filter)
    return gainIfPresent(c, dest)
}

suspend fun Player.gainSpecificNocturneCard(namePile : String, nameCard : String, dest: Destination.PlayerZone = Destination.PlayerZone.Discard, number: Int = 1) : Card?{
    var c : Card? = null
    for(i in 0 until number){
        c = game.getSpecificNightCard(namePile, nameCard) ?: break
        gainIfPresent(c, dest)
    }
    return c
}

suspend fun Player.gainSpecificAsideCard(namePile : String, nameExtension : String ,dest: Destination.PlayerZone = Destination.PlayerZone.Discard, number: Int = 1) : Card?{
    var c : Card? = null

    for (i in 0 until number){
        c = game.getAvailableAsideCard(namePile, nameExtension) ?: break
        gainIfPresent(c, dest)
    }

    return c
}


suspend fun Player.gainMultiplyCardFromSupply(cardName: String, dest : Destination.PlayerZone, numberOfCards: Int) : Card? {
    var c : Card? = null
    repeat(numberOfCards) {
        c = gainFromSupply(cardName, dest)
        if (c == null) return null
    }
    return c
}


suspend fun Player.gainDifferentCardFromSupply(vararg cardNames : String, dest: Destination.PlayerZone = Destination.PlayerZone.Discard) : List<Card> {
    return cardNames.mapNotNull { name -> gainFromSupply(name, dest) }
}

suspend fun Player.gainMultipleCardFromSupply(instruction: String, filter : (Card) -> Boolean, dest: Destination.PlayerZone, number: Int, canPass: Boolean = false): Card? {
    var c: Card? = null
    for (index in 0 until number) {
        c = gainFromSupply(instruction, filter, dest, canPass)
        if (c == null) break
    }
    return c
}

@JvmOverloads
suspend fun Player.gainFromSupply(cardName: String, dest : Destination.PlayerZone = Destination.PlayerZone.Discard, silent: Boolean = false ) : Card? {
    val card = getCardFromSupply(cardName)
    return gainIfPresent(card, dest, silent)
}

suspend fun Player.gainFromTrash(instruction: String, filter: (Card) -> Boolean, dest: Destination.PlayerZone, canPass: Boolean = false, extraAction : Player.(Card) -> Unit = {}) : Card? {
    val list = game.trashedCards
    val c = self.chooseCardFromList(instruction, list, canPass, filter)
    c?.let { self.extraAction(c) }
    return gainIfPresent(c, dest)
}

@JvmOverloads
suspend fun Player.gainFromSupply(instruction: String, filter: (Card) -> Boolean, dest: Destination.PlayerZone, canPass: Boolean = false
): Card? {
    val available = game.availableSupplyCard.filter(filter)
    val chosenCard = self.chooseCardFromList(instruction, available, canPass) ?: return null
    return gainIfPresent(chosenCard, dest, false)
}

suspend fun Player.forceGainFromSupply(opponent : Player, instruction: String, filter: (Card) -> Boolean, dest: Destination.PlayerZone = Destination.PlayerZone.Discard, canPass: Boolean = false) : Card? {
    val available = game.availableSupplyCard.filter(filter)
    val chosenCard = self.chooseCardFromList(instruction, available, canPass) ?: return null
    return opponent.gainIfPresent(chosenCard, dest, false)
}


suspend fun Player.gainIfPresent(card: Card?, dest: Destination.PlayerZone, silent: Boolean = false ) : Card? {
    if(card != null){
        if(silent) gainSilent(card, dest, true)
        else gain(card, dest)
    }
    return card
}

fun Player.getTopCards(count: Int): MutableList<Card> {
    val draw = getCards(count)
    val result: MutableList<Card> = ArrayList()
    val actualCount = min(draw.size, count)

    for (i in 0 until actualCount) {
        result.add(draw[draw.size - 1 - i])

    }

    return result
}
fun Player.getBottomCards( count: Int = 1): MutableList<Card> {
    val draw = getCards( count)
    val result: MutableList<Card> = ArrayList()
    val actualCount = min(draw.size, count)

    for (i in 0 until actualCount) {
        result.add(draw[i])
    }
    return result
}

fun Player.getCards(count: Int): List<Card> {
    if ((getCopyOf(Destination.PlayerZone.Draw)?.size ?:0) < count) shuffle()
    return getCopyOf(Destination.PlayerZone.Draw)?:emptyList()
}

inline fun Player.moveTo(getter : () -> Card?, setter : (Card?) -> Unit? ,dest: Destination) {
    getter()?.let {
        moveTo(it, dest)
        setter(null)
    }
}

suspend fun Player.executeAmbassador(self:Card){
    chooseCardFromHand("Reveal a card", false)
        ?.let{
        reveals(it)
        handleReplacements(it, 2)
        game.processAttack(this, self){ vi ->
            vi.gainFromSupply(it.name, Destination.PlayerZone.Discard)
        }
    }
}

private suspend fun Player.handleReplacements(revealed: Card, max: Int) {
    for (i in 0 until max) {
        val chosenOptional = chooseCardFromHand(
            "Put back in supply (max $max)",
            predicate = { card -> revealed.hasSameNameAs(card) },
        )

        val card = chosenOptional ?: break

        card.replaceInSupply(revealed)
    }
}

/**
 * Permet de combiner deux prédicats avec un opérateur infix 'or'
 */
infix fun <T> ((T) -> Boolean).or(other: (T) -> Boolean): (T) -> Boolean {
    return { item -> this(item) || other(item) }
}

infix fun <T> ((T) -> Boolean).and(other: (T) -> Boolean): (T) -> Boolean {
    return { item -> this(item) && other(item) }
}

operator fun MutableStateFlow<Int>.plusAssign(value: Int) {
    this.value += value
}

operator fun MutableStateFlow<Int>.minusAssign(value: Int) {
    this.value -= value
}

operator fun MutableStateFlow<Int>.inc(): MutableStateFlow<Int> {
    this.value += 1
    return this
}

operator fun IntegerProperty.dec(): IntegerProperty {
    this.set(this.get() - 1)
    return this
}

operator fun IntegerProperty.plusAssign(value: Int) {
    this.set(this.get() + value)
}

fun <T> MutableStateFlow<List<T>>.getCopy(): MutableList<T> = this.value.toMutableList()



infix fun Flow<Boolean>.or(other: Flow<Boolean>): Flow<Boolean> {
    return combine(this, other) { left, right -> left || right }
}

fun Flow<Int>.greaterThanOrEqualTo(value: Int): Flow<Boolean> {
    return this.map { it >= value }
}

infix fun Flow<Boolean>.and(other: Flow<Boolean>): Flow<Boolean> {
    return combine(this, other) { left, right -> left && right }
}



inline fun List<Card>.count(crossinline counter : Card.() -> Boolean) : Int {
    return this.count { c -> c.counter() }
}

fun MutableList<Card>.addIfNotNull(card : Card?){
    if(card!=null){
        this.add(card)
    }
}

infix fun <E> List<E>.sizeIsEqualTo(other : List<E>) : Boolean {
    return size == other.size
}

operator fun MutableList<Card>.plusAssign(card: Card?) {
    this.addIfNotNull(card)
}

operator fun MutableStateFlow<Boolean>.plusAssign(newValue: Boolean) {
    this.value = newValue
}


fun Pair<List<Card>, List<Card>>.assemble() : List<Card>{
    return mutableListOf<Card>().apply {
        addAll(this@assemble.first)
        addAll(this@assemble.second)
    }
}

fun displayCoin(amount: Int) = "$amount \uD83D\uDFE1"


suspend inline fun <T> Iterable<T>.forEachSuspend(action: suspend (T) -> Unit) {
    for (item in this) {
        action(item)
    }
}
