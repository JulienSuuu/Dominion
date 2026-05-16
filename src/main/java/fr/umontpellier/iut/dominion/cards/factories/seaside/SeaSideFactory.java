package fr.umontpellier.iut.dominion.cards.factories.seaside;

import fr.umontpellier.iut.dominion.*;
import fr.umontpellier.iut.dominion.Annotation.Dominion_Card;
import fr.umontpellier.iut.dominion.Annotation.InSet;
import fr.umontpellier.iut.dominion.Player.Player;
import fr.umontpellier.iut.dominion.cards.*;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;
import fr.umontpellier.iut.dominion.cards.component.OnPlayComponent;
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent;
import fr.umontpellier.iut.dominion.cards.factories.FactoryUtil;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static fr.umontpellier.iut.dominion.cards.CardConfigurator.empty;
import static fr.umontpellier.iut.dominion.cards.factories.FactoryUtil.*;

public class SeaSideFactory {
    @Dominion_Card(extension = "Seaside")
    public static Card Ambassador() {
        return new Card("Ambassador", RegistryPrice.SeasidePrice(3), CardType.ActionAndAttack).setup(
                config -> config.onPlay(CardUtil::executeAmbassador));
    }

    @Dominion_Card(extension = "Seaside")
    public static Card Astrolabe(){
        Bonus same = Bonus.empty().with(Item.BUY, 1).with(Item.MONEY, 1);
        return new Card("Astrolabe", RegistryPrice.SeasidePrice(3), CardType.TREASURE, CardType.DURATION).
                setup(config -> config.registerSimplePlayAndDuration(same, same));
    }
    @Dominion_Card(extension = "Seaside")
    public static Card Bazaar(){
        Bonus action = Bonus.empty().with(Item.ACTION, 2).with(Item.MONEY, 1).draw(1);
        return Card.action("Bazaar", RegistryPrice.SeasidePrice(5))
                .setup(config -> config.registerSimpleAction(action));
    }
    @Dominion_Card(extension = "Seaside")
    public static Card Blockade(){
        return new Card("Blockade", RegistryPrice.SeasidePrice(4), CardType.ACTION, CardType.DURATION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay(empty(OnPlayComponent.class)
                                .first((player, self) -> self.set("Players", player.getGame().scanImmunity(player)))
                                .then((player, self) ->
                                    player.chooseCardFromSupply("Choississez une carte qui coûte au maximum 4 de pièces", buy -> buy.isAtMost(4), false )
                                            .ifPresent(blocked -> {
                                                self.getCollection("Blocked").add( CardUtil.gainIfPresent(player, blocked, Destination.ASIDE, true));
                                                player.log(String.format("%s bloquée", blocked.getName().toUpperCase()));
                                            })
                                )
                        )
                        .onDurationWithTrigger(
                                empty(DurationComponent.duration.class)
                                        .lookingAt((player, card) -> card.<Card>getCollection("Blocked"))
                                        .thenWith((player, list) -> {
                                            if(list.isEmpty())return;
                                            list.forEach(card -> player.moveTo(card, Destination.HAND));
                                            list.clear();
                                        })
                                        .end(),
                                self -> Optional.ofNullable(self.getCollection("Blocked")).map(c -> !c.isEmpty()).orElse(false)
                        )
                        .afterGain(empty(TriggerComponent.AfterPlayerGain.class)
                                .lookingAt((event, player) -> event.getPlayer())
                                .thenDo(victim -> victim.gain(victim.getCardFromSupply("Curse"), Destination.DISCARD))
                                .end()
                        )
                        .afterGainCondition((event, player) -> {
                             Collection<Card> blocked = config.get().getCollection("Blocked");

                             if(blocked == null || blocked.isEmpty())return false;
                             return  blocked.stream().anyMatch(card -> card.hasForLocation(Destination.ASIDE))
                                     && blocked.stream().anyMatch(card -> event.getCard().hasSameNameAs(card))
                                     && event.getPlayer() != player
                                     && event.getPlayer().isActive();
                        }
                        ));
    }

    @Dominion_Card(extension = "Seaside")
    @InSet(value = {"Repetition"})
    public static Card Caravan(){
        Bonus play = Bonus.empty().with(Item.ACTION, 1).draw(1);
        Bonus d =  Bonus.empty().draw(1);
        return new Card("Caravan", RegistryPrice.SeasidePrice(4), CardType.ACTION, CardType.DURATION)
                .setup(config -> config.registerSimplePlayAndDuration(play, d));
    }

