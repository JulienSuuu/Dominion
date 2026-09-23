package fr.umontpellier.iut.dominion.Player.Skills

import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.PlayerComponent.PlayerComponent
import fr.umontpellier.iut.dominion.cards.Card

class MoveSkills : PlayerComponent

fun Player.moveToHand(c : Card?){
    c?.moveTo(get(Destination.PlayerZone.Hand), Destination.PlayerZone.Hand)
}

fun Player.moveTo(c : Card?, destination: Destination.PlayerZone){
    if(c?.getFlag("unable") == true) return
    c?.moveTo(get(destination), destination)
}

fun Player.moveToTemp(c :Card?){
    if(c?.getFlag("unable") == true) return
    c?.moveToTemp(get(Destination.TempZone.Temp))
}

fun Player.moveToTemp(scope : Card, c : Card?, extraAction: Player.(Card) -> Unit = {}){
    if(c == null || c.getFlag("unable") ) return
    c.moveToTemp(tempZone(scope))
    self.extraAction(c)
}

fun Player.moveTo(c: Card?, destination: Destination?){
    if(c?.getFlag("unable") == true) return
    val dest = Destination.PlayerZone.valueOf(destination)
    dest?.let { moveTo(c, dest) }
}


fun Player.moveCardsFromTemp(cardPlayed: Card?){
    cardPlayed?.let {
        tempZone(it).value.forEach { card ->
            val dest = Destination.PlayerZone.valueOf(card.loc.value)
            dest?.let { moveTo(card, dest) }
        }
    }

}

fun Player.moveToBottom(c: Card?, destination: Destination.PlayerZone){
    if(c?.getFlag("unable") == true) return
    c?.moveToBottom(get(destination), destination)
}

suspend fun Player.moveTo(from: Destination.PlayerZone, to: Destination.PlayerZone, instruction: String = "Move cards from $from to $to", number : Int = 1, canPass : Boolean = false){
    val list = getCopyOf(from) ?: mutableListOf()
    for(i in 0 until number ) {
        if(list.isEmpty()) break
        val card = self.chooseCardFromList(instruction, list, canPass) ?: break
        self.moveTo(card, to)
        list.remove(card)
    }
}

fun Player.moveAll(from : Destination.PlayerZone, to : Destination.PlayerZone, extraAction: Player.(Card) -> Unit = {}){
    getCopyOf(from)?.forEach { moveTo(it, to); self.extraAction(it) }
}

fun Player.moveAll(cards: List<Card>, destination: Destination.PlayerZone, extraAction: Player.(Card) -> Unit = {}){
    cards.forEach { moveTo(it, destination); self.extraAction(it) }
}

suspend fun Player.moveAllAndChooseTheOrder(from : Destination.PlayerZone, to : Destination.PlayerZone){
    while(getCopyOf(from)?.isNotEmpty() == true) {
        self.chooseCardFromList("move all your $from to $to in any order",getCopyOf(from)?:emptyList(), false, {true})
            ?.let { moveTo(it, to) }
    }
}


suspend fun Player.move(from: Destination.PlayerZone, to : Destination.PlayerZone, number : Int = 1, canPass : Boolean = false, filter: (Card) -> Boolean = {true}, extraAction: suspend Player.(Card) -> Unit = {}){
    val list = getCopyOf(from) ?: mutableListOf()
    for(i in 0 until number ) {
        if(list.isEmpty()) break
        val c = self.chooseCardFromList("${if(canPass) "you may" else ""} move ${if(number == 1) "" else "again"} ${number - i} card(s) from $from to $to", list, canPass, filter) ?: break
        self.moveTo(c, to)
        self.extraAction(c)
        list.remove(c)
    }
}

suspend fun Player.moveAllAndChooseTheOrder(
    cards: MutableList<Card>,
    from: Destination,
    to: Destination.PlayerZone
) {
    if (cards.isEmpty()) return
    if (cards.size == 1) {
        moveTo(cards.first(), to)
        return
    }

    while (cards.isNotEmpty()) {
        self.chooseCardFromList(
            instruction = "move cards from $from to $to in the order of your choice",
            cards = cards,
            canPass = false,
        )?.let { selectedCard ->
            moveTo(selectedCard, to)
            cards.remove(selectedCard)
        } ?: break
    }
}


suspend fun Player.moveList(instruction: String, cards: MutableList<Card>, filter : (Card) -> Boolean = {true}, to : Destination.PlayerZone, canPass: Boolean = false, action : suspend Player.(Card) -> Unit = {moveTo(it, to)}, extraAction: suspend Player.(List<Card>) -> Unit = {} ) : MutableList<Card> {
    val list = mutableListOf<Card>()
    if(cards.isEmpty()) return list

    while(cards.any{ filter(it) }) {
        self.chooseCardFromList(instruction, cards, canPass, filter)
            ?.let {
                self.action(it)
                cards.remove(it)
                list.add(it)
            } ?: break
    }

    self.extraAction(list)
    return list
}