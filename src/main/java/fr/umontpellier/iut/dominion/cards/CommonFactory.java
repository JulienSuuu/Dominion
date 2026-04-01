package fr.umontpellier.iut.dominion.cards;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.cards.component.OnPlayComponent;
import fr.umontpellier.iut.dominion.cards.component.ScoreComponent;

public class CommonFactory {

    public static Card createTreasure(String name, int cost, int value){
        Card c =  new Card(name, cost, CardType.TREASURE);
        c.addComponent(OnPlayComponent.class, p ->{
            CardUtil.TriggerEffect(p, value, 0, 0, 0, "Effect", c);
        });
        return c;
    }

    public static Card createVictoryCard(String name, int cost, int value){
        Card c =  new Card(name, cost, CardType.VICTORY);
        c.addComponent(new ScoreComponent(value));
        return c;
    }
}
