package fr.umontpellier.iut.dominion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import fr.umontpellier.iut.dominion.cards.Card;

public class CardsProfTest extends BaseTestClass {
    @Test
    public void test_astrolabe() {
        setup2pGame("Astrolabe");
        Card c = getCardFromSupply("Astrolabe");
        c.moveTo(p1.hand);

        playTurn("HAND:Astrolabe", "");
        assertCardInLocation(c, p1.inPlay);
        assertPlayerState(p1, 1, 1, 2);
        endTurn();
        assertCardInLocation(c, p1.inPlay);

        playTurn("");
        endTurn();

        playTurn("");
        assertCardInLocation(c, p1.inPlay);
        assertPlayerState(p1, 1, 1, 2);
        endTurn();
        assertFalse(p1.inPlay.contains(c));
    }

    @Test
    void test_bazaar() {
        setup2pGame("Bazaar");
        Card c = getCardFromSupply("Bazaar");
        Card duchy = getCardFromSupply("Duchy");
        c.moveTo(p1.hand);
        duchy.moveTo(p1.draw);

        playTurn("HAND:Bazaar", "");
        assertPlayerState(p1, 1, 2, 1);
        assertCardInLocation(c, p1.inPlay);
        assertCardInLocation(duchy, p1.hand);
        endTurn();
        assertCardInLocation(c, p1.discard);
    }

    @Test
    void test_blockade_simple() {
        setup2pGame("Blockade", "Caravan");
        Card c = getCardFromSupply("Blockade");
        Card duchy = getCardFromSupply("Duchy");
        Card caravan = getCardFromSupply("Caravan");

        // p1
        c.moveTo(p1.hand);
        playTurn("HAND:Blockade", "SUPPLY:Duchy", "SUPPLY:Caravan", "");
        assertPlayerState(p1, 0, 0, 1);
        assertCardInLocation(c, p1.inPlay);
        assertCardInLocation(duchy, getSupply("Duchy"));
        assertCardInLocation(caravan, p1.cardsSetAside);
        endTurn();
        assertCardInLocation(c, p1.inPlay);

        // p2
        playTurn("");
        endTurn();

        // p3
        playTurn("");
        assertPlayerState(p1, 0, 1, 1);
        assertCardInLocation(c, p1.inPlay);
        assertCardInLocation(caravan, p1.hand);
        endTurn();
        assertFalse(p1.discard.contains(c));
    }

    @Test
    void test_blockade_attack() {
        setup2pGame("Blockade", "Caravan");
        Card c = getCardFromSupply("Blockade");
        Card curse = getCardFromSupply("Curse");
        Card[] caravan = getCardsFromSupply("Caravan", 2);
        Card[] silver = getCardsFromSupply("Silver", 2);
        c.moveTo(p1.hand);
        silver[0].moveTo(p2.hand);
        silver[1].moveTo(p2.hand);

        playTurn("HAND:Blockade", "SUPPLY:Caravan", "");
        assertPlayerState(p1, 0, 0, 1);
        assertCardInLocation(c, p1.inPlay);
        assertCardInLocation(caravan[0], p1.cardsSetAside);
        assertCardInLocation(caravan[1], getSupply("Caravan"));
        assertCardInLocation(curse, getSupply("Curse"));
        endTurn();
        assertCardInLocation(c, p1.inPlay);

        playTurn("HAND:Silver", "HAND:Silver", "SUPPLY:Caravan");
        assertPlayerState(p2, 0, 1, 0);
        assertCardInLocation(c, p1.inPlay);
        assertCardInLocation(caravan[1], p2.discard);
        assertCardInLocation(curse, p2.discard);
        endTurn();
        assertFalse(p1.discard.contains(c));
    }
    // Ajouter test Blockade attaque :
    // - différentes sources de gain
    // - si plus de Curse
    // - si Lighthouse

    @Test
    void test_caravan() {
        setup2pGame("Caravan");
        Card c = getCardFromSupply("Caravan");
        Card[] duchy = getCardsFromSupply("Duchy", 2);
        c.moveTo(p1.hand);
        // duchy[0] est placée au fond de la pioche
        duchy[0].moveTo(p1.draw);
        p1.draw.add(0, p1.draw.removeLast());
        duchy[1].moveTo(p1.draw);

        playTurn("HAND:Caravan", "");
        assertPlayerState(p1, 0, 1, 1);
        assertCardInLocation(c, p1.inPlay);
        assertCardInLocation(duchy[1], p1.hand);
        endTurn();
        assertCardInLocation(c, p1.inPlay);

        playTurn("");
        endTurn();

        assertCardInLocation(duchy[0], p1.draw);
        playTurn("");
        assertCardInLocation(c, p1.inPlay);
        assertCardInLocation(duchy[0], p1.hand);
        endTurn();
        assertFalse(p1.inPlay.contains(c));
    }

    @Test
    void test_corsair_simple() {
        setup2pGame("Corsair");
        Card c = getCardFromSupply("Corsair");
        Card estate = getCardFromSupply("Estate");
        c.moveTo(p1.hand);
        // estate est placée au fond de la pioche
        estate.moveTo(p1.draw);
        p1.draw.add(0, p1.draw.removeLast());

        playTurn("HAND:Corsair", "");

        assertPlayerState(p1, 2, 0, 1);
        assertCardInLocation(c, p1.inPlay);
        endTurn();
        assertCardInLocation(c, p1.inPlay);

        playTurn("");
        endTurn();

        playTurn("");
        assertPlayerState(p1, 0, 1, 1);
        assertCardInLocation(c, p1.inPlay);
        assertCardInLocation(estate, p1.hand);
        endTurn();
        assertFalse(p1.inPlay.contains(c));
    }