    @Dominion_Card(extension = "Seaside")
    public static Card Corsair() {
        Bonus d =  Bonus.empty().draw(1);
        Bonus play = Bonus.empty().with(Item.MONEY, 2);

        return new Card("Corsair", RegistryPrice.SeasidePrice(5), CardType.ACTION, CardType.DURATION, CardType.ATTACK).setup(
                config -> config
                        .registerSimpleDuration(d)
                        .onPlay((p, self) -> {
                            CardUtil.TriggerEffect(p, FactoryUtil.EFFECT, self, play);
                            self.set("Players", p.getGame().scanImmunity(p));
                        })
                        .onCardPlayed((event, owner) -> {
                            Player actor = event.getPlayer();
                            int currentTurn = owner.getGame().getTurnNumber();
                            Map<Player, Integer> history = config.get().getMap("attackHistory");
                                if (history == null) {
                                    history = new HashMap<>();
                                    config.get().set("attackHistory", history);
                                }

                                if (history.getOrDefault(actor, -1) != currentTurn) {
                                    if (owner.getGame().isImmune(config.get(), actor)) return;

                                    actor.trash(event.getCard());
                                    history.put(actor, currentTurn);

                                    owner.log(String.format("TRIGGER %s : %s écarte %s ",
                                            config.get().getName().toUpperCase(), actor.getName(), event.getCard().getName().toUpperCase()));
                                }
                        })
                        .cardPlayedCondition(((event, player) -> player != event.getPlayer() && (event.getCard().hasName("Silver") || event.getCard().hasName("Gold"))))
                );
    }

