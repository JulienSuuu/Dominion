package fr.umontpellier.iut.dominion.cards.Events

import fr.umontpellier.iut.dominion.Enums.Locations.Destination

/**
 * Événement classique de gain.
 */
class OnGainEvent(private val event: Event) : Event(
    card = event.card,
    destination = event.destination,
    player = event.player,
    isBuy = event.isBuy,
    discard = event.discard
) {

    override fun hasGainType(t: GainType): Boolean {
        return event.hasGainType(t)
    }

    override fun updateGainType(gainType: GainType) {
        event.updateGainType(gainType)
    }

    override val hasMoved: Boolean
        get() = destination != event.origin

    override val notMoved: Boolean
        get() = destination == event.origin

    override val isSameCard: Boolean
        get() = card == event.originalCard

    override fun cameFrom(destination: Destination): Boolean = event.cardOrigin == destination
    override fun initialCameFrom(destination: Destination): Boolean = event.initialOrigin == destination
}
