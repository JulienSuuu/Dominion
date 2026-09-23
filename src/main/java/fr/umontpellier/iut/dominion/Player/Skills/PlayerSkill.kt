package fr.umontpellier.iut.dominion.Player.Skills

import fr.umontpellier.iut.dominion.Button
import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.PlayerComponent.PlayerComponent
import fr.umontpellier.iut.dominion.Player.PlayerComponent.PlayerInTurnState
import fr.umontpellier.iut.dominion.Player.PlayerComponent.PlayerTurnPhase
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.Events.Event
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent
import fr.umontpellier.iut.dominion.Player.PlayerComponent.checkPlayToken
import fr.umontpellier.iut.dominion.cards.isNotIn
import fr.umontpellier.iut.dominion.client.StatKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update


class PlayerSkill : PlayerComponent {

}

suspend fun Player.playCard(c : Card?, number : Int = 1){
    if(c == null || number == 0) return
    state.update { it.changeInTurnState(PlayerInTurnState.PlayingCard(c)) }
    logPlay(c)
    c.moveTo(get(Destination.PlayerZone.InPlay), Destination.PlayerZone.InPlay)
    val event = Event(c, Destination.PlayerZone.InPlay, self)
    repeat(number){
        triggerPlayEvent(event)
    }

    client.recordHoverDetail(StatKey.TOTAL_CARDS_PLAYED, c.name, number)
}


suspend fun Player.playCardFromList(list : List<Card>, instruction : String = "play those cards in any order", canPass : Boolean = false, filter : (Card) -> Boolean = {true}, action : suspend Player.(Card) -> Unit = {}){
    if(list.isEmpty())return
    if(list.size == 1){ self.playCard(list.first()); self.action(list.first()); return}
    val mutable = list.toMutableList()
    while(mutable.isNotEmpty()){
        val c = self.chooseCardFromList(instruction, mutable, canPass, filter) ?: break

        mutable.remove(c)
        self.playCard(c)
        self.action(c)
    }
}

private suspend fun Player.triggerPlayEvent(event: Event) {
    val card = event.card

    if (card?.hasType(CardType.ACTION) == true) increment(Item.ACTION_PLAYED, 1)

    checkPlayToken(event)
    triggerEvent<TriggerComponent.BeforeCardPlayed>(event)

    card?.play(self)
    if (card?.isNotIn(Destination.PlayerZone.InPlay) == true) card.set("unable", true)

    triggerEvent<TriggerComponent.OnCardPlayed>(event)
    triggerEvent<TriggerComponent.AfterCardPlayed>(event)

    if (card != null && card.hasType(CardType.RESERVE) && !card.getFlag("unable")) {
        card.moveTo(get(Destination.OtherZone.Tavern), Destination.OtherZone.Tavern)
    }
}

suspend fun Player.handleStartTurn(){
    setUpTurn()
    nextTurnEffect.forEach { self.it() }
    nextTurnEffect.clear()

    triggerDurationCards()
    triggerStart<TriggerComponent.OnStartTurn>()
}

fun Player.handleActionPhase() {
    getFlag("Action").value = true
    getFlag("Treasure").value = true
    getFlag("StartBuyPhase").value = false
}

suspend fun Player.playTurn(){
    while (!turnWasEnded){
        val choice = chooseYourChoice().takeIf { it.isNotBlank() } ?: break
        val (type, value) = splitChoice(choice)

        when(type){
            "BUTTON" -> doButtonAction(value)
            "HAND" -> playPhases(value)
            "SUPPLY", "EVENT" -> buyPhase(value, type)
        }

         if(!canBuy) triggerEndBuyPhases()
    }
}

private val Player.canBuy get() = getValueOf(Item.BUY) > 0
private val Player.canPlayAction get() = getFlag("Action").value
private val Player.canPlayTreasure get() = getFlag("Treasure").value
private val Player.turnWasEnded get() = !canPlayAction && !canPlayTreasure && !canBuy

private suspend fun Player.triggerEndBuyPhases(){
    triggerEvent<TriggerComponent.OnEndBuy>()
    triggerActiveEffect<TriggerComponent.OnEndBuy>()
    triggerPlayerAndCardTavern<TriggerComponent.OnEndBuy>(Event(self))
}

private fun Player.doButtonAction(action : String){
    when(action){
        "COFFER" -> useCoffer()
        "DEBT" -> repayDebt()
    }
}

