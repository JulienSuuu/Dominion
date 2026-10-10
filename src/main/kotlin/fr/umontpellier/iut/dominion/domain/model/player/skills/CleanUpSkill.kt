package fr.umontpellier.iut.dominion.domain.model.player.skills

import fr.umontpellier.iut.dominion.domain.model.location.Destination
import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.components.player.controller
import fr.umontpellier.iut.dominion.domain.components.player.mustBeDiscarded
import fr.umontpellier.iut.dominion.domain.components.player.underPossession
import fr.umontpellier.iut.dominion.domain.model.event.Discard_Type
import fr.umontpellier.iut.dominion.domain.components.card.duration.DurationComponent
import fr.umontpellier.iut.dominion.domain.components.card.duration.Follower
import fr.umontpellier.iut.dominion.domain.components.card.trigger.TriggerComponent

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
    resetComponents()
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

