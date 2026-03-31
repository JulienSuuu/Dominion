package fr.umontpellier.iut.dominion;

import java.util.List;

import fr.umontpellier.iut.dominion.cards.Card;

public class TestPlayer {
    Player player;
    List<Card> hand;
    List<Card> draw;
    List<Card> discard;
    List<Card> inPlay;
    List<Card> cardsSetAside;
    List<Card> islandMat;
    List<Card> nativeVillageMat;

    TestPlayer(Player p) {
        this.player = p;
        this.hand = TestUtils.getAttribute(p, "hand");
        this.draw = TestUtils.getAttribute(p, "draw");
        this.discard = TestUtils.getAttribute(p, "discard");
        this.inPlay = TestUtils.getAttribute(p, "inPlay");
        this.cardsSetAside = TestUtils.getAttribute(p, "cardsSetAside");
        this.islandMat = TestUtils.getAttribute(p, "islandMat");
        this.nativeVillageMat = TestUtils.getAttribute(p, "nativeVillageMat");
    }

    int getMoney() {
        return player.getMoney();
    }

    int getNumberOfActions() {
        return player.getNumberOfActions();
    }

    int getNumberOfBuys() {
        return player.getNumberOfBuys();
    }
}
