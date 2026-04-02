package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.Button;
import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.component.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

public class SeaSideFactory {

    public static Card Ambassador(){
        Card c = new Card("Ambassador", 3, CardType.ACTION, CardType.ATTACK);
        c.addComponent(OnPlayComponent.class, p -> {
            Card choice = p.chooseCardFromHand("Dévoile une carte de ta main", false);

            if(choice != null){
                p.log(p.getName() + " a dévoilé " + choice.getName());
                    for(int i = 0; i < 2; i++){
                        Card choosed = p.chooseCardFromHand("Choisi la même carte à remetttre dans la réserve", choice::hasSameNameAs, true);
                        if(choosed == null) break;
                        if(!p.getGame().replaceCardInSupply(choosed, choice)){
                            i--;
                        }
                    }
                p.getGame().processGain(p, c, Destination.DISCARD, choice.getName());
            }
        });
        return c;
    }

    public static Card Astrolabe(){
        Card c = new Card("Astrolabe", 3, CardType.TREASURE, CardType.DURATION);
        CardUtil.addSimpleComponent(c, 0, 0, 1, 1, 0, 0, 1, 1);
        return c;
    }

    public static Card Bazaar(){
        Card c = new Card("Bazaar", 5, CardType.ACTION);
        CardUtil.addSimpleAction(c, 1, 2, 0, 1);
        return c;
    }

    public static Card Blockade(){
        Card c = new Card("Blockade", 4, CardType.ACTION, CardType.DURATION, CardType.ATTACK);
        final AtomicReference<Card> blocked = new AtomicReference<>();

        c.addComponent(OnPlayComponent.class, p -> {
            Card choice = p.chooseCardFromSupply("Choississez une carte qui coûte au maximum 4 de pièces", card -> card.getCost()<=4, false );
            blocked.set(CardUtil.gainIfPresent(p, choice, Destination.ASIDE, true));
            if(blocked.get() != null){
                p.log(String.format("%s bloquée", choice.getName().toUpperCase()));
            }
        });

        c.addComponent(new DurationComponent(0, player -> {
            Optional.ofNullable(blocked.get()).ifPresent( card -> player.moveTo(card, Destination.HAND));
            blocked.set(null);
        } , c));

        c.addComponent(TriggerComponent.OnPlayerGain.class, (owner, victim, c1) -> {
            if(blocked.get() != null && c1.hasSameNameAs(blocked.get()) && victim!= owner){
                victim.gain(victim.getCardFromSupply("Curse"), Destination.DISCARD);
                owner.log(String.format("TRIGGER %s : %s gagne une malédiction ", c.getName().toUpperCase(), victim.getName()));
            }
        });

        return c;
    }

    public static Card Caravan(){
        Card c = new Card("Caravan", 4, CardType.ACTION, CardType.DURATION);
        CardUtil.addSimpleComponent(c, 1, 1, 0, 0, 1, 0, 0, 0);
        return c;
    }

    public static Card Corsair(){
        Card c = new Card("Corsair", 5, CardType.ACTION, CardType.DURATION, CardType.ATTACK);

        final Map<Player, Integer> lastAttackTour = new HashMap<>();

        CardUtil.addSimpleComponent(c, 0, 0, 0, 2, 1, 0, 0, 0);

        c.addComponent(TriggerComponent.OnCardPlayed.class, (owner, actor, card) -> {
            if (owner == actor) return;

            if (card.hasName("Silver") || card.hasName("Gold")) {
                int currentTurn = owner.getGame().getTurnNumber();

                if (lastAttackTour.getOrDefault(actor, -1) != currentTurn) {
                    if(owner.getGame().isImmune(c, actor))return;
                    actor.getGame().moveToTrash(card);
                    lastAttackTour.put(actor, currentTurn);
                    owner.log(String.format("TRIGGER %s : %s écarte %s ", c.getName().toUpperCase(), actor.getName(), card.getName().toUpperCase()));
                }
            }
        });
        return c;
    }

    public static Card Cutpurse(){
        Card c = new Card("Cutpurse", 4, CardType.ACTION, CardType.ATTACK);
        c.addComponent(OnPlayComponent.class, p -> {
            CardUtil.TriggerEffect(p, 2,0,0,0,"Effect", c);
            p.getGame().discardOrShowHand(p, "Copper", c);

        });
        return c;
    }

