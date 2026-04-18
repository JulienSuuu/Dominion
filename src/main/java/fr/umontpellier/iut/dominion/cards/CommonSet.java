package fr.umontpellier.iut.dominion.cards;

import fr.umontpellier.iut.dominion.Flags;

import java.util.Map;


public class CommonSet {

    private static final Map<String, FactorySupplyPile.PileConfig> PILE_COMMON = Map.ofEntries(
            Map.entry("Copper", FactorySupplyPile.PileConfig.copper(() -> CommonFactory.createTreasure("Copper", 0, 1))),
            Map.entry("Silver", FactorySupplyPile.PileConfig.silver(() -> CommonFactory.createTreasure("Silver", 3, 2))),
            Map.entry("Gold", FactorySupplyPile.PileConfig.gold(() -> CommonFactory.createTreasure("Gold", 6, 3))),
            Map.entry("Estate", FactorySupplyPile.PileConfig.estate(() -> CommonFactory.createVictoryCard("Estate", 2, 1))),
            Map.entry("Duchy", FactorySupplyPile.PileConfig.victory(() -> CommonFactory.createVictoryCard("Duchy", 5, 3))),
            Map.entry("Province", FactorySupplyPile.PileConfig.victory(() -> CommonFactory.createVictoryCard("Province", 8, 6))),
            Map.entry("Curse", FactorySupplyPile.PileConfig.curse(() -> CommonFactory.createCurseCard("Curse", 0, -1))),
            Map.entry("Potion", FactorySupplyPile.PileConfig.potion(CommonFactory::createPotion)),
            Map.entry("Platinum", FactorySupplyPile.PileConfig.platinum(() -> CommonFactory.createTreasure("Platinum", 9,5 ))),
            Map.entry("Colony", FactorySupplyPile.PileConfig.victory(() -> CommonFactory.createVictoryCard("Colony", 11, 10)))
    );

    public static Map<String, FactorySupplyPile.PileConfig> get() {
        return PILE_COMMON;
    }
}
