package fr.umontpellier.iut.dominion.cards;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RegistrySet {
    static Map<String, List<String>> sets = Map.of(
            "First Game", Arrays.asList(
                    "Cellar",
                    "Market",
                    "Merchant",
                    "Militia",
                    "Mine",
                    "Moat",
                    "Remodel",
                    "Smithy",
                    "Village",
                    "Workshop"
            ),
            "Size Distortion", Arrays.asList(
                    "Artisan",
                    "Bandit",
                    "Bureaucrat",
                    "Chapel",
                    "Festival",
                    "Gardens",
                    "Sentry",
                    "Throne Room",
                    "Witch",
                    "Workshop"
            ),
            "Deck Top", Arrays.asList(
                    "Artisan",
                    "Bureaucrat",
                    "Council Room",
                    "Festival",
                    "Harbinger",
                    "Laboratory",
                    "MoneyLender",
                    "Sentry",
                    "Vassal",
                    "Village"
            )
    );



    public static List<String> get(String key) {
        return sets.get(key);
    }
}