    /**
     * Carte Embargo
     * <p>
     * +2 Pièces
     * Écartez ceci pour placer un jeton Embargo sur une pile de la réserve.
     * (Pendant le reste de la partie, quand un joueur achète une carte de cette
     * pile, il reçoit une Malédiction (Curse).)
     */
    public static Card Embargo(){
        Card c = new Card("Embargo", 2, CardType.ACTION);
        c.addComponent(OnPlayComponent.class, p -> {

            CardUtil.TriggerEffect(p, 2,0,0,0,"Effect", c);

            String choice = p.chooseStringFromButtons("Veux tu écarter cette carte pour poser une malédiction sur une des pile de la réserve", List.of(
                    new Button("Oui", "y"), new Button("Non", "n")
            ), false);

            if(choice.equals("y")) {
                p.getGame().moveToTrash(c);
                Card chosen = p.chooseCardFromSupply("Choisissez une pile de la réserve à maudir", Objects::nonNull, false);
                p.getGame().setToken(chosen.getName());
                p.log(String.format("Curse %s : %s a été maudit", c.getName().toUpperCase(), chosen.getName().toUpperCase()));
            }

        });
        return c;
    }

    /**
     * Carte Explorateur (Explorer)
     * <p>
     * Vous pouvez dévoiler une Province de votre main. Si vous le faites, recevez
     * un Or (Gold) en main. Sinon, recevez un Argent (Silver) en main.
     */
    public static Card Explorer(){
        Card c = new Card("Explorer", 5, CardType.ACTION);

        c.addComponent(OnPlayComponent.class, p -> {
            Card province = p.getCardsInHand().stream().filter(card-> card.hasName("Province")).findFirst().orElse(null);

            boolean revealed = false;
            if (province != null) {
                String choice = p.chooseStringFromButtons("Voulez-vous dévoiler une Province ? ", List.of(new Button(
                        "Oui", "y"
                ), new Button("Non", "n")), true);

                if(choice.equals("y")){
                    revealed = true;
                }
            }
            CardUtil.gainFromSupply(p, revealed? "Gold":"Silver", Destination.HAND, false);
        });
        return c;
    }

    /**
     * Carte Village de pêcheurs (Fishing Village)
     * <p>
     * +2 Actions
     * +1 Pièce
     * Au début de votre prochain tour, +1 Action et +1 Pièce.
     */
    public static Card FishingVillage(){
        Card c = new Card("Fishing Village", 3, CardType.ACTION, CardType.DURATION);
        CardUtil.addSimpleComponent(c, 0, 2, 0, 1, 0, 1, 0, 1);
        return c;
    }

    /**
     * Carte Vaisseau fantôme (Ghost Ship)
     * <p>
     * +2 Cartes
     * Tous vos adversaires ayant au moins 4 cartes en main placent des cartes
     * de leur main sur leur pioche jusqu'à avoir 3 cartes en main.
     */
    public static Card GhostShip(){
        Card c = new Card("Ghost Ship", 5, CardType.ACTION, CardType.ATTACK);
        c.addComponent(OnPlayComponent.class, p -> {
            CardUtil.TriggerEffect(p, 0,0,2,0,"Effect", c);
            p.getGame().processMoveTo(p, c,  Destination.DRAW);
        });
        return c;
    }

    /**
     * Carte Havre (Haven)
     * <p>
     * +1 Carte
     * +1 Action
     * Mettez de côté une carte de votre main face cachée (sous cette carte).
     * Au début de votre prochain tour, prenez-la en main.
     */
    public static Card Haven(){
        Card c = new Card("Haven", 2, CardType.ACTION, CardType.DURATION);
        AtomicReference<Card> reference = new AtomicReference<>();

        c.addComponent(OnPlayComponent.class, p -> {
            CardUtil.TriggerEffect(p,0,1,1,0,"Duration", c);
            Card hide = p.chooseCardFromHand("Choissisez une carte de votre main", false );
            if(hide != null) {
                reference.set(hide);
                p.moveTo(hide, Destination.ASIDE);
                p.log(String.format("Action %s : %s cache %s", c.getName().toUpperCase(), p.getName(), hide.getName().toUpperCase()));
            }
        });

        c.addComponent(new DurationComponent(0, player -> {
            if(reference.get() != null) {
                player.moveTo(reference.get(), Destination.HAND);
                player.log(String.format("Duration %s : %s récupère %s qui été cachée", c.getName().toUpperCase(), player.getName(), reference.get().getName().toUpperCase()));
                reference.set(null);
            }
        }, c));

        return c;
    }

