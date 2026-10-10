package fr.umontpellier.iut.dominion.domain.model.player.skills

import fr.umontpellier.iut.dominion.domain.model.location.Destination
import fr.umontpellier.iut.dominion.domain.model.core.Flags
import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.card.Card
import fr.umontpellier.iut.dominion.domain.model.event.Discard_Type
import fr.umontpellier.iut.dominion.domain.model.event.Event
import fr.umontpellier.iut.dominion.domain.components.card.trigger.TriggerComponent
import fr.umontpellier.iut.dominion.domain.model.card.isNotIn


private suspend fun Player.processEvent(event: Event) = with(event) {
    if (card?.isNotIn(Destination.PlayerZone.Discard) == true) {
        moveTo(card, Destination.PlayerZone.Discard)
    }

    if (event.isActionDiscard) logDiscard(card)

    card?.let {
        triggerOneCard<TriggerComponent.CheckItselfDiscarded>(card, event)
        hooks.forEach { hook -> hook(event, Unit) }
    }

    if (card?.isNotIn(destination) == false) {
        moveTo(card, destination)
    }
}

suspend fun Player.discard(
    c: Card?,
    type: Discard_Type = Discard_Type.ACTION,
    extraAction: (suspend Player.(Card) -> Unit)? = null
): Boolean {
    val card = c?.takeUnless {
        (getFlag(Flags.resolveBandOfMisfit).value && it.getFlag("cant")) || it.getFlag("unable")
    } ?: return false

    processEvent(Event(card, Destination.PlayerZone.Discard, this, discard = type))
    extraAction?.invoke(this, card)

    return true
}

suspend fun Player.discardFromHand(
    number: Int = 1,
    action: (suspend (Card) -> Boolean)? = null,
    nextAction: (suspend Player.(Int) -> Unit)? = null,
    canPass: Boolean = false,
    instruction: String = "",
    filter: (Card) -> Boolean = { true }
) {
    val handSize = getList(Destination.PlayerZone.Hand).size
    if (handSize == 0) return

    val target = minOf(number, handSize)
    var numberDiscarded = 0
    for (index in 0 until target) {
        val remaining = target - index
        val card = chooseCardFromHand("discard ${if (index > 1) "again" else ""} $remaining $instruction card(s) from your hand", canPass, filter) ?: break
        val success = action?.invoke(card) ?: discard(card)
        if (success) numberDiscarded++
    }

    nextAction?.invoke(this, numberDiscarded)
}

suspend fun Player.discardTo(
    number: Int = -1,
    action: (suspend (Card) -> Boolean)? = null
) {
    val currentSize = getList(Destination.PlayerZone.Hand).size
    val target = if (number == -1) currentSize - 1 else number
    if (currentSize <= target) return
    discardFromHand(currentSize - target, action = action)
}

suspend fun Player.discardAndDo(
    from: Destination.PlayerZone,
    number: Int = 1,
    action: (suspend Player.(Card) -> Boolean)? = null,
    filter: (Card) -> Boolean = { true },
    canPass: Boolean = false,
    instruction: String = "",
    nextAction: (suspend Player.(Int) -> Unit)? = null
): Int {
    val list = getList(from)
    val target = minOf(number, list.size)
    var count = 0
    for (index in 0 until target) {
        val remaining = target - index
        val card = chooseCardFromList("${if (canPass) "you may" else ""} discard again $remaining card(s) from $from $instruction", list, canPass, filter) ?: break
        val success = action?.invoke(this, card) ?: discard(card)
        if (success) count++
    }
    nextAction?.invoke(this, count)
    return count
}

suspend fun Player.discardUntilYouStop(
    from: Destination.PlayerZone,
    instruction: String = "you may discard any card you want from $from",
    playerAction: suspend (Int) -> Unit
) {
    val list = getCopyOf(from)?.toMutableList() ?: mutableListOf()
    var count = 0
    while (list.isNotEmpty()) {
        val card = self.chooseCardFromList(instruction, list, true) ?: break

        if (discard(card)) {
            list.remove(card)
            count++
        }
    }

    playerAction(count)
}

suspend fun Player.discardUntilYouStopAndDo(
    from: Destination.PlayerZone,
    instruction: String = "",
    extraCardInformation: String = "",
    filter: (Card) -> Boolean = { true },
    action: suspend Player.(Card) -> Unit
) {
    val list = getCopyOf(from)?.toMutableList() ?: mutableListOf()
    while (list.isNotEmpty()) {
        val card = self.chooseCardFromList("you may discard any $extraCardInformation card you want from $from $instruction", list, true, filter) ?: break
        if (discard(card)) {
            list.remove(card)
            self.action(card)
        }
    }
}

suspend fun Player.discardAll(from: Destination.PlayerZone) {
    val cardsToDiscard = getCopyOf(from) ?: emptyList()
    if (cardsToDiscard.isEmpty()) return

    cardsToDiscard.forEach { card ->
        moveTo(card, Destination.PlayerZone.Discard)
    }

    cardsToDiscard.forEach { card ->
        discard(card)
    }
}

suspend fun Player.discardList(
    list: List<Card>,
    action: (suspend Player.(Card) -> Unit)? = null
) {
    list.forEach { discard(it, extraAction = action) }
}

suspend fun Player.discardUntil(
    check: (Card) -> Boolean,
    action: suspend Player.(Card) -> Unit
) {
    val toDiscard = mutableListOf<Card>()
    var matchedCard: Card? = null

    while (true) {
        val card = getCardFromDeck() ?: break

        if (check(card)) {
            matchedCard = card
            break
        }
        toDiscard.add(card)
        card.moveToTemp(get(Destination.TempZone.Temp))
    }

    matchedCard?.let { self.action(it) }
    reveals(toDiscard)
    toDiscard.forEach { discard(it) }
}

suspend fun Player.discardUntilAndDo(
    stopCondition: Player.(Int) -> Boolean = { true },
    checkCard: Player.(Card) -> Boolean,
    action: suspend Player.(List<Card>) -> Unit
) {
    val toDiscard = mutableListOf<Card>()
    val toAction = mutableListOf<Card>()
    var number = 0

    while (self.stopCondition(number)) {
        val card = getCardFromDeck() ?: break

        if (self.checkCard(card)) {
            toAction.add(card)
            number++
        } else {
            toDiscard.add(card)
        }

        card.moveToTemp(get(Destination.TempZone.Temp))
    }
    self.action(toAction)
    toDiscard.forEach { discard(it) }
}

suspend fun Player.discardFromDeck() {
    discard(getCardFromDeck())
}

suspend fun Player.discardAList(toDiscard: MutableList<Card>, number: Int): List<Card> {
    if (toDiscard.isEmpty()) return toDiscard

    val target = minOf(number, toDiscard.size)

    for (index in 0 until target) {
        if (toDiscard.isEmpty()) break
        val card = chooseCardFromList("discard again ${target - index} card from this list", toDiscard) ?: break
        discard(card)
        toDiscard.remove(card)
    }

    return toDiscard
}

suspend fun Player.discardListUntilYouStop(
    instructionList: String,
    list: MutableList<Card>,
    filter: (Card) -> Boolean = { true },
    extraAction: (suspend Player.(Card) -> Unit)? = null
): List<Card> {
    if (list.isEmpty()) return list

    while (list.isNotEmpty()) {
        val card = chooseCardFromList(instructionList, list, canPass = true, filter) ?: break
        if (discard(card)) {
            extraAction?.invoke(this, card)
            list.remove(card)
        }
    }

    return list
}