    @Test
    void test_corsair_attack() {
        setup3pGame("Corsair");
        Card c = getCardFromSupply("Corsair");
        Card estate = getCardFromSupply("Estate");
        c.moveTo(p1.hand);
        // estate est placée au fond de la pioche
        estate.moveTo(p1.draw);
        p1.draw.add(0, p1.draw.removeLast());

        discardHand(p2);
        Card copper = getCardFromSupply("Copper");
        copper.moveTo(p2.hand);
        Card[] silver = getCardsFromSupply("Silver", 3);
        silver[0].moveTo(p2.hand);
        silver[1].moveTo(p2.hand);

        discardHand(p3);
        silver[2].moveTo(p3.hand);
        Card gold = getCardFromSupply("Gold");
        gold.moveTo(p3.hand);

        playTurn("HAND:Corsair", "");
        assertPlayerState(p1, 2, 0, 1);
        assertCardInLocation(c, p1.inPlay);
        endTurn();
        assertCardInLocation(c, p1.inPlay);

        playTurn("HAND:Copper", "HAND:Silver", "HAND:Silver", "");
        assertPlayerState(p2, 5, 1, 1);
        assertCardInLocation(copper, p2.inPlay);
        assertCardInLocation(silver[0], trashedCards);
        assertCardInLocation(silver[1], p2.inPlay);
        endTurn();

        playTurn("HAND:Gold", "HAND:Silver", "");
        assertPlayerState(p3, 5, 1, 1);
        assertCardInLocation(gold, trashedCards);
        assertCardInLocation(silver[2], p3.inPlay);
        endTurn();

        playTurn("");
        assertPlayerState(p1, 0, 1, 1);
        assertCardInLocation(c, p1.inPlay);
        assertCardInLocation(estate, p1.hand);
        endTurn();
        assertFalse(p1.inPlay.contains(c));
    }

    @Test
    void test_corsair_attack_with_lighthouse() {
        setup3pGame("Corsair", "Lighthouse");
        Card c = getCardFromSupply("Corsair");
        Card lighthouse = getCardFromSupply("Lighthouse");
        Card[] silver = getCardsFromSupply("Silver", 2);
        Card estate = getCardFromSupply("Estate");

        // p1
        lighthouse.moveTo(p1.hand);
        playTurn("HAND:Lighthouse", "");
        endTurn();
        assertCardInLocation(lighthouse, p1.inPlay);

        // p2
        playTurn("");
        endTurn();

        // p3
        c.moveTo(p3.hand);
        // estate est placée au fond de la pioche de p3
        estate.moveTo(p3.draw);
        p3.draw.add(0, p3.draw.removeLast());

        playTurn("HAND:Corsair", "");
        endTurn();
        assertCardInLocation(c, p3.inPlay);

        // p1
        silver[0].moveTo(p1.hand);
        playTurn("HAND:Silver", "");
        assertPlayerState(p1, 3, 1, 1);
        assertCardInLocation(lighthouse, p1.inPlay);
        assertCardInLocation(silver[0], p1.inPlay);
        endTurn();
        assertFalse(p1.inPlay.contains(lighthouse));

        // p2
        silver[1].moveTo(p2.hand);
        playTurn("HAND:Silver", "");
        assertPlayerState(p2, 2, 1, 1);
        assertCardInLocation(silver[1], trashedCards);
        endTurn();

        // p3
        playTurn("");
        assertPlayerState(p3, 0, 1, 1);
        assertCardInLocation(c, p3.inPlay);
        assertCardInLocation(estate, p3.hand);
        endTurn();
        assertFalse(p3.inPlay.contains(c));
    }

    @Test
    void test_cutpurse() {
        setup3pGame("Cutpurse");
        Card c = getCardFromSupply("Cutpurse");
        Card[] coppers = getCardsFromSupply("Copper", 2);
        Card[] estates = getCardsFromSupply("Estate", 4);

        c.moveTo(p1.hand);
        discardHand(p2);
        coppers[0].moveTo(p2.hand);
        coppers[1].moveTo(p2.hand);
        estates[0].moveTo(p2.hand);
        discardHand(p3);
        estates[1].moveTo(p3.hand);
        estates[2].moveTo(p3.hand);
        estates[3].moveTo(p3.hand);

        playTurn("HAND:Cutpurse", "");
        assertPlayerState(p1, 2, 0, 1);
        assertEquals(2, p2.hand.size());
        assertCardInLocation(coppers[0], p2.discard);
        assertEquals(3, p3.hand.size());
        endTurn();
        assertCardInLocation(c, p1.discard);
    }

    @Test
    void test_cutpurse_lighthouse() {
        setup3pGame("Cutpurse", "Lighthouse");
        Card c = getCardFromSupply("Cutpurse");
        Card lighthouse = getCardFromSupply("Lighthouse");
        Card[] coppers = getCardsFromSupply("Copper", 4);
        Card[] estates = getCardsFromSupply("Estate", 2);

        // p1
        lighthouse.moveTo(p1.hand);
        playTurn("HAND:Lighthouse", "");
        endTurn();

        // p2
        playTurn("");
        endTurn();

        discardHand(p1);
        coppers[0].moveTo(p1.hand);
        coppers[1].moveTo(p1.hand);
        estates[0].moveTo(p1.hand);
        discardHand(p2);
        coppers[2].moveTo(p2.hand);
        coppers[3].moveTo(p2.hand);
        estates[1].moveTo(p2.hand);
        c.moveTo(p3.hand);

        // p3
        playTurn("HAND:Cutpurse", "");
        assertPlayerState(p3, 2, 0, 1);
        assertEquals(3, p1.hand.size());
        assertEquals(2, p2.hand.size());
        assertCardInLocation(coppers[2], p2.discard);
        endTurn();
        assertCardInLocation(c, p3.discard);
    }