    /**
     * Carte Île (Island)
     * <p>
     * 2 VP
     * Placez cette carte et une carte de votre main sur votre plateau Île (Island
     * Mat).
     */
    public static Card Island(){
        Card c = new Card("Island", 4, CardType.ACTION, CardType.VICTORY);
        c.addComponent(OnPlayComponent.class, p -> {
            p.moveTo(c, Destination.ISLAND);
            Card choosen = p.chooseCardFromHand("Choississez une carte de votre main à placer sur l'île", false);
            p.moveTo(choosen, Destination.ISLAND);
            p.log(String.format("Action %s : %s place %s ", c.getName().toUpperCase(), p.getName(), choosen.getName().toUpperCase())  );
        });
        c.addComponent(new ScoreComponent(2));
        return c;
    }

    /**
     * Carte Phare (Lighthouse)
     * <p>
     * +1 Action
     * Maintenant et au début de votre prochain tour, +1 Pièce.
     * D'ici là, les cartes Attaque jouées par vos adversaires ne vous affectent
     * pas.
     */
    public static Card LightHouse(){
        Card c = new Card("Lighthouse", 2, CardType.ACTION, CardType.DURATION);
        CardUtil.addSimpleComponent(c, 0, 1, 0, 1, 0, 0, 0, 1);
        c.addComponent(TriggerComponent.Immunity.class,new TriggerComponent.Immunity() {} );

        return c;
    }

    /**
     * Carte Vigie (Lookout)
     * <p>
     * +1 Action
     * Consultez les 3 premières cartes des votre pioche. Écartez-en une.
     * Défaussez-en une. Placez la carte restante sur le haut de votre pioche.
     */
    public static Card Lookout(){
        Card c = new Card("Lookout", 3, CardType.ACTION);
        c.addComponent(OnPlayComponent.class, p -> {
            CardUtil.TriggerEffect(p,0,1,0,0,"Effect", c);


            List<Card> view = CardUtil.getTopCards(p, 3);
            Card trash = p.chooseCardFromButtons("Choississez une carte à écarter", view, false);
            p.getGame().moveToTrash(trash);
            view.remove(trash);
            Card discard = p.chooseCardFromButtons("Choississez une carte à défaussez", view, false);
            view.remove(discard);
            p.moveTo(discard, Destination.DISCARD);
            p.log(String.format("Action %s : %s défausse %s", c.getName().toUpperCase(), p.getName(), discard.getName().toUpperCase()));

            if(!view.isEmpty()){
                p.moveTo(view.getFirst(), Destination.DRAW);
            }
        });
        return c;
    }
    /**
     * Carte Navire marchand (Merchant Ship)
     * <p>
     * Maintenant et au début de votre prochain tour, +2 Pièces.
     */
    public static Card MerchantShip(){
        Card c = new Card("Merchant Ship", 5, CardType.ACTION, CardType.DURATION);
        CardUtil.addSimpleComponent(c, 0, 0, 0, 2, 0, 0, 0, 2);
        return c;
    }
    /**
     * Carte Singe (Monkey)
     * <p>
     * Jusqu'à votre prochain tour, quand le joueur à votre droite reçoit une
     * carte, +1 Carte.
     * Au début de votre prochain tour, +1 Carte.
     */
    public static Card Monkey(){
        Card c = new Card("Monkey", 3, CardType.ACTION, CardType.DURATION);
        CardUtil.addSimpleDuration(c, 1, 0, 0, 0);

        c.addComponent(TriggerComponent.OnPlayerGain.class, (owner, victim, c1) -> {
            if(owner.getGame().onTheRight(owner, victim)){
                owner.draw(1);
            }
        });
        return c;
    }
    /**
     * Carte Village indigène (Native Village)
     * <p>
     * +2 Actions
     * Choisissez : placez la carte du haut de votre pioche, face cachée, sur votre
     * plateau Village indigène (vous pouvez consulter ces cartes à tout moment);
     * ou prenez en main toutes les cartes du plateau.
     */
    public static Card NativeVillage(){
        Card c = new Card("Native Village", 2, CardType.ACTION);
        c.addComponent(OnPlayComponent.class, p -> {
            CardUtil.TriggerEffect(p, 0,2,0,0,"Effect", c);
            List<Button> buttons = new ArrayList<>();
            buttons.add(new Button("add", "add"));
            buttons.add(new Button("take", "take"));

            String choice = p.chooseStringFromButtons("Choississez entre poser une carte sur votre village ou de récupérer toutes vos cartes", buttons, false);

            if (choice.equals("add")) {
                p.drawTo(Destination.NATIVE);
            }

            if(choice.equals("take")) {
                for (Card card : p.getCardsOnNativeVillageMat()){
                    p.moveTo(card, Destination.HAND);
                }
            }
        });
        return c;
    }
    /**
     * Carte Navigateur (Navigator)
     * <p>
     * +2 Pièces
     * Consultez les 5 premières cartes de votre pioche.
     * Défaussez-les toutes ou replacez-les sur votre pioche dans l'ordre de
     * votre choix.
     */
    public static Card Navigator(){
        Card c = new Card("Navigator", 4, CardType.ACTION);
        c.addComponent(OnPlayComponent.class, p -> {
            CardUtil.TriggerEffect(p, 2,0,0,0,"Effect", c);
            List<Card> view = CardUtil.getTopCards(p, 5);

            String choice = p.chooseStringFromButtons("Défausse tout ou replace les cartes dans l'ordre que tu veux", List.of(new Button("discard", "y"), new Button("replace", "n")), false);
            if(choice.equals("y")) {
                for(Card card : view) {
                    p.moveTo(card, Destination.DISCARD);
                }
            }else if(choice.equals("n")) {
                while(!view.isEmpty()) {
                    Card card = p.chooseCardFromButtons("Remet les cartes dans l'ordre que tu veux", view, false );
                    if(card != null) {
                        view.remove(card);
                        p.moveTo(card, Destination.DRAW);
                    }
                }
            }
        });
        return c;
    }

