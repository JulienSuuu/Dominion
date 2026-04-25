package fr.umontpellier.iut.dominion.cards;

import fr.umontpellier.iut.dominion.Item;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public record Bonus(Map<Item, Integer> items, int cardsToDraw) {

    public static Bonus empty() {
        return new Bonus(new EnumMap<>(Item.class), 0);
    }

    public Bonus with(Item item, int qty) {
        Map<Item, Integer> newItems = new EnumMap<>(items);
        newItems.put(item, qty);
        return new Bonus(newItems, this.cardsToDraw);
    }

    public Bonus draw(int qty) {
        return new Bonus(this.items, qty);
    }
}