    @Test
    void test_fishing_village() {
        setup2pGame("Fishing Village");
        Card c = getCardFromSupply("Fishing Village");
        c.moveTo(p1.hand);
        playTurn("HAND:Fishing Village", "");
        assertPlayerState(p1, 1, 2, 1);
        endTurn();
        assertCardInLocation(c, p1.inPlay);

        playTurn("");
        endTurn();

        playTurn("");
        assertPlayerState(p1, 1, 2, 1);
        assertCardInLocation(c, p1.inPlay);
        endTurn();
        assertFalse(p1.discard.contains(c));
    }

    @Test
    void test_haven() {
        setup2pGame("Haven");
        Card c = getCardFromSupply("Haven");
        Card silver = getCardFromSupply("Silver");
        c.moveTo(p1.hand);
        silver.moveTo(p1.hand);

        Card drawnCard = p1.draw.getLast();
        playTurn("HAND:Haven", "HAND:Silver", "");
        assertPlayerState(p1, 0, 1, 1);
        assertCardInLocation(drawnCard, p1.hand);
        assertCardInLocation(silver, p1.cardsSetAside);
        endTurn();
        assertCardInLocation(c, p1.inPlay);

        playTurn("");
        endTurn();

        playTurn("");
        assertPlayerState(p1, 0, 1, 1);
        assertCardInLocation(c, p1.inPlay);
        assertCardInLocation(silver, p1.hand);
        endTurn();
        assertFalse(p1.inPlay.contains(c));
    }

    @Test
    void test_island() {
        setup2pGame("Island");
        Card c = getCardFromSupply("Island");
        Card duchy = getCardFromSupply("Duchy");
        c.moveTo(p1.hand);
        duchy.moveTo(p1.hand);

        playTurn("HAND:Island", "HAND:Duchy", "");
        assertPlayerState(p1, 0, 0, 1);
        assertCardInLocation(c, p1.islandMat);
        assertCardInLocation(duchy, p1.islandMat);
        endTurn();
    }

    @Test
    void test_lighthouse_simple() {
        setup2pGame("Lighthouse");
        Card c = getCardFromSupply("Lighthouse");
        c.moveTo(p1.hand);
        playTurn("HAND:Lighthouse", "");
        assertPlayerState(p1, 1, 1, 1);
        endTurn();

        playTurn("");
        endTurn();

        playTurn("");
        assertPlayerState(p1, 1, 1, 1);
        assertCardInLocation(c, p1.inPlay);
        endTurn();
        assertFalse(p1.inPlay.contains(c));
    }

    @Test
    void test_lookout() {
        setup2pGame("Lookout");
        Card c = getCardFromSupply("Lookout");
        Card copper = getCardFromSupply("Copper");
        Card silver = getCardFromSupply("Silver");
        Card gold = getCardFromSupply("Gold");
        c.moveTo(p1.hand);
        silver.moveTo(p1.draw);
        gold.moveTo(p1.draw);
        copper.moveTo(p1.draw);

        playTurn(
                "HAND:Lookout",
                "BUTTON:Copper", // carte à écarter (trash)
                "BUTTON:Silver", // carte à défausser
                // (la dernière carte est automatiquement remise sur la pioche)
                "");
        assertPlayerState(p1, 0, 1, 1);
        assertCardInLocation(copper, trashedCards);
        assertCardInLocation(silver, p1.discard);
        assertCardInLocation(gold, p1.draw);
        assertSame(gold, p1.draw.getLast());
        endTurn();
        assertCardInLocation(c, p1.discard);
    }

    @Test
    void test_merchant_ship() {
        setup2pGame("Merchant Ship");
        Card c = getCardFromSupply("Merchant Ship");
        c.moveTo(p1.hand);
        playTurn("HAND:Merchant Ship", "");
        assertPlayerState(p1, 2, 0, 1);
        endTurn();
        assertCardInLocation(c, p1.inPlay);

        playTurn("");
        endTurn();

        playTurn("");
        assertPlayerState(p1, 2, 1, 1);
        assertCardInLocation(c, p1.inPlay);
        endTurn();
        assertFalse(p1.inPlay.contains(c));
    }

    @Test
    void test_monkey_simple() {
        setup2pGame("Monkey");
        Card c = getCardFromSupply("Monkey");
        Card silver = getCardFromSupply("Silver");
        c.moveTo(p1.hand);
        playTurn("HAND:Monkey", "");
        assertPlayerState(p1, 0, 0, 1);
        endTurn();
        assertCardInLocation(c, p1.inPlay);

        playTurn("");
        endTurn();

        silver.moveTo(p1.draw);
        playTurn("");
        assertPlayerState(p1, 0, 1, 1);
        assertCardInLocation(c, p1.inPlay);
        assertCardInLocation(silver, p1.hand);
        assertEquals(6, p1.hand.size());
        endTurn();
        assertFalse(p1.inPlay.contains(c));
    }

