package fr.umontpellier.iut.dominion.cards.component;

import fr.umontpellier.iut.dominion.cards.Events.Event;

/**
 * Event global du jeu
 */
@FunctionalInterface
public interface EventLink {
    void execute(Event event);
}


