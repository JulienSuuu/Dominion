package fr.umontpellier.iut.dominion.cards.common;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;

import java.util.function.Consumer;

public class Treasure extends Card {
    private int power;
    public Treasure(String name, int cost,  int power) {

        super(name, cost, CardType.TREASURE);
        this.power = power;
    }

    @Override
    public void play(Player p) {
        p.incrementMoney(power);
        p.log(getName().toUpperCase() + ": " + "+ " + power + " pieces");
    }
}
