package fr.umontpellier.iut.dominion.cards;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RegistryName {
    static Map<String, List<String>> kingdomCards =Map.of(
            "Seaside", Arrays.asList(
                    "Ambassador",
                    "Astrolabe",
                    "Bazaar",
                    "Blockade",
                    "Caravan",
                    "Corsair",
                    "Cutpurse",
                    "Embargo",
                    "Explorer",
                    "Fishing Village",
                    "Ghost Ship",
                    "Haven",
                    "Island",
                    "Lighthouse",
                    "Lookout",
                    "Merchant Ship",
                    "Monkey",
                    "Native Village",
                    "Navigator",
                    "Outpost",
                    "Pearl Diver",
                    "Pirate",
                    "Pirate Ship",
                    "Sailor",
                    "Salvager",
                    "Sea Chart",
                    "Sea Hag",
                    "Sea Witch",
                    "Smugglers",
                    "Tactician",
                    "Tide Pools",
                    "Treasure Map",
                    "Treasury",
                    "Warehouse",
                    "Wharf"
            ),
            "Dominion",Arrays.asList(
                    "Artisan",
                    "Bandit",
                    "Bureaucrat",
                    "Cellar",
                    "Chapel",
                    "Council Room",
                    "Festival",
                    "Gardens",
                    "Harbinger",
                    "Laboratory",
                    "Library",
                    "Market",
                    "Merchant",
                    "Militia",
                    "Mine",
                    "Moat",
                    "MoneyLender",
                    "Poacher",
                    "Remodel",
                    "Sentry",
                    "Smithy",
                    "Throne Room",
                    "Vassal",
                    "Village",
                    "Witch",
                    "WorkShop",
                    "Adventurer",
                    "Chancellor",
                    "Feast",
                    "Spy",
                    "Thief",
                    "Woodcutter"
            )
    );



    public static List<String> getExtension(String kingdom){
        return kingdomCards.get(kingdom);
    }
}