    @Dominion_Card(extension = "Seaside")
    @InSet(value = {"Reach for Tomorrow"})
    public static Card Cutpurse(){
        Bonus play  = Bonus.empty().with(Item.MONEY, 2);
        return new Card("Cutpurse", RegistryPrice.SeasidePrice(4), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, card) -> {
                            CardUtil.TriggerEffect(player, "Effect", card, play);
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
    @Dominion_Card(extension = "Seaside")
    public static Card Embargo(){
        Bonus play = Bonus.empty().with(Item.MONEY, 2);

        return new Card("Embargo", RegistryPrice.SeasidePrice(2), CardType.ACTION).setup(
                config -> config.onPlay(
                        (p, self) -> {

                            Runnable logic = () ->
                                    p.chooseCardFromSupply("Choisissez une pile de la réserve à maudir", Objects::nonNull, false)
                                            .ifPresent(card ->{
                                                if(p.trash(self)){
                                                    p.getGame().setToken(card.getName());
                                                    p.log(String.format("Curse %s : %s a été maudit", self.getName().toUpperCase(), card.getName().toUpperCase()));
                                                }
                                            }
                            );

                            CardUtil.TriggerEffect(p, "Effect", self, play);

                            CardUtil.executeOrOtherwise(
                                    () -> p.chooseWhatToDo("Veux tu écarter cette carte pour poser une malédiction sur une des pile de la réserve", List.of(self) ,Button.yesOrNo,false),
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
    @Dominion_Card(extension = "Seaside")
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
                                            () -> p.chooseWhatToDo("Reveal a Province ?", new ArrayList<>(), Button.yesOrNo,false),
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
    @Dominion_Card(extension = "Seaside")
    public static Card FishingVillage(){
        Bonus play = Bonus.empty().with(Item.ACTION, 2).with(Item.MONEY, 1);
        Bonus duration = Bonus.empty().with(Item.ACTION, 1).with(Item.MONEY, 1);

        return new Card("Fishing Village", RegistryPrice.SeasidePrice(3), CardType.ACTION, CardType.DURATION)
                .setup(config -> config.registerSimplePlayAndDuration(play, duration));
    }

    /**
     * Carte Vaisseau fantôme (Ghost Ship)
     * <p>
     * +2 Cartes
     * Tous vos adversaires ayant au moins 4 cartes en main placent des cartes
     * de leur main sur leur pioche jusqu'à avoir 3 cartes en main.
     */
    @Dominion_Card(extension = "Seaside")
    public static Card GhostShip(){
        Bonus play = Bonus.empty().draw(2);

        return new Card("Ghost Ship", RegistryPrice.SeasidePrice(5), CardType.ACTION, CardType.ATTACK).setup(
                config -> config.onPlay((p, c) -> {
                    CardUtil.TriggerEffect(p, EFFECT, c, play);
                    p.getGame().processHandDown(p, c,  Destination.DRAW, 3, false);
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
    @Dominion_Card(extension = "Seaside")
    public static Card Haven(){

        Bonus play = Bonus.empty().with(Item.ACTION, 1).draw(1);

        return new Card("Haven", RegistryPrice.SeasidePrice(2), CardType.ACTION, CardType.DURATION)
                .setup(config -> config
                        .onPlay((p, self ) ->{
                            CardUtil.TriggerEffect(p, EFFECT, self, play);
                            p.chooseCardFromHand("Choissisez une carte de votre main", false )
                                    .ifPresent(card -> {
                                        self.getCollection("Hidden").add(CardUtil.moveIfPresent(p, card, Destination.ASIDE));
                                        p.log(String.format("Action %s : %s cache %s", self.getName().toUpperCase(), p.getName(), card.getName().toUpperCase()));
                                    });
                        })
                        .onDurationWithTrigger(
                                (player, self) -> {
                                    Collection<Card> stock = self.getCollection("Hidden");
                                    if (stock == null || stock.isEmpty()) return;
                                    stock.forEach(c -> {
                                        CardUtil.moveTo(player, () -> c, card -> {}, Destination.HAND);
                                        player.log(String.format("Duration %s : %s récupère %s",
                                                self.getName().toUpperCase(), player.getName(), c.getName().toUpperCase()));
                                    });
                                    stock.clear();
                                },
                                self -> {
                                    Collection<Card> c = self.getCollection("Hidden");
                                    return !c.isEmpty();
                                }
                        ));
    }

    /**
     * Carte Île (Island)
     * <p>
     * 2 VP
     * Placez cette carte et une carte de votre main sur votre plateau Île (Island
     * Mat).
     */
    @Dominion_Card(extension = "Seaside")
    public static Card Island(){
        return new Card("Island", RegistryPrice.SeasidePrice(4), CardType.ACTION, CardType.VICTORY).setup(
                config -> config
                        .onPlay((p, self) -> {
                            p.moveTo(self, Destination.ISLAND);
                            p.chooseCardFromHand("Choississez une carte de votre main à placer sur l'île", false)
                                    .ifPresent(card -> {
                                        p.moveTo(card, Destination.ISLAND);
                                        p.log(String.format("Action %s : %s place %s ", self.getName().toUpperCase(), p.getName(), card.getName().toUpperCase()));
                                    });
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
    @Dominion_Card(extension = "Seaside")
    public static Card LightHouse(){
        Bonus play = Bonus.empty().with(Item.ACTION, 1).with(Item.MONEY, 1);
        Bonus duration = Bonus.empty().with(Item.MONEY, 1);

        return new Card("Lighthouse", RegistryPrice.SeasidePrice(2), CardType.ACTION, CardType.DURATION)
                .setup(
                config -> config
                        .registerSimplePlayAndDuration(play, duration)
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
    @Dominion_Card(extension = "Seaside")
    @InSet(value = {"Reach for Tomorrow"})
    public static Card Lookout(){
        Bonus play = Bonus.empty().with(Item.ACTION, 1);

        return new Card("Lookout", RegistryPrice.SeasidePrice(3), CardType.ACTION).setup(
                config -> config
                        .onPlay( (p, self) ->{

                            CardUtil.TriggerEffect(p,EFFECT, self, play);

                            List<Card> view = CardUtil.getTopCards(p, 3);
                            p.chooseCardFromList("Choississez une carte à écarter", card -> true, view, false)
                                    .ifPresent(card -> {
                                        p.trash(card);
                                        view.remove(card);
                                    });

                            p.chooseCardFromList("Choississez une carte à défaussez", card -> true, view, false)
                                    .ifPresent(card -> {
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
    @Dominion_Card(extension = "Seaside")
    public static Card MerchantShip(){
        Bonus same = Bonus.empty().with(Item.MONEY, 2);
        return new Card("Merchant Ship", RegistryPrice.SeasidePrice(5), CardType.ACTION, CardType.DURATION).setup(
                config -> config.registerSimplePlayAndDuration(same, same));
    }
    /**
     * Carte Singe (Monkey)
     * <p>
     * Jusqu'à votre prochain tour, quand le joueur à votre droite reçoit une
     * carte, +1 Carte.
     * Au début de votre prochain tour, +1 Carte.
     */
    @Dominion_Card(extension = "Seaside")
    @InSet(value = {"Reach for Tomorrow"})
    public static Card Monkey(){
        Bonus play = Bonus.empty().draw(1);


        return new Card("Monkey", RegistryPrice.SeasidePrice(3), CardType.ACTION, CardType.DURATION).setup(
                config -> config
                        .registerSimpleDuration(play)
                        .afterGain((event, owner) -> owner.draw(1))
                        .afterGainCondition((event, player) -> player.getGame().onTheRight(player, event.getPlayer()) && activate.test(config.get()))
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
    @Dominion_Card(extension = "Seaside")
    public static Card NativeVillage(){
        Bonus play = Bonus.empty().with(Item.ACTION, 2);
        return new Card("Native Village", RegistryPrice.SeasidePrice(2), CardType.ACTION).setup(
                config -> config
                        .onPlay((p, self)-> {
                            CardUtil.TriggerEffect(p,EFFECT, self, play);
                            CardUtil.executeOrOtherwise(
                                    () -> p.chooseWhatToDo("Choississez entre poser une carte sur votre village ou de récupérer toutes vos cartes",p.getCopyOf(Destination.NATIVE),List.of(new Button("add", "add"), new Button("take", "take")), false),
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
    @Dominion_Card(extension = "Seaside")
    public static Card Navigator(){
        Bonus bonus = Bonus.empty().with(Item.MONEY, 2);

        return new Card("Navigator", RegistryPrice.SeasidePrice(4), CardType.ACTION).setup(
                config -> config
                        .onPlay((p, self) ->{
                            CardUtil.TriggerEffect(p, EFFECT, self, bonus);
                            List<Card> view = CardUtil.getTopCards(p, 5);

                            Runnable chooseOrder = () -> {
                                while(!view.isEmpty()){
                                    p.chooseCardFromList("Remet les cartes dans l'ordre que tu veux",card -> true , view, false)
                                            .ifPresent(card -> {
                                                view.remove(card);
                                                p.moveTo(card, Destination.DRAW);
                                            })
                                    ;}
                            };

                            CardUtil.executeOrOtherwise(
                                    () ->p.chooseWhatToDo("Défausse tout ou replace les cartes dans l'ordre que tu veux", List.of(self) ,List.of(new Button("discard", "y"), new Button("replace", "n")), false),
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
    @Dominion_Card(extension = "Seaside")
    @InSet(value = {"Repetition"})
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
    @Dominion_Card(extension = "Seaside")
    public static Card PearlDiver(){
        Bonus play = Bonus.empty().with(Item.ACTION, 1).draw(1);
        return new Card("Pearl Diver", RegistryPrice.SeasidePrice(2), CardType.ACTION).setup(
                config -> config
                        .onPlay((p, self) -> {
                            CardUtil.TriggerEffect(p,EFFECT, self, play);
                            Card card = CardUtil.getBottomCards(p, 1).getFirst();
                            if(card!=null){
                                List<Button> buttons = new ArrayList<>();
                                buttons.add(new Button("onTop", "y"));
                                buttons.add(new Button("onBottom", "n"));

                                CardUtil.executeOrOtherwise(
                                        () ->p.chooseWhatToDo("Choix: Placez votre carte au dessus de votre pioche : " + card.getName() , List.of(card),  buttons, true),
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
    @Dominion_Card(extension = "Seaside")
    @InSet(value = {"Repetition"})
    public static Card Pirate(){
        return new Card("Pirate", RegistryPrice.SeasidePrice(5), CardType.ACTION, CardType.DURATION, CardType.REACTION)
                .setup(config -> config
                    .onDuration((p, self) -> CardUtil.executeIfSelected(
                            () -> CardUtil.gainFromSupply(p, "Choississez un trésor (maximum 6 pièces) ",
                                    card -> card.hasType(CardType.TREASURE)&& card.isAtMost(6),
                                    Destination.HAND,
                                    false ),
                            card ->  p.log(String.format("Action %s : %s récupère %s coutant %d pièces", self.getName().toUpperCase(), p.getName(), card.getName().toUpperCase(), card.getCost()))
                            ))
                    .afterGain((event, owner) -> owner.chooseCardFromHand("Veux tu jouer ton pirate ?", card -> card.hasSameNameAs(config.get()), true).ifPresent(
                                card -> {
                                    owner.playCard(config.get());
                                    owner.log(String.format("Reaction %s", config.get().getName().toUpperCase()));
                                }))
                    .afterGainCondition(
                            (event, player) -> event.getCard().hasType(CardType.TREASURE))
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
    @Dominion_Card(extension = "Seaside")
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
    @Dominion_Card(extension = "Seaside")
    public static Card Sailor(){
        Bonus play = Bonus.empty().with(Item.ACTION, 1);
        Bonus dura =  Bonus.empty().with(Item.MONEY, 2);

        return new Card("Sailor", RegistryPrice.SeasidePrice(4), CardType.ACTION, CardType.DURATION)
                .setup(config -> config
                        .registerSimpleAction(play)
                        .onDuration((p, self) ->{
                            CardUtil.TriggerEffect(p, DURATION, self, dura);
                            p.chooseCardFromHand("Choisie une carte à écarter", true)
                                    .ifPresent(p::trash);
                        })
                        .onGain((c, owner) -> {
                                List<Button> buttons = new ArrayList<>();
                                buttons.add(new Button("play", "y"));
                                buttons.add(new Button("skip", "n"));
                                CardUtil.executeOrOtherwise(
                                        () ->owner.chooseWhatToDo("Veux-tu jouer ta carte " + c.getCard().getName(),card -> !card.hasSameNameAs(c.getCard()) , List.of(c.getCard()), buttons , true),
                                        choice -> "y".equals(choice) || c.getCard().hasName(choice),
                                        choice -> {
                                            owner.playCard(c.getCard());
                                            c.setDest(Destination.INPLAY);
                                            linkedCard(config.get(),  c.getCard());
                                            config.get().set("used", true);
                                        },
                                        () -> {}
                                );
                        })
                        .duringGainCondition((event, player) ->
                                event.getCard().hasType(CardType.DURATION)
                                        && player == event.getPlayer()
                                        && !config.get().getFlag("used")
                                        && activate.test(config.get())
                                        && event.notMoved()
                        )
                        .stayInPlayCondition(checkLink.or(checkDuration))
                );
    }

    /**
     * Carte Sauveteur (Salvager)
     * <p>
     * +1 Achat
     * Écartez une carte de votre main. +1 Pièce par Pièce de son coût.
     */
    @Dominion_Card(extension = "Seaside")
    public static Card Salvager(){
        Bonus play = Bonus.empty().with(Item.BUY, 1);

        return new Card("Salvager", RegistryPrice.SeasidePrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay((p, self) -> {
                            CardUtil.TriggerEffect(p, EFFECT, self, play);
                            p.chooseCardFromHand("Choisis une carte à écarter", false)
                                    .ifPresent(card -> {
                                        p.trash(card);
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
    @Dominion_Card(extension = "Seaside")
    @InSet(value = {"Repetition"})
    public static Card SeaChart(){
        Bonus play =  Bonus.empty().with(Item.ACTION, 1).draw(1);

        return new Card("Sea Chart", RegistryPrice.SeasidePrice(3), CardType.ACTION)
                .setup( config -> config
                        .onPlay((p, self) -> {
                            CardUtil.TriggerEffect(p, EFFECT, self, play);
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
    @Dominion_Card(extension = "Seaside")
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
    @Dominion_Card(extension = "Seaside")
    @InSet(value = {"Reach for Tomorrow"})
    public static Card SeaWitch(){
        Bonus same = Bonus.empty().draw(2);
        return new Card("Sea Witch", RegistryPrice.SeasidePrice(5), CardType.ACTION, CardType.DURATION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((p, self) -> {
                            CardUtil.TriggerEffect(p, EFFECT, self, same);
                            p.getGame().processGain(p, self, Destination.DISCARD, "Curse");
                        })
                        .onDuration((p, self) -> {
                            CardUtil.TriggerEffect(p, DURATION, self, same);
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
    @Dominion_Card(extension = "Seaside")
    public static Card Smugglers(){
        return new Card("Smugglers", RegistryPrice.SeasidePrice(3), CardType.ACTION)
                .setup(config -> config
                        .onPlay((p, self) ->{
                            Player right = p.getGame().onTheRight(p);
                            if(right == null) return;
                            if(right.getCardGainedLastTurn().isEmpty()) return;
                            p.chooseCardFromList("Choose a card; And copy it in your discard", card -> card.isAtMost(6), right.getCardGainedLastTurn(), true)
                                    .ifPresent(c ->{
                                        p.gain(c, Destination.DISCARD);
                                        p.log(String.format("Action %s : %s est copié", self.getName().toUpperCase(), c.getName().toUpperCase()));
                                    });
                        })
                );
    }
    @Dominion_Card(extension = "Seaside")
    public static Card Tactician() {
        Bonus duration = Bonus.empty().draw(5).with(Item.ACTION, 1).with(Item.BUY, 1);


        return new Card("Tactician", RegistryPrice.SeasidePrice(5), CardType.ACTION, CardType.DURATION)
                .setup(config -> config
                        .onPlay((p, self) -> {

                            if (p.getCopyOf(Destination.HAND).isEmpty()) {
                                p.log(p.getName() + " joue un Tactician mais n'a rien à défausser.");
                                self.set("activated", true);
                                return;
                            }
                            p.getCopyOf(Destination.HAND).forEach(p::discard);

                            p.log(String.format("TACTICIAN : %s défausse sa main pour activer le bonus du tour prochain", p.getName()));

                            self.set("activated", false);
                        })
                        .onDurationWithTrigger((player, self) -> CardUtil.TriggerEffect(player, DURATION, self, duration),
                                self -> self.getFlag("activated"))
                );
    }

    /**
     * Carte Marée (Tide Pools)
     * <p>
     * +3 Cartes
     * +1 Action
     * Au début de votre prochain tour, défaussez 2 cartes.
     */
    @Dominion_Card(extension = "Seaside")
    public static Card TidePools(){
        Bonus play = Bonus.empty().draw(3).with(Item.ACTION, 1);

        return new Card("Tide Pools", RegistryPrice.SeasidePrice(4), CardType.ACTION, CardType.DURATION)
                .setup(config -> config
                        .registerSimpleAction(play)
                        .onDuration((p, self) -> p.discardFromHand(2))
                );
    }

    /**
     * Carte aux trésors (Treasure Map)
     * <p>
     * Écartez ceci et une Carte aux trésors de votre main. Si vous avez écarté
     * deux Cartes aux trésors, recevez 4 Ors (Gold) sur votre pioche.
     */
    @Dominion_Card(extension = "Seaside")
    @InSet(value = {"Reach for Tomorrow"})
    public static Card TreasureMap(){
        return new Card("Treasure Map", RegistryPrice.SeasidePrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay((p, self) -> {
                            if(p.trash(self)){

                                Card other = p.getCopyOf(Destination.HAND).stream().filter(self::hasSameNameAs).findFirst().orElse(null);
                                if (other != null) {
                                    p.trash(other);

                                    IntStream.range(0,4)
                                            .mapToObj(c -> p.getCardFromSupply("Gold"))
                                            .filter(Objects::nonNull)
                                            .forEach(gold -> p.gain(gold, Destination.DRAW));
                                    }
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
    @Dominion_Card(extension = "Seaside")
    @InSet(value = {"Repetition"})
    public static Card Treasury(){
        Bonus play = Bonus.empty().with(Item.ACTION, 1).with(Item.BUY, 1).with(Item.MONEY, 1);
        return new Card("Treasury", RegistryPrice.SeasidePrice(5), CardType.ACTION)
                .setup(config -> config
                        .registerSimpleAction(play)
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
    @Dominion_Card(extension = "Seaside")
    public static Card Warehouse(){
        Bonus play = Bonus.empty().with(Item.ACTION, 1).draw(3);

        return new Card("Warehouse", RegistryPrice.SeasidePrice(3), CardType.ACTION)
                .setup( config -> config
                        .onPlay(
                                (p, self) -> {
                                    CardUtil.TriggerEffect(p, EFFECT, self, play);
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
    @Dominion_Card(extension = "Seaside")
    public static Card Wharf(){
        Bonus same = Bonus.empty().draw(2).with(Item.BUY, 1);
        return new Card("Wharf", RegistryPrice.SeasidePrice(5), CardType.ACTION, CardType.DURATION)
                .setup(config -> config.registerSimplePlayAndDuration(same, same));
    }
}
