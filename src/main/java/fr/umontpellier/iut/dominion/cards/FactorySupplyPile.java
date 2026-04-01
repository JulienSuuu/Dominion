package fr.umontpellier.iut.dominion.cards;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import fr.umontpellier.iut.dominion.SupplyPile;
import fr.umontpellier.iut.dominion.cards.seaside.*;

/**
 * Classe de fabrication de listes de cartes
 */
public class FactorySupplyPile {

    private record PileConfig(Supplier<Card> cardSupplier, Function<Integer, Integer> countFunction) {
        static PileConfig kingdom(Supplier<Card> supplier) {
            return new PileConfig(supplier, n -> 10);
        }

        static PileConfig victory(Supplier<Card> supplier) {
            return new PileConfig(supplier, n -> n <= 2 ? 8 : 12);
        }

        static PileConfig copper(Supplier<Card> supplier) {
            return new PileConfig(supplier, n -> 60 + 7 * n);
        }

        static PileConfig silver(Supplier<Card> supplier) {
            return new PileConfig(supplier, n -> 40);
        }

        static PileConfig gold(Supplier<Card> supplier) {
            return new PileConfig(supplier, n -> 30);
        }

        static PileConfig estate(Supplier<Card> supplier) {
            return new PileConfig(supplier, n -> n <= 2 ? 8 + 3 * n : 12 + 3 * n);
        }

        static PileConfig curse(Supplier<Card> supplier) {
            return new PileConfig(supplier, n -> 10 * (n - 1));
        }
    }

    private static final Map<String, PileConfig> PILE_CONFIGS = Map.ofEntries(
            // Cartes communes
            Map.entry("Copper", PileConfig.copper(() -> CommonFactory.createTreasure("Copper", 0, 1))),
            Map.entry("Silver", PileConfig.silver(() -> CommonFactory.createTreasure("Silver", 3, 2))),
            Map.entry("Gold", PileConfig.gold(() -> CommonFactory.createTreasure("Gold", 6, 3))),
            Map.entry("Estate", PileConfig.estate(() -> CommonFactory.createVictoryCard("Estate", 2, 1))),
            Map.entry("Duchy", PileConfig.victory(() -> CommonFactory.createVictoryCard("Duchy", 5, 3))),
            Map.entry("Province", PileConfig.victory(() -> CommonFactory.createVictoryCard("Province", 8, 6))),
            Map.entry("Curse", PileConfig.curse(() -> CommonFactory.createVictoryCard("Curse", 0, -1))),
            // Cartes Royaume (Seaside)
            Map.entry("Ambassador", PileConfig.kingdom(SeaSideFactory::Ambassador)),
            Map.entry("Astrolabe", PileConfig.kingdom(SeaSideFactory::Astrolabe)),
            Map.entry("Bazaar", PileConfig.kingdom(SeaSideFactory::Bazaar)),
            Map.entry("Blockade", PileConfig.kingdom(SeaSideFactory::Blockade)),
            Map.entry("Caravan", PileConfig.kingdom(SeaSideFactory::Caravan)),
            Map.entry("Corsair", PileConfig.kingdom(SeaSideFactory::Corsair)),
            Map.entry("Cutpurse", PileConfig.kingdom(SeaSideFactory::Cutpurse)),
            Map.entry("Embargo", PileConfig.kingdom(Embargo::new)),
            Map.entry("Explorer", PileConfig.kingdom(Explorer::new)),
            Map.entry("Fishing Village", PileConfig.kingdom(FishingVillage::new)),
            Map.entry("Ghost Ship", PileConfig.kingdom(GhostShip::new)),
            Map.entry("Haven", PileConfig.kingdom(Haven::new)),
            Map.entry("Island", PileConfig.victory(Island::new)),
            Map.entry("Lighthouse", PileConfig.kingdom(Lighthouse::new)),
            Map.entry("Lookout", PileConfig.kingdom(Lookout::new)),
            Map.entry("Merchant Ship", PileConfig.kingdom(MerchantShip::new)),
            Map.entry("Monkey", PileConfig.kingdom(Monkey::new)),
            Map.entry("Native Village", PileConfig.kingdom(NativeVillage::new)),
            Map.entry("Navigator", PileConfig.kingdom(Navigator::new)),
            Map.entry("Outpost", PileConfig.kingdom(Outpost::new)),
            Map.entry("Pearl Diver", PileConfig.kingdom(PearlDiver::new)),
            Map.entry("Pirate", PileConfig.kingdom(Pirate::new)),
            Map.entry("Pirate Ship", PileConfig.kingdom(PirateShip::new)),
            Map.entry("Sailor", PileConfig.kingdom(Sailor::new)),
            Map.entry("Salvager", PileConfig.kingdom(Salvager::new)),
            Map.entry("Sea Chart", PileConfig.kingdom(SeaChart::new)),
            Map.entry("Sea Hag", PileConfig.kingdom(SeaHag::new)),
            Map.entry("Sea Witch", PileConfig.kingdom(SeaWitch::new)),
            Map.entry("Smugglers", PileConfig.kingdom(Smugglers::new)),
            Map.entry("Tactician", PileConfig.kingdom(Tactician::new)),
            Map.entry("Tide Pools", PileConfig.kingdom(TidePools::new)),
            Map.entry("Treasure Map", PileConfig.kingdom(TreasureMap::new)),
            Map.entry("Treasury", PileConfig.kingdom(Treasury::new)),
            Map.entry("Warehouse", PileConfig.kingdom(Warehouse::new)),
            Map.entry("Wharf", PileConfig.kingdom(Wharf::new))
    );

    /**
     * Renvoie une pile de cartes pour la réserve
     *
     * @param cardName        le nom de la carte
     * @param numberOfPlayers le nombre de joueurs
     * @return une pile de cartes de même type
     */
    public static SupplyPile createSupplyPile(String cardName, int numberOfPlayers) {
        PileConfig config = PILE_CONFIGS.get(cardName);
        return new SupplyPile(config.cardSupplier(), config.countFunction().apply(numberOfPlayers));
    }
}
