package fr.umontpellier.iut.dominion.cards.Events;

import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;

public class OnGainEvent extends Event {
    public OnGainEvent(Card card, Destination destination, Player player) {
        super(card, destination, player);
    }
}
