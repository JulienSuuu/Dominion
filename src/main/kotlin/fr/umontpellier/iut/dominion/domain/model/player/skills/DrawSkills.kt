package fr.umontpellier.iut.dominion.domain.model.player.skills

import fr.umontpellier.iut.dominion.domain.model.location.Destination
import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.card.Card
import fr.umontpellier.iut.dominion.domain.components.player.isAffectedBy
import fr.umontpellier.iut.dominion.domain.components.player.updateTokenFlag
import fr.umontpellier.iut.dominion.domain.model.token.Token


fun Player.drawToHand() = drawTo(Destination.PlayerZone.Hand)

private fun Player.verifyTokenStatus(token : Token.OnPlayer) : Boolean{
    if(isAffectedBy(token)){
        updateTokenFlag(token, false)
        return true
    }
    return false
}

fun Player.drawTo(destination : Destination.PlayerZone) : Card?{
    if(verifyTokenStatus(Token.OnPlayer.MinusOneCardToken)) return null
    val c = getCardFromDeck()?: return null
    moveTo(c, destination)
    return c
}

fun Player.draw(numberToDraw : Int = 1 ){
    if(numberToDraw == 0) return
    for(i in 0 until numberToDraw){
        drawToHand()?:break
    }
}


suspend fun Player.drawByAction(action: suspend Player.() -> Int) {
    draw(self.action())
}


suspend fun Player.drawUntilAndDo(filter: Player.(Card) -> Boolean, action : suspend Player.(List<Card>) -> Unit ){
    val list = mutableListOf<Card>()
    while(true){
        val c = getCardFromDeck()?: break
        if(!self.filter(c)) break
        c.moveToTemp(get(Destination.TempZone.Temp))
        list.add(c)
    }

    self.action(list)
}

fun Player.putCardInDraw(card: Card, selected: Card?) {

    val drawPile = getList(Destination.PlayerZone.Draw)

    if (selected == null) {
        moveTo(card, Destination.PlayerZone.Draw)
    } else {
        while (drawPile.isNotEmpty()) {
            val top = getCardFromDeck() ?: break

            if (top == selected) {
                break
            }
            moveTo(top, Destination.TempZone.Temp)
        }

        moveTo(card, Destination.PlayerZone.Draw)

        getCopyOf(Destination.TempZone.Temp)?.reversed()?.forEach {
            moveTo(it, Destination.PlayerZone.Draw)
        }
    }
}