    /**
     * Carte Avant-poste (Outpost)
     * Au prochain tour, pioche seulement 3 cartes puis joue un tour supplémentaire
     */
    public static Card Outpost(){
        Card c = new Card("Outpost", 5, CardType.ACTION, CardType.DURATION);
        AtomicBoolean b = new AtomicBoolean(false);
        c.addComponent(OnPlayComponent.class, p -> {
            p.updateDrawBonusValue(-2);
            b.set(false);
        });
        c.addComponent(new ExtraTurnComponent(b));
        c.addComponent(new DurationComponent(0, p -> {}, c));

        return c;
    }

    /**
     * Carte Plongeur de perles (Pearl Diver)
     * <p>
     * +1 Carte
     * +1 Action
     * Consultez la carte du bas de votre pioche. Vous pouvez la placer sur le haut.
     */
    public static Card PearlDiver(){
        Card c = new Card("Pearl Diver", 2, CardType.ACTION);
        c.addComponent(OnPlayComponent.class, p -> {
            CardUtil.TriggerEffect(p, 0, 1, 1, 0, "Effect", c);

            Card card = CardUtil.getBottomCards(p, 1).getFirst();
            if(card != null) {
                List<Button> buttons = new ArrayList<>();
                buttons.add(new Button("onTop", "y"));
                buttons.add(new Button("onBottom", "n"));
                String choice = p.chooseStringFromButtons("Choix: Placez votre carte au dessus de votre pioche : " + c.getName() , buttons, true);

                if(choice.equals("y")){
                    p.moveTo(card, Destination.DRAW);
                }
            }
        });

        return c;
    }

