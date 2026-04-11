package fr.umontpellier.iut.dominion;

import java.util.ArrayList;
import java.util.function.Supplier;

import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.component.Price;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class SupplyPile extends ArrayList<Card> {
    private final String name;
    private final Price cost;
    private int Cursed;

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
        return Math.max(cost.price() - Card.getReduction(), 0);
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

    public boolean isCursed() {
        return Cursed > 0;
    }
}
