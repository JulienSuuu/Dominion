package fr.umontpellier.iut.dominion.Supply;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.component.Price;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;


public class MixedSupplyPile extends SupplyPile {
    private enum checkName{
        Knight,
        Ruins;
    }

    private final Card template;
    private final Set<String> namesCard = new HashSet<>();

    public MixedSupplyPile(List<Supplier<Card>> cardSupplier, int numberOfCopy, Card template) {
        super(cardSupplier.getLast(), 0);
        cardSupplier.forEach((supplier)->{
            for(int i=0;i<numberOfCopy;i++) supplier.get().moveTo(this, Destination.SUPPLY);
            namesCard.add(supplier.get().getName());
        });
        this.template = template;

    }

    public void setType() {
        types.addAll(template.getTypes());
    }

    @Override
    public void update() {
        super.update();
        if (isEmpty()) {
            name.set(template.getName());
        } else {
            name.set(getLast().getName());
        }
    }

    public int getCost(){
        return isEmpty()? 0 : Math.max(getLast().getCost(), 0);
    }
    public Price getPrice(){
        return isEmpty()? template.getPrice() : getLast().getPrice();
    }
    public boolean verifyName(String name){
        return namesCard.contains(name);
    }
    public String getName(){
        return isEmpty()? template.getName() : getLast().getName();
    }
}