    @Test
    void test_monkey_other_player_buys() {
        setup3pGame("Monkey");
        Card c = getCardFromSupply("Monkey");
        Card[] silvers = getCardsFromSupply("Silver", 2);
        Card[] coppers = getCardsFromSupply("Copper", 3);

        c.moveTo(p1.hand);
        // p1
        playTurn("HAND:Monkey", "");
        assertPlayerState(p1, 0, 0, 1);
        endTurn();
        assertCardInLocation(c, p1.inPlay);

        silvers[1].moveTo(p1.draw);
        silvers[0].moveTo(p1.draw);
        // p2
        playTurn("SUPPLY:Copper");
        assertCardInLocation(coppers[0], p2.discard);
        assertCardInLocation(silvers[0], p1.draw);
        assertCardInLocation(silvers[1], p1.draw);
        endTurn();

        // p3
        playTurn("SUPPLY:Copper");
        assertCardInLocation(coppers[1], p3.discard);
        assertCardInLocation(silvers[0], p1.hand);
        assertCardInLocation(silvers[1], p1.draw);
        endTurn();

        // p1
        playTurn("");
        assertPlayerState(p1, 0, 1, 1);
        assertCardInLocation(silvers[0], p1.hand);
        assertCardInLocation(silvers[1], p1.hand);
        assertEquals(7, p1.hand.size());
        assertCardInLocation(c, p1.inPlay);
        endTurn();
        assertFalse(p1.inPlay.contains(c));
    }

    @Test
    void test_native_village_add_card_and_take_on_next_turn() {
        setup2pGame("Native Village");
        Card[] nativeVillages = getCardsFromSupply("Native Village", 2);
        Card silver = getCardFromSupply("Silver");

        nativeVillages[0].moveTo(p1.hand);
        silver.moveTo(p1.draw);
        playTurn("HAND:Native Village", "BUTTON:add", "");
        assertPlayerState(p1, 0, 2, 1);
        assertCardInLocation(silver, p1.nativeVillageMat);
        endTurn();
        assertCardInLocation(silver, p1.nativeVillageMat);
        assertCardInLocation(nativeVillages[0], p1.discard);

        playTurn("");
        endTurn();

        nativeVillages[1].moveTo(p1.hand);
        playTurn("HAND:Native Village", "BUTTON:take", "");
        assertPlayerState(p1, 0, 2, 1);
        assertCardInLocation(silver, p1.hand);
        assertTrue(p1.nativeVillageMat.isEmpty());
        endTurn();
    }

    @Test
    void test_native_village_add_card_and_take_on_same_turn() {
        setup2pGame("Native Village");
        Card[] nativeVillages = getCardsFromSupply("Native Village", 2);
        Card silver = getCardFromSupply("Silver");

        nativeVillages[0].moveTo(p1.hand);
        nativeVillages[1].moveTo(p1.hand);
        silver.moveTo(p1.draw);
        playTurn("HAND:Native Village", "BUTTON:add", "HAND:Native Village", "BUTTON:take", "");
        assertPlayerState(p1, 0, 3, 1);
        assertCardInLocation(silver, p1.hand);
        assertTrue(p1.nativeVillageMat.isEmpty());
        endTurn();
        assertCardInLocation(nativeVillages[0], p1.discard);
        assertCardInLocation(nativeVillages[1], p1.discard);
        assertCardInLocation(silver, p1.discard);
    }

    @Test
    void test_outpost_simple() {
        setup2pGame("Outpost");
        Card c = getCardFromSupply("Outpost");
        c.moveTo(p1.hand);

        // p1
        playTurn("HAND:Outpost", "");
        assertPlayerState(p1, 0, 0, 1);
        endTurn();
        assertEquals(3, p1.hand.size());
        assertCardInLocation(c, p1.inPlay);

        // p1 (tour supplémentaire)
        playTurn("HAND:Copper", "");
        assertPlayerState(p1, 1, 1, 1);
        assertCardInLocation(c, p1.inPlay);
        endTurn();
        assertFalse(p1.inPlay.contains(c));
    }

    @Test
    void test_pirate_simple() {
        setup2pGame("Pirate");
        Card c = getCardFromSupply("Pirate");
        Card gold = getCardFromSupply("Gold");

        c.moveTo(p1.hand);
        playTurn("HAND:Pirate", "");
        assertPlayerState(p1, 0, 0, 1);
        endTurn();
        assertCardInLocation(c, p1.inPlay);

        playTurn("");
        endTurn();

        playTurn("SUPPLY:Gold", "");
        assertPlayerState(p1, 0, 1, 1);
        assertCardInLocation(c, p1.inPlay);
        assertCardInLocation(gold, p1.hand);
        endTurn();
        assertFalse(p1.inPlay.contains(c));
    }

    @Test
    void test_pirate_reaction_to_self_gain() {
        setup2pGame("Pirate");
        Card c = getCardFromSupply("Pirate");
        Card copper = getCardFromSupply("Copper");
        Card gold = getCardFromSupply("Gold");

        // p1
        c.moveTo(p1.hand);
        playTurn("SUPPLY:Copper",
                "HAND:Pirate"); // p1 joue le Pirate en réaction
        assertPlayerState(p1, 0, 1, 0);
        assertCardInLocation(c, p1.inPlay);
        assertCardInLocation(copper, p1.discard);
        endTurn();
        assertCardInLocation(c, p1.inPlay);

        // p2
        playTurn("");
        endTurn();

        // p1
        playTurn("SUPPLY:Gold", "HAND:Gold", "");
        assertPlayerState(p1, 3, 1, 1);
        assertCardInLocation(c, p1.inPlay);
        assertCardInLocation(gold, p1.inPlay);
        endTurn();
        assertFalse(p1.inPlay.contains(c));
    }

