package fr.umontpellier.iut.dominion.cards.Events;

import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;

public class Event {
    private static int Id = 0;
    private int id;
    private Card card;
    private Destination destination;
    private Player player;
    public Event(Card card, Destination destination, Player player) {
        this.card = card;
        this.destination = destination;
        this.player = player;
        id = Id++;
    }

    public Card getCard() {
        return card;
    }
    public Destination getDest() {
        return destination;
    }
    public Destination getNextDest(){return destination;}
    public Player getPlayer() {return player;}
    public int getId() {return id;}

    public void setCard(Card card) {
        this.card = card;
    }
    public void setPlayer(Player player) {
        this.player = player;
    }
    public void setDest(Destination destination) {
        this.destination = destination;
    }
    public void setNextDest(Destination destination) {this.destination = destination;}
}
