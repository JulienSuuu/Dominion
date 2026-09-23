package fr.umontpellier.iut.dominion.Player.Skills

import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Flags
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.PlayerComponent.PlayerComponent
import fr.umontpellier.iut.dominion.Player.PlayerComponent.addToDiscarded
import fr.umontpellier.iut.dominion.Player.PlayerComponent.controller
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.Events.Event
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent
import fr.umontpellier.iut.dominion.cards.isNotIn

class TrashSkill : PlayerComponent {
}

suspend fun Player.trash(c: Card?): Boolean {
    if (c == null) return false

    if ((getFlag(Flags.resolveBandOfMisfit).value && c.getFlag("cant")) || c.getFlag("unable")) {
        log(String.format("%s stay in ${c.loc.value} (impossible movement).", c.name))
        return false
    }


    val trash = Event(c, Destination.Trash, self)

    game.moveCardToTrash(c)
    logTrash(c)
    triggerTrashEvent(trash)

    if (controller !== self) {
        trash.card?.let {
            it.moveTo(get(Destination.PlayerZone.Aside), Destination.PlayerZone.Aside)
            addToDiscarded(it)
        }
        return false
    }

    if (trash.card?.isNotIn(trash.destination) == true) {
        moveTo(trash.card, trash.destination)
    }

    if (trash.card?.hasForLocation(Destination.Trash) == true) {
        trash.card?.clear()
    }

    return true
}


suspend fun Player.trashAll(sourceZone : Destination.PlayerZone) {
    val cardToTrash = getCopyOf(sourceZone)?: emptyList()
    if(cardToTrash.isEmpty()) return
    cardToTrash.forEach(game::moveCardToTrash)

    val triggerPending = cardToTrash.filter{ it.hasComponent<TriggerComponent.CheckItselfTrashed>()}.toMutableList()

    while(triggerPending.isNotEmpty()){
        var chosen : Card?
        if(triggerPending.size == 1) {
            chosen = triggerPending.removeAt(triggerPending.lastIndex)
        }
        else chosen = self.chooseCardFromList(
            "choose the order of trashing", triggerPending)

        if(chosen == null) continue

        val event = Event(chosen, Destination.Trash , self )
        triggerTrashEvent(event)
        triggerPending.remove(chosen)
    }

}

suspend fun Player.trash(number : Int = 1, canPass : Boolean = true) : Boolean{
    for(i in 0 until number){
        trash(self.chooseCardFromHand("trash " + (number - i) + "card(s) from your hand", canPass))
    }

    return true;
}


suspend fun Player.trashWithCondition(instruction: String = "", number : Int = 1, filter : (Card) -> (Boolean), from : Destination.PlayerZone, canPass : Boolean = true) : Int{
    val list = getCopyOf(from) ?: mutableListOf()
    if(list.isEmpty()) return 0
    var success = 0
    for(i in 0 until number){
        val card = self.chooseCardFromList("trash again " + (number - i) + "card(s) from $from $instruction", list, canPass, filter)
            ?: break

        if(trash(card)){
            list.remove(card)
            success++
        }
    }
    return success
}


suspend fun Player.trashAndDo(instruction: String = "", number : Int = 1, from : Destination.PlayerZone, canPass: Boolean = true, filter: (Card) -> Boolean = { true }, action : suspend Player.(Int) -> Unit) =
    self.action(trashWithCondition(instruction, number,  filter, from, canPass))


suspend fun Player.trashAndEffect(extraInstruction: String = "", number: Int = 1, from : Destination.PlayerZone, canPass: Boolean = true, filter: (Card) -> Boolean = {true}, action: suspend Player.(Card) -> Unit){
    val list = getCopyOf(from) ?: mutableListOf()
    if(list.isEmpty())return
    for(i in 0 until number){
        val card = self.chooseCardFromList("trash again " + (number - i) + "card(s) from your $from $extraInstruction", list, canPass, filter) ?: break
        if(trash(card)){
            list.remove(card)
            self.action(card)
        }
    }
}

suspend fun Player.trashUntilYouStopAndDo(from : Destination.PlayerZone, instruction: String = "you may trash any card from $from until you stop", canPass: Boolean = true, filter: (Card) -> Boolean = {true}, action: suspend Player.(Int) -> Unit){
    val list = getCopyOf(from) ?: mutableListOf()
    if(list.isEmpty()) return
    var success = 0
    while(list.isNotEmpty()){
        val card = self.chooseCardFromList(instruction, list, canPass, filter) ?: break
        if(trash(card)){
            list.remove(card)
            success++
        }
    }

    self.action(success)
}


suspend fun Player.trashUntilYouStopAndDo(fromList: MutableList<Card>, instruction: String = "trash card from this list until you stop", canPass: Boolean = true, filter: (Card) -> Boolean = {true}, action: suspend Player.(Int) -> Unit){
    if(fromList.isEmpty()) return
    var success = 0
    while(fromList.isNotEmpty()){
        val card = self.chooseCardFromList(instruction, fromList, canPass, filter) ?: break
        if(trash(card)){
            fromList.remove(card)
            success++
        }
    }

    self.action(success)
}

suspend fun Player.trashFromList(fromList : List<Card>, number : Int = 1, canPass: Boolean = false, instruction: String = "${if(canPass) "you may" else ""} trash again $number cards form this list", filter: (Card) -> Boolean = { true }, extraAction: suspend Player.(Card) -> Unit = {}){
    val list = fromList.toMutableList()
    for(i in 0 until number){
        val card = self.chooseCardFromList(instruction, list, canPass, filter) ?: break
        if(trash(card)){
            list.remove(card)
            self.extraAction(card)
        }
    }
}

private suspend fun Player.triggerTrashEvent(event : Event) {
    triggerOneCard<TriggerComponent.CheckItselfTrashed>( event.card, event)
    triggerOthersEvent<TriggerComponent.OnCardTrashed>(event)

    chooseOrder<TriggerComponent.OnCardTrashed>("", {game.landMarks}){
        it.getComponent<TriggerComponent.OnCardTrashed>()?.let {
            t -> t(event, it)
        }
    }

}