package fr.umontpellier.iut.dominion.cards.factories.Dark_Ages;

import fr.umontpellier.iut.dominion.*;
import fr.umontpellier.iut.dominion.Annotation.Dominion_Card;
import fr.umontpellier.iut.dominion.Annotation.PileType;
import fr.umontpellier.iut.dominion.Player.Player;
import fr.umontpellier.iut.dominion.cards.Bonus;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.RegistryPrice;
import javafx.beans.property.BooleanProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

import static fr.umontpellier.iut.dominion.cards.factories.FactoryUtil.*;

public class Dark_AgesFactory {


    @Dominion_Card(extension = DA)
    public static Card Altar(){
        return Card.action("Altar", RegistryPrice.DarkAges(5))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            player.chooseCardFromHand("Trash a card from your hand", false).ifPresent(player::trash);
                            CardUtil.gainFromSupply(player, "Choose a card costing up to 5$", card -> card.isAtMost(5), Destination.DISCARD, false);
                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Armory(){
        return Card.action("Armory", RegistryPrice.DarkAges(4))
                .setup(config -> config
                        .onPlay((player, self) -> CardUtil.gainFromSupply(player, "Choose a card costing up to 4$", card -> card.isAtMost(4), Destination.DRAW, false ))
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Band_Of_Misfits(){
        BiPredicate<Card, Card> buy = (self, card) ->
                lessThan.test(self, card)
                        && !card.hasType(CardType.DURATION)
                        && !card.hasType(CardType.COMMAND)
                        && card.hasType(CardType.ACTION);

        return new Card("Band of Misfits", RegistryPrice.DarkAges(5), CardType.COMMAND, CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) ->{
                            player.chooseCardFromSupply(
                                    "Choose a card costing less than this",
                                            card -> buy.test(self, card),
                                            false)
                                    .ifPresent(card -> {
                                        BooleanProperty prop = player.getFlag(Flags.resolveBandOfMisfit);
                                        prop.set(true);
                                        card.set("cant", true);

                                        card.play(player);
                                        linkedCard(self, card);

                                        card.set("cant", false);
                                        prop.set(false);
                                    });
                        })
                        .stayInPlayCondition(checkLink)
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Bandit_Camp(){
        Bonus card_actions = Bonus.empty().draw(1).with(Item.ACTION, 2);
        return Card.action("Bandit Camp", RegistryPrice.DarkAges(5))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, card_actions);
                            Card c = player.getGame().getAvailableAsideCard("Spoils", "Dark Ages");
                            if(c != null) player.gain(c, Destination.DISCARD);
                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Beggar(){
        return new Card("Beggar", RegistryPrice.DarkAges(2), CardType.ACTION, CardType.REACTION)
                .setup(config -> config
                        .onPlay((player, self) -> CardUtil.gainMultiplyCardFromSupply(player, "Copper", Destination.HAND, 3))
                        .beforeCardPlayed((event, owner) ->{
                            Card self = config.get();
                            String choice = owner.chooseWhatToDo("Do you want to discard this ?", List.of(self), Button.yesOrNo, true);
                            if("y".equals(choice)){
                                owner.discard(self);
                                CardUtil.gainFromSupply(owner, "Silver", Destination.DRAW, false);
                                CardUtil.gainFromSupply(owner, "Silver", Destination.DISCARD, false);
                            }
                        })
                        .beforeCardPlayedCondition((event, player) -> player != event.getPlayer() && event.getCard().hasType(CardType.ATTACK))
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Catacombs(){
        return Card.action("Catacombs", RegistryPrice.DarkAges(5))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            List<Card> view = CardUtil.getTopCards(player, 3);
                            if(view.isEmpty())return;
                            String choice = player.chooseWhatToDo("Choose : put the three in your hand or discard them and +3Cards", view, Button.DeckOrDiscard, false);
                            if("deck".equals(choice)){
                                view.forEach(c -> player.moveTo(c, Destination.HAND));
                            } else if ("discard".equals(choice)) {
                                view.forEach(player::discard);
                                player.draw(3);
                            }
                        })
                        .checkItselfTrash((event, self) ->{
                            Player player = event.getPlayer();
                            CardUtil.gainFromSupply(player, "Choose a card cheaper than it", card -> lessThan.test(self, card), Destination.DISCARD, false);
                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Count(){
        List<Button> firstChoice = new ArrayList<>();
        firstChoice.add(new Button("Discard 2 cards", "d"));
        firstChoice.add(new Button(" 1 hand to draw", "HtD"));
        firstChoice.add(new Button("+Copper", "c"));

        List<Button> secondChoice = new ArrayList<>();
        secondChoice.add(new Button("+3$", "money"));
        secondChoice.add(new Button("trash all Hand", "t"));
        secondChoice.add(new Button("+Duchy", "d"));

        return Card.action("Count", RegistryPrice.DarkAges(5))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            String choice = player.chooseWhatToDo("Choose : ", List.of(self), firstChoice, false);
                            switch (choice) {
                                case "d" -> player.discardFromHand(2);
                                case "HtD" -> player.chooseCardFromHand("Move a card from your hand in your deck", false).ifPresent(card -> player.moveTo(card, Destination.DRAW));
                                case "c" -> CardUtil.gainFromSupply(player, "Copper", Destination.DISCARD, false);
                            }

                            String second = player.chooseWhatToDo("Choose : ", List.of(self), secondChoice, false);
                            switch (second) {
                                case "money" -> player.increment(Item.MONEY, 3);
                                case "t" -> player.trashAll(Destination.HAND);
                                case "d" -> CardUtil.gainFromSupply(player, "Duchy", Destination.DISCARD, false);
                            }
                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Counterfeit(){
        Bonus buy_Money = Bonus.empty().with(Item.BUY, 1).with(Item.MONEY, 1);
        return Card.treasure("Counterfeit", RegistryPrice.DarkAges(5))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, buy_Money);
                            player.chooseCardFromHand("You may choose a treasure from your hand ( play it twice and trash it )", card -> card.hasType(CardType.TREASURE) && !card.hasType(CardType.DURATION), true)
                                    .ifPresent(card -> {
                                        player.playCard(card, 2);
                                        linkedCard(self, card);
                                        player.trash(card);
                                    });
                        })
                        .stayInPlayCondition(checkLink)
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Cultist(){
        Bonus cards= Bonus.empty().draw(2);
        return new Card("Cultist", RegistryPrice.DarkAges(5), CardType.ACTION, CardType.ATTACK, CardType.LOOTER)
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, cards);

                            player.getGame().processAttack(player, self, vi -> CardUtil.gainFromSupply(player, "Ruins", Destination.DISCARD, false));

                            player.chooseCardFromHand("You may play a Cultist from your hand", card -> card.hasSameNameAs(self), true)
                                    .ifPresent(card -> {player.playCard(card); linkedCard(self, card);});
                        })
                        .checkItselfTrash((event, self) -> {
                            Player player = event.getPlayer();
                            player.draw(3);
                        })
                        .stayInPlayCondition(checkLink)
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Death_Cart(){
        return new Card("Death Cart", RegistryPrice.DarkAges(4), CardType.ACTION, CardType.LOOTER)
                .setup(config -> config
                        .onPlay((player, self) ->{
                            String choice = player.chooseWhatToDo("You may trash this or a card from your hand", List.of(self), List.of(new Button("this", "t"), new Button("Hand", "h")), true);
                            boolean success = false;
                            if("t".equals(choice)){
                                success = player.trash(self);
                            }else if("h".equals(choice)){
                                Optional<Card> card = player.chooseCardFromHand("Choose a card to trash from your hand", false);
                                if(card.isPresent()){
                                    player.trash(card.get());
                                    success = true;
                                }
                            }

                            if(success) player.increment(Item.MONEY, 5);
                        })
                        .checkGain((event, self) -> {
                            Player player = event.getPlayer();
                            CardUtil.gainMultiplyCardFromSupply(player, "Ruins", Destination.DISCARD, 2);
                        })
                );
    }

    @Dominion_Card(extension = DA, pileType = PileType.VICTORY)
    public static Card Feodum(){
        return Card.Victory("Feodum", RegistryPrice.DarkAges(4))
                .setup(config -> config
                        .score(player -> player.getCopyOf(Destination.HAND).stream().filter(c -> c.hasName("Silver")).toList().size() / 3)
                        .checkItselfTrash((event, card) -> {
                            Player player = event.getPlayer();
                            CardUtil.gainMultiplyCardFromSupply(player, "Silver", Destination.DISCARD, 3);
                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Forager(){
        Bonus action_Buy = Bonus.empty().with(Item.ACTION, 1).with(Item.BUY, 1);
        return Card.action("Forager", RegistryPrice.DarkAges(5))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, action_Buy);
                            player.chooseCardFromHand("Trash a card from your hand", false)
                                    .ifPresent(player::trash);
                            Number number = player.getDistinctCards(Destination.TRASH).stream().filter(card -> card.hasType(CardType.TREASURE)).count();
                            player.increment(Item.MONEY, number.intValue());
                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Fortress(){
        Bonus card_Action = Bonus.empty().draw(1).with(Item.ACTION, 2);
        return Card.action("Fortress", RegistryPrice.DarkAges(4))
                .setup(config -> config
                        .registerSimpleAction(card_Action)
                        .checkItselfTrash((event, self) -> event.setDest(Destination.HAND))
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Graverobber(){
        List<Button> buttons = new ArrayList<>();
        buttons.add(new Button("Gain a Trash Card", "g"));
        buttons.add(new Button("Trash a HAND Card", "t"));
        return Card.action("Graverobber", RegistryPrice.DarkAges(5))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            String choice = player.chooseWhatToDo("Choose : Gain a card from the trash, or trash a card from your hand + a Gain", List.of(self), buttons, false);
                            if("g".equals(choice)){
                                List<Card> trashed = player.getGame().getTrashCards().stream().filter(c -> c.isBetween(3, 6)).toList();
                                player.chooseCardFromList("Choose a card in this list", card -> true, trashed, false)
                                        .ifPresent(card ->{
                                                player.log("Choose :" + card.toLog());
                                                player.gain(card, Destination.DRAW);});
                            }else if("t".equals(choice)){
                                player.chooseCardFromHand("Trash a card from your hand", false)
                                        .ifPresent(card -> {
                                            player.trash(card);
                                            CardUtil.gainFromSupply(player, "Choose a card costing up to "+ (card.getCost() + 3) + "$",c -> c.isAtMostWithBonus(card, 3), Destination.DISCARD, false);
                                        });
                            }
                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Hermit(){
        return  Card.action("Hermit", RegistryPrice.DarkAges(3))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            List<Card> discard = player.getCopyOf(Destination.DISCARD);
                            player.chooseCardFromList("you may choose a non Treasure Card and trash it ( gain a card afterwards )", card -> !card.hasType(CardType.TREASURE), discard, true)
                                    .ifPresent(player::trash);
                            CardUtil.gainFromSupply(player, "Choose a card costing up to 3$",c -> c.isAtMost(3), Destination.DISCARD, true);
                        })
                        .onEndBuy((player, self) ->{
                            if(!player.getCardGainedCurrentTurn().isEmpty())return;
                            Card madman = player.getGame().getAvailableAsideCard("Madman", "Dark Ages");
                            if(madman != null){
                                player.getGame().replaceCardInSupply(self);
                                player.moveTo(madman, Destination.DISCARD);
                            }

                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Hunting_Grounds(){
        Bonus cards = Bonus.empty().draw(4);
        return Card.action("Hunting Grounds", RegistryPrice.DarkAges(6))
                .setup(config -> config
                        .registerSimpleAction(cards)
                        .checkItselfTrash((event, self) -> {
                            Player player = event.getPlayer();
                            String choice = player.chooseWhatToDo("Choose : 1 duchy or 3 estate ", List.of(self), List.of(new Button("+Duchy","Duchy"), new Button("+3Estate","Estate")), false);
                            if(choice.isEmpty())return;
                            int number = "Duchy".equals(choice) ? 1 : 3;
                            CardUtil.gainMultiplyCardFromSupply(player, choice, Destination.DISCARD, number);
                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Ironmonger(){
        Bonus card_Action = Bonus.empty().draw(1).with(Item.ACTION, 1);
        return Card.action("Ironmonger", RegistryPrice.DarkAges(4))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, card_Action);
                            Card top = player.getCardFromDeck();
                            if(top == null) return;
                            String choice = player.chooseWhatToDo("Do you want to discard this card ?", List.of(top), Button.yesOrNo, true);
                            if("y".equals(choice)){
                                player.discard(top);
                                if(top.hasType(CardType.ACTION)) player.increment(Item.ACTION, 1);
                                if(top.hasType(CardType.TREASURE)) player.increment(Item.MONEY, 1);
                                if(top.hasType(CardType.VICTORY)) player.draw(1);
                            }
                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Junk_Dealer(){
        Bonus card_action_money = Bonus.empty().draw(1).with(Item.ACTION, 1).with(Item.MONEY, 1);
        return Card.action("Junk Dealer", RegistryPrice.DarkAges(5))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, card_action_money);
                            player.chooseCardFromHand("Trash a card from your hand", false)
                                    .ifPresent(player::trash);
                        })
                );
    }

    @Dominion_Card(extension = DA, pileType = PileType.MIXED)
    public static Card Knights(){
        return Knight("Knights").addType(CardType.TEMPLATE);
    }


    public static Card Knight(String name){
        return new Card(name, RegistryPrice.DarkAges(5), CardType.ACTION, CardType.ATTACK, CardType.KNIGHT);

    }

    @Dominion_Card(extension = DA)
    public static Card Marauder(){
        return new Card("Marauder", RegistryPrice.DarkAges(4), CardType.ACTION, CardType.ATTACK, CardType.LOOTER)
                .setup(config -> config
                        .onPlay((player, self) ->{
                            Card c = player.getGame().getAvailableAsideCard("Spoils", "Dark Ages");
                            if(c != null) player.gain(c, Destination.DISCARD);
                            player.getGame().processGain(player, self, Destination.DISCARD, "Ruins");
                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Market_Square(){
        Bonus card_action_buy = Bonus.empty().draw(1).with(Item.ACTION, 1).with(Item.BUY, 1);
        return new Card("Market_Square", RegistryPrice.DarkAges(3), CardType.REACTION, CardType.ACTION)
                .setup(config -> config
                        .registerSimpleAction(card_action_buy)
                        .onCardTrash((event, self) ->{
                            Player player = event.getPlayer();
                            String choice = player.chooseWhatToDo("Do you want to discard this for a gold ?", List.of(self),  Button.yesOrNo, true);
                            if("y".equals(choice)){
                                player.discard(self);
                                CardUtil.gainFromSupply(player, "Gold", Destination.DISCARD, false);
                            }
                        })
                        .onTrashCondition((event, self) -> !event.cameFrom(Destination.SUPPLY))
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Mystic(){
        Bonus action_Money = Bonus.empty().with(Item.ACTION, 1).with(Item.MONEY, 2);
        return Card.action("Mystic", RegistryPrice.DarkAges(5))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, action_Money);
                            Card top = player.getCardFromDeck();
                            String nameCard = player.choose("Name a card ( chatBox )", false);
                            if(top == null) return;
                            player.log("Reveals : " + top.getName());
                            if(nameCard.equals(top.getName())){
                                player.moveTo(top, Destination.HAND);
                            }
                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Pillage(){
        return new Card("Pillage", RegistryPrice.DarkAges(5), CardType.ActionAndAttack)
                .setup(config -> config
                        .onPlay((player, self) ->{
                            String choice = player.chooseWhatToDo("Do you want to trash this ?", List.of(self),  Button.yesOrNo, true);
                            boolean trashed = false;
                            if("y".equals(choice)){
                                trashed = player.trash(self);
                            }
                            if(trashed){
                                for(int i = 0; i < 2; i++){
                                    Card card = player.getGame().getAvailableAsideCard("Spoils", "Dark Ages");
                                    if(card == null) break;
                                    player.gain(card, Destination.DISCARD);
                                }

                                player.getGame().processAttack(player, self, vi -> {
                                    if(vi.getCopyOf(Destination.HAND).size() >=5 ){
                                        vi.log("Reveals : " + vi.getCopyOf(Destination.HAND));
                                        player.chooseCardFromList("Choose a card to discard in this hand", card -> true, vi.getCopyOf(Destination.HAND), false)
                                                .ifPresent(vi::discard);
                                    }
                                });
                            }
                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Poor_House(){
        Bonus money = Bonus.empty().with(Item.MONEY, 4);
        return Card.action("Poor House", RegistryPrice.DarkAges(1))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, money);
                            List<Card> hand = player.getCopyOf(Destination.HAND);
                            player.log("Reveals : " + hand);
                            Number treasure = hand.stream().filter(card -> card.hasType(CardType.TREASURE)).count();
                            int toDiscount = Math.min(player.getMoney(), treasure.intValue());
                            player.decrement(Item.MONEY, toDiscount);
                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Procession(){
        return Card.action("Procession", RegistryPrice.DarkAges(4))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            player.chooseCardFromHand("You may play a non duration Action card twice", card -> card.hasType(CardType.ACTION) && !card.hasType(CardType.DURATION), true)
                                    .ifPresent(card -> {
                                        player.playCard(card, 2);
                                        player.trash(card);
                                        CardUtil.gainFromSupply(player, "Choose an action card costing exactly " + (card.getCost() +1) + "$",c-> c.hasType(CardType.ACTION) && c.isEqualWithBonus(card, 1) , Destination.DISCARD, false);
                                    });
                        })
                );
    }

    @Dominion_Card(extension = DA,pileType = PileType.RATS)
    public static Card Rats(){
        Bonus card_action = Bonus.empty().draw(1).with(Item.ACTION, 1);
        return Card.action("Rats", RegistryPrice.DarkAges(4))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, card_action);
                            CardUtil.gainFromSupply(player, "Rats", Destination.DISCARD, false);
                            player.chooseCardFromHand("Trash a card from your hand other than a Rats", card -> !card.hasName("Rats"), false)
                                    .ifPresentOrElse(
                                            player::trash,
                                            () -> {
                                                if(player.getCopyOf(Destination.HAND).stream().allMatch(card -> card.hasName("Rats"))){
                                                    player.log("Reveals : " + player.getCopyOf(Destination.HAND));
                                                }
                                            });
                        })
                        .checkItselfTrash((event, self) -> {
                            Player player = event.getPlayer();
                            player.draw(1);
                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Rebuild(){
        Bonus action = Bonus.empty().with(Item.ACTION, 1);
        return Card.action("Rebuild", RegistryPrice.DarkAges(5))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, action);
                            String namedCard = player.choose("Name a card (chatBox)", false);
                            player.revealsUntil(
                                    card -> card.hasType(CardType.VICTORY) && !card.hasName(namedCard),
                                    victory -> {
                                        player.trash(victory);
                                        CardUtil.gainFromSupply(player, "Choose a victory card costing up to " + (victory.getCost()+3) + "$",
                                                card -> card.hasType(CardType.VICTORY) && card.isAtMostWithBonus(victory, 3),
                                                Destination.DISCARD,
                                                false);
                                    }
                            );
                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Rogue(){
        Bonus money = Bonus.empty().with(Item.MONEY, 2);
        return new Card("Rogue", RegistryPrice.DarkAges(5), CardType.ActionAndAttack)
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, money);
                            List<Card> trashed = player.getGame().getTrashCards().stream().filter(card -> card.isBetween(3, 6)).toList();
                            if(!trashed.isEmpty()){
                                player.chooseCardFromList("Choose a card in it", card -> true, trashed, false)
                                        .ifPresent(card ->{
                                            player.log("Reveals : " + card);
                                            player.gain(card, Destination.DISCARD);}
                                        );
                            }else {
                                player.getGame().processAttackWithReveal(player, self,2,
                                        card -> card.isBetween(3, 6),
                                        (attacker, victim, options) -> attacker.getGame().chooseACard(victim, options)
                                );
                            }
                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Sage(){
        Bonus action = Bonus.empty().with(Item.ACTION, 1);
        return Card.action("Sage", RegistryPrice.DarkAges(3))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, action);
                            player.revealsUntil(
                                    card -> card.getCost() >=3,
                                    card -> player.moveTo(card, Destination.HAND)
                            );
                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Scavenger(){
        Bonus money = Bonus.empty().with(Item.MONEY, 2);
        return Card.action("Scavenger", RegistryPrice.DarkAges(4))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, money);
                            String choice = player.chooseWhatToDo("Do you want to put your deck into your discard ?", List.of(self), Button.yesOrNo, true);
                            if("y".equals(choice)){
                                player.moveAll(Destination.DRAW, Destination.DISCARD);
                            }
                            player.chooseCardFromList("Choose a card in it", card -> true, player.getCopyOf(Destination.DISCARD), false)
                                    .ifPresent(card -> player.moveTo(card, Destination.DRAW));
                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Squire(){
        Bonus money = Bonus.empty().with(Item.MONEY, 1);
        return Card.action("Squire", RegistryPrice.DarkAges(2))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, money);
                            String choice = player.chooseWhatToDo("Choose : +2Action or +2Buy or 1 Silver", List.of(self), List.of(new Button("Action", "a"), new Button("Buy", "b"), new Button("Silver", "s")), false);
                            switch(choice){
                                case "a" -> player.increment(Item.ACTION, 2);
                                case "b" -> player.increment(Item.BUY, 2);
                                case "s" -> CardUtil.gainFromSupply(player, "Silver", Destination.DISCARD, false);
                            }
                        })
                        .checkItselfTrash((event, self) ->{
                            Player player = event.getPlayer();
                            CardUtil.gainFromSupply(player, "Choose a Attack Card", card -> card.hasType(CardType.ATTACK), Destination.DISCARD, false);
                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Storeroom(){
        Bonus buy = Bonus.empty().with(Item.BUY, 1);
        return Card.action("Storeroom", RegistryPrice.DarkAges(3))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, buy);
                            player.discardUntilYouStop(Destination.HAND, player::draw);
                            player.discardUntilYouStop(Destination.HAND, count -> player.increment(Item.MONEY, count));
                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Urchin(){
        Bonus card_action = Bonus.empty().draw(1).with(Item.ACTION, 1);
        return new Card("Urchin", RegistryPrice.DarkAges(3), CardType.ActionAndAttack)
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, card_action);
                            player.getGame().processHandDown(player, self, Destination.DISCARD, 4, true);
                        })
                        .beforeCardPlayed((event, owner) ->{
                            String choice = owner.chooseWhatToDo("Do you want to trash this ?", List.of(config.get()), Button.yesOrNo, true);
                            if("y".equals(choice)){
                                owner.trash(config.get());
                                Card mercenary = owner.getGame().getAvailableAsideCard("Mercenary", "Dark Ages");
                                owner.gain(mercenary, Destination.DISCARD);
                            }
                        })
                        .beforeCardPlayedCondition((event, player) -> event.getPlayer() == player && event.getCard().hasType(CardType.ATTACK) && event.getCard() != config.get())
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Vagrant(){
        Bonus card_action =  Bonus.empty().draw(1).with(Item.ACTION, 1);
        return Card.action("Vagrant", RegistryPrice.DarkAges(2))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, card_action);
                            Card top = player.getCardFromDeck();
                            if(top==null) return;
                            player.reveals(top);
                            if(top.hasType(CardType.CURSE) || top.hasType(CardType.RUINS) || top.hasType(CardType.SHELTER) || top.hasType(CardType.VICTORY)){
                                player.moveTo(top, Destination.HAND);
                            }
                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Wandering_Minstrel(){
        Bonus card_action =  Bonus.empty().draw(1).with(Item.ACTION, 2);
        return Card.action("Wandering Minstrel", RegistryPrice.DarkAges(4))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, card_action);
                            List<Card> view = CardUtil.getTopCards(player, 3);
                            player.reveals(view);
                            if(view.isEmpty()) return;
                            while(view.stream().anyMatch(card -> card.hasType(CardType.ACTION))){
                                player.chooseCardFromList("Put action card in any order onto your deck", card -> card.hasType(CardType.ACTION), view, false)
                                        .ifPresent(card -> {
                                            view.remove(card);
                                            player.moveTo(card, Destination.DRAW);
                                        });
                            }

                            if(!view.isEmpty()){
                                view.forEach(player::discard);
                            }
                        })
                );
    }

    @Dominion_Card(extension = DA ,pileType = PileType.MIXED)
    public static Card Dame_Anna(){
        return Knight("Dame Anna")
                .setup(config -> config
                    .onPlay((player, self) ->{
                        player.trash(2);
                        processKnightAttacks(player, self);
                    })
                );
    }
    @Dominion_Card(extension = DA ,pileType = PileType.MIXED)
    public static Card Dame_Josephine(){
        return Knight("Dame Josephine").addType(CardType.VICTORY)
                .setup(config -> config
                        .onPlay(Dark_AgesFactory::processKnightAttacks)
                        .score(player -> 2)
                );
    }
    @Dominion_Card(extension = DA ,pileType = PileType.MIXED)
    public static Card Dame_Molly(){
        Bonus action = Bonus.empty().with(Item.ACTION, 2);
        return Knight("Dame Molly")
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, action);
                            processKnightAttacks(player, self);
                        })
                );
    }
    @Dominion_Card(extension = DA ,pileType = PileType.MIXED)
    public static Card Dame_Natalie(){
        return Knight("Dame Natalie")
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.gainFromSupply(player, "Choose a card costing up to 3$", card -> card.isAtMost(3), Destination.DISCARD, false);
                            processKnightAttacks(player, self);
                        })
                );
    }
    @Dominion_Card(extension = DA ,pileType = PileType.MIXED)
    public static Card Dame_Sylvia(){
        Bonus money =  Bonus.empty().with(Item.MONEY, 2);
        return Knight("Dame Sylvia")
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, money);
                            processKnightAttacks(player, self);
                        })
                );
    }
    @Dominion_Card(extension = DA ,pileType = PileType.MIXED)
    public static Card Sir_Bailey(){
        Bonus card_action = Bonus.empty().draw(1).with(Item.ACTION, 1);
        return Knight("Sir Bailey")
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, card_action);
                            processKnightAttacks(player, self);
                        })
                );
    }
    @Dominion_Card(extension = DA ,pileType = PileType.MIXED)
    public static Card Sir_Destry(){
        Bonus cards = Bonus.empty().draw(2);
        return Knight("Sir Destry")
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, cards);
                            processKnightAttacks(player, self);
                        })
                );
    }

    @Dominion_Card(extension = DA ,pileType = PileType.MIXED)
    public static Card Sir_Martin(){
        Bonus buys = Bonus.empty().with(Item.BUY, 2);
        return Knight("Sir Martin").setPrice(4)
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, buys);
                            processKnightAttacks(player, self);
                        })
                );
    }

    @Dominion_Card(extension = DA ,pileType = PileType.MIXED)
    public static Card Sir_Michael(){
        return Knight("Sir Michael")
                .setup(config -> config
                        .onPlay((player, self) ->{
                            player.getGame().processHandDown(player, self, Destination.DISCARD, 3, true);
                            processKnightAttacks(player, self);
                        })
                );
    }
    @Dominion_Card(extension = DA ,pileType = PileType.MIXED)
    public static Card Sir_Vander(){
        return Knight("Sir Vander")
                .setup(config -> config
                        .onPlay(Dark_AgesFactory::processKnightAttacks)
                        .checkItselfTrash((event, card) -> {
                            Player player = event.getPlayer();
                            CardUtil.gainFromSupply(player, "Gold", Destination.DISCARD, false);
                        })
                );
    }

    @Dominion_Card(extension = DA ,pileType = PileType.RUINS)
    public static Card Abandoned_Mine(){
        Bonus money = Bonus.empty().with(Item.MONEY, 1);
        return Ruins("Abandoned Mine")
                .setup(config -> config
                        .registerSimpleAction(money)
                );
    }
    @Dominion_Card(extension = DA ,pileType = PileType.RUINS)
    public static Card Ruined_Library(){
        Bonus card = Bonus.empty().draw(1);
        return Ruins("Ruined Library")
                .setup(config -> config
                        .registerSimpleAction(card)
                );
    }
    @Dominion_Card(extension = DA ,pileType = PileType.RUINS)
    public static Card Ruined_Market(){
        Bonus buy =  Bonus.empty().with(Item.BUY, 1);
        return Ruins("Ruined Market")
                .setup(config -> config
                        .registerSimpleAction(buy)
                );
    }
    @Dominion_Card(extension = DA ,pileType = PileType.RUINS)
    public static Card Ruined_Village(){
        Bonus action = Bonus.empty().with(Item.ACTION, 1);
        return Ruins("Ruined Village")
                .setup(config -> config
                        .registerSimpleAction(action)
                );
    }
    @Dominion_Card(extension = DA ,pileType = PileType.RUINS)
    public static Card Survivors(){
        return Ruins("Survivors")
                .setup(config -> config
                        .onPlay((player, self) ->{
                            List<Card> view = CardUtil.getTopCards(player, 2);
                            if(view.isEmpty())return;
                            String choice = player.chooseWhatToDo("Discard them or put them back in any order", view, Button.DeckOrDiscard, false);
                            if("discard".equals(choice)){
                               for(Card card : view){
                                   player.discard(card);
                               }
                            }else{
                                while(!view.isEmpty()){
                                    player.chooseCardFromList("Put them in any order", card -> true, view, false)
                                            .ifPresent(card -> {
                                                player.moveTo(card, Destination.DRAW);
                                                view.remove(card);
                                            });
                                }
                            }
                        })
                );
    }

    @Dominion_Card(extension = DA ,pileType = PileType.MIXED)
    public static Card Hovel(){
        return new Card("Hovel", RegistryPrice.DarkAges(1), CardType.SHELTER, CardType.REACTION)
                .setup(config -> config
                        .onGain((event, owner) -> {
                            String choice = owner.chooseWhatToDo("Do you want to trash this ?", List.of(config.get()), Button.yesOrNo, false);
                            if("y".equals(choice)){
                                owner.trash(config.get());
                            }
                        })
                        .duringGainCondition((event, player) -> event.getPlayer() == player && event.getCard().hasType(CardType.VICTORY))
                );
    }
    @Dominion_Card(extension = DA ,pileType = PileType.MIXED)
    public static Card Necropolis(){
        Bonus actions =  Bonus.empty().with(Item.ACTION, 2);
        return new Card("Necropolis", RegistryPrice.DarkAges(1), CardType.SHELTER, CardType.ACTION)
                .setup(config -> config
                        .registerSimpleAction(actions)
                );
    }
    @Dominion_Card(extension = DA ,pileType = PileType.MIXED)
    public static Card OverGrown_Estate(){
        return new Card("Overgrown Estate", RegistryPrice.DarkAges(1), CardType.SHELTER, CardType.VICTORY)
                .setup(config -> config
                        .score(player -> 0)
                        .checkItselfTrash((event, self) -> {
                            Player player = event.getPlayer();
                            player.draw(1);
                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Madman(){
        Bonus action =  Bonus.empty().with(Item.ACTION, 2);
        return Card.action("Madman", RegistryPrice.DarkAges(0)).addType(CardType.ASIDE)
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, action);
                            if(player.getGame().replaceCardInAsideSupply(self, "Dark Ages")) player.draw(player.getCopyOf(Destination.HAND).size());
                        })
                );
    }
    @Dominion_Card(extension = DA)
    public static Card Mercenary(){
        Bonus cards_moneys = Bonus.empty().draw(2).with(Item.MONEY, 2);
        return new Card("Mercenary", RegistryPrice.DarkAges(0), CardType.ActionAndAttack).addType(CardType.ASIDE)
                .setup(config -> config
                        .onPlay((player, self) ->{
                            int i;
                            for(i = 0; i < 2; i++){
                                Optional<Card> totrash = player.chooseCardFromHand("you may choose " + (2-i) + " card to trash to apply some effect", true);
                                if(totrash.isPresent()){
                                    player.trash(totrash.get());
                                }else break;
                            }
                            if(i < 2) return;
                            CardUtil.TriggerEffect(player, ACTION, self, cards_moneys);
                            player.getGame().processHandDown(player, self, Destination.DISCARD, 3, true);
                        })
                );
    }

    @Dominion_Card(extension = DA)
    public static Card Spoils(){
        Bonus moneys = Bonus.empty().with(Item.MONEY, 3);
        return Card.treasure("Spoils", RegistryPrice.DarkAges(0)).addType(CardType.ASIDE)
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, moneys);
                            player.getGame().replaceCardInAsideSupply(self, "Dark Ages");
                        })
                );

    }

    public static Card Ruins(String name){
        return new Card(name, RegistryPrice.DarkAges(0), CardType.ACTION, CardType.RUINS);
    }



    public static void processKnightAttacks(Player player, Card self){
        List<Card> trashed = player.getGame().processAttackWithReveal(
                player,
                self,
                2,
                card -> card.isBetween(3, 6),
                (attacker, victim, options) -> attacker.getGame().chooseACard(victim, options)
        );
        if(trashed.stream().anyMatch(card -> card.hasType(CardType.KNIGHT))){
            player.trash(self);
        }
    }
}
