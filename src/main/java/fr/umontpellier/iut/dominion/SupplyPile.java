package fr.umontpellier.iut.dominion;

import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;

import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.GameStat;
import fr.umontpellier.iut.dominion.cards.component.Price;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ModifiableObservableListBase;

public class SupplyPile extends ModifiableObservableListBase<Card> {
    private final String name;
    private final Price cost;
    private int Cursed;
    private ArrayList<Card> cards = new ArrayList<>();
    private Predicate<Player> available;
    private int token;



    public SupplyPile(Supplier<Card> cardSupplier, int numberOfCopies) {
        Card card = cardSupplier.get();
        name = card.getName();
        cost = card.getPrice();
        for (int i = 0; i < numberOfCopies; i++) {
            cardSupplier.get().moveTo(this);
        }
    }

    public String getName() {
        return name;
    }
    public void setCard(Card card) {
        card.moveTo(this);
    }
    public int getCost() {
        return Math.max(cost.price().get(), 0);
    }
    public int getToken() {return token;}
    public void setToken(int token) {this.token = token;}
    public boolean hasToken(){return token!=0;}

    public void setCursed(int cursed) {
        Cursed += cursed;
    }
    public Price getPrice() {
        return cost;
    }
    public int getCursed() {
        return Cursed;
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
