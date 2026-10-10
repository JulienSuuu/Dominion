package fr.umontpellier.iut.dominion.domain.components.core

import fr.umontpellier.iut.dominion.domain.model.event.Event

/**
 * Event global du jeu
 */
fun interface EventLink<T : Event> : suspend (T) -> Unit {
    override suspend fun invoke(event : T)
}