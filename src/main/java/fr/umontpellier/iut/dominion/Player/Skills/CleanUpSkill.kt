package fr.umontpellier.iut.dominion.Player.Skills

import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.PlayerComponent.controller
import fr.umontpellier.iut.dominion.Player.PlayerComponent.mustBeDiscarded
import fr.umontpellier.iut.dominion.Player.PlayerComponent.underPossession
import fr.umontpellier.iut.dominion.cards.Events.Discard_Type
import fr.umontpellier.iut.dominion.cards.component.DurationComponent
import fr.umontpellier.iut.dominion.cards.component.Follower
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent

suspend fun Player.cleanup(){
    cleanUpEffect.forEach { self.it() }
    cleanUpEffect.clear()
    
    getCopyOf(Destination.PlayerZone.Hand)?.forEach { discard(it, Discard_Type.CLEANUP); it.clear() }

    getCopyOf(Destination.PlayerZone.InPlay)
        ?.filter {
            (it.getComponent<DurationComponent>()?.isFinished(self) ?: true) &&
                    (it.getComponent<Follower>()?.inactive ?: true)
        }
        ?.forEach {
            discard(it, Discard_Type.CLEANUP)
            it.clear()
        }

    takeIf { underPossession }
        .let {
            controller = self
            mustBeDiscarded.forEach { discard(it, Discard_Type.CLEANUP); it.clear() }
        }

    val numberToDraw = 0.coerceAtLeast(5.plus(drawBonusNextTurn))
    cleanUpDraw(numberToDraw)

    triggerEndTurnEffect()

    drawBonusNextTurn = 0
    clearList()
    resetItems()
    resetFlags()
    resetProperties()
}

private suspend fun Player.triggerEndTurnEffect(){
    endTurnEffect.forEach { self.it() }
    endTurnEffect.clear()

    val landMark = game.landMarks
    chooseOrder<TriggerComponent.OnEndTurn>("choose the resolution order for your 'On End Turn phase' effects :", {landMark}){ card ->
        card.getComponent<TriggerComponent.OnEndTurn>()?.invoke(self, card)
    }

}

private fun Player.cleanUpDraw(numberToDraw : Int){
    for(i in 0 until numberToDraw){
        val card = getCardFromDeck()?: break
        moveTo(card, Destination.PlayerZone.Hand)
    }
}



suspend fun Player.generalCleanUp(){
    getCopyOf(Destination.PlayerZone.InPlay)
        ?.filter {
            (it.getComponent<DurationComponent>()?.isFinished(self) ?: true) &&
                    (it.getComponent<Follower>()?.inactive ?: true)
        }
        ?.forEach {
            discard(it, Discard_Type.CLEANUP)
            it.clear()
        }

    resetFlags()
    resetItems()
    resetProperties()
    hooks.clear()
}

