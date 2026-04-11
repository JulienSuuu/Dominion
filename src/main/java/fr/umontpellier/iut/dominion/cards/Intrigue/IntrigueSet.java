package fr.umontpellier.iut.dominion.cards.Intrigue;

import fr.umontpellier.iut.dominion.cards.FactorySupplyPile.PileConfig;

import java.util.Map;

public class IntrigueSet {
    private static final Map<String, PileConfig> PILE_INTRIGUE =  Map.<String, PileConfig>ofEntries(
            Map.entry("Baron", PileConfig.kingdom(IntrigueFactory::Baron)),
            Map.entry("Bridge", PileConfig.kingdom(IntrigueFactory::Bridge)),
            Map.entry("Conspirator", PileConfig.kingdom(IntrigueFactory::Conspirator)),
            Map.entry("Courtier", PileConfig.kingdom(IntrigueFactory::Courtier)),
            Map.entry("Courtyard", PileConfig.kingdom(IntrigueFactory::Courtyard)),
            Map.entry("Diplomat", PileConfig.kingdom(IntrigueFactory::Diplomat)),
            Map.entry("Duke", PileConfig.victory(IntrigueFactory::Duke)),
            Map.entry("Farm", PileConfig.victory(IntrigueFactory::Farm)),
            Map.entry("Ironworks", PileConfig.kingdom(IntrigueFactory::Ironworks)),
            Map.entry("Lurker", PileConfig.kingdom(IntrigueFactory::Lurker)),
            Map.entry("Masquerade", PileConfig.kingdom(IntrigueFactory::Masquerade)),
            Map.entry("Mill", PileConfig.victory(IntrigueFactory::Mill)),
            Map.entry("Mining_Village", PileConfig.kingdom(IntrigueFactory::Mining_Village)),
            Map.entry("Minion", PileConfig.kingdom(IntrigueFactory::Minion)),
            Map.entry("Nobles", PileConfig.victory(IntrigueFactory::Nobles)),
            Map.entry("Patrol", PileConfig.kingdom(IntrigueFactory::Patrol)),
            Map.entry("Pawn", PileConfig.kingdom(IntrigueFactory::Pawn)),
            Map.entry("Replace", PileConfig.kingdom(IntrigueFactory::Replace)),
            Map.entry("Secret_Passage", PileConfig.kingdom(IntrigueFactory::Secret_Passage)),
            Map.entry("Shanty_Town", PileConfig.kingdom(IntrigueFactory::Shanty_Town)),
            Map.entry("Steward", PileConfig.kingdom(IntrigueFactory::Steward)),
            Map.entry("Swindler", PileConfig.kingdom(IntrigueFactory::Swindler)),
            Map.entry("Torturer", PileConfig.kingdom(IntrigueFactory::Torturer)),
            Map.entry("Trading_Post", PileConfig.kingdom(IntrigueFactory::Trading_Post)),
            Map.entry("Upgrade", PileConfig.kingdom(IntrigueFactory::Upgrade)),
            Map.entry("Wishing_Well", PileConfig.kingdom(IntrigueFactory::Wishing_Well)),
            Map.entry("Coppersmith", PileConfig.kingdom(IntrigueFactory::Coppersmith)),
            Map.entry("Great_Hall", PileConfig.victory(IntrigueFactory::GreatHall)),
            Map.entry("Saboteur", PileConfig.kingdom(IntrigueFactory::Saboteur)),
            Map.entry("Scout", PileConfig.kingdom(IntrigueFactory::Scout)),
            Map.entry("Secret_Chamber", PileConfig.kingdom(IntrigueFactory::Secret_Chamber)),
            Map.entry("Tribute", PileConfig.kingdom(IntrigueFactory::Tribute))
    );

    public static Map<String, PileConfig> get() {
        return PILE_INTRIGUE;
    }

}
