package fr.umontpellier.iut.dominion.cards.Events

import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Player.Player

class OnMoveEvent(private val event: Event, var gainType: GainType) : Event(
    card = event.card,
    destination = event.destination,
    player = event.player,
    isBuy = event.isBuy,
    discard = event.discard
    ) {

    override val hasMoved: Boolean
    get() = destination != event.origin

    override val notMoved: Boolean
    get() = destination == event.origin

    override val isSameCard: Boolean
    get() = card == event.originalCard

    override fun hasGainType(t: GainType) = gainType == t
    override fun updateGainType(gainType: GainType) { this.gainType = gainType }

    override fun cameFrom(destination: Destination): Boolean = event.cardOrigin == destination
    override fun initialCameFrom(destination: Destination): Boolean = event.initialOrigin == destination
}