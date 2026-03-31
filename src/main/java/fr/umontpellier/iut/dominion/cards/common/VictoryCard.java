package fr.umontpellier.iut.dominion.cards.common;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.cards.Card;

public class VictoryCard extends Card {
    private int power;

    public VictoryCard(String name, int cost, int power) {
        super(name, cost, CardType.VICTORY);
        this.power = power;
    }

    @Override
    public int getVictoryValue() {
        return power;
    }
}
