package fr.umontpellier.iut.dominion.cards.dominion;

import fr.umontpellier.iut.dominion.*;
import fr.umontpellier.iut.dominion.Annotation.Dominion_Card;
import fr.umontpellier.iut.dominion.Annotation.PileType;
import fr.umontpellier.iut.dominion.cards.*;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent;

import java.util.*;
import java.util.function.Consumer;


public class DominionFactory {
    public static List<Button> yesOrNo = List.of(new Button("Yes", "y"), new Button("No", "n"));
    @Dominion_Card(extension = "Dominion")
    public static Card Artisan(){
        return new Card("Artisan", RegistryPrice.DominionPrice(6), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.executeIfSelected(
                                    () -> CardUtil.gainFromSupply(player, "Choisi une carte coûtant au maximum 5", c -> c.getCost()<6, Destination.HAND, false),
                                    card -> player.log(String.format("Action %s : %s gagne %s", self.getName().toUpperCase(), player.getName(), card.getName().toUpperCase()))
                            );

                            CardUtil.executeIfSelected(
                                    () ->  player.chooseCardFromHand("Défausse une carte", false),
                                    card -> {
                                        player.log(String.format("Action %s : %s remet en pioche %s", self.getName().toUpperCase(), player.getName(), card.getName().toUpperCase()));
                                        player.moveTo(card, Destination.DRAW);
                                    }
                            );
                        })
                );
    }
    @Dominion_Card(extension = "Dominion")
    public static Card Bandit(){
        return new Card("Bandit", RegistryPrice.DominionPrice(5), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) ->
                                CardUtil.execute(
                                    () -> CardUtil.executeIfSelected(
                                            () -> CardUtil.gainFromSupply(player, "Gold", Destination.DISCARD, false),
                                            card ->player.log(String.format("Action %s : %s gagne %s", self.getName().toUpperCase(), player.getName(), card.getName().toUpperCase()))
                                    ),

                                    () -> player.getGame().processAttackWithReveal(player,
                                            self,
                                            2,
                                            card -> !card.hasName("Copper") && card.hasType(CardType.TREASURE),
                                            (attacker, victim, options) -> attacker.getGame().chooseACard(victim, options)
                                    )
                                )
                        )
                );
    }
    @Dominion_Card(extension = "Dominion")
    public static Card Bureaucrat(){
        return new Card("Bureaucrat", RegistryPrice.DominionPrice(4), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) ->
                                CardUtil.execute(
                                    () -> CardUtil.executeIfSelected(
                                            () -> CardUtil.gainFromSupply(player, "Silver", Destination.DRAW, false),
                                            card -> player.log(String.format("Action %s : %s gagne %s", self.getName().toUpperCase(), player.getName(), card.getName().toUpperCase()))),
                                    () -> player.getGame().checkHandOrShow(
                                            player,
                                            self,
                                            card -> card.hasType(CardType.VICTORY),
                                            (vi, cards) -> Optional.ofNullable(vi.chooseCardFromList("Choisie une carte victoire à défausser", card -> true, cards, false)),
                                            Destination.DRAW
                                    )
                                )
                        )
                );
    }
    @Dominion_Card(extension = "Dominion")
    public static Card Cellar(){
        return new Card("Cellar", RegistryPrice.DominionPrice(2),CardType.ACTION)
                .setup(config ->config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0, 1, 0, 0, "Effect", self);
                            int count = 0;
                            while(true){
                                Card c = player.discard();
                                if(c == null) break;
                                count++;
                            }

                            if(count > 0){
                                player.draw(count);
                                player.log("Action Cellar : " + count + " cartes défaussées, " + count + " cartes piochées.");
                            }
                        })
                );
    }
    @Dominion_Card(extension = "Dominion")
    public static Card Chapel(){
        return new Card("Chapel", RegistryPrice.DominionPrice(2),CardType.ACTION)
                .setup(config ->  config
                        .onPlay((player, self) -> {
                            self.set("continu", true);
                            for(int i = 0; i < 4 && self.get("continu", Boolean.class); i++){
                                CardUtil.executeOrOtherwise(
                                        () -> player.chooseCardFromHand("Choisi au maximum 4 cartes à défausser", true),
                                        Objects::nonNull,
                                        player::moveToTrash,
                                        () -> self.set("continu", false)
                                );
                            }
                        })
                );
    }
    @Dominion_Card(extension = "Dominion")
    public static Card Council_Room(){
        return new Card("CouncilRoom",  RegistryPrice.DominionPrice(5),CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0, 0, 4, 1, "Effect", self);
                            player.getGame().processBenefit(
                                    player,
                                    victim -> {
                                        victim.draw(1);
                                        player.log(String.format("Benefit %s : %s donne 1 une carte à %s", self.getName().toUpperCase(), player.getName(), victim.getName()));
                                    });
                        })
                );
    }
    @Dominion_Card(extension = "Dominion")
    public static Card Festival(){
        return new Card("Festival", RegistryPrice.DominionPrice(5),CardType.ACTION)
                .setup(config -> config.registerSimpleAction(0,2,1,2));
    }
    @Dominion_Card(extension = "Dominion", pileType = PileType.VICTORY)
    public static Card Gardens(){
        return new Card("Gardens",  RegistryPrice.DominionPrice(4),CardType.VICTORY)
                .setup(config -> config.score(player -> player.getAllOwnedCards().size()/10));
    }
    @Dominion_Card(extension = "Dominion")
    public static Card Harbinger(){
        return new Card("Harbinger", RegistryPrice.DominionPrice(3),CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0, 1, 1, 0, "Effect", self);

                            List<Card> distinctDiscard = player.getCopyOf(Destination.DISCARD).stream()
                                    .distinct()
                                    .toList();

                            CardUtil.executeIfSelected(
                                    () -> player.chooseCardFromList("Choisit une carte de ta défausse à mettre sur ton deck", card -> true, distinctDiscard, true),
                                    card -> {
                                        player.moveTo(card, Destination.DRAW);
                                        player.log(String.format("Action HARBINGER : %s met en pioche %s", player.getName(), card.getName().toUpperCase()));
                                    }
                            );
                        })
                );
    }
    @Dominion_Card(extension = "Dominion")
    public static Card Laboratory(){
        return new Card("Laboratory", RegistryPrice.DominionPrice(5),CardType.ACTION)
                .setup(config -> config.registerSimpleAction(1,2,0,0));
    }
    @Dominion_Card(extension = "Dominion")
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
                                            () -> player.chooseStringFromButtons("Veux tu mettre cette carte sur le côté ou la récupérer dans la main ? " + drawn, List.of(new Button("Aside", "y"), new Button("Hand", "n")),false),
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
    public static Card Market(){
       return new Card("Market", RegistryPrice.DominionPrice(5),CardType.ACTION)
                .setup(config -> config.registerSimpleAction(1,1,1,1));
    }
    @Dominion_Card(extension = "Dominion")
    public static Card Merchant(){
        return new Card("Merchant", RegistryPrice.DominionPrice(3),CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player,  0, 1, 1, 0, "Effect", self);
                            self.set("used", false);
                        })
                        .onCardPlayed((owner, victim, c) -> {
                                owner.increment(Item.MONEY,1);
                                owner.log(String.format("Trigger %s : %s récupère une pièce", config.get().getName().toUpperCase(), victim.getName()));
                                config.get().set("used", true);
                        })
                        .onCondition((event, player) -> (config.get().getFlag("used") && event.getCard().hasName("Silver") && event.getPlayer() != player))
                );
    }
    @Dominion_Card(extension = "Dominion")
    public static Card Militia() {
        return new Card("Militia", RegistryPrice.DominionPrice(4), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 2, 0, 0, 0, "Effect", self);
                            player.getGame().processMoveTo(player, self, Destination.DISCARD, 3, true);
                        })
                );
    }
    @Dominion_Card(extension = "Dominion")
    public static Card Mine(){
        return new Card("Mine", RegistryPrice.DominionPrice(5),CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) ->
                            CardUtil.executeIfSelected(
                                    () -> player.chooseCardFromHand("Choisis un trésor un jetter", card -> card.hasType(CardType.TREASURE), true),
                                    card -> {
                                        player.moveToTrash(card);
                                        CardUtil.executeIfSelected(
                                                () -> CardUtil.gainFromSupply(
                                                        player,
                                                        "Choisi une trésor (Max " + (card.getCost()+2) + " pièces)",
                                                        filter -> filter.hasType(CardType.TREASURE) && filter.getCost() - filter.getCost() < 3,
                                                        Destination.HAND,
                                                        false),
                                                gained -> player.log(String.format("Action %s : %s jette %s pour %s", self.getName().toUpperCase(), player.getName(), card.getName().toUpperCase(), gained.getName().toUpperCase()))
                                        );
                                    }
                            )
                        )
                );
    }
    @Dominion_Card(extension = "Dominion")
    public static Card Moat(){
        return new Card("Moat", RegistryPrice.DominionPrice(2),CardType.ACTION,CardType.REACTION)
                .setup(config -> config
                        .registerSimpleAction(2,0,0,0)
                        .immunity(new TriggerComponent.Immunity() {
                            @Override
                            public boolean revealed(Player player, Card self) {
                                self.set("used", false);
                                CardUtil.executeOrOtherwise(
                                        () -> player.chooseStringFromButtons(String.format("Révèle %s pour te protéger", self.getName().toUpperCase()), List.of(new Button("reveal", "y"), new Button("keep", "n")), true),
                                        "y"::equals,
                                        choice ->{
                                            player.log(player.getName() + "révèle Moat");
                                           self.set("used", true);
                                            },
                                        () -> {}
                                );
                                return self.get("used", Boolean.class);
                            }
                        })
                        .onCondition((event, player) -> event.getCard().hasType(CardType.ATTACK) &&  config.get().getFlag("used"))
                );
    }
    @Dominion_Card(extension = "Dominion")
    public static Card MoneyLender(){
        return new Card("Moneylender", RegistryPrice.DominionPrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) ->
                                CardUtil.executeIfSelected(
                                    () -> player.chooseCardFromHand("Choisis de jetter un Copper pour gagner 3 pièces", card -> card.hasName("Copper"), true),
                                        card -> {
                                        player.moveToTrash(card);
                                        player.increment(Item.MONEY,3);
                                        player.log(String.format("%s écarte un CUIVRE pour +3 pièces", player.getName()));
                                        })
                        )
                );
    }
    @Dominion_Card(extension = "Dominion")
    public static Card Poacher(){
        return new Card("Poacher", RegistryPrice.DominionPrice(4), CardType.ACTION)
                .setup(config -> config
                    .onPlay((player, self) ->{
                        Number emptyPile = GameStat.emptyPiles.get();
                        CardUtil.TriggerEffect(player, 1,1,1,0,"Effect", self);
                        player.discardFromHand(emptyPile.intValue());
                    })
                );
    }
    @Dominion_Card(extension = "Dominion")
    public static Card Remodel(){
        return new Card("Remodel", RegistryPrice.DominionPrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) ->
                                CardUtil.executeIfSelected(
                                        () -> player.chooseCardFromHand("Jette une carte de ta main pour en récupèrer une nouvelle coutant jusqu'à 2 pièce de plus", false),
                                        card ->{ player.moveToTrash(card);
                                        CardUtil.gainFromSupply(
                                                player,
                                                "Choisi une carte (Max " + (card.getCost()+2) + " pièces)",
                                                filter -> filter.getCost() - card.getCost() <=2,
                                                Destination.DISCARD,
                                                false );}
                                )
                        )
                );

    }
    @Dominion_Card(extension = "Dominion")
    public static Card Sentry() {
        return new Card("Sentry", RegistryPrice.DominionPrice(5), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0, 1, 1, 0, "Effect", self);

                            List<Card> view = CardUtil.getTopCards(player, 2);

                            while (!view.isEmpty()) {
                                List<Button> actions = List.of(new Button("Trash", "t"), new Button("Discard", "d"), new Button("Done", "x"));

                                String choice = player.chooseStringFromButtons("Sentinelle : Écarter ou Défausser ? " + view, actions, false);

                                if ("x".equals(choice)) break;

                                CardUtil.executeIfSelected(
                                        () -> player.chooseCardFromList("Quelle carte ?", card -> true, view, true),
                                        selected -> {
                                            if ("t".equals(choice)) player.moveToTrash(selected);
                                            else player.discard(selected);
                                            view.remove(selected);
                                        }
                                );
                            }

                            while (!view.isEmpty()) {
                                CardUtil.executeIfSelected(
                                        () -> player.chooseCardFromList("Ordre du Deck (La prochaine sera sur le dessus)", card -> true, view, false),
                                        selected -> {
                                            player.moveTo(selected, Destination.DRAW);
                                            view.remove(selected);
                                        }
                                );
                            }
                        })
                );
    }

    @Dominion_Card(extension = "Dominion")
    public static Card Smithy(){
        return new Card("Smithy", RegistryPrice.DominionPrice(4), CardType.ACTION)
                .setup(config -> config.registerSimpleAction(3,0,0,0)
                );
    }

    @Dominion_Card(extension = "Dominion")
    public static Card Throne_Room() {
        return new Card("ThroneRoom", RegistryPrice.DominionPrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            self.set("justPlayed", true);
                            self.set("linkedCard", null);

                            CardUtil.executeIfSelected(
                                    () -> player.chooseCardFromHand("Action à doubler", c -> c.hasType(CardType.ACTION), true),
                                    selected -> {
                                        player.log(player.getName() + " Throne Room -> " + selected.getName());

                                        player.playCard(selected);
                                        player.increment(Item.ACTION_PLAYED, 1);
                                        player.triggerEvent(TriggerComponent.OnCardPlayed.class, new Event(selected, null, player));
                                        selected.play(player);
                                        if (selected.hasComponent(DurationComponent.class)) {
                                            self.set("linkedCard", selected);
                                            self.set("justPlayed", false);
                                        }

                                    }
                            );
                        })
                        .onDurationWithTrigger((player, self) -> {
                            Card selected = self.get("linkedCard", Card.class);
                            if (selected != null) {
                                selected.as(DurationComponent.class).ifPresent(d -> {
                                    player.log("Throne Room relance l'effet de " + selected.getName());
                                    d.execute(player, selected);
                                });
                                self.set("linkedCard", null);
                            }
                        }, self -> self.getFlag("justPlayed"))
                );
    }
    @Dominion_Card(extension = "Dominion")
    public static Card Vassal(){
        return new Card("Vassal", RegistryPrice.DominionPrice(3), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 2, 0, 0, 0, "Effect", self);

                            Card top = player.getCardFromDeck();
                            if(top == null) return;

                            if(top.hasType(CardType.ACTION)){
                                CardUtil.executeOrOtherwise(
                                        () -> player.chooseStringFromButtons("Veux tu jouer la carte", yesOrNo, true ),
                                        "y"::equals,
                                        choice-> player.playCard(top),
                                        () -> player.moveTo(top, Destination.DISCARD)
                                );
                            }else player.moveTo(top, Destination.DISCARD);
                        })
                );
    }

    @Dominion_Card(extension = "Dominion")
    public static Card Village(){
        return new Card("Village", RegistryPrice.DominionPrice(3), CardType.ACTION)
                .setup(config -> config.registerSimpleAction(1, 2, 0, 0));
    }

    @Dominion_Card(extension = "Dominion")
    public static Card Witch(){
        return new Card("Witch", RegistryPrice.DominionPrice(3), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                    .onPlay((player, self) -> {
                        CardUtil.TriggerEffect(player, 0, 0, 2, 0, "Effect", self);
                        player.getGame().processGain(player, self, Destination.DISCARD, "Curse");
                    })
                );
    }

    @Dominion_Card(extension = "Dominion")
    public static Card Workshop(){
        return new Card("Workshop", RegistryPrice.DominionPrice(3), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.gainFromSupply(player, "Choisit une carte ( Max 4 pièces )", card -> card.getCost() <= 4, Destination.DISCARD,  false);
                        })
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
        return new Card("Chancellor", RegistryPrice.DominionPrice(3), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 2, 0, 0, 0, "Effect", self);
                            CardUtil.executeOrOtherwise(
                                    () -> player.chooseStringFromButtons("Veux-tu vider ta pioche dans la défausse ?", yesOrNo, true),
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
                            player.moveToTrash(self);
                            CardUtil.gainFromSupply(player, "Choisit une carte de la réserve ( max 5 pièce )", filter -> filter.getCost()<=5, Destination.DISCARD, false);
                        })
                );
    }
    @Dominion_Card(extension = "Dominion")
    public static Card Spy(){
        return new Card("Spy", RegistryPrice.DominionPrice(4), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0, 1, 1, 0, "Effect", self);

                            Consumer<Player> action = p -> {
                                Card c =  p.getCardFromDeck();
                                CardUtil.executeOrOtherwise(
                                    () -> player.chooseStringFromButtons("Discard the card or do nothing (your choice) " + c.getName(), yesOrNo, true),
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
                                    p.moveToTrash(chosen);

                                    Card Final = chosen;
                                    CardUtil.executeOrOtherwise(
                                            () -> player.chooseStringFromButtons("Voulez vous récupérer la carte " + Final.getName(), yesOrNo, true),
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
        return new Card("Woodcutter", RegistryPrice.DominionPrice(3), CardType.ACTION)
                .setup(config -> config.registerSimpleAction(0,0,1,2));
    }



}
