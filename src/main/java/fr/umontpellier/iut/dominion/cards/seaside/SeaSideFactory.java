package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.*;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.RegistryPrice;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class SeaSideFactory {

    public static Predicate<Card> activate = card -> card.as(DurationComponent.class).map(d -> !d.isFinished()).orElse(false);

    public static Card Ambassador() {
        return new Card("Ambassador", RegistryPrice.SeasidePrice(3), CardType.ACTION, CardType.ATTACK).setup(
                config -> config.onPlay(CardUtil::executeAmbassador));
    }
    public static Card Astrolabe(){
        return new Card("Astrolabe", RegistryPrice.SeasidePrice(3), CardType.TREASURE, CardType.DURATION).
                setup(config -> config.registerSimpleComponent(0,0,1,1,0,0,1,1));
    }

    public static Card Bazaar(){
        return new Card("Bazaar", RegistryPrice.SeasidePrice(5), CardType.ACTION)
                .setup(config -> config.registerSimpleAction(1,2,0,1));
    }

    public static Card Blockade(){
        return new Card("Blockade", RegistryPrice.SeasidePrice(4), CardType.ACTION, CardType.DURATION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((p, self) ->{
                                self.set("Players", p.getGame().scanImmunity(p));

                                CardUtil.executeIfSelected(
                                () -> p.chooseCardFromSupply("Choississez une carte qui coûte au maximum 4 de pièces", buy -> buy.getCost()<=4, false ),
                                blocked -> {
                                    self.set("Blocked", CardUtil.gainIfPresent(p, blocked, Destination.ASIDE, true));
                                    p.log(String.format("%s bloquée", blocked.getName().toUpperCase()));
                                });
                        })
                        .onDurationWithTrigger(
                                (player, self )-> CardUtil.moveTo(player, () -> self.get("Blocked", Card.class), c -> self.set("Blocked", c), Destination.HAND),
                                card -> card.get("Blocked", Card.class) == null
                        )
                        .onGain((owner, victim, c) -> {

                                if(c.getCard().hasName("Curse")){
                                    Card curse;
                                    while((curse = victim.getCardFromSupply("Curse"))!= null){
                                        victim.gainSilent(curse, Destination.DISCARD, true);
                                    }
                                }

                                victim.gain(victim.getCardFromSupply("Curse"), Destination.DISCARD);
                                owner.log(String.format("TRIGGER %s : %s gagne une malédiction ", c.getCard().getName().toUpperCase(), victim.getName()));
                        })
                        .onCondition((event, player) ->
                                config.get().get("Blocked", Card.class) != null
                                        && event.getCard().hasSameNameAs(config.get().get("Blocked", Card.class))
                                        && event.getPlayer()!= player
                        ));
    }

    public static Card Caravan(){
        return new Card("Caravan", RegistryPrice.SeasidePrice(4), CardType.ACTION, CardType.DURATION)
                .setup(config -> config.registerSimpleComponent(1,1,0,0,1,0,0,0));
    }

    public static Card Corsair() {
        return new Card("Corsair", RegistryPrice.SeasidePrice(5), CardType.ACTION, CardType.DURATION, CardType.ATTACK).setup(
                config -> config
                        .registerSimpleDuration(1,0,0,0)
                        .onPlay((p, self) -> {
                            CardUtil.TriggerEffect(p, 2,0,0,0, "Effect", self);
                            self.set("Players", p.getGame().scanImmunity(p));
                        })
                        .onCardPlayed((owner, actor, playedCard) -> {

                            int currentTurn = owner.getGame().getTurnNumber();
                            Map<Player, Integer> history = config.get().getMap("attackHistory");
                                if (history == null) {
                                    history = new HashMap<>();
                                    config.get().set("attackHistory", history);
                                }

                                if (history.getOrDefault(actor, -1) != currentTurn) {
                                    if (owner.getGame().isImmune(config.get(), actor)) return;

                                    actor.moveToTrash(playedCard.getCard());
                                    history.put(actor, currentTurn);

                                    owner.log(String.format("TRIGGER %s : %s écarte %s ",
                                            config.get().getName().toUpperCase(), actor.getName(), playedCard.getCard().getName().toUpperCase()));
                                }
                        })
                        .onCondition(((event, player) -> player != event.getPlayer() && (event.getCard().hasName("Silver") || event.getCard().hasName("Gold"))))
                );
    }

    public static Card Cutpurse(){
        return new Card("Cutpurse", RegistryPrice.SeasidePrice(4), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, card) -> {
                            CardUtil.TriggerEffect(player, 2,0,0,0,"Effect", card);
                            player.getGame().checkHandOrShow(
                                    player,
                                    card,
                                    check -> check.hasName("Copper"),
                                    (p, cards) -> cards.stream().findFirst(),
                                    Destination.DISCARD
                            );
                        })
                );
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
        return new Card("Embargo", RegistryPrice.SeasidePrice(2), CardType.ACTION).setup(
                config -> config.onPlay(
                        (p, self) -> {
                            Runnable logic = () -> CardUtil.executeIfSelected(
                                    () -> p.chooseCardFromSupply("Choisissez une pile de la réserve à maudir", Objects::nonNull, false),
                                    card ->{
                                        p.moveToTrash(self);
                                        p.getGame().setToken(card.getName());
                                        p.log(String.format("Curse %s : %s a été maudit", self.getName().toUpperCase(), card.getName().toUpperCase()));
                                    }
                            );

                            CardUtil.TriggerEffect(p, 2,0,0,0,"Effect", self);

                            CardUtil.executeOrOtherwise(
                                    () -> p.chooseStringFromButtons("Veux tu écarter cette carte pour poser une malédiction sur une des pile de la réserve", List.of(
                                            new Button("Oui", "y"), new Button("Non", "n")
                                    ), false),
                                    "y"::equals,
                                    choice -> logic.run(),
                                    () -> {}
                            );
                        }));
    }

    /**
     * Carte Explorateur (Explorer)
     * <p>
     * Vous pouvez dévoiler une Province de votre main. Si vous le faites, recevez
     * un Or (Gold) en main. Sinon, recevez un Argent (Silver) en main.
     */
    public static Card Explorer(){
        return new Card("Explorer", RegistryPrice.SeasidePrice(5), CardType.ACTION)
                .setup(config ->config.
                        onPlay((p, self) -> {
                            Consumer<String> gainTreasure = treasure -> {
                                CardUtil.gainFromSupply(p, treasure, Destination.HAND, false);
                                p.log(p.getName() + " gagne un " + treasure.toUpperCase() + " en main.");
                            };

                            Optional<Card> province = p.getCopyOf(Destination.HAND).stream()
                                    .filter(card -> card.hasName("Province"))
                                    .findFirst();

                            province.ifPresentOrElse(
                                    prov -> CardUtil.executeOrOtherwise(
                                            () -> p.chooseStringFromButtons("Dévoiler Province ?", List.of(new Button("Oui", "y"), new Button("Non", "n")), false),
                                            "y"::equals,
                                            choice ->{
                                                p.log("Dévoile Province");
                                                gainTreasure.accept("Gold");
                                                },
                                            () -> gainTreasure.accept("Silver")

                                            ), () -> gainTreasure.accept("Silver"));
                                })
                );
    }

    /**
     * Carte Village de pêcheurs (Fishing Village)
     * <p>
     * +2 Actions
     * +1 Pièce
     * Au début de votre prochain tour, +1 Action et +1 Pièce.
     */
    public static Card FishingVillage(){
        return new Card("Fishing Village", RegistryPrice.SeasidePrice(3), CardType.ACTION, CardType.DURATION)
                .setup(config -> config.registerSimpleComponent(0,2,0,1,0,1,0,1));
    }

    /**
     * Carte Vaisseau fantôme (Ghost Ship)
     * <p>
     * +2 Cartes
     * Tous vos adversaires ayant au moins 4 cartes en main placent des cartes
     * de leur main sur leur pioche jusqu'à avoir 3 cartes en main.
     */
    public static Card GhostShip(){
        return new Card("Ghost Ship", RegistryPrice.SeasidePrice(5), CardType.ACTION, CardType.ATTACK).setup(
                config -> config.onPlay((p, c) -> {
                    CardUtil.TriggerEffect(p, 0,0,2,0,"Effect", c);
                    p.getGame().processMoveTo(p, c,  Destination.DRAW, 3, false);
                })
        );
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
        return new Card("Haven", RegistryPrice.SeasidePrice(2), CardType.ACTION, CardType.DURATION).setup(
                config -> config
                        .onPlay((p, self ) ->{
                            CardUtil.TriggerEffect(p,0,1,1,0,"Duration", self);
                            CardUtil.executeIfSelected(
                                    () -> p.chooseCardFromHand("Choissisez une carte de votre main", false ),
                                    card -> {
                                        self.set("Hidden",CardUtil.moveIfPresent(p, card, Destination.ASIDE));
                                        p.log(String.format("Action %s : %s cache %s", self.getName().toUpperCase(), p.getName(), card.getName().toUpperCase()));
                                    }
                            );
                        })
                        .onDurationWithTrigger(
                                (player, self) -> {
                                    player.log(String.format("Duration %s : %s récupère %s qui été cachée", self.getName().toUpperCase(), player.getName(), self.get("Hidden", Card.class).getName().toUpperCase()));
                                    CardUtil.moveTo(player, () -> self.get("Hidden", Card.class), c -> self.set("Hidden", c), Destination.HAND);
                                },
                                card -> card.get("Hidden", Card.class) == null
                        ));
    }

    /**
     * Carte Île (Island)
     * <p>
     * 2 VP
     * Placez cette carte et une carte de votre main sur votre plateau Île (Island
     * Mat).
     */
    public static Card Island(){
        return new Card("Island", RegistryPrice.SeasidePrice(4), CardType.ACTION, CardType.VICTORY).setup(
                config -> config
                        .onPlay((p, self) -> {
                            p.moveTo(self, Destination.ISLAND);
                            CardUtil.executeIfSelected(
                                () -> p.chooseCardFromHand("Choississez une carte de votre main à placer sur l'île", false),
                                card -> {
                                    p.moveTo(card, Destination.ISLAND);
                                    p.log(String.format("Action %s : %s place %s ", self.getName().toUpperCase(), p.getName(), card.getName().toUpperCase()));
                                }
                            );
                        })
                        .score(player -> 2));
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
        return new Card("Lighthouse", RegistryPrice.SeasidePrice(2), CardType.ACTION, CardType.DURATION)
                .setup(
                config -> config
                        .registerSimpleComponent(0,1,0,1,0,0,0,1)
                        .immunity(new TriggerComponent.Immunity() {
                            @Override
                            public boolean immune(Card self) {
                                return activate.test(self);
                            }
                        })
                );
    }

    /**
     * Carte Vigie (Lookout)
     * <p>
     * +1 Action
     * Consultez les 3 premières cartes des votre pioche. Écartez-en une.
     * Défaussez-en une. Placez la carte restante sur le haut de votre pioche.
     */
    public static Card Lookout(){
        return new Card("Lookout", RegistryPrice.SeasidePrice(3), CardType.ACTION).setup(
                config -> config
                        .onPlay( (p, self) ->{

                            CardUtil.TriggerEffect(p,0,1,0,0,"Effect", self);

                            List<Card> view = CardUtil.getTopCards(p, 3);
                            CardUtil.executeIfSelected(
                                    () -> p.chooseCardFromList("Choississez une carte à écarter", card -> true, view, false),
                                    card -> {
                                        p.moveToTrash(card);
                                        view.remove(card);
                                    });

                            CardUtil.executeIfSelected(
                                    () -> p.chooseCardFromList("Choississez une carte à défaussez", card -> true, view, false),
                                    card -> {
                                        p.moveTo(card, Destination.DISCARD);
                                        p.log(String.format("Action %s : %s défausse %s", self.getName().toUpperCase(), p.getName(), card.getName().toUpperCase()));
                                        view.remove(card);
                                    });

                                    if(!view.isEmpty()){
                                        p.moveTo(view.getFirst(), Destination.DRAW);
                                    }
                        })
        );
    }
    /**
     * Carte Navire marchand (Merchant Ship)
     * <p>
     * Maintenant et au début de votre prochain tour, +2 Pièces.
     */
    public static Card MerchantShip(){
        return new Card("Merchant Ship", RegistryPrice.SeasidePrice(5), CardType.ACTION, CardType.DURATION).setup(
                config -> config.registerSimpleComponent(0,0,0,2,0,0,0,2));
    }
    /**
     * Carte Singe (Monkey)
     * <p>
     * Jusqu'à votre prochain tour, quand le joueur à votre droite reçoit une
     * carte, +1 Carte.
     * Au début de votre prochain tour, +1 Carte.
     */
    public static Card Monkey(){
        return new Card("Monkey", RegistryPrice.SeasidePrice(3), CardType.ACTION, CardType.DURATION).setup(
                config -> config
                        .registerSimpleDuration(1,0,0,0)
                        .onGain((owner, victim, c) -> owner.draw(1))
                        .onCondition((event, player) -> player.getGame().onTheRight(player, event.getPlayer()) && activate.test(config.get()))
        );
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
        return new Card("Native Village", RegistryPrice.SeasidePrice(2), CardType.ACTION).setup(
                config -> config
                        .onPlay((p, self)-> {
                            CardUtil.TriggerEffect(p, 0,2,0,0,"Effect", self);
                            CardUtil.executeOrOtherwise(
                                    () -> p.chooseStringFromButtons("Choississez entre poser une carte sur votre village ou de récupérer toutes vos cartes", List.of(new Button("add", "add"), new Button("take", "take")), false),
                                    "add"::equals,
                                    isPresent -> p.drawTo(Destination.NATIVE),
                                    () -> p.getCopyOf(Destination.NATIVE).forEach( card -> p.moveTo(card, Destination.HAND))
                            );
                        })
        );
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
        return new Card("Navigator", RegistryPrice.SeasidePrice(4), CardType.ACTION).setup(
                config -> config
                        .onPlay((p, self) ->{
                            CardUtil.TriggerEffect(p,2,0,0,0, "Effect", self);
                            List<Card> view = CardUtil.getTopCards(p, 5);

                            Runnable chooseOrder = () -> {
                                while(!view.isEmpty()){
                                    CardUtil.executeIfSelected(
                                            () -> p.chooseCardFromList("Remet les cartes dans l'ordre que tu veux",card -> true , view, false),
                                            card -> {
                                                view.remove(card);
                                                p.moveTo(card, Destination.DRAW);
                                            })
                                    ;}
                            };

                            CardUtil.executeOrOtherwise(
                                    () ->p.chooseStringFromButtons("Défausse tout ou replace les cartes dans l'ordre que tu veux", List.of(new Button("discard", "y"), new Button("replace", "n")), false),
                                    "y"::equals,
                                    isPresent -> view.forEach(card -> p.moveTo(card, Destination.DISCARD)),
                                    chooseOrder
                            );
                        })
        );
    }

    /**
     * Carte Avant-poste (Outpost)
     * Au prochain tour, pioche seulement 3 cartes puis joue un tour supplémentaire
     */
    public static Card Outpost(){
        AtomicBoolean b = new AtomicBoolean(false);
        return new Card("Outpost", RegistryPrice.SeasidePrice(5), CardType.ACTION, CardType.DURATION).setup(
                config -> config
                        .onPlay((player, self) ->{
                                    player.updateDrawBonusValue(-2);
                                    b.set(false);
                        })
                        .onExtraTurn(b)
                        .onDuration((player, card) -> {})
        );
    }

    /**
     * Carte Plongeur de perles (Pearl Diver)
     * <p>
     * +1 Carte
     * +1 Action
     * Consultez la carte du bas de votre pioche. Vous pouvez la placer sur le haut.
     */
    public static Card PearlDiver(){
        return new Card("Pearl Diver", RegistryPrice.SeasidePrice(2), CardType.ACTION).setup(
                config -> config
                        .onPlay((p, self) -> {
                            CardUtil.TriggerEffect(p, 0, 1, 1, 0, "Effect", self);
                            Card card = CardUtil.getBottomCards(p, 1).getFirst();
                            if(card!=null){
                                List<Button> buttons = new ArrayList<>();
                                buttons.add(new Button("onTop", "y"));
                                buttons.add(new Button("onBottom", "n"));

                                CardUtil.executeOrOtherwise(
                                        () ->p.chooseStringFromButtons("Choix: Placez votre carte au dessus de votre pioche : " + self.getName() , buttons, true),
                                        "y"::equals,
                                        isPresent -> p.moveTo(card, Destination.DRAW),
                                        () -> {}
                                );}
                        })
        );
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
        return new Card("Pirate", RegistryPrice.SeasidePrice(5), CardType.ACTION, CardType.DURATION, CardType.REACTION)
                .setup(config -> config
                    .onDuration((p, self) -> CardUtil.executeIfSelected(
                            () -> CardUtil.gainFromSupply(
                                    p,
                                    "Choississez un trésor (maximum 6 pièces) ",
                                    card -> card.hasType(CardType.TREASURE)&& card.getCost() <= 6,
                                    Destination.HAND,
                                    false ),
                            card ->  p.log(String.format("Action %s : %s récupère %s coutant %d pièces", self.getName().toUpperCase(), p.getName(), card.getName().toUpperCase(), card.getCost()))
                            ))
                    .onGain((owner, victim, c) -> {
                            CardUtil.executeIfSelected(
                                    () ->  owner.chooseCardFromHand("Veux tu jouer ton pirate ?", card -> card.hasSameNameAs(config.get()), true),
                                    card -> {
                                        owner.playCard(config.get());
                                        owner.log(String.format("Reaction %s", config.get().getName().toUpperCase()));
                                    });
                    })
                    .onCondition(
                            (event, player) -> event.getCard().hasType(CardType.TREASURE) && player.getCopyOf(Destination.HAND).contains(config.get())
                    )
                );
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
        return new Card("Pirate Ship", RegistryPrice.SeasidePrice(4), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((p, current) -> {
                            Runnable money = () -> {
                                int numberOfCoin = p.getCoins();
                                p.increment(Item.MONEY, numberOfCoin);
                                p.log(String.format("Action %s : %s récupère %d pièce sur son bateau", current.getName().toUpperCase(), p.getName(), numberOfCoin));
                            };

                            Runnable attack = ()-> {
                                List<Card> treasureRemoved = p.getGame().processAttackWithReveal(
                                        p,
                                        current,
                                        2,
                                        card -> card.hasType(CardType.TREASURE),
                                        (attacker, victim, options) -> attacker.getGame().chooseACard(attacker, options)
                                );
                                if(treasureRemoved.isEmpty()) return;
                                p.increment(Item.COIN_TOKEN_SHIP,1);
                            };

                            CardUtil.executeOrOtherwise(
                                    () -> p.chooseStringFromButtons("Choisissez : Récupèrer de l'argent du Bateau ou attaquer ?", List.of(new Button("Money", "m"), new Button("Attack", "a")), false),
                                    "m"::equals,
                                    choice -> money.run(),
                                    attack
                            );}
                        )
                );
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
        return new Card("Sailor", RegistryPrice.SeasidePrice(4), CardType.ACTION, CardType.DURATION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0, 1, 0, 0, "Effect", self);
                            self.set("used", false);
                        })
                        .onDuration((p, self) ->{
                            CardUtil.TriggerEffect(p, 2, 0, 0, 0, "Effect", self);
                            CardUtil.executeIfSelected(
                                    () -> p.chooseCardFromHand("Choisie une carte à écarter", true),
                                    p::moveToTrash
                            );
                        })
                        .onGain((owner, victim, c) -> {
                                List<Button> buttons = new ArrayList<>();
                                buttons.add(new Button("play", "y"));
                                buttons.add(new Button("skip", "n"));
                                CardUtil.executeOrOtherwise(
                                        () ->owner.chooseStringFromButtons("Veux-tu jouer ta carte " + c.getCard().getName(), buttons, true),
                                        "y"::equals,
                                        choice -> {
                                            owner.playCard(c.getCard());
                                            config.get().set("used", true);
                                        },
                                        () -> {}
                                );
                        })
                        .onCondition((event, player) ->
                                event.getCard().hasType(CardType.DURATION)
                                        && player == event.getPlayer()
                                        && !config.get().getFlag("used")
                                        && !player.getCopyOf(Destination.INPLAY).contains(event.getCard())
                                        && activate.test(config.get())

                        )
                );
    }

    /**
     * Carte Sauveteur (Salvager)
     * <p>
     * +1 Achat
     * Écartez une carte de votre main. +1 Pièce par Pièce de son coût.
     */
    public static Card Salvager(){
        return new Card("Salvager", RegistryPrice.SeasidePrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay((p, self) -> {
                            CardUtil.TriggerEffect(p, 0,0,0,1, "Effect", self);
                            CardUtil.executeIfSelected(
                                    () -> p.chooseCardFromHand("Choisis une carte à écarter", false),
                                    card -> {
                                        p.moveToTrash(card);
                                        p.increment(Item.MONEY, card.getCost());
                                    });
                        })
                );
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
        return new Card("Sea Chart", RegistryPrice.SeasidePrice(3), CardType.ACTION)
                .setup( config -> config
                        .onPlay((p, self) -> {
                            CardUtil.TriggerEffect(p, 0,1,1,0, "Effect", self);
                            Card c = p.getCardFromDeck();
                            if(c != null){
                                boolean hasCopy = p.getCopyOf(Destination.INPLAY).stream().anyMatch(card -> card.hasSameNameAs(c));
                                if(hasCopy) p.moveTo(c, Destination.HAND);
                            }
                        })
                );
    }

    /**
     * Carte Sorcière de mer (Sea Hag)
     * <p>
     * Tous vos adversaires défaussent la carte du haut de leur pioche, puis
     * reçoivent une Malédiction (Curse) sur leur pioche.
     */
    public static Card SeaHag(){
        return new Card("Sea Hag", RegistryPrice.SeasidePrice(4), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((p, self) -> {
                            p.getGame().processDiscard(p, self);
                            p.getGame().processGain(p, self, Destination.DRAW, "Curse");
                        })
                );
    }

    /**
     * Carte Sorcière marine (Sea Witch)
     * <p>
     * +2 Cartes
     * Tous vos adversaires reçoivent une Malédiction (Curse).
     * Au début de votre prochain tour, +2 Cartes, puis défaussez 2 cartes.
     */
    public static Card SeaWitch(){
        return new Card("Sea Witch", RegistryPrice.SeasidePrice(5), CardType.ACTION, CardType.DURATION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((p, self) -> {
                            CardUtil.TriggerEffect(p, 0,0,2,0, "Effect", self);
                            p.getGame().processGain(p, self, Destination.DISCARD, "Curse");
                        })
                        .onDuration((p, self) -> {
                            CardUtil.TriggerEffect(p, 0,0,2,0, "Duration", self);
                            p.discardFromHand(2);
                        })
                );
    }

    /**
     * Contrebandiers (Smugglers)
     * <p>
     * Recevez un exemplaire d'une carte coûtant jusqu'à 6 Pièces que le joueur
     * à votre droite a reçues à son dernier tour.
     */
    public static Card Smugglers(){
        return new Card("Smugglers", RegistryPrice.SeasidePrice(3), CardType.ACTION)
                .setup(config -> config
                        .onPlay((p, self) ->{
                            Player right = p.getGame().onTheRight(p);
                            if(right == null) return;
                            if(right.getCardGainedLastTurn().isEmpty()) return;
                            Card c = p.chooseCardFromList("Choose a card; And copy it in your discard", card -> card.getCost() <= 6 && card.buyCondition(0,0), right.getCardGainedLastTurn(), true);
                            if(c!= null){
                                p.gain(c, Destination.DISCARD);
                                p.log(String.format("Action %s : %s est copié", self.getName().toUpperCase(), c.getName().toUpperCase()));
                            }
                        })
                );
    }

    public static Card Tactician() {
        return new Card("Tactician", RegistryPrice.SeasidePrice(5), CardType.ACTION, CardType.DURATION)
                .setup(config -> config
                        .onPlay((p, self) -> {

                            if (p.getCopyOf(Destination.HAND).isEmpty()) {
                                p.log(p.getName() + " joue un Tactician mais n'a rien à défausser.");
                                self.set("activated", false);
                                return;
                            }
                            p.getCopyOf(Destination.HAND).forEach(p::discard);

                            p.log(String.format("TACTICIAN : %s défausse sa main pour activer le bonus du tour prochain", p.getName()));

                            self.set("activated", true);
                        })
                        .onDurationWithTrigger((player, self) -> {
                            CardUtil.TriggerEffect(player, 0, 1, 5, 1, "Duration", self);
                            self.set("activated", false);
                        }, self -> !self.getFlag("activated"))
                );
    }

    /**
     * Carte Marée (Tide Pools)
     * <p>
     * +3 Cartes
     * +1 Action
     * Au début de votre prochain tour, défaussez 2 cartes.
     */
    public static Card TidePools(){
        return new Card("Tide Pools", RegistryPrice.SeasidePrice(4), CardType.ACTION, CardType.DURATION)
                .setup(config -> config
                        .registerSimpleAction(3, 1, 0, 0)
                        .onDuration((p, self) -> p.discardFromHand(2))
                );
    }

    /**
     * Carte aux trésors (Treasure Map)
     * <p>
     * Écartez ceci et une Carte aux trésors de votre main. Si vous avez écarté
     * deux Cartes aux trésors, recevez 4 Ors (Gold) sur votre pioche.
     */
    public static Card TreasureMap(){
        return new Card("Treasure Map", RegistryPrice.SeasidePrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay((p, self) -> {
                            p.moveToTrash(self);

                            Card other = p.getCopyOf(Destination.HAND).stream().filter(self::hasSameNameAs).findFirst().orElse(null);
                            if (other != null) {
                                p.moveToTrash(other);

                                IntStream.range(0,4)
                                        .mapToObj(c -> p.getCardFromSupply("Gold"))
                                        .filter(Objects::nonNull)
                                        .forEach(gold -> p.gain(gold, Destination.DRAW));
                            }
                        })
                );
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
        return new Card("Treasury", RegistryPrice.SeasidePrice(5), CardType.ACTION)
                .setup(config -> config
                        .registerSimpleAction(1,1,0,1)
                        .onEndBuy((owner, c) -> {
                            if (!owner.getCopyOf(Destination.INPLAY).contains(config.get())) return;

                            boolean victory = owner.getCardGainedCurrentTurn()
                                    .stream()
                                    .anyMatch(card -> card.hasType(CardType.VICTORY));

                            if(!victory){
                                List<Button> buttons = new ArrayList<>();
                                buttons.add(new Button("deck" , "y"));
                                buttons.add(new Button("discard" , "n"));

                                CardUtil.executeOrOtherwise(
                                        () -> owner.chooseStringFromButtons("Voulez vous remettre la trésorerie sur la pioche ? ", buttons, true),
                                        "y"::equals,
                                        choice -> owner.moveTo(config.get(), Destination.DRAW),
                                        () -> {}
                                );
                            }
                        })
                );
    }

    public static Card Warehouse(){
        return new Card("Warehouse", RegistryPrice.SeasidePrice(3), CardType.ACTION)
                .setup( config -> config
                        .onPlay(
                                (p, self) -> {
                                    CardUtil.TriggerEffect(p,0,1,3,0,"Effect", self);
                                    p.discardFromHand(3);
                                }
                        )
        );
    }

    /**
     * Carte Quai (Wharf)
     * <p>
     * Maintenant et au début de votre prochain tour : +2 Cartes et +1 Achat.
     */
    public static Card Wharf(){
        return new Card("Wharf", RegistryPrice.SeasidePrice(5), CardType.ACTION, CardType.DURATION)
                .setup(config -> config.registerSimpleComponent(2,0,1,0,2,0,1,0));
    }
}