    /**
     * Carte Pirate
     * <p>
     * Au début de votre prochain tour, recevez en main un Trésor coûtant jusqu'à
     * 6 Pièces.
     * Quand un joueur reçoit un Trésor, vous pouvez jouer cette carte depuis votre
     * main.
     */
    public static Card Pirate(){
        Card pirate = new Card("Pirate", 5, CardType.ACTION, CardType.DURATION, CardType.REACTION);

        pirate.addComponent(new DurationComponent(0, p -> {
            Card c = CardUtil.gainFromSupply(
                    p, "Choississez un trésor (maximum 6 pièces) ",
                    card -> card.hasType(CardType.TREASURE)&& card.getCost() <= 6,
                    Destination.HAND,
                    false );

            if( c!= null ) p.log(String.format("Action %s : %s récupère %s coutant %d pièces", pirate.getName().toUpperCase(), p.getName(), c.getName().toUpperCase(), c.getCost()));
        }, pirate));

        pirate.addComponent(TriggerComponent.OnPlayerGain.class, (owner, victim, c) -> {
            if(c.hasType(CardType.TREASURE) && owner.getCardsInHand().contains(pirate)) {
                Card choice = owner.chooseCardFromHand("Veux tu jouer ton pirate ?", card -> card.hasSameNameAs(pirate), true);
                if(choice != null ){
                    owner.playCard(pirate);
                    owner.log(String.format("Reaction %s", pirate.getName().toUpperCase()));
                }

            }
        });

        return pirate;
    }
    /**
     * Carte Bateau pirate (Pirate Ship)
     * <p>
     * Choisissez : +1 Pièce par jeton Pièce sur votre plateau Bateau pirate ;
     * ou tous vos adversaires dévoilent les 2 premières cartes de leur pioche,
     * écartent un Trésor (Treasure) dévoilé de votre choix et défaussent le reste,
     * et si au moins un Trésor a été écarté, placez un jeton Pièce sur votre
     * plateau Bateau pirate.
     */
    public static Card PirateShip(){
        Card current = new Card("Pirate Ship", 4, CardType.ACTION, CardType.ATTACK);
        current.addComponent(OnPlayComponent.class, p -> {
            String choice = p.chooseStringFromButtons("Choisissez : Récupèrer de l'argent du Bateau ou attaque ?", List.of(new Button("Money", "j"), new Button("Attack", "a")), false);
            if(choice.equals("j")) {
                int numberOfCoin = p.getCoins();
                p.incrementMoney(numberOfCoin);
                p.log(String.format("Action %s : %s récupère %d pièce sur son bateau", current.getName().toUpperCase(), p.getName(), numberOfCoin));
                return;
            }
            if(choice.equals("a")) {
                List<Card> treasureRemoved = p.getGame().processTreasureToTrash(p, current, 2);
                if(treasureRemoved.isEmpty()) return;
                p.incrementCoin(1);
            }
        });
        return current;
    }

    /**
     * Carte Navigatrice (Sailor)
     * <p>
     * +1 Action
     * Une fois durant ce tour, quand vous recevez une carte Durée (Duration),
     * vous pouvez la jouer.
     * Au début de votre prochain tour, +2 Pièces et vous pouvez écarter une carte
     * de votre main.
     */
    public static Card Sailor(){
        Card current = new Card("Sailor", 4, CardType.ACTION, CardType.DURATION);
        AtomicBoolean b = new AtomicBoolean(false);
        current.addComponent(OnPlayComponent.class, p -> {
            CardUtil.TriggerEffect(p, 0, 1, 0, 0, "Effect", current);
            b.set(false);
        });

        current.addComponent(new DurationComponent(0, player -> {
                    CardUtil.TriggerEffect(player, 2, 0, 0, 0, "Effect", current);
                    Card choice = player.chooseCardFromHand("Choisie une carte à écarter", true);
                    if(choice!= null){
                        player.getGame().moveToTrash(choice);
                    }}, current
        ));

        current.addComponent(TriggerComponent.OnPlayerGain.class, (owner, victim, c) -> {
            if(c.hasType(CardType.DURATION) && owner == victim && !b.get()){
                List<Button> buttons = new ArrayList<>();
                buttons.add(new Button("play", "y"));
                buttons.add(new Button("skip", "n"));

                String choice = owner.chooseStringFromButtons("Veux-tu jouer ta carte " + c.getName(), buttons, true);
                if(choice.equals("y")){
                    owner.playCard(c);
                    b.set(true);
                }

            }
        });

        return current;
    }