    @Test
    void test_pirate_reaction_to_other_gain() {
        setup2pGame("Pirate");
        Card c = getCardFromSupply("Pirate");
        Card copper = getCardFromSupply("Copper");
        Card gold = getCardFromSupply("Gold");

        // p1
        c.moveTo(p2.hand);
        playTurn(
                "SUPPLY:Copper",
                "HAND:Pirate"); // p2 joue le Pirate en réaction
        assertPlayerState(p1, 0, 1, 0);
        assertCardInLocation(copper, p1.discard);
        assertCardInLocation(c, p2.inPlay);
        endTurn();
        assertCardInLocation(c, p2.inPlay);

        // p2
        playTurn("SUPPLY:Gold", "HAND:Gold", "");
        assertPlayerState(p2, 3, 1, 1);
        assertCardInLocation(c, p2.inPlay);
        assertCardInLocation(gold, p2.inPlay);
        endTurn();
        assertCardInLocation(c, p2.discard);
    }

    @Test
    void test_pirate_reaction_to_other_gain_pass() {
        setup2pGame("Pirate");
        Card c = getCardFromSupply("Pirate");
        Card copper = getCardFromSupply("Copper");

        // p1
        c.moveTo(p2.hand);
        playTurn("SUPPLY:Copper",
                ""); // p2 ne joue pas le Pirate en réaction
        assertPlayerState(p1, 0, 1, 0);
        assertCardInLocation(copper, p1.discard);
        endTurn();

        playTurn("");
        assertPlayerState(p2, 0, 1, 1);
        assertCardInLocation(c, p2.hand);
        endTurn();
    }

    @Test
    void test_sailor_simple() {
        setup2pGame("Sailor");
        Card c = getCardFromSupply("Sailor");
        Card duchy = getCardFromSupply("Duchy");

        c.moveTo(p1.hand);
        playTurn("HAND:Sailor", "");
        assertPlayerState(p1, 0, 1, 1);
        endTurn();
        assertCardInLocation(c, p1.inPlay);

        playTurn("");
        endTurn();

        duchy.moveTo(p1.hand);
        playTurn("HAND:Duchy", "");
        assertPlayerState(p1, 2, 1, 1);
        assertCardInLocation(c, p1.inPlay);
        assertCardInLocation(duchy, trashedCards);
        endTurn();
        assertFalse(p1.inPlay.contains(c));
    }

    @Test
    void test_sailor_buy_and_play_merchant_ship() {
        setup2pGame("Sailor", "Merchant Ship");
        Card c = getCardFromSupply("Sailor");
        Card merchantShip = getCardFromSupply("Merchant Ship");
        Card[] golds = getCardsFromSupply("Gold", 2);

        c.moveTo(p1.hand);
        golds[0].moveTo(p1.hand);
        golds[1].moveTo(p1.hand);
        playTurn(
                "HAND:Sailor",
                "HAND:Gold", // +3 pièces
                "HAND:Gold", // +3 pièces
                "SUPPLY:Merchant Ship", // achète Merchant Ship (-5 pièces)
                "BUTTON:y" // choisit de jouer Merchant Ship (+2 pièces)
        );
        assertPlayerState(p1, 3, 1, 0);
        endTurn();
        assertCardInLocation(c, p1.inPlay);
        assertCardInLocation(merchantShip, p1.inPlay);

        playTurn("");
        endTurn();

        playTurn(
                "", // n'écarte aucune carte
                "");
        assertPlayerState(p1, 4, 1, 1);
        assertCardInLocation(c, p1.inPlay);
        assertCardInLocation(merchantShip, p1.inPlay);
        endTurn();
        assertFalse(p1.inPlay.contains(c));
        assertFalse(p1.inPlay.contains(merchantShip));
    }

    @Test
    void test_sailor_buy_merchant_ship_dont_play_it() {
        setup2pGame("Sailor", "Merchant Ship");
        Card c = getCardFromSupply("Sailor");
        Card merchantShip = getCardFromSupply("Merchant Ship");
        Card[] golds = getCardsFromSupply("Gold", 2);

        c.moveTo(p1.hand);
        golds[0].moveTo(p1.hand);
        golds[1].moveTo(p1.hand);
        playTurn(
                "HAND:Sailor",
                "HAND:Gold", // +3 pièces
                "HAND:Gold", // +3 pièces
                "SUPPLY:Merchant Ship", // achète Merchant Ship (-5 pièces)
                "" // choisit de ne pas jouer Merchant Ship
        );
        assertPlayerState(p1, 1, 1, 0);
        endTurn();
        assertCardInLocation(c, p1.inPlay);
        assertCardInLocation(merchantShip, p1.discard);

        playTurn("");
        endTurn();

        playTurn(
                "", // n'écarte aucune carte
                "");
        assertPlayerState(p1, 2, 1, 1);
        assertCardInLocation(c, p1.inPlay);
        endTurn();
        assertFalse(p1.inPlay.contains(c));
    }

