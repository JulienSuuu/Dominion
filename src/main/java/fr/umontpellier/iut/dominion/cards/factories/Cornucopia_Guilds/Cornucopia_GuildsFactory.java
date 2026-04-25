package fr.umontpellier.iut.dominion.cards.factories.Cornucopia_Guilds;

import fr.umontpellier.iut.dominion.*;
import fr.umontpellier.iut.dominion.Annotation.Dominion_Card;
import fr.umontpellier.iut.dominion.Annotation.ExtraSet;
import fr.umontpellier.iut.dominion.Annotation.InSet;
import fr.umontpellier.iut.dominion.Annotation.PileType;
import fr.umontpellier.iut.dominion.Properties;
import fr.umontpellier.iut.dominion.cards.*;
import fr.umontpellier.iut.dominion.cards.Events.Event;
import fr.umontpellier.iut.dominion.cards.factories.FactoryUtil;
import javafx.beans.property.IntegerProperty;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static fr.umontpellier.iut.dominion.cards.factories.FactoryUtil.*;

public class Cornucopia_GuildsFactory {
    @Dominion_Card(extension = CG)
    public static Card Advisor(){
        Bonus action = Bonus.empty().with(Item.ACTION, 1);
        return Card.action("Advisor", RegistryPrice.Cornucopia(4))
                .setup(config -> config
                        .onPlay((player, self) -> {
                            Player left = player.getGame().onTheLeft(player);
                            List<Card> view = CardUtil.getTopCards(player, 3);
                            if(view.isEmpty())return;

                            player.log("Reveals " +  view);

                            left.chooseCardFromList("Choose one to discard ( others will be leave onto the deck )", card -> true, view, false)
                                    .ifPresent(card ->{
                                        view.remove(card);
                                        player.moveTo(card, Destination.DISCARD );
                                    });


                            while(!view.isEmpty()){
                                player.chooseCardFromList("Put cards on your deck ( not matters the order )", card -> true, view, false)
                                        .ifPresent(card ->{
                                            view.remove(card);
                                            player.moveTo(card, Destination.DRAW);
                                        });
                            }
                        })
                );
    }
    @Dominion_Card(extension = CG)
    public static Card Baker(){
        Bonus draw_Action_Coffers = Bonus.empty().with(Item.ACTION, 1).with(Item.COFFER, 1).draw(1);
        return Card.action("Baker", RegistryPrice.Cornucopia(5))
                .setup(config -> config.registerSimpleAction(draw_Action_Coffers));
    }
    @Dominion_Card(extension = CG)
    public static Card Butcher(){
        Bonus coffers= Bonus.empty().with(Item.COFFER, 2);
        return Card.action("Butcher", RegistryPrice.Cornucopia(5))
                .setup(config -> config
                    .onPlay((player, self) -> {
                        CardUtil.TriggerEffect(player, EFFECT, self, coffers);
                        player.chooseCardFromHand("Trash a card from your hand ( optional )", true)
                                .ifPresent(card -> {
                                    player.moveToTrash(card);
                                    int toUse = card.getCost();
                                    while(true){
                                        int finalUse = toUse;
                                        List<Card> viewSupply = player.getGame().getAvailableSupplyCards().stream().filter(c -> c.isAtMost(finalUse)).toList();
                                        List<Button> button = new ArrayList<>();
                                        String choices = "";
                                        if(player.getValueOf(Item.COFFER)!= 0){
                                            button.add(new Button("LVl up", "l"));
                                            choices = "or increment choices by spending Coffers";
                                        }

                                        String choose = player.chooseWhatToDo("Choose a Card to gain " + choices, c -> true ,viewSupply, button, false);
                                        if("l".equals(choose)){
                                            toUse += 1;
                                            player.decrement(Item.COFFER, 1);
                                        }else {
                                            Card gain = CardUtil.gainFromSupply(player, choose, Destination.DISCARD, false);
                                            if(gain != null) break;
                                        }
                                    }
                                });
                    })
                );
    }
    @Dominion_Card(extension = CG)
    @InSet(value = {"Gilding the Lily"})
    public static Card Candlestick_Maker(){
        Bonus action_Coffer_Buy =  Bonus.empty().with(Item.ACTION, 1).with(Item.COFFER, 1).with(Item.BUY, 1);
        return Card.action("Candlestick Maker", RegistryPrice.Cornucopia(2))
                .setup(config -> config.registerSimpleAction(action_Coffer_Buy));
    }
    @Dominion_Card(extension = CG)
    public static Card Carnival(){
        return Card.action("Carnival", RegistryPrice.Cornucopia(5))
                .setup(config -> config
                    .onPlay((player, self) -> {
                        List<Card> view = CardUtil.getTopCards(player, 4);
                        if(view.isEmpty())return;
                        player.log("Reveals " +  view);
                        List<Card> distinctCards = view.stream()
                                .collect(Collectors.toMap(Card::getName, c -> c, (c1, c2) -> c1))
                                .values()
                                .stream()
                                .toList();

                        distinctCards.forEach(card -> {
                            view.remove(card);
                            player.moveTo(card, Destination.HAND);

                        });

                        if(view.isEmpty())return;

                        view.forEach(player::discard);

                    })
                );
    }
    @Dominion_Card(extension = CG, pileType = PileType.VICTORY)
    public static Card Fairgrounds(){
        return Card.Victory("Fairgrounds", RegistryPrice.Cornucopia(6))
                .setup(config -> config
                                .score(player ->{
                                    int number = player.getCopyOf(Destination.HAND).stream()
                                            .collect(Collectors.toMap(Card::getName, c -> c, (c1, c2) -> c1))
                                            .size();
                                    return (number / 5) *2;
                                })
                );
    }
    @Dominion_Card(extension = CG)
    public static Card Farmhands(){
        Bonus draw_Action = Bonus.empty().with(Item.ACTION, 2).draw(1);
        return Card.action("Farmhands", RegistryPrice.Cornucopia(4))
                .setup(config ->
                    config.registerSimpleAction(draw_Action)
                            .checkGain((event, self) -> {
                                Player player = event.getPlayer();
                                player.chooseCardFromHand("Set Aside a Action or treasure from you hand ( optional )", card -> card.hasType(CardType.TREASURE) || card.hasType(CardType.ACTION) , true)
                                        .ifPresent(card -> player.moveTo(card, Destination.ASIDE_ACTIVE));
                            })
                );
    }
    @Dominion_Card(extension = CG)

