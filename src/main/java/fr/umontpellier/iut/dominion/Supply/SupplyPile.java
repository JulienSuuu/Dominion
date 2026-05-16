package fr.umontpellier.iut.dominion.Supply;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.component.Price;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ModifiableObservableListBase;
import javafx.fxml.FXML;

public class SupplyPile extends ModifiableObservableListBase<Card> {
    protected final StringProperty name = new SimpleStringProperty(this, "Supply", "");
    protected Price cost;
    protected int Cursed;
    protected ArrayList<Card> cards = new ArrayList<>();
    protected Predicate<Player> available;
    protected int token;
    protected Set<CardType> types = new HashSet<>();
    protected Map<String, Object> copy =  new HashMap<>();

    public SupplyPile(Supplier<Card> cardSupplier, int numberOfCopies) {
        Card card = cardSupplier.get();
        name.setValue(card.getName());
        cost = card.getPrice();

        ListChangeListener<Card> listener = change -> {
            boolean hasChanged = false;
            while (change.next()) {
                hasChanged = true;
                if (change.wasRemoved()) {
                    for (Card c : change.getRemoved()) {
                        c.getPrice().price().unbind();
                        c.getPrice().debt().unbind();
                    }
                }
            }

            if (hasChanged) {
                update();
            }
        };

        addListener(listener);
        for (int i = 0; i < numberOfCopies; i++) {
            cardSupplier.get().moveTo(this, Destination.SUPPLY);
        }
    }

    protected void setType() {
        if(isEmpty())return;
        types.addAll(getLast().getTypes());
    }
    public IntegerProperty priceProperty() {
        return cost.price();
    }
    public String getName() {
        return name.getValue();
    }
    public void setCard(Card card) {
        card.clear();
        copy.forEach(card::set);
        card.moveTo(this, Destination.SUPPLY);
    }
    public int getCost() {
        return Math.max(cost.price().get(), 0);
    }
    public int getToken() {return token;}
    public void setToken(int token) {this.token = token;}
    public boolean hasToken(){return token!=0;}

    public void update() {
        var pileCost = priceProperty();
        if(isEmpty()) return;

        Card card = getLast();
        if(card != null){
            var cost = card.getPrice();
            cost.price().unbind();
            cost.debt().unbind();
            cost.price().bind(pileCost);
            copy = new HashMap<>(card.getProperties());
        }

    }

    public boolean hasType(CardType type) {
        if(types.isEmpty()) setType();
        if(types.isEmpty()) return false;
        return types.contains(type);
    }

    public void setCursed(int cursed) {
        Cursed += cursed;
    }
    public Price getPrice() {
        return cost;
    }
    public int getCursed() {
        return Cursed;
    }
    public boolean verifyName(String name){
        return getName().equals(name);
    }
    public boolean isCursed() {
        return Cursed > 0;
    }

    @Override
    public Card get(int i) {
        return cards.get(i);
    }

    @Override
    public int size() {
        return cards.size();
    }

    @Override
    protected void doAdd(int i, Card card) {
        cards.add(i, card);
    }

    @Override
    protected Card doSet(int i, Card card) {
        return cards.set(i, card);
    }

    @Override
    protected Card doRemove(int i) {
        return cards.remove(i);
    }
}
