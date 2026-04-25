package fr.umontpellier.iut.dominion.cards.factories;

import fr.umontpellier.iut.dominion.Annotation.Dominion_Card;
import fr.umontpellier.iut.dominion.Annotation.PileType;
import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Item;
import fr.umontpellier.iut.dominion.cards.*;
import fr.umontpellier.iut.dominion.cards.component.OnPlayComponent;
import fr.umontpellier.iut.dominion.cards.component.ScoreComponent;

public class CommonFactory {

    public static Card createTreasure(String name, int cost, int value) {
        Bonus bonus =  Bonus.empty().with(Item.MONEY, value);
        Card c =  new Card(name, RegistryPrice.SeasidePrice(cost), CardType.TREASURE);
        c.addComponent(OnPlayComponent.class, (player, card) -> CardUtil.TriggerEffect(player, FactoryUtil.EFFECT, card, bonus));
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
        c.addComponent(OnPlayComponent.class, (player, card) -> player.increment(Item.MONEY, c.hasType(CardType.TREASURE) ? 1 : 0));
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

        @Dominion_Card(pileType = PileType.COPPER)
        public static Card Copper() {
            return CommonFactory.createTreasure("Copper", 0, 1);
        }

        @Dominion_Card(pileType = PileType.SILVER)
        public static Card Silver() {
            return CommonFactory.createTreasure("Silver", 3, 2);
        }

        @Dominion_Card(pileType = PileType.GOLD)
        public static Card Gold() {
            return CommonFactory.createTreasure("Gold", 6, 3);
        }

        @Dominion_Card(pileType = PileType.PLATINUM)
        public static Card Platinum() {
            return CommonFactory.createTreasure("Platinum", 9, 5);
        }


        @Dominion_Card( pileType = PileType.ESTATE)
        public static Card Estate() {
            return CommonFactory.createVictoryCard("Estate", 2, 1);
        }

        @Dominion_Card( pileType = PileType.VICTORY) // Utilise la règle Duché/Province
        public static Card Duchy() {
            return CommonFactory.createVictoryCard("Duchy", 5, 3);
        }

        @Dominion_Card( pileType = PileType.VICTORY)
        public static Card Province() {
            return CommonFactory.createVictoryCard("Province", 8, 6);
        }

        @Dominion_Card(pileType = PileType.VICTORY)
        public static Card Colony() {
            return CommonFactory.createVictoryCard("Colony", 11, 10);
        }

        // --- CARTES SPÉCIALES ---

        @Dominion_Card( pileType = PileType.CURSE)
        public static Card Curse() {
            return CommonFactory.createCurseCard("Curse", 0, -1);
        }

        @Dominion_Card(pileType = PileType.POTION)
        public static Card Potion() {
            return CommonFactory.createPotion();
        }
}
