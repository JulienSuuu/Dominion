package fr.umontpellier.iut.dominion.cards.factories.dominion;

import fr.umontpellier.iut.dominion.*;
import fr.umontpellier.iut.dominion.Annotation.Dominion_Card;
import fr.umontpellier.iut.dominion.Annotation.ExtraSet;
import fr.umontpellier.iut.dominion.Annotation.InSet;
import fr.umontpellier.iut.dominion.Annotation.PileType;
import fr.umontpellier.iut.dominion.Player.Player;
import fr.umontpellier.iut.dominion.cards.*;
import fr.umontpellier.iut.dominion.cards.component.*;


import java.util.*;
import java.util.function.Consumer;

import static fr.umontpellier.iut.dominion.Button.yesOrNo;
import static fr.umontpellier.iut.dominion.cards.CardConfigurator.bonus;
import static fr.umontpellier.iut.dominion.cards.CardConfigurator.run;
import static fr.umontpellier.iut.dominion.cards.factories.FactoryUtil.*;


public class DominionFactory {

    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"Size Distortion", "Deck Top", "Improvements", "Grand Scheme",
            "Reach for Tomorrow", "Biggest Money"})
    public static Card Artisan(){
        return new Card("Artisan", RegistryPrice.DominionPrice(6), CardType.ACTION)
                .setup(config -> config
                        .onPlay(run((Player player, Card self) ->
                                CardUtil.executeIfSelected(
                                        () -> CardUtil.gainFromSupply(player, "Choisi une carte coûtant au maximum 5", c -> c.isAtMost(5), Destination.HAND, false),
                                        card -> player.log(String.format("Action %s : %s gagne %s", self.getName().toUpperCase(), player.getName(), card.getName().toUpperCase()))
                            )).then((player, self) ->
                                player.chooseCardFromHand("Défausse une carte", false)
                                        .ifPresent(card -> {
                                            player.log(String.format("Action %s : %s remet en pioche %s", self.getName().toUpperCase(), player.getName(), card.getName().toUpperCase()));
                                            player.moveTo(card, Destination.DRAW);
                                        }))
                        )
                );
    }

    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"Size Distortion", "Silver & Gold", "Deconstruction", "Forbidden Arts"})
    public static Card Bandit(){
        return new Card("Bandit", RegistryPrice.DominionPrice(5), CardType.ActionAndAttack)
                .setup(config -> config
                        .onPlay(run((Player player, Card self) ->
                                CardUtil.executeIfSelected(
                                        () -> CardUtil.gainFromSupply(player, "Gold", Destination.DISCARD, false),
                                        card ->player.log(String.format("Action %s : %s gagne %s", self.getName().toUpperCase(), player.getName(), card.getName().toUpperCase()))
                                )).then((player, self) ->
                                        player.getGame().processAttackWithReveal(player, self, 2,
                                                card -> !card.hasName("Copper") && card.hasType(CardType.TREASURE),
                                                (attacker, victim, options) -> attacker.getGame().chooseACard(victim, options)
                                    )
                                )
                        )
                );
    }

    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"Size Distortion", "Deck Top", "Silver & Gold",
            "Chemistry Lesson", "The King's Army"})
    public static Card Bureaucrat(){
        return new Card("Bureaucrat", RegistryPrice.DominionPrice(4), CardType.ActionAndAttack)
                .setup(config -> config
                        .onPlay(run((Player player, Card self) ->
                                        CardUtil.executeIfSelected(
                                                () -> CardUtil.gainFromSupply(player, "Silver", Destination.DRAW, false),
                                                card -> player.log(String.format("Action %s : %s gagne %s", self.getName().toUpperCase(), player.getName(), card.getName().toUpperCase())))
                                ).then((player, self) ->
                                        player.getGame().checkHandOrShow(
                                                player,
                                                self,
                                                card -> card.hasType(CardType.VICTORY),
                                                (vi, cards) -> vi.chooseCardFromList("Choisie une carte victoire à défausser", card -> true, cards, false),
                                                Destination.DRAW)
                                )
                        )
                );
    }

    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"First Game", "Sleight of Hand", "Improvements", "Underlings", "Reach for Tomorrow"
            , "Forbidden Arts", "Potion Mixers", "Bounty of the Hunt"})
    public static Card Cellar(){
        Bonus play = Bonus.empty().with(Item.ACTION, 1);
        return new Card("Cellar", RegistryPrice.DominionPrice(2),CardType.ACTION)
                .setup(config ->config
                        .onPlay(bonus(play)
                                .then((player, card) -> player.discardUntilYouStop(Destination.HAND, player::draw))
                        )
                );
    }

    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"Size Distortion", "Silver & Gold"})
    public static Card Chapel(){
        return new Card("Chapel", RegistryPrice.DominionPrice(2),CardType.ACTION)
                .setup(config ->  config
                        .onPlay(run((Player player, Card self) ->
                                player.chooseCardFromHand("Choisi au maximum 4 cartes à défausser", true)
                                    .ifPresentOrElse(
                                            player::discard,
                                            () -> self.set("stop", true)
                                    ))
                                .repeatWhile((player, self) -> !self.getFlag("stop"), 4)
                        )
                );
    }

    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"Deck Top", "Sleight of Hand", "Grand Scheme",
            "Reach for Tomorrow", "Forbidden Arts", "The King's Army"})
    public static Card Council_Room(){
        Bonus  play = Bonus.empty().with(Item.BUY, 1).draw(4);
        return new Card("CouncilRoom",  RegistryPrice.DominionPrice(5),CardType.ACTION)
                .setup(config -> config
                        .onPlay(bonus(play)
                                .then((player, self) ->
                                    player.getGame().processBenefit(player, victim -> {
                                        victim.draw(1);
                                        player.log(String.format("Benefit %s : %s donne 1 une carte à %s", self.getName().toUpperCase(), player.getName(), victim.getName()));
                                    })
                                )
                        )
                );
    }

    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"Size Distortion", "Deck Top", "Sleight of Hand", "Underlings",
            "Repetition", "Potion Mixers", "Bounty of the Hunt"})
    public static Card Festival(){
        Bonus play = Bonus.empty().with(Item.ACTION,2).with(Item.BUY,1).with(Item.MONEY, 2);
        return new Card("Festival", RegistryPrice.DominionPrice(5),CardType.ACTION)
                .setup(config -> config.registerSimpleAction(play));
    }

    @Dominion_Card(extension = "Dominion", pileType = PileType.VICTORY)
    @InSet(value = {"Size Distortion", "Sleight of Hand", "Forbidden Arts"})
    public static Card Gardens(){
        return new Card("Gardens",  RegistryPrice.DominionPrice(4),CardType.VICTORY)
                .setup(config -> config.score(player -> player.getAllOwnedCards().size()/10));
    }

    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"Deck Top", "Sleight of Hand", "Silver & Gold", "Repetition"
            , "Biggest Money"})
    public static Card Harbinger(){
        Bonus play = Bonus.empty().with(Item.ACTION, 1).draw(1);
        return new Card("Harbinger", RegistryPrice.DominionPrice(3),CardType.ACTION)
                .setup(config -> config
                        .onPlay(bonus(play)
                                .then((player, self) -> {
                                    List<Card> distinctDiscard = player.getCopyOf(Destination.DISCARD).stream().distinct().toList();

                                    player.chooseCardFromList("Choisit une carte de ta défausse à mettre sur ton deck", card -> true, distinctDiscard, true)
                                            .ifPresent(card -> {
                                                player.moveTo(card, Destination.DRAW);
                                                player.log(String.format("Action HARBINGER : %s met en pioche %s", player.getName(), card.getName().toUpperCase()));
                                            });
                                })
                        )
                );
    }
    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"Deck Top", "Silver & Gold", "Forbidden Arts", "Biggest Money"})
    public static Card Laboratory(){
        Bonus play = Bonus.empty().draw(2).with(Item.ACTION, 1);
        return new Card("Laboratory", RegistryPrice.DominionPrice(5),CardType.ACTION)
                .setup(config -> config.registerSimpleAction(play));
    }
    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"Sleight of Hand", "Underlings", "Gilding the Lily"})
    public static Card Library(){
        return new Card("Library", RegistryPrice.DominionPrice(5),CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            List<Card> sideTrack = new ArrayList<>();
                            while(player.getCopyOf(Destination.HAND).size() < 7){
                                Card drawn = player.getCardFromDeck();

                                if(drawn == null) break;

                                if(drawn.hasType(CardType.ACTION)){
                                    CardUtil.executeOrOtherwise(
                                            () -> player.chooseWhatToDo("Veux tu mettre cette carte sur le côté ou la récupérer dans la main ? ", List.of(drawn),List.of(new Button("Aside", "y"), new Button("Hand", "n")),false),
                                            "y"::equals,
                                            choice ->{
                                                player.moveTo(drawn, Destination.ASIDE);
                                                sideTrack.add(drawn);
                                            },
                                            () -> player.moveTo(drawn, Destination.HAND)
                                    );
                                }else player.moveTo(drawn, Destination.HAND);
                            }
                            sideTrack.forEach(card -> player.moveTo(card, Destination.DISCARD));
                        })
                );
    }
    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"First Game", "Improvements", "Grand Scheme",
            "Chemistry Lesson", "Gilding the Lily"})
    public static Card Market(){
        Bonus bonus =  Bonus.empty().draw(1).with(Item.ACTION, 1).with(Item.BUY,1).with(Item.MONEY,1);
        return new Card("Market", RegistryPrice.DominionPrice(5),CardType.ACTION)
                .setup(config -> config.registerSimpleAction(bonus));
    }
    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"First Game", "Improvements", "Silver & Gold",
            "The King's Army", "Gilding the Lily"})
    public static Card Merchant(){
        Bonus play = Bonus.empty().with(Item.ACTION, 1).draw(1);
        return new Card("Merchant", RegistryPrice.DominionPrice(3),CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player,  EFFECT, self, play);
                            self.set("used", false);
                        })
                        .onCardPlayed((c, owner) -> {
                                owner.increment(Item.MONEY,1);
                                owner.log(String.format("Trigger %s : %s récupère une pièce", config.get().getName().toUpperCase(), c.getPlayer().getName()));
                                config.get().set("used", true);
                        })
                        .cardPlayedCondition((event, player) -> (!config.get().getFlag("used") && event.getCard().hasName("Silver") && event.getPlayer() == player))
                );
    }
    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"First Game", "Sleight of Hand", "Grand Scheme",
            "Repetition", "Potion Mixers", "Bounty of the Hunt"})
    public static Card Militia() {
        Bonus play = Bonus.empty().with(Item.MONEY,2 );
        return new Card("Militia", RegistryPrice.DominionPrice(4), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, play);
                            player.getGame().processHandDown(player, self, Destination.DISCARD, 3, true);
                        })
                );
    }
    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"First Game", "Improvements", "Silver & Gold",
            "Deconstruction", "Biggest Money"})
    public static Card Mine(){
        return new Card("Mine", RegistryPrice.DominionPrice(5),CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) ->
                            player.chooseCardFromHand("Choose a Card to Trash", card -> card.hasType(CardType.TREASURE), true)
                                    .ifPresent(card -> {
                                        if(player.trash(card)){
                                        CardUtil.executeIfSelected(
                                                () -> CardUtil.gainFromSupply(
                                                        player,
                                                        "Choose a Treasure (Max " + (card.getCost()+3) + "$)",
                                                        filter -> filter.hasType(CardType.TREASURE) && filter.isAtMostWithBonus(card, 3) ,
                                                        Destination.HAND,
                                                        false),
                                                gained -> player.log(String.format("Action %s : %s trash %s for %s", self.getName().toUpperCase(), player.getName(), card.getName().toUpperCase(), gained.getName().toUpperCase()))
                                        );}
                                    }
                            )
                        )
                );
    }
    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"First Game", "Improvements", "Chemistry Lesson", "The King's Army"})
    public static Card Moat(){
        Bonus play = Bonus.empty().draw(2);
        return new Card("Moat", RegistryPrice.DominionPrice(2),CardType.ACTION,CardType.REACTION)
                .setup(config -> config
                        .registerSimpleAction(play)
                        .immunity(new TriggerComponent.Immunity() {
                            @Override
                            public boolean revealed(Player player, Card self) {
                                self.set("used", false);
                                CardUtil.executeOrOtherwise(
                                        () -> player.chooseWhatToDo(String.format("Révèle %s pour te protéger", self.getName().toUpperCase()), List.of(self) , List.of(new Button("reveal", "y"), new Button("keep", "n")), true),
                                        "y"::equals,
                                        choice ->{
                                            player.log(player.getName() + "révèle Moat");
                                           self.set("used", true);
                                            },
                                        () -> {}
                                );
                                return self.getFlag("used");
                            }
                        })
                        .ImmunityCondition((event, player) -> event.getCard().hasType(CardType.ATTACK) &&  config.get().getFlag("used"))
                );
    }
    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"Deck Top", "Improvements", "Silver & Gold",
            "Biggest Money", "Bounty of the Hunt"})
    public static Card MoneyLender(){
        return new Card("Moneylender", RegistryPrice.DominionPrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) ->
                         player.chooseCardFromHand("Choisis de jetter un Copper pour gagner 3 pièces", card -> card.hasName("Copper"), true)
                                 .ifPresent(card -> {
                                     if(player.trash(card)){
                                        player.increment(Item.MONEY,3);
                                        player.log(String.format("%s écarte un CUIVRE pour +3 pièces", player.getName()));
                                    }
                                 })
                        )
                );
    }
    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"Sleight of Hand", "Improvements", "Potion Mixers"})
    public static Card Poacher(){
        Bonus play = Bonus.empty().with(Item.ACTION, 1).with(Item.MONEY, 1).draw(1);
        return new Card("Poacher", RegistryPrice.DominionPrice(4), CardType.ACTION)
                .setup(config -> config
                    .onPlay((player, self) ->{
                        Number emptyPile = GameStat.emptyPiles.get();
                        CardUtil.TriggerEffect(player, EFFECT, self, play);
                        player.discardFromHand(emptyPile.intValue());
                    })
                );
    }
    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"First Game", "Improvements", "Deconstruction",
            "Repetition", "Chemistry Lesson", "Gilding the Lily"})
    public static Card Remodel(){
        return new Card("Remodel", RegistryPrice.DominionPrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) ->
                         player.chooseCardFromHand("Jette une carte de ta main pour en récupèrer une nouvelle coutant jusqu'à 2 pièce de plus", false)
                                 .ifPresent(card ->{
                                     if(player.trash(card))
                                        CardUtil.gainFromSupply(
                                                player,
                                                "Choisi une carte (Max " + (card.getCost()+2) + " pièces)",
                                                filter -> filter.isAtMostWithBonus(card, 2),
                                                Destination.DISCARD,
                                                false );}
                                )
                        )
                );

    }
    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"Size Distortion", "Deck Top", "Underlings", "Gilding the Lily"})
    public static Card Sentry() {
        Bonus play = Bonus.empty().with(Item.ACTION, 1).draw(1);
        return new Card("Sentry", RegistryPrice.DominionPrice(5), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player,  EFFECT, self, play);

                            List<Card> view = CardUtil.getTopCards(player, 2);

                            while (!view.isEmpty()) {
                                List<Button> actions = List.of(new Button("Trash", "t"), new Button("Discard", "d"), new Button("Done", "x"));

                                String choice = player.chooseWhatToDo("Sentinelle : Écarter ou Défausser ? ", view, actions, false);

                                if ("x".equals(choice)) break;
                                player.chooseCardFromList("Quelle carte ?", card -> true, view, true)
                                        .ifPresent(selected -> {
                                            if ("t".equals(choice)) player.trash(selected);
                                            else player.discard(selected);
                                            view.remove(selected);
                                        }
                                );
                            }

                            while (!view.isEmpty()) {
                                 player.chooseCardFromList("Ordre du Deck (La prochaine sera sur le dessus)", card -> true, view, false)
                                         .ifPresent(selected -> {
                                             player.moveTo(selected, Destination.DRAW);
                                             view.remove(selected);
                                        }
                                );
                            }
                        })
                );
    }

    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"First Game", "Sleight of Hand", "Potion Mixers", "Bounty of the Hunt"})
    public static Card Smithy(){
        Bonus  play = Bonus.empty().draw(3);
        return new Card("Smithy", RegistryPrice.DominionPrice(4), CardType.ACTION)
                .setup(config -> config.registerSimpleAction(play)
                );
    }

    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"Size Distortion", "Sleight of Hand", "Silver & Gold",
            "Deconstruction", "Forbidden Arts"})
    public static Card Throne_Room() {
        return new Card("ThroneRoom", RegistryPrice.DominionPrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) ->
                            player.chooseCardFromHand("Action à doubler", c -> c.hasType(CardType.ACTION), true)
                                    .ifPresent(selected -> {
                                        player.log(player.getName() + " Throne Room -> " + selected.getName());
                                        player.playCard(selected, 2);
                                        linkedCard(self, selected);
                                    })
                        )
                        .onDuration((player, self) -> {
                            Collection<Card> selected = self.getCollection("LinkedCard");
                            selected.forEach(card -> card.getComponent(DurationComponent.class).ifPresent(d -> {
                                player.log("Throne Room relance l'effet de " + card.getName());
                                d.execute(player, card);
                            }));
                        })
                        .stayInPlayCondition(checkLink)

                );
    }
    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"Deck Top", "Silver & Gold", "Underlings",
            "Reach for Tomorrow", "Chemistry Lesson"})
    @ExtraSet(value = {"Gilding the Lily"})
    public static Card Vassal(){
        Bonus play = Bonus.empty().with(Item.MONEY, 2);
        return new Card("Vassal", RegistryPrice.DominionPrice(3), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player,  EFFECT, self, play);

                            Card top = player.getCardFromDeck();
                            if(top == null) return;

                            if(top.hasType(CardType.ACTION)){
                                CardUtil.executeOrOtherwise(
                                        () -> player.chooseWhatToDo("Veux tu jouer la carte", List.of(top), yesOrNo, true ),
                                        "y"::equals,
                                        choice->{
                                            player.playCard(top);
                                            linkedCard(self, top);
                                            },
                                        () -> player.moveTo(top, Destination.DISCARD)
                                );
                            }else player.moveTo(top, Destination.DISCARD);
                        })
                        .stayInPlayCondition(checkLink)
                );
    }

    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"First Game", "Deck Top", "Deconstruction", "Reach for Tomorrow", "The King's Army"})
    public static Card Village(){
        Bonus  play = Bonus.empty().with(Item.ACTION, 2).draw(1);
        return new Card("Village", RegistryPrice.DominionPrice(3), CardType.ACTION)
                .setup(config -> config.registerSimpleAction(play));
    }

    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"Size Distortion", "Improvements", "Chemistry Lesson"})
    public static Card Witch(){
       Bonus play = Bonus.empty().draw(2);
        return new Card("Witch", RegistryPrice.DominionPrice(5), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                    .onPlay((player, self) -> {
                        CardUtil.TriggerEffect(player,  EFFECT, self, play);
                        player.getGame().processGain(player, self, Destination.DISCARD, "Curse");
                    })
                );
    }

    @Dominion_Card(extension = "Dominion")
    @InSet(value = {"First Game", "Size Distortion", "Grand Scheme", "Repetition"})
    public static Card Workshop(){
        return new Card("Workshop", RegistryPrice.DominionPrice(3), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> CardUtil.gainFromSupply(player, "Choisit une carte ( Max 4 pièces )", card -> card.isAtMost(4), Destination.DISCARD,  false))
                );
    }
    @Dominion_Card(extension = "Dominion")
    public static Card Adventurer() {
        return new Card("Adventurer", RegistryPrice.DominionPrice(6), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            List<Card> treasuresFound = new ArrayList<>();
                            List<Card> revealedOther = new ArrayList<>();

                            while (treasuresFound.size() < 2) {
                                Card drawn = player.getCardFromDeck();

                                if (drawn == null) {
                                    break;
                                }

                                player.moveTo(drawn, Destination.ASIDE);
                                if (drawn.hasType(CardType.TREASURE)) {
                                    treasuresFound.add(drawn);
                                    player.log(player.getName() + " révèle un Trésor : " + drawn.getName());
                                } else {
                                    revealedOther.add(drawn);
                                    player.log(player.getName() + " révèle : " + drawn.getName());
                                }
                            }

                            treasuresFound.forEach(c -> player.moveTo(c, Destination.HAND));

                            revealedOther.forEach(c -> player.moveTo(c, Destination.DISCARD));

                            player.log(String.format("ADVENTURIER : %d trésor(s) ajouté(s) en main.", treasuresFound.size()));
                        })
                );
    }
    @Dominion_Card(extension = "Dominion")
    public static Card Chancellor(){
        Bonus play =  Bonus.empty().with(Item.MONEY, 2);
        return new Card("Chancellor", RegistryPrice.DominionPrice(3), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, play);
                            CardUtil.executeOrOtherwise(
                                    () -> player.chooseWhatToDo("Veux-tu vider ta pioche dans la défausse ?", List.of(self) , yesOrNo, true),
                                    "y"::equals,
                                    choice -> player.getCopyOf(Destination.DRAW).forEach(card -> player.moveTo(card, Destination.DISCARD)),
                                    () ->{}
                            );
                        })
                );
    }
    @Dominion_Card(extension = "Dominion")
    public static Card Feast(){
        return new Card("Feast", RegistryPrice.DominionPrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            player.trash(self);
                            CardUtil.gainFromSupply(player, "Choisit une carte de la réserve ( max 5 pièce )", filter -> filter.isAtMost(5), Destination.DISCARD, false);
                        })
                );
    }
    @Dominion_Card(extension = "Dominion")
    public static Card Spy(){
        Bonus play = Bonus.empty().with(Item.ACTION, 1).draw(1);
        return new Card("Spy", RegistryPrice.DominionPrice(4), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, play);

                            Consumer<Player> action = p -> {
                                Card c =  p.getCardFromDeck();
                                CardUtil.executeOrOtherwise(
                                    () -> player.chooseWhatToDo("Discard the card or do nothing (your choice) " + c.getName(), List.of(c), yesOrNo, true),
                                    "y"::equals,
                                    choice -> p.moveTo(c, Destination.DISCARD),
                                    ()-> {}
                                );
                            };

                            action.accept(player);

                            player.getGame().processAttack(player, self, action);


                        })
                );
    }
    @Dominion_Card(extension = "Dominion")
    public static Card Thief(){
        return new Card("Thief", RegistryPrice.DominionPrice(4), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            Consumer<Player> action = p -> {
                                List<Card> revealed = CardUtil.getTopCards(p, 2);
                                player.log(String.format("Attack %s: %s dévoile %s",
                                        self.getName().toUpperCase(), p.getName(), revealed));

                                List<Card> treasures = revealed.stream().filter(card->  card.hasType(CardType.TREASURE)).toList();

                                Card chosen = null;
                                if(!treasures.isEmpty()){
                                    chosen = player.getGame().chooseACard(player, treasures);
                                    p.trash(chosen);

                                    Card Final = chosen;
                                    CardUtil.executeOrOtherwise(
                                            () -> player.chooseWhatToDo("Voulez vous récupérer la carte " + Final.getName(),List.of(Final), yesOrNo, true),
                                            "y"::equals,
                                            choice -> player.gain(Final, Destination.DISCARD),
                                            () -> {}
                                    );
                                }

                                Card finalChosen = chosen;

                                revealed.stream()
                                        .filter(card -> card != finalChosen)
                                        .forEach(card -> p.moveTo(card, Destination.DISCARD));
                            };

                            player.getGame().processAttack(player, self, action);
                        })
                );
    }
    @Dominion_Card(extension = "Dominion")
    public static Card WoodCutter(){
        Bonus bonus =  Bonus.empty().with(Item.BUY, 1).with(Item.MONEY, 2);
        return new Card("Woodcutter", RegistryPrice.DominionPrice(3), CardType.ACTION)
                .setup(config -> config.registerSimpleAction(bonus));
    }



}
