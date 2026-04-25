package fr.umontpellier.iut.dominion.cards.Events;

import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;

public class MoveEvent extends Event {
    private Destination nextDest;
    public MoveEvent(Card card, Destination destination, Player player, Destination nextDest) {
        super(card, destination, player);
        this.nextDest = nextDest;
    }

    public Destination getNextDest() {
        return nextDest;
    }

    public void setNextDest(Destination nextDest) {
        this.nextDest = nextDest;
    }

}
