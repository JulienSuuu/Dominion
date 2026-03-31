package fr.umontpellier.iut.dominion;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AssertionFailureBuilder;
import org.junit.jupiter.api.BeforeAll;

import fr.umontpellier.iut.dominion.cards.Card;

// @Timeout(value = 1, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
public class BaseTestClass {
    public static final long TIMEOUT_VALUE = 500;

    IOGame game;
    List<SupplyPile> supplyPiles;
    List<Card> trashedCards;
    List<TestPlayer> players;
    TestPlayer p1, p2, p3;

    @BeforeAll
    static void disableConsole() {
        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int arg0) {
            }
        }));
    }

    public void setGame(IOGame game) {
        this.game = game;
        supplyPiles = TestUtils.getAttribute(game, "supplyPiles");
        trashedCards = TestUtils.getAttribute(game, "trashedCards");
        List<Player> gamePlayers = TestUtils.getAttribute(game, "players");
        players = gamePlayers.stream().map(TestPlayer::new).toList();
        p1 = players.get(0);
        p2 = players.get(1);
        p3 = gamePlayers.size() >= 3 ? players.get(2) : null;
    }

    public void setup2pGame(String... kingdomCardsNames) {
        setGame(new IOGame(new String[] { "Marco", "Polo" }, kingdomCardsNames));
    }

    public void setup3pGame(String... kingdomCardsNames) {
        setGame(new IOGame(new String[] { "Salt", "Pepper", "Cumin" }, kingdomCardsNames));
    }

    public SupplyPile getSupply(String cardName) {
        for (SupplyPile pile : supplyPiles) {
            if (pile.getName().equals(cardName)) {
                return pile;
            }
        }
        return null;
    }

    public Card getCardFromSupply(String cardName) {
        SupplyPile supply = getSupply(cardName);
        return supply.getLast();
    }

    public Card[] getCardsFromSupply(String cardName, int n) {
        SupplyPile supply = getSupply(cardName);
        Card[] cards = new Card[n];
        for (int i = 0; i < n; i++) {
            cards[i] = supply.get(supply.size() - 1 - i);
        }
        return cards;
    }

    public void playTurn(List<String> instructions) {
        game.setInput(instructions);
        game.getCurrentTurnPlayer().playTurn();
        assertTrue(game.getInstructions().isEmpty());
    }

    public void playTurn(String... instructions) {
        List<String> instructionsList = new ArrayList<>(Arrays.asList(instructions));
        playTurn(instructionsList);
    }

    public void endTurn(String... instructions) {
        game.setInput(instructions);
        game.getCurrentTurnPlayer().cleanup();
        game.moveToNextPlayer();
        assertTrue(game.getInstructions().isEmpty());
    }

    public static void discardHand(TestPlayer p) {
        for (Card c : new ArrayList<>(p.hand)) {
            c.moveTo(p.discard);
        }
    }

    public void assertPlayerState(TestPlayer p, int coins, int actions, int buys) {
        if (coins != p.getMoney()) {
            AssertionFailureBuilder.assertionFailure()
                    .message("Incorrect number of coins")
                    .expected(coins)
                    .actual(p.getMoney())
                    .buildAndThrow();
        }
        if (actions != p.getNumberOfActions()) {
            AssertionFailureBuilder.assertionFailure()
                    .message("Incorrect number of actions")
                    .expected(actions)
                    .actual(p.getNumberOfActions())
                    .buildAndThrow();
        }
        if (buys != p.getNumberOfBuys()) {
            AssertionFailureBuilder.assertionFailure()
                    .message("Incorrect number of buys")
                    .expected(buys)
                    .actual(p.getNumberOfBuys())
                    .buildAndThrow();
        }
    }

    public void assertCardInLocation(Card card, List<Card> location) {
        List<List<Card>> locations = new ArrayList<>(supplyPiles);
        locations.add(trashedCards);
        for (TestPlayer p : players) {
            locations.add(p.hand);
            locations.add(p.draw);
            locations.add(p.discard);
            locations.add(p.inPlay);
            locations.add(p.cardsSetAside);
            locations.add(p.islandMat);
            locations.add(p.nativeVillageMat);
        }

        if (!location.contains(card)) {
            AssertionFailureBuilder.assertionFailure()
                    .message("Card is not in expected location")
                    .buildAndThrow();
        }
        for (List<Card> l : locations) {
            if (l != location && l.contains(card)) {
                AssertionFailureBuilder.assertionFailure()
                        .message("Card is in another location")
                        .buildAndThrow();
            }
        }
    }
}
