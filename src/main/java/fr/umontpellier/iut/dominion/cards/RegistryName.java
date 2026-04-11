package fr.umontpellier.iut.dominion.cards;

import java.util.ArrayList;
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
                    "Workshop",
                    "Adventurer",
                    "Chancellor",
                    "Feast",
                    "Spy",
                    "Thief",
                    "Woodcutter"
            ),
            "Intrigue", Arrays.asList(
                    "Baron",
                    "Bridge",
                    "Conspirator",
                    "Courtier",
                    "Courtyard",
                    "Diplomat",
                    "Duke",
                    "Farm",
                    "Ironworks",
                    "Lurker",
                    "Masquerade",
                    "Mill",
                    "Mining_Village",
                    "Minion",
                    "Nobles",
                    "Patrol",
                    "Pawn",
                    "Replace",
                    "Secret_Passage",
                    "Shanty_Town",
                    "Steward",
                    "Swindler",
                    "Torturer",
                    "Trading_Post",
                    "Upgrade",
                    "Wishing_Well",
                    "Coppersmith",
                    "Great_Hall",
                    "Saboteur",
                    "Scout",
                    "Secret_Chamber",
                    "Tribute"
            ),
            "Alchemy", Arrays.asList(
                    "Alchemist",
                    "Apothecary",
                    "Apprentice",
                    "Familiar",
                    "Golem",
                    "Herbalist",
                    "Philosopher's Stone",
                    "Possession",
                    "Scrying Pool",
                    "Transmute",
                    "University",
                    "Vineyard"
            )
    );



    public static List<String> getExtension(String kingdom){
        return kingdomCards.get(kingdom);
    }
    public static List<String> getExtensions(String... kingdom){
        List<String> kingdoms = new ArrayList<>();
        for(String king : kingdom){
            kingdoms.addAll(kingdomCards.get(king));
        }
        return kingdoms;

    }
}
