package fr.umontpellier.iut.dominion.cards.Intrigue;

import fr.umontpellier.iut.dominion.*;
import fr.umontpellier.iut.dominion.Annotation.Dominion_Card;
import fr.umontpellier.iut.dominion.Annotation.PileType;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.GameStat;
import fr.umontpellier.iut.dominion.cards.RegistryPrice;

import java.util.*;

public class IntrigueFactory {
    public static List<Button> yesOrNo = List.of(new Button("Yes", "y"), new Button("No", "n"));

    @Dominion_Card(extension = "Intrigue")
    public static Card Baron(){
        return new Card("Baron", RegistryPrice.IntriguePrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay( (player, self) ->{
                                CardUtil.TriggerEffect(player, 0, 0, 0, 1, "Effect", self);
                                CardUtil.executeOrOtherwise(
                                        () -> player.chooseCardFromHand("You may choose an Estate to discard ", c -> c.hasName("Estate"), true),
                                        Objects::nonNull,
                                        card -> {
                                            player.moveTo(card, Destination.DISCARD);
                                            player.increment(Item.MONEY, 4);
                                            player.log("Action %s : %s discard an Estate".formatted(self.getName().toUpperCase(), player.toLog()));
                                        },
                                        () -> CardUtil.gainFromSupply(player, "Estate", Destination.DISCARD, false)
                                );}
                        )
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Bridge(){
        return new Card("Bridge", RegistryPrice.IntriguePrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 1,0,0,1, "Effect", self);
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
                            CardUtil.TriggerEffect(player, 2,toeffect,toeffect,0, "Effect", self);

                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Courtier() {
        return new Card("Courtier", RegistryPrice.IntriguePrice(5), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.executeIfSelected(
                                    () -> player.chooseCardFromHand("Reveal a card for Courtier", false),

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

                                            String choice = player.chooseStringFromButtons(
                                                    "Choice " + (i + 1) + "/" + count,
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
                            );
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Courtyard(){
        return new Card("Courtyard", RegistryPrice.IntriguePrice(2), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0,0,3,0, "Effect", self);
                            CardUtil.executeIfSelected(
                                    () -> player.chooseCardFromHand("Choose a card to put onto deck", false),
                                    card -> player.moveTo(card, Destination.DRAW)
                            );
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Diplomat(){
        return new Card("Diplomat", RegistryPrice.IntriguePrice(4), CardType.ACTION, CardType.REACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            int value = 0;
                            if(player.getCopyOf(Destination.HAND).size() <= 5) value = 2;
                            CardUtil.TriggerEffect(player, 0,value,2,0, "Effect", self);
                        })
                        .onCardPlayed((owner, victim, event) -> {
                            owner.log("reveals Diplomat");
                            owner.draw(2);
                            owner.discardFromHand(3);
                        })
                        .onCondition((event, player) -> {
                            List <Card> hand = player.getCopyOf(Destination.HAND);
                            return event.getCard().hasType(CardType.ATTACK)
                                    && hand.contains(config.get())
                                    && hand.size() >= 5
                                    && event.getPlayer() != player;
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue", pileType = PileType.VICTORY)
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
    public static Card Farm(){
        return new Card("Farm", RegistryPrice.IntriguePrice(6), CardType.TREASURE, CardType.VICTORY)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 2,0,0,0, "Effect", self);
                        })
                        .score(player -> 2)
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Ironworks(){
        return new Card("Ironworks", RegistryPrice.IntriguePrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            Card card = CardUtil.gainFromSupply(player, "Choose a card from supply costing up to 4", c -> c.getCost() <= 4, Destination.DISCARD, false);
                            if(card == null)return;
                            CardUtil.TriggerEffect(player, card.hasType(CardType.TREASURE) ? 1 : 0,card.hasType(CardType.ACTION) ? 1 : 0,card.hasType(CardType.VICTORY) ? 1 : 0,0, "Effect", self);
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Lurker(){
        return new Card("Lurker", RegistryPrice.IntriguePrice(2), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0,1,0,0, "Effect", self);
                            String choice = player.chooseStringFromButtons("Choose, trash an Action from supply or take one from trash", List.of(new Button("Trash card", "Trash"), new Button("Gained", "gained")), false);
                            if("Trash".equals(choice)) {
                                CardUtil.executeIfSelected(
                                        () -> player.chooseCardFromSupply("Trash an Action card from the supply", card -> card.hasType(CardType.ACTION), false),
                                        player::moveToTrash
                                );
                            }else if("gained".equals(choice)) {
                                List<Card> trashedAction = player.getGame().getTrashCards().stream().filter(card -> card.hasType(CardType.ACTION)).toList();
                                if(trashedAction.isEmpty()){
                                    player.log("No Action card found in trash");
                                    return;
                                }
                                CardUtil.executeIfSelected(
                                        () -> player.chooseCardFromList("Choose an Action card from Trash", card -> true,trashedAction, false),
                                        card -> player.gain(card, Destination.DISCARD)

                                );
                            }
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Masquerade(){
        return new Card("Masquerade", RegistryPrice.IntriguePrice(3), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0,0,2,0, "Effect", self);
                            Map<Player, Card> shares = new HashMap<>();

                            player.getGame().processGlobalEffect(player, victim -> {
                                        CardUtil.executeIfSelected(
                                                () -> victim.chooseCardFromHand("Masquerade : Pass to the left", false),
                                                card ->{
                                                    shares.put(victim, card);
                                                    victim.moveTo(card, Destination.ASIDE);
                                                }
                                        );}
                                    );


                            shares.forEach((giver, card) -> {
                                Player receiver = player.getGame().onTheLeft(giver);
                                receiver.moveTo(card, Destination.HAND);
                                giver.log("passed a card to " + receiver.getName());
                                    }
                            );


                            CardUtil.executeIfSelected(
                                    () -> player.chooseCardFromHand("You may trash a card from your hand", true),
                                    player::moveToTrash
                            );

                        })
                );
    }
    @Dominion_Card(extension = "Intrigue", pileType = PileType.VICTORY)
    public static Card Mill(){
        return new Card("Mill", RegistryPrice.IntriguePrice(4), CardType.ACTION, CardType.VICTORY)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0,1,1,0, "Effect", self);
                            int i;
                            for(i = 0; i < 2 && !self.getFlag("skip"); i++) {
                                self.set("skip", false);
                                CardUtil.executeOrOtherwise(
                                        () -> player.chooseCardFromHand("Choose a card to discard (2)", true ),
                                        Objects::nonNull,
                                        player::discard,
                                        () -> self.set("skip", true)
                                );
                            }
                            if(i == 2){
                                CardUtil.TriggerEffect(player, 2,0,0,0, "Action", self);
                            }
                        })
                        .score(player -> 1)
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Mining_Village(){
        return new Card("MiningVillage",  RegistryPrice.IntriguePrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0,2,1,0, "Effect", self);

                            if(!player.getCopyOf(Destination.INPLAY).contains(self)) return;

                            CardUtil.executeOrOtherwise(
                                    () -> player.chooseStringFromButtons("You may trash Mining Village to gain 2 Money", yesOrNo, true),
                                    "y"::equals,
                                    choice ->  {
                                        player.moveToTrash(self);
                                        CardUtil.TriggerEffect(player, 2,0,0,0, "Trash Awards", self);
                                    },
                                    ()->{}
                            );
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Minion(){
        return new Card("Minion", RegistryPrice.IntriguePrice(5), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0,1,0,0, "Effect", self);
                            CardUtil.executeOrOtherwise(
                                    () -> player.chooseStringFromButtons("Choose one : +2 Money or discard and Attack others", List.of(new Button("Money", "m"), new Button("Discard", "d")), false),
                                    "m"::equals,
                                    choice -> CardUtil.TriggerEffect(player, 2,0,0,0, "Action Money", self),
                                    ()-> {
                                        player.getCopyOf(Destination.HAND).forEach(card ->player.moveTo(card, Destination.DISCARD));
                                        player.draw(4);
                                        player.getGame().processAttack(
                                                player,
                                                self,
                                                victim -> {
                                                    if(victim.getCopyOf(Destination.HAND).size() >= 5){
                                                        victim.getCopyOf(Destination.HAND).forEach(c -> victim.moveTo(c, Destination.DISCARD));
                                                        victim.draw(4);
                                                    }
                                                }
                                        );
                                    }

                            );
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue", pileType = PileType.VICTORY)
    public static Card Nobles(){
        return new Card("Nobles", RegistryPrice.IntriguePrice(6), CardType.ACTION, CardType.VICTORY)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.executeOrOtherwise(
                                    () -> player.chooseStringFromButtons(" Nobles, Choose : 3 cards or 2 actions", List.of(new Button("Cards", "c"), new Button("Actions", "a")), false),
                                    "c"::equals,
                                    choice -> player.draw(3),
                                    () -> player.increment(Item.ACTION, 2)
                            );
                        })
                        .score(player -> 2)
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Patrol(){
        return new Card("Patrol", RegistryPrice.IntriguePrice(5), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0,0,3,0, "Effect", self);
                            List<Card> view = CardUtil.getTopCards(player, 4);
                            player.log("Patrol view: " + view);
                            view.stream().filter(card -> card.hasType(CardType.CURSE) || card.hasType(CardType.VICTORY)).forEach(card ->{
                                player.moveTo(card, Destination.HAND);
                            });
                            view.removeIf(card -> card.hasType(CardType.CURSE) || card.hasType(CardType.VICTORY));
                            while(!view.isEmpty()){
                                CardUtil.executeIfSelected(
                                        () -> player.chooseCardFromList("Put the rest in any order in your deck", card ->true, view, false ),
                                        card ->{
                                            player.moveTo(card, Destination.DRAW);
                                            view.remove(card);}
                                );
                            }
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
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
                                        () -> player.chooseStringFromButtons("Pawn: Choose 2 different options ", currentOptions, false),
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
    public static Card Replace(){
        return new Card("Replace", RegistryPrice.IntriguePrice(5), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                           Card c = player.chooseCardFromHand("Trash a card", false);
                           if(c!=null){
                               player.moveToTrash(c);
                               int cost = c.getCost();
                               Card gained = CardUtil.gainFromSupply(player, "Choose a card cost up +" + cost, card -> card.getCost() <= card.getCost()+cost, Destination.DISCARD, false);
                               if(gained!=null){
                                   if(gained.hasType(CardType.VICTORY)){
                                       player.getGame().processGain(player, self,Destination.DISCARD, "Curse");
                                   }else{
                                       if(!player.getCopyOf(Destination.DISCARD).contains(gained))return;
                                       player.moveTo(gained, Destination.DRAW);
                                   }
                               }
                           }
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Secret_Passage(){
        return new Card("SecretPassage", RegistryPrice.IntriguePrice(4), CardType.ACTION).setup(
                config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0,1,2,0, "Effect", self);
                            CardUtil.executeIfSelected(
                                    () -> player.chooseCardFromHand("Put a card from your hand everywhere in your deck ", false),
                                    card -> CardUtil.executeIfSelected(
                                            () -> player.chooseCardFromList("Choose where you want to put it ( click on the card you want to place the card)", card1 -> true, player.getCopyOf(Destination.DRAW), false),
                                            index -> {
                                                player.putACardInDraw(card, index);
                                            }
                                    )
                            );
                        })
        );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Shanty_Town(){
        return new Card("ShantyTown", RegistryPrice.IntriguePrice(3), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0,2,0,0, "Effect", self);
                            boolean ActionInHand = player.getCopyOf(Destination.HAND).stream().anyMatch(card -> card.hasType(CardType.ACTION));
                            if(!ActionInHand){
                                CardUtil.TriggerEffect(player, 0,0,2,0, "Action", self);
                            }
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Steward(){
        return new Card("Steward", RegistryPrice.IntriguePrice(3), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            String choice = player.chooseStringFromButtons("Choose: 2 Cards or 2$ or 2 cards to trash", List.of(new Button("Card", "action"), new Button("Money", "money"), new Button("Trash", "trash")), false);
                            switch (choice) {
                                case "action" -> player.draw(2);
                                case "money" -> player.increment(Item.MONEY, 2);
                                case "trash" -> {
                                    for(int i =0; i < 2; i++){
                                        CardUtil.executeIfSelected(
                                                () -> player.chooseCardFromHand("Choose a card to trash", false),
                                                player::moveToTrash
                                        );
                                    }
                                }
                                default -> {}
                            }
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Swindler(){
        return new Card("Swindler", RegistryPrice.IntriguePrice(3), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 2,0,0,0, "Effect", self);
                            player.getGame().processAttack(
                                    player,
                                    self,
                                    vi -> {
                                        Card toTrash = vi.getCardFromDeck();
                                        if(toTrash!=null){
                                            vi.moveToTrash(toTrash);
                                            Card toGained = player.chooseCardFromSupply("You can choose a card that cost the same as the card your oppenent trashed (" + toTrash.getCost() + ")", card -> card.getCost() == toTrash.getCost(), false);
                                            if(toGained!=null){
                                                vi.gain(toGained, Destination.DISCARD);
                                            }

                                        }
                                    }
                            );
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Torturer(){
        return new Card("Torturer", RegistryPrice.IntriguePrice(5), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0,0,3,0, "Effect", self);
                            player.getGame().processAttack(
                                    player, self,
                                    vi ->
                                        CardUtil.executeOrOtherwise(
                                                () -> vi.chooseStringFromButtons("Choose:Discard or gain a curse", List.of(new Button("Discard", "d"), new Button("Curse", "c")), false),
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
                                    int used = i;
                                    CardUtil.executeOrOtherwise(
                                            () -> player.chooseCardFromHand("Choose " + (2 - used) + " to trash to gain a silver", true),
                                            Objects::nonNull,
                                            player::moveToTrash,
                                            () -> self.set("continu", false)
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
        return new Card("Upgrade", RegistryPrice.IntriguePrice(5), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0,1,1,0, "Effect", self);
                            CardUtil.executeIfSelected(
                                    () -> player.chooseCardFromHand("Choose a card to trash", false),
                                    card -> {
                                        player.moveToTrash(card);
                                        CardUtil.gainFromSupply(player, "Choose a card costing exactly 1$ more that " + card + "(" + card.getCost() + 1 + "$ )", gained -> gained.getCost() == card.getCost() +1 , Destination.HAND, false);
                                    }
                            );
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Wishing_Well(){
        return new Card("WishingWell", RegistryPrice.IntriguePrice(3), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0,1,1,0, "Effect", self);
                            String choice = player.choose("On channel,  compute a name of a card", false);
                            Card revealed = player.getCardFromDeck();
                            if (choice != null && !choice.isEmpty()) {
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
                            int i = self.getOrDefault("increment", Integer.class);
                            self.set("increment", i + 1);})
                        .onCardPlayed( (owner, victim, event) ->owner.increment(Item.MONEY, config.get().get("increment", Integer.class)))
                        .onCondition((event, player) -> player == event.getPlayer() && event.getCard().hasName("Copper"))
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card GreatHall(){
        return new Card("GreatHall", RegistryPrice.IntriguePrice(4), CardType.ACTION, CardType.VICTORY)
                .setup(config -> config
                        .registerSimpleAction(1,1,0,0)
                        .score(player -> 1)
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Saboteur(){
        return new Card("Saboteur", RegistryPrice.IntriguePrice(5), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            player.getGame().processAttack(
                                    player,
                                    self,
                                    vi -> {
                                            List<Card> aside = new ArrayList<>();
                                            Card c;
                                        while (true) {
                                            c = player.getCardFromDeck();
                                            if (c == null) break;
                                            c.moveTo(aside);
                                            if (c.getCost() >= 3) {
                                                break;
                                            }
                                        }
                                            if(c!= null && c.getCost() >= 3){
                                                player.log( vi.getName() + "Revealed Card : " + aside);
                                                player.moveToTrash(c);
                                                Card finalCard = c;
                                                CardUtil.gainFromSupply(player, "Choose an card cost up (" + (c.getCost() - 2) +")", card -> card.getCost() <= finalCard.getCost() - 2 && card.getPotion() == 0, Destination.DISCARD, false);
                                            }
                                            new ArrayList<>(aside).forEach(card -> player.moveTo(card, Destination.DISCARD));
                                    }
                            );
                        })
                );
    }
    @Dominion_Card(extension = "Intrigue")
    public static Card Scout(){
        return new Card("Scout", RegistryPrice.IntriguePrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0,1,0,0, "Effect", self);
                            List<Card> view = CardUtil.getTopCards(player, 4);
                            while(view.stream().anyMatch(card -> card.hasType(CardType.VICTORY))){
                                CardUtil.executeIfSelected(
                                        () ->player.chooseCardFromList("Choose Card Victory", c -> c.hasType(CardType.VICTORY), view, false),
                                        card ->{
                                            player.moveTo(card, Destination.HAND);
                                            view.remove(card);
                                        }
                                );
                            }
                            while(!view.isEmpty()){
                                CardUtil.executeIfSelected(
                                        () -> player.chooseCardFromList("Move other cards in your hand", c -> true, view, false),
                                        card ->{
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
                                    CardUtil.executeOrOtherwise(
                                            () -> player.chooseCardFromHand("Choose an card to discard from your hand ( you can stop )", true),
                                            Objects::nonNull,
                                            card ->{
                                                player.moveTo(card, Destination.DISCARD);
                                                player.increment(Item.MONEY, 1);
                                            },
                                            () -> self.set("stop", true)
                                    );
                                }
                        })
                        .onCardPlayed((owner, actor, playedCard) -> {
                            config.get().set("last_ID", playedCard.getId());
                            CardUtil.executeIfSelected(
                                    () -> owner.chooseCardFromHand("Reveal Secret_Chamber", card -> card.hasName("SecretChamber"), true),
                                    card -> {
                                        owner.draw(2);
                                        for(int i = 0; i <2; i++){
                                            CardUtil.executeIfSelected(
                                                    () -> owner.chooseCardFromHand("Choose card to put on your draw (2)", false),
                                                    draw -> owner.moveTo(draw, Destination.DRAW)
                                            );
                                        }
                                    }
                            );
                        })
                        .onCondition((event, player) -> event.getCard().hasType(CardType.ATTACK) && player != event.getPlayer() && !config.get().get("last_ID", Integer.class).equals(event.getId()))
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