    /**
     * Carte Sauveteur (Salvager)
     * <p>
     * +1 Achat
     * Écartez une carte de votre main. +1 Pièce par Pièce de son coût.
     */
    public static Card Salvager(){
        Card current = new Card("Salvager", 4, CardType.ACTION);
        current.addComponent(OnPlayComponent.class, p -> {
            CardUtil.TriggerEffect(p, 0,0,0,1, "Effect", current);
            Card c = p.chooseCardFromHand("Choisis une carte à écarter", false);
            if(c != null){
                p.getGame().moveToTrash(c);
                p.incrementMoney(c.getCost());
            }
        });
        return current;
    }

    /**
     * Carte marine (Sea Chart)
     * <p>
     * +1 Carte
     * +1 Action
     * Dévoilez la carte du haut de votre pioche. Si vous en avez un exemplaire
     * en jeu, prenez-la en main.
     */
    public static Card SeaChart(){
        Card current = new Card("Sea Chart", 3, CardType.ACTION);
        current.addComponent(OnPlayComponent.class, p -> {
            CardUtil.TriggerEffect(p, 0,1,1,0, "Effect", current);
            Card c = p.getCardFromDeck();
            if(c != null){
                boolean hasCopy = p.getCardsInPlay().stream().anyMatch(card -> card.hasSameNameAs(c));
                if(hasCopy) p.moveToHand(c);
            }
        });
        return current;
    }

    /**
     * Carte Sorcière de mer (Sea Hag)
     * <p>
     * Tous vos adversaires défaussent la carte du haut de leur pioche, puis
     * reçoivent une Malédiction (Curse) sur leur pioche.
     */
    public static Card SeaHag(){
        Card current = new Card("Sea Hag", 4, CardType.ACTION, CardType.ATTACK);
        current.addComponent(OnPlayComponent.class, p -> {
            p.getGame().processDiscard(p, current);
            p.getGame().processGain(p, current, Destination.DRAW, "Curse");
        });
        return current;
    }

    /**
     * Carte Sorcière marine (Sea Witch)
     * <p>
     * +2 Cartes
     * Tous vos adversaires reçoivent une Malédiction (Curse).
     * Au début de votre prochain tour, +2 Cartes, puis défaussez 2 cartes.
     */
    public static Card SeaWitch(){
        Card current = new Card("Sea Witch", 5, CardType.ACTION, CardType.DURATION, CardType.ATTACK);
        current.addComponent(OnPlayComponent.class, p -> {
            CardUtil.TriggerEffect(p, 0,0,2,0, "Effect", current);
            p.getGame().processGain(p, current, Destination.DISCARD, "Curse");
        });

        current.addComponent(new DurationComponent(0, p -> {
            CardUtil.TriggerEffect(p, 0,0,2,0, "Duration", current);
            p.discardFromHand(2);
        }, current));

        return current;
    }

    /**
     * Contrebandiers (Smugglers)
     * <p>
     * Recevez un exemplaire d'une carte coûtant jusqu'à 6 Pièces que le joueur
     * à votre droite a reçues à son dernier tour.
     */
    public static Card Smugglers(){
        Card smugglers = new Card("Smugglers", 3, CardType.ACTION);

        smugglers.addComponent(OnPlayComponent.class, p -> {
            Player right = p.getGame().onTheRight(p);
            if(right == null) return;
            List<String> choices = new ArrayList<>();
            List<Button> buttons = new ArrayList<>();

            for(Card c : right.getCardGainedLastTurn()){
                if(c.getCost()>6) continue;
                choices.add("SUPPLY:" + c.getName());
                buttons.add(new Button(c.getName(), c.getName()));
            }
            String choice = p.choose("Choississez une carte de la liste du joueur de votre droite (prix max 6)" + right.getCardGainedLastTurn(), choices, buttons, false);
            if(choice.isEmpty()) return;
            Card c = p.getCardFromSupply(choice.split(":")[1]);
            if(c!= null){
                p.gain(c, Destination.DISCARD);
                p.log(String.format("Action %s : %s est copié", smugglers.getName().toUpperCase(), c.getName().toUpperCase()));
            }
        });

        return smugglers;
    }