    @Test
    void test_sailor_buy_two_merchant_ship_can_only_play_one() {
        setup2pGame("Astrolabe", "Sailor", "Merchant Ship");
        Card c = getCardFromSupply("Sailor");
        Card astrolabe = getCardFromSupply("Astrolabe");
        Card[] merchantShip = getCardsFromSupply("Merchant Ship", 2);
        Card[] golds = getCardsFromSupply("Gold", 3);

        c.moveTo(p1.hand);
        astrolabe.moveTo(p1.hand);
        golds[0].moveTo(p1.hand);
        golds[1].moveTo(p1.hand);
        golds[2].moveTo(p1.hand);
        playTurn(
                "HAND:Sailor",
                "HAND:Astrolabe", // +1 pièce +1 achat
                "HAND:Gold", // +3 pièces
                "HAND:Gold", // +3 pièces
                "HAND:Gold", // +3 pièces
                "SUPPLY:Merchant Ship", // achète Merchant Ship (-5 pièces)
                "BUTTON:y", // choisit de jouer Merchant Ship (+2 pièces)
                "SUPPLY:Merchant Ship" // achète Merchant Ship (-5 pièces)
        );
        assertPlayerState(p1, 2, 1, 0);
        endTurn();
        assertCardInLocation(c, p1.inPlay);
        assertCardInLocation(merchantShip[0], p1.inPlay);
        assertCardInLocation(merchantShip[1], p1.discard);

        playTurn("");
        endTurn();

        playTurn(
                "", // n'écarte aucune carte
                "");
        assertPlayerState(p1, 5, 1, 2); // 1 pièce de Astrolabe, 2 pièces de Sailor et 2 pièces de Merchant Ship
        assertCardInLocation(c, p1.inPlay);
        assertCardInLocation(merchantShip[0], p1.inPlay);
        endTurn();
        assertFalse(p1.inPlay.contains(c));
        assertFalse(p1.inPlay.contains(merchantShip[0]));
    }

    @Test
    void test_salvager() {
        setup2pGame("Salvager");
        Card c = getCardFromSupply("Salvager");
        Card duchy = getCardFromSupply("Duchy");

        c.moveTo(p1.hand);
        duchy.moveTo(p1.hand);
        playTurn("HAND:Salvager", "HAND:Duchy", "");
        assertPlayerState(p1, 5, 0, 2);
        assertCardInLocation(duchy, trashedCards);
        endTurn();
        assertCardInLocation(c, p1.discard);
    }

    @Test
    void test_sea_chart_simple() {
        setup2pGame("Sea Chart");
        Card c = getCardFromSupply("Sea Chart");
        Card duchy = getCardFromSupply("Duchy");
        Card silver = getCardFromSupply("Silver");

        c.moveTo(p1.hand);
        silver.moveTo(p1.draw);
        duchy.moveTo(p1.draw);
        playTurn("HAND:Sea Chart", "");
        assertPlayerState(p1, 0, 1, 1);
        assertCardInLocation(duchy, p1.hand);
        assertCardInLocation(silver, p1.draw);
        assertSame(silver, p1.draw.getLast()); // Silver est sur le dessus de la pioche
        endTurn();
        assertCardInLocation(c, p1.discard);
    }

    @Test
    void test_sea_chart_with_copy_of_revealed_card_in_play() {
        setup2pGame("Bazaar", "Sea Chart");
        Card c = getCardFromSupply("Sea Chart");
        Card[] duchies = getCardsFromSupply("Duchy", 2);
        Card[] bazaars = getCardsFromSupply("Bazaar", 2);

        c.moveTo(p1.hand);
        bazaars[0].moveTo(p1.hand); // joué en début de tour
        bazaars[1].moveTo(p1.draw); // révélé par Sea Chart -> pris en main
        duchies[1].moveTo(p1.draw); // pioché par Sea Chart
        duchies[0].moveTo(p1.draw); // pioché par Bazaar
        playTurn("HAND:Bazaar", "HAND:Sea Chart", "");
        assertPlayerState(p1, 1, 2, 1);
        assertCardInLocation(duchies[0], p1.hand);
        assertCardInLocation(duchies[1], p1.hand);
        assertCardInLocation(bazaars[1], p1.hand);
        endTurn();
        assertCardInLocation(c, p1.discard);
    }

    @Test
    void test_sea_witch() {
        setup2pGame("Sea Witch");
        Card c = getCardFromSupply("Sea Witch");
        Card[] silvers = getCardsFromSupply("Silver", 4);
        Card[] duchies = getCardsFromSupply("Duchy", 2);
        Card curse = getCardFromSupply("Curse");

        c.moveTo(p1.hand);
        silvers[1].moveTo(p1.draw);
        silvers[0].moveTo(p1.draw);
        playTurn("HAND:Sea Witch", "");
        assertPlayerState(p1, 0, 0, 1);
        assertCardInLocation(silvers[0], p1.hand);
        assertCardInLocation(silvers[1], p1.hand);
        assertCardInLocation(curse, p2.discard);
        assertEquals(9, getSupply("Curse").size());
        endTurn();
        assertCardInLocation(c, p1.inPlay);

        playTurn("");
        endTurn();

        silvers[3].moveTo(p1.draw);
        silvers[2].moveTo(p1.draw);
        duchies[1].moveTo(p1.hand);
        duchies[0].moveTo(p1.hand);
        playTurn("HAND:Duchy", "HAND:Duchy", "");
        assertPlayerState(p1, 0, 1, 1);
        assertCardInLocation(c, p1.inPlay);
        assertCardInLocation(silvers[2], p1.hand);
        assertCardInLocation(silvers[3], p1.hand);
        assertCardInLocation(duchies[0], p1.discard);
        assertCardInLocation(duchies[1], p1.discard);
        endTurn();
        assertFalse(p1.inPlay.contains(c));
    }

