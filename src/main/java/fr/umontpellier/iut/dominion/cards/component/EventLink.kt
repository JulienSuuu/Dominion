package fr.umontpellier.iut.dominion.cards.component

import fr.umontpellier.iut.dominion.cards.Events.Event

/**
 * Event global du jeu
 */
fun interface EventLink<T : Event> : suspend (T) -> Unit {
    override suspend fun invoke(event : T)
}
