package fr.umontpellier.iut.dominion.cards;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Flags;
import fr.umontpellier.iut.dominion.Item;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.component.OnPlayComponent;
import fr.umontpellier.iut.dominion.cards.component.ScoreComponent;

import java.util.function.Consumer;

public class CommonFactory {

    public static Card createTreasure(String name, int cost, int value) {
        Card c =  new Card(name, RegistryPrice.SeasidePrice(cost), CardType.TREASURE);
        c.addComponent(OnPlayComponent.class, (player, card) ->{
            CardUtil.TriggerEffect(player, value, 0, 0, 0, "Effect", c);
        });
        return c;
    }



    public static Card createVictoryCard(String name, int cost, int value){
        Card c =  new Card(name, RegistryPrice.SeasidePrice(cost), CardType.VICTORY);
        c.addComponent(ScoreComponent.class, player -> value);
        return c;
    }

    public static Card createCurseCard(String name, int cost, int value){
        Card c =   new Card(name, RegistryPrice.SeasidePrice(cost), CardType.CURSE);
        c.addComponent(ScoreComponent.class, player -> value);
        c.addComponent(OnPlayComponent.class, (player, card) ->{player.increment(Item.MONEY, GameStat.charlatanPower.getValue() > 0 ? 1 : 0 );});
        return c;
    }

    public static Card createPotion(){
        return new Card("Potion", RegistryPrice.DominionPrice(4), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) ->
                                player.increment(Item.POTION, 1)
                                )
                );
    }
}
