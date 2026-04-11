package fr.umontpellier.iut.dominion.cards.Alchimie;

import fr.umontpellier.iut.dominion.cards.FactorySupplyPile;

import java.util.Map;

public class AlchimySet {
    private static final Map<String, FactorySupplyPile.PileConfig> PILE_ALCHIMY = Map.ofEntries(
            Map.entry("Alchemist", FactorySupplyPile.PileConfig.kingdom(AlchimyFactory::Alchemist)),
            Map.entry("Apothecary", FactorySupplyPile.PileConfig.kingdom(AlchimyFactory::Apothecary)),
            Map.entry("Apprentice", FactorySupplyPile.PileConfig.kingdom(AlchimyFactory::Apprentice)),
            Map.entry("Familiar", FactorySupplyPile.PileConfig.kingdom(AlchimyFactory::Familiar)),
            Map.entry("Golem", FactorySupplyPile.PileConfig.kingdom(AlchimyFactory::Golem)),
            Map.entry("Herbalist", FactorySupplyPile.PileConfig.kingdom(AlchimyFactory::Herbalist)),
            Map.entry("Philosopher's Stone", FactorySupplyPile.PileConfig.kingdom(AlchimyFactory::Philosopher_Stone)),
            Map.entry("Possession", FactorySupplyPile.PileConfig.kingdom(AlchimyFactory::Possession)),
            Map.entry("Scrying Pool", FactorySupplyPile.PileConfig.kingdom(AlchimyFactory::Scrying_Pool)),
            Map.entry("Transmute", FactorySupplyPile.PileConfig.kingdom(AlchimyFactory::Transmute)),
            Map.entry("University", FactorySupplyPile.PileConfig.kingdom(AlchimyFactory::University)),
            Map.entry("Vineyard", FactorySupplyPile.PileConfig.victory(AlchimyFactory::Vineyard))
    );

    public static Map<String, FactorySupplyPile.PileConfig> get() {
        return PILE_ALCHIMY;
    }
}
