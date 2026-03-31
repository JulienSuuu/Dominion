package fr.umontpellier.iut.dominion;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.umontpellier.iut.dominion.cards.Card;

class GameProfTest extends BaseTestClass {

    @BeforeEach
    void initGame() {
        setup3pGame("Astrolabe",
                "Bazaar",
                "Blockade",
                "Caravan",
                "Corsair",
                "Cutpurse",
                "Fishing Village",
                "Haven",
                "Island",
                "Lighthouse");
    }

    @Test
    void testAvailableSupplyCards() {
        List<String> expected = Stream.of(
                "Copper",
                "Silver",
                "Gold",
                "Estate",
                "Duchy",
                "Province",
                "Curse",
                "Astrolabe",
                "Bazaar",
                "Blockade",
                "Caravan",
                "Corsair",
                "Cutpurse",
                "Fishing Village",
                "Haven",
                "Island",
                "Lighthouse").sorted().toList();
        List<String> actual = game.getAvailableSupplyCards().stream().map(Card::getName).sorted().toList();
        assertIterableEquals(expected, actual);
    }

    @Test
    void testAvailableSupplyPilesWithEmptyPiles() {
        getSupply("Astrolabe").clear();
        getSupply("Estate").clear();
        List<String> expected = Stream.of(
                "Copper",
                "Silver",
                "Gold",
                "Duchy",
                "Province",
                "Curse",
                "Bazaar",
                "Blockade",
                "Caravan",
                "Corsair",
                "Cutpurse",
                "Fishing Village",
                "Haven",
                "Island",
                "Lighthouse").sorted().toList();
        List<String> actual = game.getAvailableSupplyCards().stream().map(Card::getName).sorted().toList();
        assertIterableEquals(expected, actual);
    }
}