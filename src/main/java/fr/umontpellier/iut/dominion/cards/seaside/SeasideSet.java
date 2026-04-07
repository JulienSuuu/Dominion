package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.FactorySupplyPile.PileConfig;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class SeasideSet  {

    private static final Map<String, PileConfig> PILE_SEASIDES = Map.<String, PileConfig>ofEntries(
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
    );

    public static Map<String, PileConfig> SeaSideSet(){
        return PILE_SEASIDES;
    }





}
