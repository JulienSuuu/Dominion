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
            Map.entry("Embargo", PileConfig.kingdom(SeaSideFactory::Embargo)),
            Map.entry("Explorer", PileConfig.kingdom(SeaSideFactory::Explorer)),
            Map.entry("Fishing Village", PileConfig.kingdom(SeaSideFactory::FishingVillage)),
            Map.entry("Ghost Ship", PileConfig.kingdom(SeaSideFactory::GhostShip)),
            Map.entry("Haven", PileConfig.kingdom(SeaSideFactory::Haven)),
            Map.entry("Island", PileConfig.victory(SeaSideFactory::Island)),
            Map.entry("Lighthouse", PileConfig.kingdom(SeaSideFactory::LightHouse)),
            Map.entry("Lookout", PileConfig.kingdom(SeaSideFactory::Lookout)),
            Map.entry("Merchant Ship", PileConfig.kingdom(SeaSideFactory::MerchantShip)),
            Map.entry("Monkey", PileConfig.kingdom(SeaSideFactory::Monkey)),
            Map.entry("Native Village", PileConfig.kingdom(SeaSideFactory::NativeVillage)),
            Map.entry("Navigator", PileConfig.kingdom(SeaSideFactory::Navigator)),
            Map.entry("Outpost", PileConfig.kingdom(SeaSideFactory::Outpost)),
            Map.entry("Pearl Diver", PileConfig.kingdom(SeaSideFactory::PearlDiver)),
            Map.entry("Pirate", PileConfig.kingdom(SeaSideFactory::Pirate)),
            Map.entry("Pirate Ship", PileConfig.kingdom(SeaSideFactory::PirateShip)),
            Map.entry("Sailor", PileConfig.kingdom(SeaSideFactory::Sailor)),
            Map.entry("Salvager", PileConfig.kingdom(SeaSideFactory::Salvager)),
            Map.entry("Sea Chart", PileConfig.kingdom(SeaSideFactory::SeaChart)),
            Map.entry("Sea Hag", PileConfig.kingdom(SeaSideFactory::SeaHag)),
            Map.entry("Sea Witch", PileConfig.kingdom(SeaSideFactory::SeaWitch)),
            Map.entry("Smugglers", PileConfig.kingdom(SeaSideFactory::Smugglers)),
            Map.entry("Tactician", PileConfig.kingdom(SeaSideFactory::Tactician)),
            Map.entry("Tide Pools", PileConfig.kingdom(SeaSideFactory::TidePools)),
            Map.entry("Treasure Map", PileConfig.kingdom(SeaSideFactory::TreasureMap)),
            Map.entry("Treasury", PileConfig.kingdom(SeaSideFactory::Treasury)),
            Map.entry("Warehouse", PileConfig.kingdom(SeaSideFactory::Warehouse)),
            Map.entry("Wharf", PileConfig.kingdom(SeaSideFactory::Wharf))
            //Kingdoms(Dominion)
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