    public static Card Tactician(){
        Card tactician = new Card("Tactician", 5, CardType.ACTION, CardType.DURATION);
        final AtomicBoolean trigger = new AtomicBoolean(false);
        tactician.addComponent(OnPlayComponent.class, p -> {
            if(p.getCardsInHand().isEmpty()){
                trigger.set(true);
                return;
            }
            p.getCardsInHand().forEach(card -> p.moveTo(card, Destination.DISCARD));
            p.log(String.format("Action %s : %s défausse toutes ses cartes", tactician.getName().toUpperCase(), p.getName()));
        });

        tactician.addComponent(new DurationComponent(0, p ->{
            CardUtil.TriggerEffect(p, 0,1,5,1,"Duration", tactician);
            trigger.set(false);
        }, tactician).setTrigger(trigger));
        return tactician;
    }

    /**
     * Carte Marée (Tide Pools)
     * <p>
     * +3 Cartes
     * +1 Action
     * Au début de votre prochain tour, défaussez 2 cartes.
     */
    public static Card TidePools(){
        Card tidePools = new Card("Tide Pools", 4, CardType.ACTION, CardType.DURATION);
        CardUtil.addSimpleAction(tidePools, 3, 1, 0, 0);
        tidePools.addComponent(new DurationComponent(
                0,
                player -> player.discardFromHand(2),
                tidePools
        ));
        return tidePools;
    }

    /**
     * Carte aux trésors (Treasure Map)
     * <p>
     * Écartez ceci et une Carte aux trésors de votre main. Si vous avez écarté
     * deux Cartes aux trésors, recevez 4 Ors (Gold) sur votre pioche.
     */
    public static Card TreasureMap(){
        Card tr = new Card("Treasure Map", 4, CardType.ACTION);
        tr.addComponent(OnPlayComponent.class, p -> {
            p.getGame().moveToTrash(tr);

            Card other = p.getCardsInHand().stream().filter(tr::hasSameNameAs).findFirst().orElse(null);
            if (other != null) {
                p.getGame().moveToTrash(other);

                IntStream.range(0,4)
                        .mapToObj(c -> p.getCardFromSupply("Gold"))
                        .filter(Objects::nonNull)
                        .forEach(gold -> p.gain(gold, Destination.DRAW));
            }
        });
        return tr;
    }

    /**
     * Carte Trésorerie (Treasury)
     * <p>
     * +1 Carte
     * +1 Action
     * +1 Pièce
     * À la fin de votre phase Achat, si vous n'avez pas reçu de carte Victoire
     * durant celle-ci, vous pouvez placer cette carte sur votre pioche.
     */
    public static Card Treasury(){
        Card tr = new Card("Treasury", 5, CardType.ACTION);
        CardUtil.addSimpleAction(tr, 1, 1, 0, 1);
        tr.addComponent(TriggerComponent.onEndBuy.class, (owner, victim, c) -> {
            if (!owner.getCardsInPlay().contains(tr)) return;

            boolean victory = owner.getCardGainedCurrentTurn()
                    .stream()
                    .anyMatch(card -> card.hasType(CardType.VICTORY));

            if(!victory){
                List<Button> buttons = new ArrayList<>();
                buttons.add(new Button("deck" , "y"));
                buttons.add(new Button("discard" , "n"));
                String choice = owner.chooseStringFromButtons("Voulez vous remettre la trésorerie sur la pioche ? ", buttons, true);

                if(choice.equals("y")){
                    owner.moveTo(tr, Destination.DRAW);
                }
            }
        });
        return tr;
    }

    public static Card Warehouse(){
        Card wr = new Card("Warehouse", 3, CardType.ACTION);
        wr.addComponent(OnPlayComponent.class, p -> {
            CardUtil.TriggerEffect(p,0,1,3,0,"Effect", wr);
            p.discardFromHand(3);
        });
        return wr;
    }

    /**
     * Carte Quai (Wharf)
     * <p>
     * Maintenant et au début de votre prochain tour : +2 Cartes et +1 Achat.
     */
    public static Card Wharf(){
        Card wr = new Card("Wharf", 5, CardType.ACTION, CardType.DURATION);
        CardUtil.addSimpleComponent(wr,2,0,1,0,2,0,1,0 );
        return wr;
    }


}