    @Test
    void test_smugglers_duchy() {
        setup2pGame("Smugglers");
        Card c = getCardFromSupply("Smugglers");

        Card[] golds = getCardsFromSupply("Gold", 2);
        Card[] duchies = getCardsFromSupply("Duchy", 2);
        golds[0].moveTo(p1.hand);
        golds[1].moveTo(p1.hand);
        playTurn("HAND:Gold", "HAND:Gold", "SUPPLY:Duchy");
        assertPlayerState(p1, 1, 1, 0);
        assertCardInLocation(duchies[0], p1.discard);
        endTurn();

        c.moveTo(p2.hand);
        playTurn(
                "HAND:Smugglers",
                "SUPPLY:Copper", // non valide
                "SUPPLY:Estate", // non valide
                "SUPPLY:Duchy",
                "" // fin du tour
        );
        assertPlayerState(p2, 0, 0, 1);
        assertCardInLocation(duchies[1], p2.discard);
        endTurn();
        assertCardInLocation(c, p2.discard);
    }

    @Test
    void test_smugglers_province() {
        setup2pGame("Smugglers");
        Card c = getCardFromSupply("Smugglers");

        Card[] golds = getCardsFromSupply("Gold", 3);
        Card[] province = getCardsFromSupply("Province", 2);
        golds[0].moveTo(p1.hand);
        golds[1].moveTo(p1.hand);
        golds[2].moveTo(p1.hand);
        playTurn("HAND:Gold", "HAND:Gold", "HAND:Gold", "SUPPLY:Province");
        assertPlayerState(p1, 1, 1, 0);
        assertCardInLocation(province[0], p1.discard);
        endTurn();

        c.moveTo(p2.hand);
        playTurn(
                "HAND:Smugglers",
                "SUPPLY:Province", // non valide (carte de coût 8)
                "SUPPLY:Duchy", // non valide
                "", // passe (Smugglers)
                "" // fin du tour
        );
        assertPlayerState(p2, 0, 0, 1);
        assertCardInLocation(province[1], getSupply("Province"));
        endTurn();
        assertCardInLocation(c, p2.discard);
    }

    @Test
    void test_smugglers_no_card_gained() {
        setup2pGame("Smugglers");
        Card c = getCardFromSupply("Smugglers");

        playTurn("");
        endTurn();

        c.moveTo(p2.hand);
        playTurn(
                "HAND:Smugglers",
                "SUPPLY:Copper", // non valide
                "", // passe (Smugglers)
                "" // fin du tour
        );
        assertPlayerState(p2, 0, 0, 1);
        endTurn();
        assertCardInLocation(c, p2.discard);
    }

    @Test
    void test_tactician() {
        setup2pGame("Tactician");
        Card c = getCardFromSupply("Tactician");

        c.moveTo(p1.hand);
        List<Card> cardsInHand = new ArrayList<>(p1.hand);
        cardsInHand.remove(c);
        playTurn("HAND:Tactician", "");
        assertPlayerState(p1, 0, 0, 1);
        assertEquals(0, p1.hand.size());
        for (Card card : cardsInHand) {
            assertCardInLocation(card, p1.discard);
        }
        endTurn();
        assertCardInLocation(c, p1.inPlay);

        playTurn("");
        endTurn();

        playTurn("");
        assertPlayerState(p1, 0, 2, 2);
        assertEquals(10, p1.hand.size());
        assertCardInLocation(c, p1.inPlay);
        endTurn();
        assertFalse(p1.inPlay.contains(c));
    }

    @Test
    void test_tactician_no_cards_in_hand() {
        setup2pGame("Tactician");
        Card c = getCardFromSupply("Tactician");

        discardHand(p1);
        c.moveTo(p1.hand);
        playTurn("HAND:Tactician", "");
        assertPlayerState(p1, 0, 0, 1);
        endTurn();
        assertCardInLocation(c, p1.discard); // la carte ne reste pas si elle n'a pas d'effet

        playTurn("");
        endTurn();

        playTurn("");
        assertPlayerState(p1, 0, 1, 1);
        assertEquals(5, p1.hand.size());
        endTurn();
    }

    @Test
    void test_tide_pools() {
        setup2pGame("Tide Pools");
        Card c = getCardFromSupply("Tide Pools");
        Card[] duchies = getCardsFromSupply("Duchy", 3);
        Card[] silvers = getCardsFromSupply("Silver", 2);

        c.moveTo(p1.hand);
        duchies[0].moveTo(p1.draw);
        duchies[1].moveTo(p1.draw);
        duchies[2].moveTo(p1.draw);
        playTurn("HAND:Tide Pools", "");
        assertPlayerState(p1, 0, 1, 1);
        assertCardInLocation(duchies[0], p1.hand);
        assertCardInLocation(duchies[1], p1.hand);
        assertCardInLocation(duchies[2], p1.hand);
        endTurn();
        assertCardInLocation(c, p1.inPlay);

        playTurn("");
        endTurn();

        silvers[0].moveTo(p1.hand);
        silvers[1].moveTo(p1.hand);
        playTurn("HAND:Silver", "HAND:Silver", "");
        assertPlayerState(p1, 0, 1, 1);
        assertCardInLocation(c, p1.inPlay);
        assertCardInLocation(silvers[0], p1.discard);
        assertCardInLocation(silvers[1], p1.discard);
        endTurn();
        assertFalse(p1.inPlay.contains(c));
    }

