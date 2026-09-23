package fr.umontpellier.iut.dominion.Player.Skills

import fr.umontpellier.iut.dominion.Annotation.AfterAllyTrigger
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.PlayerComponent.PlayerComponent
import fr.umontpellier.iut.dominion.Player.PlayerComponent.addCardCurrentTurn
import fr.umontpellier.iut.dominion.Properties
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.Events.Event
import fr.umontpellier.iut.dominion.cards.Events.GainType
import fr.umontpellier.iut.dominion.cards.Events.OnGainEvent
import fr.umontpellier.iut.dominion.cards.Events.OnMoveEvent
import fr.umontpellier.iut.dominion.cards.Events.TriggerEvent
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent.*
import fr.umontpellier.iut.dominion.Player.PlayerComponent.checkGainToken
import fr.umontpellier.iut.dominion.Player.PlayerComponent.controller
import kotlinx.coroutines.flow.MutableStateFlow

class GainSkills : PlayerComponent {
}

private fun Player.gainTo(gainedCard : Card?, location: MutableStateFlow<List<Card>>?, destination: Destination?) {
    if(gainedCard == null) return
    val i = game.tradeRoute(gainedCard)
    increment(Item.COIN_TOKEN_ROUTE, i)

    gainedCard.moveTo(location, destination)
}


suspend fun Player.gain(event: Event){ gain(event.card, event.destination, event.isBuy) }


@AfterAllyTrigger
@JvmOverloads
suspend fun Player.gain(card: Card?, dest: Destination? = Destination.PlayerZone.Discard, isBuy: Boolean = false) {
    if (card == null) return
    val event = Event(card, dest, self, isBuy)
    val onMoveEvent = OnMoveEvent(event, GainType.BEFORE)

    fun updateNumberOfBought() {
        if (!event.isBuy) return
        val prop = getProperties(Properties.Cards_Bought)
        prop.value += 1
    }

    if (controller !== self) {
        controller.gainTo(onMoveEvent.destination, onMoveEvent.card)
        return
    } else {
        gainTo(onMoveEvent.destination, onMoveEvent.card)
    }

    checkGainToken(event)
    game.fireEvent<OnMoveEvent>(onMoveEvent)

    triggerOneCard<CheckItselfGain>( event.card, event)

    event.updateGainType(GainType.DURING)

    val triggerEvent = TriggerEvent(onMoveEvent)

    triggerPlayerTavern<DuringPlayerGain>(triggerEvent)
    triggerEvent<DuringPlayerGain>(triggerEvent)
    triggerActiveEffect<SideEffectGain>(triggerEvent)

    triggerEvent.updateGainType(GainType.AFTER)

    addCardCurrentTurn(event.card)

    triggerEvent.card?.hasForLocation(triggerEvent.destination)?.let {
        if (!it) {
            moveTo(triggerEvent.card, triggerEvent.destination)
        }
    }

    triggerEvent<AfterPlayerGain>(triggerEvent)
    triggerPlayerTavern<AfterPlayerGain>(triggerEvent)
    triggerActiveEffect<SideEffectGain>(triggerEvent)
    game.fireEvent<OnGainEvent>(OnGainEvent(triggerEvent))

    updateNumberOfBought()
}


suspend fun Player.gainSilent(card: Card?, dest: Destination, gained: Boolean){
    if (card == null) return
    if(gained) addCardCurrentTurn(card)

    val event = Event(card, dest, self)

    if(controller != self){
        controller.gainTo(dest, card)
        return
    }else if (gained) gainTo(dest, event.card)

    checkGainToken(event)

    if(gained) triggerOneCard<CheckItselfGain>( card, event)
    game.fireEvent<OnGainEvent>(OnGainEvent(event))


    event.destination?.let { destination ->
        moveTo(card, destination)
    }
}

private fun Player.gainTo(dest : Destination?, c : Card?) {
    if(c == null) return

    gainTo(c, get(dest), dest)
}