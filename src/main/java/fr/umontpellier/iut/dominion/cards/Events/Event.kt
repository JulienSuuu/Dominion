package fr.umontpellier.iut.dominion.cards.Events;

import fr.umontpellier.iut.dominion.Button;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Interface.Logger;
import fr.umontpellier.iut.dominion.Player.Player;
import fr.umontpellier.iut.dominion.cards.Card;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Evenement classique du jeu
 */
public class Event implements Logger {
    private static int Id = 0;
    private final int id;
    private final Destination initialOrigin;
    private final Destination origin;
    private Destination cardOrigin;

    private Card card;
    private final Card originalCard;
    private Destination destination;


    private Player player;

    private boolean isBuy = false;
    private Discard_Type discard = Discard_Type.ACTION;

    public Event(Card card, Destination destination, Player player) {
        this.card = card;
        this.originalCard = card;
        this.cardOrigin = card.getLocation();
        this.initialOrigin = card.getLocation();
        this.destination = destination;
        this.origin = destination;
        this.player = player;
        this.id = Id++;
    }

    public Event(Card card, Destination destination, Player player,  boolean isBuy) {
        this(card, destination, player);
        this.isBuy = isBuy;
    }

    public Event(Card card, Destination destination, Player player, Discard_Type discard){
        this(card, destination, player);
        this.discard = discard;
    }

    public Event(Player player) {
        this.id = Id++;
        this.player = player;
        origin = null;
        originalCard = null;
        initialOrigin = null;
    }

    public boolean hasMoved() {
        return destination != origin;
    }
    public boolean notMoved() {
        return destination == origin;
    }
    public boolean isSameCard(){
        return card == originalCard;
    }
    public boolean cameFrom(Destination destination) {
        return cardOrigin == destination;
    }
    public boolean isCardIn(Destination destination) {
        return card.hasForLocation(destination);
    }
    public boolean isBuy() {
        return isBuy;
    }
    public boolean isActionDiscard(){return discard == Discard_Type.ACTION;}
    public boolean initialCameFrom(Destination destination) {
        return initialOrigin == destination;
    }

    public Destination getDest() {
        return destination;
    }
    public Card getCard() {return card;}
    public Player getPlayer() {return player;}
    public int getId() {return id;}
    public void setPlayer(Player player) {this.player = player;}



    public void setCard(Card card) {
        this.card = card;
        cardOrigin = card.getLocation();
    }

    public void setDest(Destination destination) {
        this.destination = destination;
    }

    @Override
    public String chooseWhatToDo(String instruction, List<Card> list, List<Button> buttons, boolean canPass) {
        return player.chooseWhatToDo(instruction, list, buttons, canPass);
    }

    @Override
    public Optional<Card> chooseCardFromHand(String instruction, Predicate<? super Card> predicate, boolean canPass) {
        return player.chooseCardFromHand(instruction, predicate, canPass);
    }

    @Override
    public Optional<Card> chooseCardFromList(String instruction, Predicate<? super Card> predicate, List<Card> cards, boolean canPass) {
        return player.chooseCardFromList(instruction, predicate, cards, canPass);
    }
}
