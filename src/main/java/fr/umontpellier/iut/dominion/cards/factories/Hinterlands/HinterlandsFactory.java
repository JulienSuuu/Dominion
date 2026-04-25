package fr.umontpellier.iut.dominion.cards.factories.Hinterlands;

import fr.umontpellier.iut.dominion.*;
import fr.umontpellier.iut.dominion.cards.*;
import fr.umontpellier.iut.dominion.cards.Events.Event;
import javafx.beans.property.IntegerProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static fr.umontpellier.iut.dominion.cards.factories.FactoryUtil.*;

public class HinterlandsFactory {
    public static Card Berserker(){
        return new Card("Berserker", RegistryPrice.Hinterlands(5), CardType.ActionAndAttack)
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.gainFromSupply(player, "Choose a card costing less than " + self.getCost(), card -> card.isAtMostWithBonus(self, -1), Destination.DISCARD, false);
                            player.getGame().processMoveTo(player ,self, Destination.DISCARD, 3, true);
                        })
                        .checkGain((event, self) ->{
                            Player player = event.getPlayer();
                            player.playCard(self);
                            event.setDest(null);
                        })
                        .onCondition((event, player) -> {
                            List<Card> inplay = player.getCopyOf(Destination.INPLAY);
                            return inplay.stream().anyMatch(c -> c.hasType(CardType.ACTION)) && event.getDest() == Destination.DISCARD;
                        })
                );
    }

    public static Card Border_Village(){
        Bonus card_Action = Bonus.empty().draw(1).with(Item.ACTION, 2);
        return Card.action("Border Village", RegistryPrice.Hinterlands(6))
                .setup(config -> config
                        .registerSimpleAction(card_Action)
                        .checkGain((event, self) ->{
                            Player player = event.getPlayer();
                            CardUtil.gainFromSupply(player, "Choose a Card costing less than " + self.getCost(), card -> buyCondition.test(self, card), Destination.DISCARD, false);
                        })
                );
    }

    public static Card Cartographer(){
        Bonus card_Action = Bonus.empty().draw(1).with(Item.ACTION, 1);
        return Card.action("Cartographer", RegistryPrice.Hinterlands(5))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, card_Action);
                            List<Card> view = CardUtil.getTopCards(player, 4);

                            if(view.isEmpty())return;

                            self.set("continue", true);

                            while(!view.isEmpty() && self.getFlag("continue")){
                                player.chooseCardFromList("Discard any card you wants (you can stop)", card -> true, view, true)
                                        .ifPresentOrElse(
                                                card ->{
                                                    player.moveTo(card, Destination.DISCARD);
                                                    view.remove(card);
                                                },
                                                () -> self.set("continue", false)
                                        );
                            }

                            if(view.isEmpty())return;

                            while(!view.isEmpty()){
                                player.chooseCardFromList("Put the rest back in any order", card -> true, view, false)
                                        .ifPresent(card -> {
                                            player.moveTo(card, Destination.DRAW);
                                            view.remove(card);
                                        });
                            }
                        })
                );
    }

    public static Card Cauldron(){
        Bonus buy_Money = Bonus.empty().with(Item.BUY, 1).with(Item.MONEY, 2);
        return new Card("Cauldron", RegistryPrice.Hinterlands(5), CardType.TREASURE, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, buy_Money);
                            config.get().set("NumberAction", 0);
                        })
                        .onGain((owner, victim, event) -> owner.getGame().processGain(owner, config.get(), Destination.DISCARD, "Curse"))
                        .onCondition((event, player) ->{
                            if(event.getPlayer() != player || !event.getCard().hasType(CardType.ACTION)) return false;

                            int number = config.get().getValue("NumberAction").intValue();
                            if(event.getCard().hasType(CardType.ACTION)){
                                number++;
                                config.get().set("NumberAction", number);
                            }
                            return number == 3;
                        })
                );
    }

    public static Card Crossroads(){
        return Card.action("Crossroads", RegistryPrice.Hinterlands(2))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            List<Card> hand = player.getCopyOf(Destination.HAND);
                            player.log("Reveals " + hand);
                            int victory = hand.stream().filter(c -> c.hasType(CardType.VICTORY)).toList().size();
                            player.draw(victory);
                            if(!player.getFlag(Flags.playedCrossroads).get()){
                                player.increment(Item.ACTION, 3);
                            }
                        })
                );
    }

    public static Card Develop(){
        return Card.action("Develop", RegistryPrice.Hinterlands(3))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            player.chooseCardFromHand("Trash a card from you hand", true)
                                    .ifPresent(card -> {
                                        player.moveToTrash(card);
                                        CardUtil.gainFromSupply(player, "Choose a card costing exactly " + (card.getCost() + 1), c -> c.isEqualWithBonus(card, 1),  Destination.DRAW, false);
                                        CardUtil.gainFromSupply(player, "Choose a card costing exactly " + (card.getCost() -1), c -> c.isEqualWithBonus(card, -1),  Destination.DRAW, false);
                                    });
                        })
                );
    }

    public static Card Farmlands(){
        return Card.Victory("Farmlands", RegistryPrice.Hinterlands(6))
                .setup(config -> config
                        .score(player -> 2)
                        .checkGain((event, self) ->{
                            Player player = event.getPlayer();
                            player.chooseCardFromHand("Trash a card from you hand", true)
                                    .ifPresent(card -> {
                                        player.moveToTrash(card);
                                        CardUtil.gainFromSupply(player,
                                                "Choose a card costing exactly " + (card.getCost()+2) + " and not a FarmLands",
                                                c -> c.getCost() == card.getCost()+2 && card.hasSameNameAs(self), Destination.DISCARD, false);
                                    });
                        })
                );
    }

    public static Card Fools_Gold(){
        Bonus basicMoney = Bonus.empty().with(Item.MONEY, 1);
        Bonus fullMoney = Bonus.empty().with(Item.MONEY, 4);
        return new Card("Fool's Gold", RegistryPrice.Hinterlands(2))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            Bonus toUse = basicMoney;
                            if(player.getFlag(Flags.playedFoolsGold).get()){
                                toUse = fullMoney;
                            }
                            CardUtil.TriggerEffect(player, EFFECT, self, toUse);
                        })
                        .onGain((owner, victim, event) -> {
                            String choice = owner.chooseWhatToDo("Do you want to trash this for a Gold ?", List.of(config.get()), Button.yesOrNo, true);
                            if("y".equals(choice)) {
                                owner.moveToTrash(config.get());
                                CardUtil.gainFromSupply(owner, "Gold", Destination.DRAW, false);
                            }
                        })
                        .onCondition((event, player) ->
                                event.getCard().hasName("Province") &&
                                event.getPlayer() != player)

                );
    }

    public static Card Guard_Dog(){
        Bonus cards = Bonus.empty().draw(2);
        return new Card("Guard Dog", RegistryPrice.Hinterlands(3), CardType.ACTION, CardType.REACTION)
                .setup(config -> config
                        .onPlay((player, self) ->{
                                CardUtil.TriggerEffect(player, EFFECT ,self, cards);
                                if(player.getCopyOf(Destination.HAND).size() <= 5){
                                    CardUtil.TriggerEffect(player, ACTION ,self, cards);
                                }
                        })
                        .onCardPlayed((owner, victim, event) -> {
                            String choice  = owner.chooseWhatToDo("Do you want to play this cards ? ",  List.of(config.get()), Button.yesOrNo, true);
                            if("y".equals(choice)) {
                                owner.playCard(config.get());
                            }
                        })
                        .onCondition((event, player) -> event.getCard().hasType(CardType.ATTACK) && event.getPlayer() != player)
                );
    }

    public static Card Haggler(){
        Bonus money = Bonus.empty().with(Item.MONEY, 2);
        return Card.action("Haggler", RegistryPrice.Hinterlands(5))
                .setup(config -> config
                        .registerSimpleAction(money)
                        .onBuy((owner, c) -> CardUtil.gainFromSupply(
                                owner,
                                "Gain a card costing less than " + c.getCost() + " and a Non victory",
                                card ->!card.hasType(CardType.VICTORY) && buyCondition.test(c, card),
                                Destination.DISCARD,
                                false ))
                );
    }

    public static Card Highway(){
        Bonus card_Action =  Bonus.empty().with(Item.ACTION, 1).draw(1);
        return Card.action("Highway", RegistryPrice.Hinterlands(5))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT ,self, card_Action);
                            IntegerProperty prop = GameStat.reduction;
                            prop.set(prop.get()+1);
                        })
                );
    }

    public static Card Inn(){
        Bonus card_Action =  Bonus.empty().with(Item.ACTION, 2).draw(2);
        return Card.action("Inn", RegistryPrice.Hinterlands(5))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT ,self, card_Action);
                            player.discardFromHand(2);
                        })
                        .checkGain((event, self) ->{
                            Player player = event.getPlayer();

                            List<Card> discard = player.getCopyOf(Destination.DISCARD).stream().filter(card -> card.hasType(CardType.ACTION)).collect(Collectors.toList());
                            if(!discard.contains(self))discard.add(self);

                            self.set("continue", true);
                            List<Card> toKeep = new ArrayList<>();
                            while(self.getFlag("continue") && discard.stream().anyMatch(c -> c.hasType(CardType.ACTION))){
                               player.chooseCardFromList("Choose any Action from your discard to put in your deck ( shuffleling )", card -> card.hasType(CardType.ACTION), discard, true)
                                       .ifPresentOrElse(card -> card.moveTo(toKeep, null),
                                               () -> self.set("continue", false)
                                       );
                            }

                            if(toKeep.isEmpty())return;
                            player.log("Reveals" + toKeep);
                            if(toKeep.contains(self)){
                                event.setDest(null);
                            }

                            new ArrayList<>(toKeep).forEach(card -> player.moveTo(card, Destination.DRAW));
                            player.shuffling(Destination.DRAW);
                        })
                );
    }


    public static Card Jack_of_All_Trades(){
        return Card.action("Jack of All Trades", RegistryPrice.Hinterlands(4))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.gainFromSupply(player, "Silver", Destination.DISCARD, false);

                            Optional.ofNullable(player.getCardFromDeck())
                                    .ifPresent(card -> {
                                        String choose = player.chooseWhatToDo("Do you want to discard it ?", List.of(card), Button.yesOrNo, true);
                                        if("y".equals(choose)) player.discard(card);
                                    });

                            int count = player.getCopyOf(Destination.HAND).size();
                            if(count < 5){
                                player.draw(5-count);
                            }

                            player.chooseCardFromHand("Trash a non treasure from your hand ( optional )", card -> !card.hasType(CardType.TREASURE) , true)
                                    .ifPresent(player::moveToTrash);
                        })
                );
    }

    public static Card Margrave(){
        Bonus cards_buy =  Bonus.empty().with(Item.BUY, 1).draw(3);
        return new  Card("Margrave", RegistryPrice.Hinterlands(5), CardType.ActionAndAttack)
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT ,self, cards_buy);
                            player.getGame().processAttack(player, self, vi -> {
                                vi.draw(1);
                                vi.discardTo(3);
                            });
                        }));
    }

    public static Card Nomads(){
        Bonus buy_Money = Bonus.empty().with(Item.BUY, 1).with(Item.MONEY,2);
        Bonus gainOrTrash = Bonus.empty().with(Item.MONEY,2);
        return  Card.action("Nomads", RegistryPrice.Hinterlands(4))
                .setup(config -> config
                        .registerSimpleAction(buy_Money)
                        .checkGain((event, self) -> CardUtil.TriggerEffect(event.getPlayer(), GAIN_ACTION ,self, gainOrTrash))
                        .onTrash((event, self) -> CardUtil.TriggerEffect(event.getPlayer(), TRASHED_ACTION, self, gainOrTrash))
                );
    }

    public static Card Oasis(){
        Bonus card_action_money =  Bonus.empty().with(Item.ACTION, 1).with(Item.MONEY, 1).draw(1);
        return  Card.action("Oasis", RegistryPrice.Hinterlands(3))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT ,self, card_action_money);
                            player.discard();
                        })
                );
    }

    public static Card Scheme() {
        Bonus card_Action = Bonus.empty().with(Item.ACTION, 1).draw(1);
        return Card.action("Scheme", RegistryPrice.Hinterlands(3))
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, card_Action);

                            IntegerProperty prop = player.getProperties(Properties.Scheme_Action);
                            prop.setValue(prop.get() + 1);

                            if (prop.get() == 1) {
                                player.addDiscardHook((event) -> {
                                    Player p = event.getPlayer();
                                    IntegerProperty check = p.getProperties(Properties.Scheme_Action);
                                    Card c = event.getCard();

                                    if (check.get() > 0 && c.hasType(CardType.ACTION) && c.getLocation() == Destination.INPLAY){

                                        String choice = p.chooseWhatToDo("Scheme: Put " + c.getName() + " on top of your deck?",
                                                List.of(c), Button.yesOrNo, true);
                                        if ("y".equals(choice)) {
                                            event.setNextDest(Destination.DRAW);
                                            check.setValue(check.get() - 1);
                                        }
                                    }
                                });
                            }
                        })
                );
    }

    public static Card Souk(){
        return Card.action("Souk", RegistryPrice.Hinterlands(5))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            int number = player.getCopyOf(Destination.HAND).size();
                            int money = Math.min(0, 7-number);
                            Bonus bonus = Bonus.empty().with(Item.BUY, 1).with(Item.MONEY, money);
                            CardUtil.TriggerEffect(player, EFFECT, self,bonus);
                        })
                        .checkGain((event, self) -> {
                            Player p = event.getPlayer();
                            for(int i = 0; i<2; i++){
                                p.chooseCardFromHand("You can trash " + (2-i) + " cards (optional)", true )
                                        .ifPresent(p::moveToTrash);
                            }
                        })
                );
    }

    public static Card Spice_Merchant(){
        Bonus c1 = Bonus.empty().with(Item.ACTION, 1).draw(2);
        Bonus c2 = Bonus.empty().with(Item.BUY, 1).with(Item.MONEY, 2);
        return Card.action("Spice Merchant", RegistryPrice.Hinterlands(3))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            player.chooseCardFromHand("You may trash a card from you hand", true)
                                    .ifPresent(card -> {
                                        player.moveToTrash(card);

                                       String choice = player.chooseWhatToDo("Choose: +2Card and +1Action or +1Buy and 2$ ", List.of(self), List.of(new Button("2 Card & 1 action", "c1"), new Button("1 buy & 2$", "c2")), false  );
                                       switch (choice) {
                                           case "c1" -> CardUtil.TriggerEffect(player, ACTION, self, c1);
                                           case "c2" -> CardUtil.TriggerEffect(player, ACTION, self, c2);
                                           case null, default -> {}
                                       }
                                    });
                        })
                );
    }

    public static Card Stables(){
        Bonus action = Bonus.empty().with(Item.ACTION, 1).draw(3);
        return Card.action("Stables", RegistryPrice.Hinterlands(5))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            player.chooseCardFromHand("You may discard a Treasure from you hand", card -> card.hasType(CardType.TREASURE), true)
                                    .ifPresent(card -> {
                                        player.discard(card);
                                        CardUtil.TriggerEffect(player, ACTION, self, action);
                                    });
                        })
                );
    }

    public static Card Trader(){
        return new Card("Trader", RegistryPrice.Hinterlands(4), CardType.ACTION, CardType.REACTION)
                .setup(config -> config
                        .onPlay((player, self) ->{
                            player.chooseCardFromHand("You may trash a card from you hand to gain Silver(s)", true)
                                    .ifPresent(card -> {
                                        player.moveToTrash(card);
                                        for (int i = 0; i< card.getCost(); i++){
                                            Card c = CardUtil.gainFromSupply(player, "Silver", Destination.DISCARD, false);
                                            if(c == null)break;
                                        }
                                    });
                        })
                        .onGain((owner, victim, event) -> {
                            Card toGained = event.getCard();
                            String choice = owner.chooseWhatToDo("Do you want to transform this card in Silver ?", List.of(toGained), Button.yesOrNo, true);
                            if("y".equals(choice)) {
                                Card c = owner.getCardFromSupply("Silver");
                                if(c!= null){
                                    toGained = c;
                                    event.setCard(toGained);
                                }
                            }
                        })
                        .onCondition((event, player) -> event.getPlayer() == player && ( event.getCard().getLocation() == Destination.SUPPLY || event.getCard().getLocation() == Destination.TRASH ) )
                );
    }

    public static Card Trail(){
        BiConsumer<Event, Card> playSelf = (event, self) -> {
            Player player = event.getPlayer();
            String choice = player.chooseWhatToDo("Do you want to play this card ?", List.of(self), Button.yesOrNo, true);
            if("y".equals(choice)) {
                player.playCard(self);
                event.setDest(Destination.INPLAY);
                event.setNextDest(null);
            }
        };

        Bonus card_action = Bonus.empty().with(Item.ACTION, 1).draw(1);
        return new Card("Trail", RegistryPrice.Hinterlands(4), CardType.ACTION, CardType.REACTION)
                .setup(config -> config
                        .registerSimpleAction(card_action)
                        .checkGain(playSelf::accept)
                        .onDiscard(playSelf::accept)
                        .onTrash(playSelf::accept)
                );
    }

    public static Card Tunnel(){
        return new Card("Tunnel", RegistryPrice.Hinterlands(3), CardType.VICTORY, CardType.REACTION)
                .setup(config -> config
                        .onDiscard((event, self) -> {
                            Player player = event.getPlayer();
                            String choice = player.chooseWhatToDo("Do you want to reveal this card for a gold ?", List.of(self), Button.yesOrNo, true);
                            if("y".equals(choice)) {
                                player.log("Reveal : " + self.toLog());
                                CardUtil.gainFromSupply(player, "Gold", Destination.DISCARD, false);
                            }
                        })
                        .score(player -> 2)

                );
    }

    public static Card Weaver(){
        return new Card("Weaver", RegistryPrice.Hinterlands(4), CardType.ACTION, CardType.REACTION)
                .setup(config -> config
                        .onPlay((player, self) ->{
                            String choice = player.chooseWhatToDo("Choose : 2 silvers or a card costing up to 4", List.of(self), List.of(new Button("2 silvers", "s"), new Button("Card", "c")), false);
                            if("s".equals(choice)) {
                                for (int i = 0; i<2; i++){
                                    CardUtil.gainFromSupply(player, "Silver", Destination.DISCARD, false);
                                }
                            } else if ("c".equals(choice)) {
                                CardUtil.gainFromSupply(player, "Choose a card costing up to 4$", card -> card.isAtMost(4), Destination.DISCARD, false);
                            }
                        })
                        .onDiscard((event, self) -> {
                            Player player = event.getPlayer();
                            String choice = player.chooseWhatToDo("Do you want to play this ?", List.of(self), Button.yesOrNo, true);
                            if("y".equals(choice)) {
                                player.playCard(self);
                                event.setNextDest(null);
                            }
                        })
                );
    }

    public static Card Wheelwright(){
        Bonus action_Card = Bonus.empty().with(Item.ACTION, 1).draw(1);
        return Card.action("Wheelwright", RegistryPrice.Hinterlands(5))
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, action_Card);
                            player.chooseCardFromHand("You may discard a card from your hand", true)
                                    .ifPresent(card -> {
                                        player.discard(card);
                                        CardUtil.gainFromSupply(player, "Gain an Action card costing up to " + card.getCost(), c -> c.hasType(CardType.ACTION) && c.isAtMostWithBonus(card, 0), Destination.DISCARD, false);
                                    });
                        })
                );
    }

    public static Card Witch_Hut(){
        Bonus cards = Bonus.empty().draw(4);
        return new Card("Witch Hut", RegistryPrice.Hinterlands(5), CardType.ActionAndAttack)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, cards);
                            int count = 0;
                            for(int i = 0; i<2; i++){
                                Card c = player.discard();
                                player.log("Reveal " + c.toLog());
                                if(c.hasType(CardType.ACTION)) count++;
                            }

                            if(count == 2) player.getGame().processGain(player, self, Destination.DISCARD, "Curse");
                        })
                );
    }

    public static Card Cache(){
        Bonus money = Bonus.empty().with(Item.MONEY, 3);
        return Card.treasure("Cache", RegistryPrice.Hinterlands(5))
                .setup(config -> config
                        .registerSimpleAction(money)
                        .checkGain((event, self) -> {
                            Player player = event.getPlayer();
                            CardUtil.gainMultiplyCardFromSupply(player, "Copper", Destination.DISCARD, 2);
                        })
                );
    }

    public static Card Duchess(){
        Bonus money = Bonus.empty().with(Item.MONEY, 2);
        return Card.action("Duchess", RegistryPrice.Hinterlands(2))
                .setup(config -> config
                        .onPlay((player, self) -> {

                            CardUtil.TriggerEffect(player, EFFECT, self, money);
                            player.getGame().processGlobalEffect(player, p ->
                                Optional.ofNullable(p.getCardFromDeck())
                                        .ifPresent(card -> {
                                            String choice = p.chooseWhatToDo("Do yuou want to discard this card ?",  List.of(card), Button.yesOrNo, true);
                                            if("y".equals(choice)) {
                                                p.discard(card);
                                            }
                                        })
                            );

                        })
                );
    }

    public static Card Embassy(){
        Bonus card = Bonus.empty().draw(5);
        return Card.action("Embassy", RegistryPrice.Hinterlands(5))
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, card);
                            player.discardFromHand(3);
                        })
                        .checkGain((event, self) -> {
                            Player player = event.getPlayer();
                            player.getGame().processBenefit(player, p ->{
                                CardUtil.gainFromSupply(p, "Silver", Destination.DISCARD, false);
                            });
                        })
                );
    }

    public static Card Ill_Gotten_Gains(){
        Bonus money = Bonus.empty().with(Item.MONEY, 1);
        return Card.treasure("Ill Gotten Gains", RegistryPrice.Hinterlands(5))
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, money);
                            String choice = player.chooseWhatToDo("Do you want to gain to your hand a copper ?",  List.of(self), Button.yesOrNo, true);
                            if("y".equals(choice)) {
                                CardUtil.gainFromSupply(player, "Copper", Destination.HAND, false);
                            }
                        })
                        .checkGain((event, self) -> {
                            Player player = event.getPlayer();
                            player.getGame().processBenefit(player, p ->{
                                CardUtil.gainFromSupply(p, "Curse", Destination.DISCARD, false);
                            });
                        })
                );
    }

    public static Card Mandarin(){
        Bonus money = Bonus.empty().with(Item.MONEY, 3);
        return  Card.action("Mandarin", RegistryPrice.Hinterlands(5))
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, money);
                            player.chooseCardFromHand("Put a card onto your deck", false)
                                    .ifPresent(card -> player.moveTo(card, Destination.DRAW));
                        })
                        .checkGain((event, self) -> {
                            Player player = event.getPlayer();
                            List<Card> treasure = player.getCopyOf(Destination.INPLAY).stream().filter(card -> card.hasType(CardType.TREASURE)).collect(Collectors.toList());
                            while(!treasure.isEmpty()){
                                player.chooseCardFromList("Put treasures form play into your deck ",card -> true, treasure, false )
                                        .ifPresent(card -> {
                                            player.moveTo(card, Destination.DRAW);
                                            treasure.remove(card);
                                        });
                            }
                        })
                );
    }

    public static Card Nomad_Camp(){
        Bonus buy_money = Bonus.empty().with(Item.BUY, 1).with(Item.MONEY, 2);
        return Card.action("Nomad Camp", RegistryPrice.Hinterlands(4))
                .setup(config -> config
                        .registerSimpleAction(buy_money)
                        .checkGain((event, self) ->event.setNextDest(Destination.DRAW))
                );
    }

    public static Card Oracle(){
        return Card.action("Oracle", RegistryPrice.Hinterlands(3))
                .setup(config -> config
                        .onPlay((player, self) -> {
                            player.getGame().processGlobalEffect(player, p -> {
                                List<Card> view = CardUtil.getTopCards(p, 2);
                                p.log("Reveals" + view);
                                String choice = p.chooseWhatToDo("Choose : discard them or let them onto your draw",view, Button.DeckOrDiscard, true);
                                if("discard".equals(choice)){
                                    view.forEach(p::discard);
                                }
                                p.draw(2);
                            });
                        })
                );
    }

    public static Card SilkRoad(){
        return Card.Victory("Silk Road", RegistryPrice.Hinterlands(4))
                .setup(config -> config
                        .score(player ->{
                            Number number = player.getCopyOf(Destination.HAND).stream().filter(c -> c.hasType(CardType.VICTORY)).count();
                            return number.intValue() / 4;
                        })
                );
    }
}