private suspend fun Player.chooseYourChoice() : String {
    val choices = mutableListOf<String>()
    computeChoicesInHand(choices)
    val buttons = computeButtons()
    val instruction = computeInstructions()

    return choose(instruction, choices, listOf(),  buttons, true)
}

private fun splitChoice(choice: String): Pair<String, String> {
    val parts = choice.split(":", limit = 2)
    return parts[0] to parts.getOrElse(1) { "" }
}


private fun Player.computeChoicesInHand(choices : MutableList<String>) : MutableList<String>{
    for(c in getCopyOf(Destination.PlayerZone.Hand)?:emptyList()){
        if(canPlayAction && c.hasType(CardType.ACTION) && getValueOf(Item.ACTION) > 0){
            choices.add("HAND:${c.name}")
        }
        else if(canPlayTreasure && c.hasType(CardType.TREASURE)){
            choices.add("HAND:${c.name}")
        }
    }

    game.availableSupplyCard.filter { canBuy(it) }.forEach { choices.add("SUPPLY:${it.name}") }
    game.eventCards.filter { canBuy(it) }.forEach { choices.add("EVENT:${it.name}") }
    return choices
}

private fun Player.computeButtons() : MutableList<Button>{
    val buttons = mutableListOf<Button>()
    if(getValueOf(Item.DEBT) > 0){
        buttons.add(Button("Repay", "DEBT"))
    }

    if(getValueOf(Item.COFFER)>0){
        buttons.add(Button("Coffer (${getValueOf(Item.COFFER)})", "COFFER"))
    }

    return buttons
}

private fun Player.computeInstructions() : String {
    return if (state.value.inBuyPhase) {
        "CHOOSE AN EVENT OR CARD TO BUY | BUY PHASE"
    } else {
        val instruction = mutableListOf<String>()
        if (canPlayAction) instruction.add("ACTION")
        if (canPlayTreasure) instruction.add("TREASURE")
        instruction.add("BUY")
        "CHOOSE : " + instruction.joinToString(" | ")
    }
}


private suspend fun Player.playPhases(card: String){
    val c = getCopyOf(Destination.PlayerZone.Hand)?.first { it.hasName(card) }
    c?.let {
        playCard(c)
        if(c.hasType(CardType.ACTION)) playActionPhase()
        else if(c.hasType(CardType.TREASURE)) getFlag("Action").value = false
    }

}

private fun Player.playActionPhase(){
    decrement(Item.ACTION, 1)
    if(getValueOf(Item.ACTION) <= 0){
        getFlag("Action").value = false
    }
}

suspend fun Player.handleStartBuyPhase(){
    getFlag("StartBuyPhase").value = true
    triggerStartBuyPhase()
}

private suspend fun Player.buyPhase(c: String, choice: String) {
    if (state.value.turnPhase == PlayerTurnPhase.ActionPhase) {
        state.update { it.changeTurnState(PlayerTurnPhase.StartBuyPhase) }
    }

    state.first{it.turnPhase == PlayerTurnPhase.BuyPhase}

    val card = when (choice) {
        "SUPPLY" -> getCardFromSupply(c)
        "EVENT" -> game.getEvent(c)
        else -> return
    } ?: return

    decrement(Item.BUY, 1)
    getFlag("Action").value = false
    getFlag("Treasure").value = false

    buyCard(card)
}

/**
 * Pioche et déplace des cartes dans la zone temporaire du joueur jusqu'à ce qu'un
 * certain nombre de cartes valident le critère [condition], ou que le deck soit vide.
 * * @param count Le nombre d'éléments cibles à trouver (ex: 2 pour l'Aventurier).
 * @param condition Le prédicat à valider (ex: est un Trésor).
 * @return La liste complète de toutes les cartes qui ont été manipulées/dévoilées.
 */
fun Player.getCardsUntil(count: Int, condition: (Card) -> Boolean): List<Card> {
    val allRevealed = mutableListOf<Card>()
    var matchesFound = 0

    while (matchesFound < count) {
        val card = getCardFromDeck() ?: break

        this.moveToTemp(card)
        allRevealed.add(card)

        if (condition(card)) {
            matchesFound++
        }
    }

    return allRevealed
}


suspend fun Player.triggerStartBuyPhase(){
    val landMarks = game.landMarks
    chooseOrder<TriggerComponent.OnStartBuyPhase>("Choose the resolution order for your 'Start of Buy phase' effects :", {landMarks}){ card ->
        card.getComponent<TriggerComponent.OnStartBuyPhase>()?.invoke(self, card)
    }
}