    @Test
    void test_treasure_map() {
        setup2pGame("Treasure Map");
        Card[] treasureMaps = getCardsFromSupply("Treasure Map", 2);
        Card[] golds = getCardsFromSupply("Gold", 4);

        treasureMaps[0].moveTo(p1.hand);
        treasureMaps[1].moveTo(p1.hand);
        playTurn("HAND:Treasure Map", "");
        assertPlayerState(p1, 0, 0, 1);
        assertCardInLocation(treasureMaps[0], trashedCards);
        assertCardInLocation(treasureMaps[1], trashedCards);
        for (int i = 0; i < 4; i++) {
            assertCardInLocation(golds[i], p1.draw);
        }
        endTurn();
        assertCardInLocation(treasureMaps[0], trashedCards);
        assertCardInLocation(treasureMaps[1], trashedCards);
    }

    @Test
    void test_treasure_map_trash_only_one() {
        setup2pGame("Treasure Map");
        Card c = getCardFromSupply("Treasure Map");
        Card[] golds = getCardsFromSupply("Gold", 4);

        c.moveTo(p1.hand);
        playTurn("HAND:Treasure Map", "");
        assertPlayerState(p1, 0, 0, 1);
        assertCardInLocation(c, trashedCards);
        SupplyPile goldSupply = getSupply("Gold");
        for (int i = 0; i < 4; i++) {
            assertCardInLocation(golds[i], goldSupply);
        }
        endTurn();
        assertCardInLocation(c, trashedCards);
    }

    @Test
    void test_treasury_put_onto_deck() {
        setup2pGame("Treasury");
        Card c = getCardFromSupply("Treasury");
        Card gold = getCardFromSupply("Gold");
    
        c.moveTo(p1.hand);
        gold.moveTo(p1.draw);
        playTurn("HAND:Treasury", "");
        assertPlayerState(p1, 1, 1, 1);
        assertEquals(6, p1.hand.size());
        assertCardInLocation(gold, p1.hand);
        endTurn("BUTTON:y");
        assertCardInLocation(c, p1.hand);
    }

    @Test
    void test_treasury_discard() {
        setup2pGame("Treasury");
        Card c = getCardFromSupply("Treasury");
        Card gold = getCardFromSupply("Gold");
    
        c.moveTo(p1.hand);
        gold.moveTo(p1.draw);
        playTurn("HAND:Treasury", "");
        assertPlayerState(p1, 1, 1, 1);
        assertEquals(6, p1.hand.size());
        assertCardInLocation(gold, p1.hand);
        endTurn("BUTTON:n");
        assertCardInLocation(c, p1.discard);
    }

    @Test
    void test_treasury_gained_victory_during_buy_phase() {
        setup2pGame("Treasury");
        Card c = getCardFromSupply("Treasury");
        Card gold = getCardFromSupply("Gold");
    
        c.moveTo(p1.hand);
        gold.moveTo(p1.draw);
        playTurn("HAND:Treasury", "HAND:Gold", "SUPPLY:Estate");
        assertPlayerState(p1, 2, 1, 0);
        assertEquals(5, p1.hand.size());
        assertCardInLocation(c, p1.inPlay);
        endTurn();  // ne demande pas de choix à l'utilisateur
        assertCardInLocation(c, p1.discard);
    }

    @Test
    void test_warehouse() {
        setup2pGame("Warehouse");
        Card c = getCardFromSupply("Warehouse");
        Card silver = getCardFromSupply("Silver");
        Card gold = getCardFromSupply("Gold");
        Card duchy = getCardFromSupply("Duchy");
        Card province = getCardFromSupply("Province");
        Card curse = getCardFromSupply("Curse");
    
        c.moveTo(p1.hand);
        silver.moveTo(p1.draw);
        gold.moveTo(p1.draw);
        duchy.moveTo(p1.draw);
        province.moveTo(p1.hand);
        curse.moveTo(p1.hand);
        playTurn(
            "HAND:Warehouse", // joue Warehouse
            "", // non valide (ne peut pas passer)
            "HAND:Duchy", 
            "HAND:Province", 
            "HAND:Silver", 
            "");
        assertPlayerState(p1, 0, 1, 1);
        assertCardInLocation(curse, p1.hand);
        assertCardInLocation(gold, p1.hand);
        assertCardInLocation(duchy, p1.discard);
        assertCardInLocation(province, p1.discard);
        assertCardInLocation(silver, p1.discard);
        endTurn();
        assertCardInLocation(c, p1.discard);
    }

    @Test
    void test_wharf() {
        setup2pGame("Wharf");
        Card c = getCardFromSupply("Wharf");
        Card[] silvers = getCardsFromSupply("Silver", 4);
    
        c.moveTo(p1.hand);
        silvers[0].moveTo(p1.draw);
        silvers[1].moveTo(p1.draw);
        playTurn("HAND:Wharf", "");
        assertPlayerState(p1, 0, 0, 2);
        assertEquals(7, p1.hand.size());
        assertCardInLocation(silvers[0], p1.hand);
        assertCardInLocation(silvers[1], p1.hand);
        endTurn();
        assertCardInLocation(c, p1.inPlay);
    
        playTurn("");
        endTurn();
    
        silvers[2].moveTo(p1.draw);
        silvers[3].moveTo(p1.draw);
        playTurn("");
        assertPlayerState(p1, 0, 1, 2);
        assertCardInLocation(c, p1.inPlay);
        assertEquals(7, p1.hand.size());
        assertCardInLocation(silvers[2], p1.hand);
        assertCardInLocation(silvers[3], p1.hand);
        endTurn();
        assertFalse(p1.inPlay.contains(c));
    }
}