    @ExtraSet(value = {"Bounty of the Hunt"})
    public static Card Farrier(){
        Bonus draw_Action_Buy =  Bonus.empty().with(Item.ACTION, 1).with(Item.BUY, 1).draw(1);
        return new Card("Farrier", RegistryPrice.Cornucopia(2), CardType.ACTION, CardType.OVERPAID)
                .setup(config ->{
                    config.registerSimpleAction(draw_Action_Buy);
                    config.overpaid((player, card) ->{
                        int numberToDraw = card.getValue("OverpaidNumber").intValue();
                        if(numberToDraw == 0)return;
                        player.updateDrawBonusValue(numberToDraw);
                    });
                });
    }


    @Dominion_Card(extension = CG)
    @InSet(value = {"Bounty of the Hunt"})
    public static Card Ferryman(){
        Bonus draw_Action = Bonus.empty().with(Item.ACTION, 1).draw(2);
        return Card.action("Ferryman", RegistryPrice.Cornucopia(5)).setup(config -> config
                .onPlay((player, self) -> {
                    CardUtil.TriggerEffect(player, EFFECT, self, draw_Action);
                    player.discardFromHand(1);})
                .checkGain((event, self) -> {
                    Player player = event.getPlayer();
                    List<Card> view = player.getGame().getAvailableAsidePilesCard("Ferryman");
                    player.log("Reveals " + view);
                    Card card = view.getFirst();
                    player.gain(card, Destination.DISCARD);
        }));
    }
    @Dominion_Card(extension = CG)
    @InSet(value = {"Gilding the Lily"})
    public static Card Footpad(){
        Bonus coffers =  Bonus.empty().with(Item.COFFER, 2);
        return new Card("Footpad", RegistryPrice.Cornucopia(5), CardType.ActionAndAttack)
                .setup(config -> config
                                .onPlay((player, self) -> {
                                    CardUtil.TriggerEffect(player, EFFECT, self, coffers);
                                    player.getGame().processMoveTo(player, self, Destination.DISCARD, 3, true );
                                })
                        );

    }
    @Dominion_Card(extension = CG)
    public static Card Hamlet(){
        Bonus Card_Action = Bonus.empty().with(Item.ACTION, 1).draw(1);
        return Card.action("Hamlet", RegistryPrice.Cornucopia(2))
                .setup(config ->config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, Card_Action);
                            player.chooseCardFromHand("You may discard a card for +1 ACTION", true)
                                    .ifPresent(card -> {
                                        player.discard(card);
                                        player.increment(Item.ACTION, 1);
                                    });
                            player.chooseCardFromHand("You may discard a card for +1 BUY", true)
                                    .ifPresent(card -> {
                                        player.discard(card);
                                        player.increment(Item.BUY, 1);
                                    });
                        })
                );
    }
    @Dominion_Card(extension = CG)
    public static Card Herald(){
        Bonus card_Action = Bonus.empty().with(Item.ACTION, 1).draw(1);
        return new Card("Herald", RegistryPrice.Cornucopia(4), CardType.ACTION, CardType.OVERPAID)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, card_Action);
                            Card c = player.getCardFromDeck();
                            if(c == null)return;
                            player.log("Reveals " + c.toLog());
                            if(c.hasType(CardType.ACTION)){
                                player.playCard(c);
                            }
                        })
                        .overpaid((player, self) ->{
                            int number = self.getValue("OverpaidNumber").intValue();
                            if(number == 0)return;
                            for(int i = 0; i < number; i++){
                                player.chooseCardFromList("Put any card from your discard in your deck", card -> true, player.getCopyOf(Destination.DISCARD), false)
                                        .ifPresent(card -> {player.moveTo(card, Destination.DRAW);});
                            }

                        })

                );
    }
    @Dominion_Card(extension = CG)
    @InSet(value = {"Bounty of the Hunt"})
    public static Card Horn_Of_Plenty(){
        return Card.treasure("Horn of Plenty", RegistryPrice.Cornucopia(5))
                .setup(config -> config
                        .onPlay((player, self) -> {
                            int number = player.getDistinctCards(Destination.INPLAY).size();
                            List<Card> card = player.getGame().getAvailableSupplyCards().stream().filter(c -> c.isAtMost(number)).toList();
                            player.chooseCardFromList("Choose a card from ths List", c -> true, card, false)
                                    .ifPresent(selectedCard -> {
                                        if(selectedCard.hasType(CardType.VICTORY)){
                                            player.moveToTrash(self);
                                        }
                                        CardUtil.gainFromSupply(player, selectedCard.getName(), Destination.DISCARD, false);
                                    });
                        })
                );
    }
    @Dominion_Card(extension = CG)
    @InSet(value = {"Bounty of the Hunt"})
    public static Card Hunting_Party(){
        Bonus Card_Action = Bonus.empty().with(Item.ACTION, 1).draw(1);
        return Card.action("Hunting Party", RegistryPrice.Cornucopia(5))
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, Card_Action);
                            List<Card> hand = player.getCopyOf(Destination.HAND);
                            player.log("Reveals : " + hand);

                            Set<String> namesInHand = hand.stream()
                                    .map(Card::getName)
                                    .collect(Collectors.toSet());

                            List<Card> toDiscard = new ArrayList<>();
                            Card c;
                            do{
                                c = player.getCardFromDeck();
                                if(c == null)break;
                                if(namesInHand.contains(c.getName()))break;
                                c.moveTo(toDiscard, null);
                            }while (true);

                            player.log("Hunting Party: Found " + (c != null ? c.getName() : "nothing") +
                                    ", discarded " + toDiscard);

                            player.moveTo(c, Destination.HAND);

                            new ArrayList<>(toDiscard).forEach(d -> player.moveTo(d, Destination.DISCARD));

                        })
                );
    }
    @Dominion_Card(extension = CG)
    public static Card Infirmary(){
        Bonus Action = Bonus.empty().draw(1);
        return new Card("Infirmary", RegistryPrice.Cornucopia(3), CardType.ACTION, CardType.OVERPAID)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, Action);
                            player.chooseCardFromHand("You may trash a card from your hand", true)
                                    .ifPresent(player::moveToTrash);
                        })
                        .overpaid((player, self) -> {
                            int number = self.getValue("OverpaidNumber").intValue();
                            if(number == 0)return;
                            for(int i = 0; i < number; i++){
                                self.play(player);
                            }
                        })
                );
    }
    @Dominion_Card(extension = CG)
    public static Card Jester(){
        Bonus money =  Bonus.empty().with(Item.MONEY, 2);
        return new Card("Jester", RegistryPrice.Cornucopia(5), CardType.ActionAndAttack)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, money);
                            player.getGame().processAttack(player, self, vi -> {
                                Card c = vi.getCardFromDeck();
                                if(c != null){
                                    vi.discard(c);
                                    if(c.hasType(CardType.VICTORY)){
                                        CardUtil.gainFromSupply(vi, "Curse",  Destination.DISCARD, false);
                                    }
                                    else {
                                        String choice = player.chooseWhatToDo("Choose : you or the victim gain a copy of this card", List.of(c), List.of(new Button("You", "y"), new Button("Other", "o")), false);
                                        Player ref = "y".equals(choice)? player : vi;
                                        CardUtil.gainFromSupply(ref, c.getName(), Destination.DISCARD, false);
                                    }
                                }
                            });
                        })
                );
    }
    @Dominion_Card(extension = CG)
    public static Card Journeyman(){
        return Card.action("Journeyman", RegistryPrice.Cornucopia(5))
                .setup(config -> config
                        .onPlay((player, self) -> {
                            String named = player.choose("Name a card (message box, pref existing)",false );
                            List<Card> toDiscard = new ArrayList<>();
                            int count = 0;
                            while(count < 3){
                               Card c = player.getCardFromDeck();
                                if(c == null)break;
                                if(c.hasName(named)){
                                    player.moveTo(c, Destination.HAND);
                                    count++;
                                    continue;
                                }
                                c.moveTo(toDiscard, null);
                            }

                            new ArrayList<>(toDiscard).forEach(d -> player.moveTo(d, Destination.DISCARD));
                        })
                );
    }
    @Dominion_Card(extension = CG)
    @InSet(value = {"Bounty of the Hunt"})
    public static Card Joust(){
        Bonus card_Action_Money = Bonus.empty().with(Item.ACTION, 1).with(Item.MONEY, 1).draw(1);
        return Card.action("Joust", RegistryPrice.Cornucopia(5))
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, card_Action_Money);
                            player.chooseCardFromHand("You may set Aside a Province", card -> card.hasName("Province"), true)
                                    .ifPresent(card ->{
                                        player.moveTo(card, Destination.ASIDE);
                                        self.set("Aside", card);



                                        List<Card> rewards = player.getGame().getAvailableAsidePilesCard("Rewards");
                                        if(rewards == null)return;
                                        player.chooseCardFromList("Choose a reward", c -> true, rewards, false)
                                                .ifPresent(reward -> player.gain(reward, Destination.HAND));

                                        ;});
                        })
                        .onEndBuy((player, self) ->{
                            Card c = self.get("Aside", Card.class);
                            if(c == null)return;
                            player.moveTo(c, Destination.DISCARD);
                        })
                );
    }
    @Dominion_Card(extension = CG)
    @InSet(value = {"Bounty of the Hunt"})
    public static Card Menagerie(){
        Bonus action = Bonus.empty().with(Item.ACTION, 1);
        return Card.action("Menagerie", RegistryPrice.Cornucopia(3))
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, action);
                            List<Card> hand = player.getCopyOf(Destination.HAND);
                            player.log("Reveals " + hand);
                            List<Card> distinct = player.getDistinctCards(Destination.HAND);
                            if(hand.size() == distinct.size() || hand.isEmpty()) player.draw(3);
                            else player.draw(1);
                        })
                );
    }
    @Dominion_Card(extension = CG)
    public static Card Merchant_Guild(){
        Bonus buy_Money = Bonus.empty().with(Item.BUY, 1).with(Item.MONEY, 1);
        return Card.action("Merchant Guild", RegistryPrice.Cornucopia(5))
                .setup(config -> config
                        .onPlay((player, self) -> {
                            int mult = self.getValue("mult").intValue();
                            CardUtil.TriggerEffect(player, EFFECT, self, buy_Money);
                            self.set("mult", mult+1);

                        })
                        .onEndBuy((player, self) ->{
                            int number = player.getProperties(Properties.Cards_Bought).get();
                            int mult = self.getValue("mult").intValue();
                            player.increment(Item.COFFER, number * mult);
                        })
                );
    }
    @Dominion_Card(extension = CG)
    @InSet(value = {"Gilding the Lily"})
    public static Card Plaza(){
        Bonus card_Action =  Bonus.empty().with(Item.ACTION, 2).draw(1);
        return Card.action("Plaza", RegistryPrice.Cornucopia(4))
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, card_Action);
                            player.chooseCardFromHand("You may trash a Treasure for +1 coffers", card -> card.hasType(CardType.TREASURE), true)
                                    .ifPresent(card ->{
                                        player.moveToTrash(card);
                                        player.increment(Item.COFFER, 1);
                                    });
                        })
                );
    }
    @Dominion_Card(extension = CG)
    @InSet(value = {"Gilding the Lily"})
    public static Card Remake(){
        return  Card.action("Remake", RegistryPrice.Cornucopia(4))
                .setup(config -> config
                        .onPlay((player, self) -> {
                            for(int i = 0; i < 2; i++){
                                player.chooseCardFromHand("Trash a card, gain after this an card costing exactly 1 more", false)
                                        .ifPresent(card ->{
                                            player.moveToTrash(card);
                                            CardUtil.gainFromSupply(player, "Choose a card costing exactly " + (card.getCost()+1), c -> c.isEqualWithBonus(card, 1), Destination.DISCARD, false);
                                        });
                            }
                        })
                );
    }
    @Dominion_Card(extension = CG)
    public static Card Shop(){
        Bonus card_Money =Bonus.empty().with(Item.MONEY, 1).draw(1);
        return Card.action("Shop", RegistryPrice.Cornucopia(3))
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, card_Money);
                            List<Card> inPlay =  player.getCopyOf(Destination.INPLAY);
                            player.chooseCardFromHand("Choose a card that you dont have in play", card -> card.hasType(CardType.ACTION) && inPlay.stream().noneMatch(check -> check.hasSameNameAs(card)), true)
                                    .ifPresent(player::playCard);
                        })
                );
    }
    @Dominion_Card(extension = CG)
    public static Card Soothsayer(){
        return Card.action("Soothsayer", RegistryPrice.Cornucopia(5))
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.gainFromSupply(player, "Gold", Destination.DISCARD, false);
                            player.getGame().processAttack(player, self, vi -> {
                                Card c = CardUtil.gainFromSupply(vi, "Curse",  Destination.DISCARD, false);
                                if(c!=null){
                                    vi.draw(1);
                                }
                            });
                        })
                );
    }
    @Dominion_Card(extension = CG)
    public static Card Stonemason(){
        return new Card("Stonemason", RegistryPrice.Cornucopia(2), CardType.ACTION, CardType.OVERPAID)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            Optional<Card> choice = player.chooseCardFromHand("Choose a card to trash", false);
                            if(choice.isPresent()){
                                player.moveToTrash(choice.get());
                                if(choice.get().getCost()==0)return;
                                for(int i = 0; i < 2; i++){
                                    CardUtil.gainFromSupply(player, "Choose " + (2-i) + " card(s) to gain costing up" + (choice.get().getCost()-1), card -> card.isAtMostWithBonus(choice.get(), -1)  ,Destination.DISCARD, false);
                                }
                            }
                        })
                        .overpaid((player, self) -> {
                            int overpaid = self.getValue("OverpaidNumber").intValue();
                            int potion = self.getValue("Potion").intValue();
                            if(overpaid == 0)return;
                            for(int i = 0; i < 2; i++){
                                CardUtil.gainFromSupply(player, "Choose"+ (2-i) +" actions cards costing exactly " + overpaid, card -> card.hasType(CardType.ACTION) && card.isAtMost(overpaid, potion, 0),  Destination.DISCARD, false);
                            }
                        })
                );
    }
    @Dominion_Card(extension = CG)
    @InSet(value = {"Gilding the Lily"})
    public static Card Young_Witch(){
        Bonus cards = Bonus.empty().draw(2);
        return new Card("Young Witch", RegistryPrice.Cornucopia(4), CardType.ActionAndAttack)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, cards);
                            player.discardFromHand(2);
                            String bane = player.getGame().getBanes();
                            player.getGame().processAttack(player, self, vi -> {
                                vi.chooseCardFromHand("Reveal a Bane card from your hand or gain a Curse " + bane, card -> card.hasName(bane), true )
                                        .ifPresentOrElse(
                                                card -> vi.log("Reveals " + card ),
                                                () -> CardUtil.gainFromSupply(vi, "Curse", Destination.DISCARD, false));
                            });
                        })
                );
    }
    @Dominion_Card(extension = CG, pileType = PileType.REWARDS)
    public static Card Coronet(){
        return new Card("Coronet", RegistryPrice.Cornucopia(0), CardType.ACTION, CardType.ATTACK, CardType.REWARDS)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            player.chooseCardFromHand("Choose an Action card to play it 2 times", card -> card.hasType(CardType.ACTION) && !card.hasType(CardType.REWARDS), true)
                                    .ifPresent(card -> player.playCard(card, 2));
                            player.chooseCardFromHand("Choose a Treasure to play it 2 times",card -> card.hasType(CardType.TREASURE) && !card.hasType(CardType.REWARDS), true )
                                    .ifPresent(card -> player.playCard(card, 2));
                        })
                );
    }
    @Dominion_Card(extension = CG, pileType = PileType.REWARDS)
    public static Card Courser(){
        return new Card("Courser", RegistryPrice.Cornucopia(0), CardType.ACTION, CardType.REWARDS)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            List<Button> options = new ArrayList<>(List.of(
                                    new Button("+2 Card", "card"),
                                    new Button("+2 Action", "action"),
                                    new Button("+2 Money", "money"),
                                    new Button("+4 silver", "silver")
                            ));

                            for (int i = 0; i < 2; i++) {
                                final List<Button> currentOptions = new ArrayList<>(options);

                                CardUtil.executeIfSelected(
                                        () -> player.chooseWhatToDo("Courser: Choose 2 different options ", List.of(self) ,currentOptions, false),
                                        choiceValue -> {
                                            switch (choiceValue) {
                                                case "card" -> player.draw(2);
                                                case "action" -> player.increment(Item.ACTION, 2);
                                                case "money" -> player.increment(Item.BUY, 2);
                                                case "silver" -> {
                                                    for(int j = 0; j < 4 ; j++){
                                                        Card c = CardUtil.gainFromSupply(player, "Silver", Destination.DISCARD, false);
                                                        if(c== null) break;
                                                    }
                                                }
                                            }

                                            options.removeIf(btn -> btn.value().equals(choiceValue));

                                            player.log("chooses " + choiceValue);
                                        }
                                );
                            }
                        })
                );
    }
    @Dominion_Card(extension = CG, pileType = PileType.REWARDS)
    public static Card Demesne(){
        Bonus action_Buy = Bonus.empty().with(Item.ACTION, 2).with(Item.BUY, 2);
        return new Card("Demesne", RegistryPrice.Cornucopia(0), CardType.ACTION,CardType.VICTORY,  CardType.REWARDS)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, action_Buy);
                            CardUtil.gainFromSupply(player, "Gold", Destination.DISCARD, false);
                        })
                        .score(player -> player.getCopyOf(Destination.HAND).stream().filter(card -> card.hasName("Gold")).toList().size())
                );
    }
    @Dominion_Card(extension = CG, pileType = PileType.REWARDS)
    public static Card Housecarl(){
        return new Card("Housecarl",RegistryPrice.Cornucopia(0), CardType.ACTION, CardType.REWARDS)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            int numberOfAction = player.getDistinctCards(Destination.INPLAY).stream().filter(card -> card.hasType(CardType.ACTION)).toList().size();
                            player.draw(numberOfAction);
                        })
                );
    }
    @Dominion_Card(extension = CG, pileType = PileType.REWARDS)
    public static Card Huge_Turnip(){
        Bonus coffers = Bonus.empty().with(Item.COFFER, 2);
        return new Card("Huge Turnip", RegistryPrice.Cornucopia(0), CardType.TREASURE, CardType.REWARDS)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, coffers);
                            int numberOfCoffer = player.getValueOf(Item.COFFER);
                            player.increment(Item.MONEY, numberOfCoffer);
                        })
                );
    }
    @Dominion_Card(extension = CG, pileType = PileType.REWARDS)
    public static Card Renown(){
        Bonus buy =  Bonus.empty().with(Item.BUY,1 );
        return new Card("Renown", RegistryPrice.Cornucopia(0), CardType.ACTION, CardType.REWARDS)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, buy);
                            IntegerProperty prop = GameStat.reduction;
                            prop.set(prop.get() + 2);
                        })
                );
    }




}
