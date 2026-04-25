package fr.umontpellier.iut.dominion.cards.component;

import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Events.Event;

public interface TriggerEffect {
    void execute(Player owner, Player victim, Event event);
}
