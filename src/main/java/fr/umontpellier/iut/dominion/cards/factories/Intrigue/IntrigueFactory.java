package fr.umontpellier.iut.dominion.cards.factories.Intrigue;

import fr.umontpellier.iut.dominion.*;
import fr.umontpellier.iut.dominion.Annotation.Dominion_Card;
import fr.umontpellier.iut.dominion.Annotation.InSet;
import fr.umontpellier.iut.dominion.Annotation.PileType;
import fr.umontpellier.iut.dominion.Player.Player;
import fr.umontpellier.iut.dominion.cards.*;

import java.util.*;
import java.util.function.BiConsumer;

import static fr.umontpellier.iut.dominion.Button.yesOrNo;
import static fr.umontpellier.iut.dominion.cards.factories.FactoryUtil.*;

public class IntrigueFactory {

    @Dominion_Card(extension = "Intrigue")
    public static Card Baron(){
        Bonus play = Bonus.empty().with(Item.BUY, 1);

        return new Card("Baron", RegistryPrice.IntriguePrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay( (player, self) ->{
                                CardUtil.TriggerEffect(player, EFFECT, self, play);
                                 player.chooseCardFromHand("You may choose an Estate to discard ", c -> c.hasName("Estate"), true)
                                         .ifPresentOrElse(card -> {
                                            player.discard(card);
                                            player.increment(Item.MONEY, 4);
                                            player.log("Action %s : %s discard an Estate".formatted(self.getName().toUpperCase(), player.toLog()));
                                            }
                                            , () -> CardUtil.gainFromSupply(player, "Estate", Destination.DISCARD, false)
                                         );}
                        )
                );
    }
    @Dominion_Card(extension = "Intrigue")
    @InSet(value = {"Grand Scheme"})
    public static Card Bridge(){
        Bonus play = Bonus.empty().with(Item.BUY, 1).with(Item.MONEY, 1);
        return new Card("Bridge", RegistryPrice.IntriguePrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self ,play);
                            GameStat.reduction.set(GameStat.reduction.get() + 1);
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Conspirator(){
        return new Card("Conspirator", RegistryPrice.IntriguePrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            int value = player.getValueOf(Item.ACTION_PLAYED);
                            int toeffect = 0;
                            if(value > 3){
                                toeffect = 1;
                            }
                            Bonus play = Bonus.empty().with(Item.ACTION, toeffect).with(Item.MONEY, 2).draw(toeffect);
                            CardUtil.TriggerEffect(player, EFFECT, self, play);

                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    @InSet(value = {"Underlings"})
    public static Card Courtier() {
        return new Card("Courtier", RegistryPrice.IntriguePrice(5), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> player.chooseCardFromHand("Reveal a card for Courtier", false).ifPresent(
                               revealedCard -> {
                                   player.log("%s reveals %s".formatted(player.toLog(), revealedCard.getName()));

                                   int count = revealedCard.numberType();

                                   // On prépare la liste des options disponibles
                                   List<Button> options = new ArrayList<>(List.of(
                                           new Button("+1 Action", "action"),
                                           new Button("+1 Buy", "buy"),
                                           new Button("+3 Money", "money"),
                                           new Button("Gain a Gold", "gold")
                                   ));

                                   for (int i = 0; i < count; i++) {
                                       if (options.isEmpty()) break;

                                       String choice = player.chooseWhatToDo(
                                               "Choice " + (i + 1) + "/" + count,
                                               List.of(revealedCard),
                                               options,
                                               false
                                       );

                                       switch (choice) {
                                           case "action" -> player.increment(Item.ACTION, 1);
                                           case "buy" -> player.increment(Item.BUY, 1);
                                           case "money" -> player.increment(Item.MONEY, 3);
                                           case "gold" -> CardUtil.gainFromSupply(player, "Gold", Destination.DISCARD, false);
                                       }

                                       options.removeIf(b -> b.value().equals(choice));
                                   }
                               }
                       ))
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Courtyard(){
        Bonus play = Bonus.empty().draw(3);
        return new Card("Courtyard", RegistryPrice.IntriguePrice(2), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player,  EFFECT, self, play);
                            player.chooseCardFromHand("Choose a card to put onto deck", false)
                                    .ifPresent(card -> player.moveTo(card, Destination.DRAW));
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    @InSet(value = {"Underlings", "Deconstruction"})
    public static Card Diplomat(){
        return new Card("Diplomat", RegistryPrice.IntriguePrice(4), CardType.ACTION, CardType.REACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            int value = 0;
                            Bonus play = Bonus.empty().draw(2);
                            if(player.getCopyOf(Destination.HAND).size() <= 5) value = 2;
                            play.with(Item.ACTION, value);
                            CardUtil.TriggerEffect(player, EFFECT, self, play);
                        })
                        .onCardPlayed((event, owner) -> {
                            owner.log("reveals Diplomat");
                            owner.draw(2);
                            owner.discardFromHand(3);
                        })
                        .cardPlayedCondition((event, player) -> {
                            List <Card> hand = player.getCopyOf(Destination.HAND);
                            return event.getCard().hasType(CardType.ATTACK)
                                    && hand.contains(config.get())
                                    && hand.size() >= 5
                                    && event.getPlayer() != player;
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue", pileType = PileType.VICTORY)
    @InSet(value = {"Underlings"})
    public static Card Duke(){
        return new Card("Duke", RegistryPrice.IntriguePrice(5), CardType.VICTORY)
                .setup(config -> config
                        .score(player ->  {
                            Number number = player.getCopyOf(Destination.HAND).stream().filter(card -> card.hasName("Duchy")).count();
                            return number.intValue();
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue", pileType = PileType.VICTORY)
    @InSet(value = {"Deconstruction"})
    public static Card Farm(){
        Bonus play =  Bonus.empty().with(Item.MONEY, 2);
        return new Card("Farm", RegistryPrice.IntriguePrice(6), CardType.TREASURE, CardType.VICTORY)
                .setup(config -> config
                        .onPlay((player, self) -> CardUtil.TriggerEffect(player,  EFFECT, self, play))
                        .score(player -> 2)
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Ironworks(){
        return new Card("Ironworks", RegistryPrice.IntriguePrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            Card card = CardUtil.gainFromSupply(player, "Choose a card from supply costing up to 4", c -> c.isAtMost(4), Destination.DISCARD, false);
                            if(card == null)return;
                            Bonus play = Bonus.empty()
                                    .with(Item.ACTION,card.hasType(CardType.ACTION) ? 1 : 0)
                                    .with(Item.MONEY, card.hasType(CardType.TREASURE) ? 1 : 0)
                                    .draw(card.hasType(CardType.VICTORY) ? 1 : 0);

                            CardUtil.TriggerEffect(player, ACTION, self, play);
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    @InSet(value = {"Deconstruction"})
    public static Card Lurker(){
        Bonus action = Bonus.empty().with(Item.ACTION,1);
        return new Card("Lurker", RegistryPrice.IntriguePrice(2), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, action);
                            String choice = player.chooseStringFromButtons("Choose, trash an Action from supply or take one from trash", List.of(new Button("Trash card", "Trash"), new Button("Gained", "gained")), false);
                            if("Trash".equals(choice)) {
                                player.chooseCardFromSupply("Trash an Action card from the supply", card -> card.hasType(CardType.ACTION), false)
                                        .ifPresent(player::trash);

                            }
                            else if("gained".equals(choice)) {
                                List<Card> trashedAction = player.getGame().getTrashCards().stream().filter(card -> card.hasType(CardType.ACTION)).toList();
                                if(trashedAction.isEmpty()){
                                    player.log("No Action card found in trash");
                                    return;
                                }

                                player.chooseCardFromList("Choose an Action card from Trash", card -> true,trashedAction, false)
                                         .ifPresent(card -> player.gain(card, Destination.DISCARD));
                            }
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Masquerade(){
        Bonus play =  Bonus.empty().draw(2);
        return new Card("Masquerade", RegistryPrice.IntriguePrice(3), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player,  EFFECT, self, play);
                            Map<Player, Card> shares = new HashMap<>();

                            player.getGame().processGlobalEffect(player, victim ->
                                         victim.chooseCardFromHand("Masquerade : Pass to the left", false)
                                                 .ifPresent(card ->{
                                                    shares.put(victim, card);
                                                    victim.moveTo(card, Destination.ASIDE);
                                                })
                            );


                            shares.forEach((giver, card) -> {
                                Player receiver = player.getGame().onTheLeft(giver);
                                receiver.moveTo(card, Destination.HAND);
                                giver.log("passed a card to " + receiver.getName());
                                    }
                            );

                             player.chooseCardFromHand("You may trash a card from your hand", true)
                                     .ifPresent(player::trash);

                        })
                );
    }
    @Dominion_Card(extension = "Intrigue", pileType = PileType.VICTORY)
    @InSet(value = {"Grand Scheme"})
    public static Card Mill(){
        Bonus effect =  Bonus.empty().with(Item.ACTION,1).draw(1);
        Bonus additionalMoney = Bonus.empty().with(Item.MONEY, 2);
        return new Card("Mill", RegistryPrice.IntriguePrice(4), CardType.ACTION, CardType.VICTORY)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, effect);
                            int i;
                            for(i = 0; i < 2 && !self.getFlag("skip"); i++) {
                                self.set("skip", false);

                                player.chooseCardFromHand("Choose a card to discard (2)", true )
                                        .ifPresentOrElse(player::discard,
                                        () -> self.set("skip", true));
                            }
                            if(i == 2){
                                CardUtil.TriggerEffect(player,ACTION, self, additionalMoney);
                            }
                        })
                        .score(player -> 1)
                );
    }
    @Dominion_Card(extension = "Intrigue")
    @InSet(value = {"Grand Scheme"})
    public static Card Mining_Village(){
        Bonus actionAndCard = Bonus.empty().with(Item.ACTION,2).draw(1);
        Bonus trashAwards= Bonus.empty().with(Item.MONEY, 2);
        return new Card("MiningVillage",  RegistryPrice.IntriguePrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, actionAndCard);

                            if(!player.getCopyOf(Destination.INPLAY).contains(self)) return;

                            CardUtil.executeOrOtherwise(
                                    () -> player.chooseStringFromButtons("You may trash Mining Village to gain 2 Money", yesOrNo, true),
                                    "y"::equals,
                                    choice ->  {
                                        if(player.trash(self))
                                            CardUtil.TriggerEffect(player, "Trash Awards", self, trashAwards);
                                    },
                                    ()->{}
                            );
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Minion(){
        Bonus action = Bonus.empty().with(Item.ACTION,1);
        Bonus money = Bonus.empty().with(Item.MONEY, 2);

        BiConsumer <Player, Card> discardAndAttack = (player, self )-> {
            player.discardAll(Destination.HAND);
            player.draw(4);
            player.getGame().processAttack(
                    player,
                    self,
                    victim -> {
                        if(victim.getCopyOf(Destination.HAND).size() >= 5){
                            victim.discardAll(Destination.HAND);
                            victim.draw(4);
                        }
                    }
            );
        };

        return new Card("Minion", RegistryPrice.IntriguePrice(5), CardType.ActionAndAttack)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player,EFFECT,  self, action);
                            CardUtil.executeOrOtherwise(
                                    () -> player.chooseWhatToDo("Choose one : +2 Money or discard and Attack others",List.of(self) ,List.of(new Button("Money", "m"), Button.Discard), false),
                                    "m"::equals,
                                    choice -> CardUtil.TriggerEffect(player,"Action Money", self, money),
                                    ()-> discardAndAttack.accept(player, self)

                            );
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue", pileType = PileType.VICTORY)
    @InSet(value = {"Underlings"})
    public static Card Nobles(){
        return new Card("Nobles", RegistryPrice.IntriguePrice(6), CardType.ACTION, CardType.VICTORY)
                .setup(config -> config
                        .onPlay((player, self) ->
                            CardUtil.executeOrOtherwise(
                                    () -> player.chooseWhatToDo(" Nobles, Choose : 3 cards or 2 actions",List.of(self),  List.of(new Button("Cards", "c"), new Button("Actions", "a")), false),
                                    "c"::equals,
                                    choice -> player.draw(3),
                                    () -> player.increment(Item.ACTION, 2)
                            )
                        )
                        .score(player -> 2)
                );
    }
    @Dominion_Card(extension = "Intrigue")
    @InSet(value = {"Grand Scheme"})
    public static Card Patrol(){
        Bonus draw = Bonus.empty().draw(3);
        return new Card("Patrol", RegistryPrice.IntriguePrice(5), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {

                            CardUtil.TriggerEffect(player, EFFECT, self, draw);

                            List<Card> view = CardUtil.getTopCards(player, 4);
                            player.log("Patrol view: " + view);

                            view.stream().filter(card -> card.hasType(CardType.CURSE) || card.hasType(CardType.VICTORY)).forEach(card -> player.moveTo(card, Destination.HAND));

                            view.removeIf(card -> card.hasType(CardType.CURSE) || card.hasType(CardType.VICTORY));
                            while(!view.isEmpty()){
                                player.chooseCardFromList("Put the rest in any order in your deck", card ->true, view, false )
                                        .ifPresent(card ->{
                                            player.moveTo(card, Destination.DRAW);
                                            view.remove(card);}
                                );
                            }
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    @InSet(value = {"Underlings"})
    public static Card Pawn() {
        return new Card("Pawn", RegistryPrice.IntriguePrice(2), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            List<Button> options = new ArrayList<>(List.of(
                                    new Button("+1 Card", "card"),
                                    new Button("+1 Action", "action"),
                                    new Button("+1 Buy", "buy"),
                                    new Button("+$1", "money")
                            ));

                            for (int i = 0; i < 2; i++) {
                                final List<Button> currentOptions = new ArrayList<>(options);

                                CardUtil.executeIfSelected(
                                        () -> player.chooseWhatToDo("Pawn: Choose 2 different options ", List.of(self) ,currentOptions, false),
                                        choiceValue -> {
                                            switch (choiceValue) {
                                                case "card" -> player.draw(1);
                                                case "action" -> player.increment(Item.ACTION, 1);
                                                case "buy" -> player.increment(Item.BUY, 1);
                                                case "money" -> player.increment(Item.MONEY, 1);
                                            }

                                            options.removeIf(btn -> btn.value().equals(choiceValue));

                                            player.log("chooses " + choiceValue);
                                        }
                                );
                            }
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    @InSet(value = {"Deconstruction"})
    public static Card Replace(){
        return new Card("Replace", RegistryPrice.IntriguePrice(5), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) ->
                            player.chooseCardFromHand("Trash a card", false)
                                    .ifPresent(c -> {
                                        player.trash(c);
                                        int cost = c.getCost() + 2;
                                        Card gained = CardUtil.gainFromSupply(player, "Choose a card cost up to " + cost, card -> card.isAtMostWithBonus(c, 2), Destination.DISCARD, false);
                                        if(gained!=null){
                                            if(gained.hasType(CardType.VICTORY)){
                                                player.getGame().processGain(player, self,Destination.DISCARD, "Curse");
                                            }else{
                                                 if(!player.getCopyOf(Destination.DISCARD).contains(gained))return;
                                                 player.moveTo(gained, Destination.DRAW);
                                            }
                                        }
                                    })
                        )
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Secret_Passage(){
        Bonus drawAndAction = Bonus.empty().draw(2).with(Item.ACTION, 1);

        return new Card("SecretPassage", RegistryPrice.IntriguePrice(4), CardType.ACTION).setup(
                config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, drawAndAction);
                            Optional<Card> card = player.chooseCardFromHand("Put a card from your hand everywhere in your deck ", false);
                            card.ifPresent(value -> player.chooseCardFromList("Choose where you want to put it ( click on the card you want to place the card)", card1 -> true, player.getCopyOf(Destination.DRAW), false)
                                    .ifPresent(index -> player.putACardInDraw(value, index))
                            );
                        })
        );
    }
    @Dominion_Card(extension = "Intrigue")
    @InSet(value = {"Grand Scheme"})
    public static Card Shanty_Town(){
        Bonus action = Bonus.empty().with(Item.ACTION, 2);
        Bonus noActionInHand = Bonus.empty().draw(2);

        return new Card("ShantyTown", RegistryPrice.IntriguePrice(3), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, action);
                            boolean ActionInHand = player.getCopyOf(Destination.HAND).stream().anyMatch(card -> card.hasType(CardType.ACTION));
                            if(!ActionInHand){
                                CardUtil.TriggerEffect(player, ACTION, self, noActionInHand);
                            }
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Steward(){
        return new Card("Steward", RegistryPrice.IntriguePrice(3), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            String choice = player.chooseWhatToDo("Choose: 2 Cards or 2$ or 2 cards to trash",List.of(self),List.of(new Button("Card", "action"), new Button("Money", "money"), new Button("Trash", "trash")), false);
                            switch (choice) {
                                case "action" -> player.draw(2);
                                case "money" -> player.increment(Item.MONEY, 2);
                                case "trash" -> {
                                    for(int i =0; i < 2; i++){
                                        player.chooseCardFromHand("Choose a card to trash", false)
                                                .ifPresent(player::trash);
                                    }
                                }
                                default -> {}
                            }
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    @InSet(value = {"Deconstruction"})
    public static Card Swindler(){
        Bonus money =  Bonus.empty().with(Item.MONEY, 2);
        return new Card("Swindler", RegistryPrice.IntriguePrice(3), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player,  EFFECT, self, money);
                            player.getGame().processAttack(
                                    player,
                                    self,
                                    vi -> {
                                        Card toTrash = vi.getCardFromDeck();
                                        if(toTrash!=null){
                                            vi.trash(toTrash);
                                            Optional<Card> toGained = player.chooseCardFromSupply("You can choose a card that cost the same as the card your oppenent trashed (" + toTrash.getCost() + ")", card -> card.getCost() == toTrash.getCost(), false);
                                            toGained.ifPresent(card -> vi.gain(card, Destination.DISCARD));
                                        }
                                    }
                            );
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Torturer(){
        Bonus draw = Bonus.empty().draw(3);
        return new Card("Torturer", RegistryPrice.IntriguePrice(5), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, draw);
                            player.getGame().processAttack(
                                    player, self,
                                    vi ->
                                        CardUtil.executeOrOtherwise(
                                                () -> vi.chooseWhatToDo("Choose:Discard or gain a curse", List.of(self),List.of(Button.Discard, new Button("Curse", "c")), false),
                                                "d"::equals,
                                                choice -> vi.discardFromHand(2),
                                                () -> CardUtil.gainFromSupply(vi, "Curse", Destination.HAND, false)
                                        )
                            );
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Trading_Post(){
        return new Card("TradingPost", RegistryPrice.IntriguePrice(3), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                                self.set("continu", true);
                                int i;

                                for(i = 0; i<2 && self.getFlag("continu"); i++  ){
                                    player.chooseCardFromHand("Choose " + (2 - i) + " to trash to gain a silver", true)
                                             .ifPresentOrElse(player::trash, () -> self.set("continu", false)
                                    );
                                }

                                if(i==2){
                                    CardUtil.gainFromSupply(player, "Silver", Destination.HAND, false);
                                }
                        })
                );
    }

    @Dominion_Card(extension = "Intrigue")
    public static Card Upgrade(){
        Bonus ActionAndCard = Bonus.empty().with(Item.ACTION, 1).draw(1);
        return new Card("Upgrade", RegistryPrice.IntriguePrice(5), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, ActionAndCard);
                             player.chooseCardFromHand("Choose a card to trash", false)
                                     .ifPresent(card -> {
                                        player.trash(card);
                                        CardUtil.gainFromSupply(player, "Choose a card costing exactly 1$ more that " + card + "(" + card.getCost() + 1 + "$ )", gained -> gained.isEqualWithBonus(card, 1) , Destination.HAND, false);
                                    }
                            );
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Wishing_Well(){
        Bonus ActionAndCard = Bonus.empty().with(Item.ACTION, 1).draw(1);
        return new Card("WishingWell", RegistryPrice.IntriguePrice(3), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, ActionAndCard);
                            String choice = player.choose("On channel,  compute a name of a card", false);
                            Card revealed = player.getCardFromDeck();
                            if (revealed != null && !choice.isEmpty()) {
                                String formattedChoice = choice.substring(0, 1).toUpperCase() + choice.substring(1).toLowerCase();

                                if (revealed.hasName(formattedChoice)) {
                                    player.moveTo(revealed, Destination.HAND);
                                }
                            }
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Coppersmith(){
        return new Card("Coppersmith", RegistryPrice.IntriguePrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            int i = self.getValue("increment").intValue();
                            self.set("increment", i + 1);})
                        .onCardPlayed( (event, owner) ->owner.increment(Item.MONEY, config.get().getValue("increment").intValue()))
                        .cardPlayedCondition((event, player) -> player == event.getPlayer() && event.getCard().hasName("Copper"))
                );
    }
    @Dominion_Card(extension = "Intrigue", pileType = PileType.VICTORY)
    public static Card GreatHall(){
        Bonus actionAndCard = Bonus.empty().with(Item.ACTION, 1).draw(1);
        return new Card("GreatHall", RegistryPrice.IntriguePrice(4), CardType.ACTION, CardType.VICTORY)
                .setup(config -> config
                        .registerSimpleAction(actionAndCard)
                        .score(player -> 1)
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Saboteur(){
        return new Card("Saboteur", RegistryPrice.IntriguePrice(5), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> player.getGame().processAttack(
                                player,
                                self,
                                vi -> {
                                        List<Card> aside = new ArrayList<>();
                                        Card c;
                                    while (true) {
                                        c = player.getCardFromDeck();
                                        if (c == null) break;
                                        c.moveTo(aside, null);
                                        if (c.getCost() >= 3) {
                                            break;
                                        }
                                    }
                                        if(c!= null && c.getCost() >= 3){
                                            player.log( vi.getName() + "Revealed Card : " + aside);
                                            player.trash(c);
                                            Card finalCard = c;
                                            CardUtil.gainFromSupply(player, "Choose an card cost up (" + (c.getCost() - 2) +")", card -> card.isAtMostWithBonus(finalCard, -2), Destination.DISCARD, false);
                                        }
                                        new ArrayList<>(aside).forEach(card -> player.moveTo(card, Destination.DISCARD));
                                }
                        ))
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Scout(){
        Bonus action =  Bonus.empty().with(Item.ACTION, 1);
        return new Card("Scout", RegistryPrice.IntriguePrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, action);
                            List<Card> view = CardUtil.getTopCards(player, 4);
                            while(view.stream().anyMatch(card -> card.hasType(CardType.VICTORY))){
                                player.chooseCardFromList("Choose Card Victory", c -> c.hasType(CardType.VICTORY), view, false)
                                        .ifPresent(card -> {
                                            player.moveTo(card, Destination.HAND);
                                            view.remove(card);
                                        }
                                );
                            }
                            while(!view.isEmpty()){
                                player.chooseCardFromList("Move other cards in your hand", c -> true, view, false)
                                        .ifPresent(card -> {
                                            player.moveTo(card, Destination.DRAW);
                                            view.remove(card);
                                        }
                                );
                            }
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Secret_Chamber(){
        return new Card("SecretChamber", RegistryPrice.IntriguePrice(2), CardType.ACTION, CardType.REACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                                self.set("stop", false);
                                while(!player.getCopyOf(Destination.HAND).isEmpty() && !self.getFlag("stop")){
                                    player.chooseCardFromHand("Choose an card to discard from your hand ( you can stop )", true)
                                            .ifPresentOrElse(card -> {
                                                player.moveTo(card, Destination.DISCARD);
                                                player.increment(Item.MONEY, 1);
                                            },
                                            () -> self.set("stop", true)
                                    );
                                }
                        })
                        .onCardPlayed((event, owner) -> {
                            config.get().set("last_ID", event.getId());
                            owner.chooseCardFromHand("Reveal Secret_Chamber", card -> card.hasName("SecretChamber"), true)
                                    .ifPresent(card -> {
                                        owner.draw(2);
                                        for(int i = 0; i <2; i++){
                                            owner.chooseCardFromHand("Choose card to put on your draw (2)", false)
                                                    .ifPresent(draw -> owner.moveTo(draw, Destination.DRAW));
                                        }
                                    }
                            );
                        })
                        .cardPlayedCondition((event, player) -> event.getCard().hasType(CardType.ATTACK) && player != event.getPlayer() && !config.get().getValue("last_ID").equals(event.getId()))
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Tribute(){
        return new Card("Tribute", RegistryPrice.IntriguePrice(5), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            Player left = player.getGame().onTheLeft(player);
                            if(left == null) return;
                            List<Card> view = CardUtil.getTopCards(left, 2);
                            view.forEach(card -> {
                                left.moveTo(card, Destination.DISCARD);
                                if(card.hasType(CardType.VICTORY)) player.draw(2);
                                if(card.hasType(CardType.TREASURE)) player.increment(Item.MONEY, 2);
                                if(card.hasType(CardType.ACTION)) player.increment(Item.ACTION, 2);
                            });

                        })
                );
    }



}
