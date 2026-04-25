package fr.umontpellier.iut.dominion.cards.factories.Prosperity;

import fr.umontpellier.iut.dominion.*;
import fr.umontpellier.iut.dominion.Annotation.Dominion_Card;
import fr.umontpellier.iut.dominion.Annotation.InSet;
import fr.umontpellier.iut.dominion.cards.*;
import fr.umontpellier.iut.dominion.cards.Events.Event;
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static fr.umontpellier.iut.dominion.Button.yesOrNo;
import static fr.umontpellier.iut.dominion.cards.factories.FactoryUtil.*;

public class ProsperityFactory {
    @Dominion_Card(extension = "Prosperity")
    public static Card Anvil(){
        Bonus money = Bonus.empty().with(Item.MONEY, 1);
        return new Card("Anvil", RegistryPrice.ProsperityPrice(3), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, money);
                             player.chooseCardFromHand("Choose a treasure to trash (optional) ", card -> card.hasType(CardType.TREASURE), true)
                                     .ifPresent(card -> {
                                        player.moveToTrash(card);
                                        CardUtil.gainFromSupply(player, "Choose a treasure costing up 4", c -> c.isAtMost(4), Destination.DISCARD, true);
                                    });
                        })
                );
    }
    @Dominion_Card(extension = "Prosperity")
    public static Card Bank(){
        return new Card("Bank", RegistryPrice.ProsperityPrice(7), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            int money = 1;
                            money += (int) player.getCopyOf(Destination.INPLAY).stream().filter(card -> card.hasType(CardType.TREASURE)).count();
                            Bonus moneyBonus = Bonus.empty().with(Item.MONEY, money);
                            CardUtil.TriggerEffect(player, EFFECT, self, moneyBonus);
                        })
                );
    }
    @Dominion_Card(extension = "Prosperity")
    public static Card Bishop(){
        Bonus money  = Bonus.empty().with(Item.MONEY, 1);
        return new Card("Bishop",  RegistryPrice.ProsperityPrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, money);
                            player.increment(Item.VICTORY_TOKEN, 1);
                             player.chooseCardFromHand("Choose a card to trash ( Victory Token )", false)
                                     .ifPresent(card -> {
                                        player.moveToTrash(card);
                                        player.increment(Item.VICTORY_TOKEN, card.getCost()/2);
                                    }
                            );

                            player.getGame().processBenefit(
                                    player,
                                    vi -> vi.chooseCardFromHand("Choose a card from your hand (optional)", true)
                                                 .ifPresent(vi::moveToTrash)
                            );

                        })
                );
    }
    @Dominion_Card(extension = "Prosperity")
    public static Card Charlatan(){
        Bonus money = Bonus.empty().with(Item.MONEY, 3);
        return new Card("Charlatan", RegistryPrice.ProsperityPrice(5), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player,EFFECT, self, money);
                            player.getGame().processGain(player, self, Destination.DISCARD, "Curse");
                        })
                );
    }
    @Dominion_Card(extension = "Prosperity")
    public static Card City(){
        Bonus actionAndCard=  Bonus.empty().with(Item.ACTION,2).draw(1);
        return new Card("City", RegistryPrice.ProsperityPrice(5), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, actionAndCard);
                            Number emptyPile = GameStat.emptyPiles.get();
                            if(emptyPile.intValue() >= 1){
                                player.draw(1);
                            }
                            if(emptyPile.intValue() >= 2){
                                player.increment(Item.BUY, 1);
                                player.increment(Item.MONEY, 1);
                            }

                        })
                );
    }
    @Dominion_Card(extension = "Prosperity")
    public static Card Clerk(){
        Bonus money = Bonus.empty().with(Item.MONEY, 2);
        return new Card("Clerk", RegistryPrice.ProsperityPrice(4), CardType.ACTION, CardType.REACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player,  EFFECT, self, money );
                            player.getGame().processAttack(
                                    player,
                                    self,
                                    vi -> {
                                        if(vi.getCopyOf(Destination.HAND).size() >= 5){
                                             vi.chooseCardFromHand("Choose a card to put in your Deck", false)
                                                     .ifPresent(card -> vi.moveTo(card, Destination.DRAW));
                                        }
                                    }
                            );
                        })
                        .onStartTurn(player -> player.playCard(config.get()))
                );
    }
    @Dominion_Card(extension = "Prosperity")
    @InSet(value = {"The King's Army"})
    public static Card Collection(){
        Bonus moneyAndBuy = Bonus.empty().with(Item.MONEY, 2).with(Item.BUY, 1);
        return new Card("Collection", RegistryPrice.ProsperityPrice(5), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player,EFFECT, self, moneyAndBuy);
                            player.addCardEffect(self);
                        })
                        .onGain((owner, victim, event) -> owner.increment(Item.VICTORY_TOKEN, 1) )
                        .onCondition((event, player) -> player == event.getPlayer() && event.getCard().hasType(CardType.ACTION))

                );
    }
    @Dominion_Card(extension = "Prosperity")
    @InSet(value = {"Biggest Money"})
    public static Card Crystal_Ball(){
        Bonus money = Bonus.empty().with(Item.MONEY, 1);
        return new Card("Crystal Ball", RegistryPrice.ProsperityPrice(5), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player,  EFFECT, self, money);
                            Card top = player.getCardFromDeck();
                            String canPlay = "";
                            if(top == null) return;
                            List<Button> buttons = new ArrayList<>(Button.DiscardOrTrash);
                            if(top.hasType(CardType.ACTION) || top.hasType(CardType.TREASURE)){
                                buttons.add(new Button("play", "p"));
                                canPlay = " or Play";
                            }

                            String choice = player.chooseWhatToDo("Choose : Trash, Discard" + canPlay, List.of(top) , buttons, true);
                            switch(choice){
                                case "" -> {}
                                case "t" -> player.moveToTrash(top);
                                case "d" -> player.moveTo(top, Destination.DISCARD);
                                case "p" -> player.playCard(top);
                            }
                        })
                );
    }
    @Dominion_Card(extension = "Prosperity")
    @InSet(value = {"The King's Army"})
    public static Card Expand(){
        return new Card("Expand", RegistryPrice.ProsperityPrice(7), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            if(player.getCopyOf(Destination.HAND).isEmpty())return;
                            player.chooseCardFromHand("Choose a card to trash", false)
                                    .ifPresent(card -> {
                                        player.moveToTrash(card);
                                        CardUtil.gainFromSupply(player,
                                                "Choose a card costing up " + (card.getCost()+3),
                                                c -> c.isAtMostWithBonus(card, 3),
                                                Destination.DISCARD,
                                                false );
                                    }
                            );
                        })
                );
    }
    @Dominion_Card(extension = "Prosperity")
    public static Card Forge(){
        return new Card("Forge", RegistryPrice.ProsperityPrice(7), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            self.set("continu", true);
                            self.set("money", 0);
                            while(!player.getCopyOf(Destination.HAND).isEmpty() && self.getFlag("continu")){

                                player.chooseCardFromHand("Choose a card to trash ( you can pass)", true)
                                        .ifPresentOrElse(card ->{
                                            player.moveToTrash(card);
                                            self.set("money", self.get("money", Integer.class) + card.getCost());
                                        },
                                        () -> self.set("continu", false)
                                );

                            }

                            CardUtil.gainFromSupply(
                                    player,
                                    "Choose a card costing exactly " + self.get("money", Integer.class),
                                    card -> card.isEqual(self.get("money", Integer.class)),
                                    Destination.DISCARD,
                                    false);
                        })
                );
    }
    @Dominion_Card(extension = "Prosperity")
    @InSet(value = {"Biggest Money"})
    public static Card Grand_Market(){
        Bonus allBonus = Bonus.empty().draw(1).with(Item.ACTION, 1).with(Item.BUY, 1).with(Item.MONEY, 2);
        return new Card("Grand Market", RegistryPrice.ProsperityPrice(6), CardType.ACTION)
                .setup(config -> config
                        .registerSimpleAction(allBonus)
                        .available(player -> !player.isFlagSet(Flags.COPPER_PLAYED))
                );
    }
    @Dominion_Card(extension = "Prosperity")
    public static Card Hoard(){
        Bonus money = Bonus.empty().with(Item.MONEY, 2);
        return new Card("Hoard", RegistryPrice.ProsperityPrice(6), CardType.TREASURE)
                .setup(config -> config
                        .registerSimpleAction(money)
                        .onBuy((player, card) -> {
                            if(card.hasType(CardType.VICTORY)){
                                CardUtil.gainFromSupply(player, "Gold", Destination.DISCARD, false);
                            }
                        })
                );
    }
    @Dominion_Card(extension = "Prosperity")
    public static Card Investment(){
        return new Card("Investment", RegistryPrice.ProsperityPrice(4), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                             player.chooseCardFromHand("Choose a card to trash (Hand)", true)
                                     .ifPresent(player::moveToTrash);

                            CardUtil.executeOrOtherwise(
                                    () -> player.chooseWhatToDo("Choose: 1$ or trash this card for VT", List.of(self), List.of(new Button("1$", "m"), Button.Trash), false),
                                    "t"::equals,
                                    choice -> {
                                        player.log("Reveals : " + player.getCopyOf(Destination.HAND));
                                        player.moveToTrash(self);
                                        Number treasure = player.getCopyOf(Destination.HAND).stream().filter(card -> card.hasType(CardType.TREASURE)).count();
                                        player.increment(Item.VICTORY_TOKEN, treasure.intValue());
                                    },
                                    () -> player.increment(Item.MONEY, 1)
                            );
                        })
                );
    }
    @Dominion_Card(extension = "Prosperity")
    @InSet(value = {"The King's Army"})
    public static Card Kings_Court(){
        return new Card("King's Court", RegistryPrice.ProsperityPrice(7), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) ->
                                player.chooseCardFromHand("Choose a card and play it three time", card -> card.hasType(CardType.ACTION), true)
                                        .ifPresent(card -> {
                                            player.playCard(card);
                                            for(int i = 0; i < 2; i++){
                                                player.increment(Item.ACTION_PLAYED, 1);
                                                player.triggerEvent(TriggerComponent.OnCardPlayed.class, new Event(card, null, player));
                                                card.play(player);
                                            }
                                        })
                        )
                );
    }
    @Dominion_Card(extension = "Prosperity")
    public static Card Magnate(){
        return new Card("Magnate", RegistryPrice.ProsperityPrice(5), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            List<Card> cards = player.getCopyOf(Destination.HAND);
                            player.log("Reveals : " + cards);
                            Number treasures = cards.stream().filter(card -> card.hasType(CardType.TREASURE)).count();
                            player.draw(treasures.intValue());
                        })
                );
    }
    @Dominion_Card(extension = "Prosperity")
    @InSet(value = {"Biggest Money"})
    public static Card Mint(){
        return new Card("Mint", RegistryPrice.ProsperityPrice(5), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) ->
                            player.chooseCardFromHand("Reveal a Treasure from your hand", card -> card.hasType(CardType.TREASURE), true)
                                    .ifPresent(card -> {
                                        player.log("Reveals : " + card.toLog());
                                        CardUtil.gainFromSupply(player, card.getName(), Destination.DISCARD, false);
                                    })
                        )
                        .checkGain((event, self) -> {
                            Player player = event.getPlayer();
                            List<Card> nonDuration = player.getCopyOf(Destination.INPLAY).stream().filter(card -> !card.hasType(CardType.DURATION) && card.hasType(CardType.TREASURE)).toList();
                            new ArrayList<>(nonDuration).forEach(player::moveToTrash);
                        })
                );
    }
    @Dominion_Card(extension = "Prosperity")
    public static Card Monument(){
        Bonus money = Bonus.empty().with(Item.MONEY, 2);
        return new Card("Monument", RegistryPrice.ProsperityPrice(4), CardType.ACTION)
                .setup(config -> config
                    .onPlay((player, self) -> {
                        CardUtil.TriggerEffect(player, EFFECT, self, money);
                        player.increment(Item.VICTORY_TOKEN, 1);
                    })
                );
    }
    @Dominion_Card(extension = "Prosperity")
    public static Card Peddler(){
        Bonus card_Action_Money = Bonus.empty().draw(1).with(Item.ACTION, 1).with(Item.MONEY, 1);
        return new Card("Peddler",  RegistryPrice.ProsperityPrice(8), CardType.ACTION)
                .setup(config ->config.registerSimpleAction(card_Action_Money));
    }
    @Dominion_Card(extension = "Prosperity")
    public static Card Quarry(){
        Bonus money = Bonus.empty().with(Item.MONEY, 1);
        return new Card("Quarry", RegistryPrice.ProsperityPrice(4), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, money);
                            var pRed = player.getProperties(Properties.quarryReduction);
                            pRed.set(pRed.get()+2);
                        }));
    }
    @Dominion_Card(extension = "Prosperity")
    @InSet(value = {"The King's Army"})
    public static Card Rabble(){
        Bonus draw = Bonus.empty().draw(3);
        return new Card("Rabble", RegistryPrice.ProsperityPrice(5), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, draw);
                            player.getGame().processAttack(
                                    player,
                                    self,
                                    vi -> {
                                        List<Card> view = CardUtil.getTopCards(vi, 3);

                                        new ArrayList<>(view).stream().filter(c -> c.hasType(CardType.ACTION) || c.hasType(CardType.TREASURE))
                                                .forEach(c -> {
                                                    vi.discard(c);
                                                    view.remove(c);
                                                }
                                        );

                                        if(view.isEmpty() || view.size() == 1)return;

                                        while(!view.isEmpty()){
                                             vi.chooseCardFromList("Put those cards in your Draw (in the order you want )", card -> true, view, false)
                                                     .ifPresent(card -> {
                                                        vi.moveTo(card, Destination.DRAW);
                                                        player.log("Moved : " + card.getName());
                                                        view.remove(card);
                                                    }
                                            );
                                        }

                                    }
                            );
                        })
                );
    }
    @Dominion_Card(extension = "Prosperity")
    @InSet(value = {"Biggest Money"})
    public static Card Tiara(){
        Bonus buy = Bonus.empty().with(Item.BUY, 1);
        return new Card("Tiara", RegistryPrice.ProsperityPrice(4), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player,  EFFECT, self, buy);
                             player.chooseCardFromHand("Choose a treasure to play it two times", card -> card.hasType(CardType.TREASURE), true)
                                     .ifPresent(card -> {
                                        player.playCard(card);
                                        player.triggerEvent(TriggerComponent.OnCardPlayed.class, new Event(card, null, player));
                                        card.play(player);
                                    }
                            );
                        })
                        .onGain((owner, victim, event) -> CardUtil.executeOrOtherwise(
                                () -> owner.chooseWhatToDo("Do you want to put this card " + event.getCard().getName() + " in your draw ? ", List.of(event.getCard()) , yesOrNo, true ),
                                "y"::equals,
                                choice -> event.setDest(Destination.DRAW),
                                () -> {}
                        ))
                        .onCondition((event, player) -> player == event.getPlayer())



                );

    }
    @Dominion_Card(extension = "Prosperity")
    @InSet(value = {"The King's Army"})
    public static Card Vault(){
        Bonus draw_2 = Bonus.empty().draw(2);
        return new Card("Vault", RegistryPrice.ProsperityPrice(5), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player,  EFFECT, self, draw_2);
                            self.set("continue", true);
                            while (self.getFlag("continue")){
                                 player.chooseCardFromHand("Choose a card to discard ( gain 1$ per card discarded )", true)
                                         .ifPresentOrElse(card -> {
                                             player.discard(card);
                                             player.increment(Item.MONEY, 1);
                                            }, () -> self.set("continue", false)
                                );
                            }

                            player.getGame().processAttack(
                                    player,
                                    self,
                                    vi-> {
                                        int i;
                                        for (i = 0; i < 2; i++){
                                            Optional<Card> choice = vi.chooseCardFromHand("Choose again " + (2-i) + " cards to draw a card", true);
                                            if(choice.isEmpty())break;
                                            vi.discard(choice.get());
                                        }
                                        if(i == 2) vi.draw(1);
                                    }
                            );
                        })
                );
    }
    @Dominion_Card(extension = "Prosperity")
    public static Card War_Chest(){
        return new Card("War Chest", RegistryPrice.ProsperityPrice(5), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            nameACard(player, "War Chest");
                            List<Card> option = player.getGame()
                                    .getAvailableSupplyCards().stream()
                                    .filter(card -> card.isAtMost(5) && !player.getGame().getNamedCardsThisTurn("War Chest").contains(card.getName()))
                                    .toList();

                            player.chooseCardFromList("Choose a card from this list", card -> true, option, false)
                                    .ifPresent(card -> player.gain(card, Destination.DISCARD));
                        })
                );
    }
    @Dominion_Card(extension = "Prosperity")
    public static Card WatchTower(){
        return new Card("Watchtower", RegistryPrice.ProsperityPrice(5), CardType.ACTION, CardType.REACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            List<Card> hand = player.getCopyOf(Destination.HAND);
                            while(hand.size() < 6){
                                player.draw(1);
                            }
                        })
                        .onGain((owner, victim, event) -> CardUtil.executeOrOtherwise(
                                () -> owner.chooseWhatToDo("Do you want to put " + event.getCard().getName() +" in trash or in deck ?", List.of(event.getCard()), Button.TrashOrDeck , false),
                                "t"::equals,
                                choice ->{
                                    event.setDest(null);
                                    owner.moveToTrash(event.getCard());
                                },
                                () -> event.setDest(Destination.DRAW)
                        ))
                        .onCondition((event, player) -> player == event.getPlayer())
                );
    }
    @Dominion_Card(extension = "Prosperity")
    public static Card Workers_Village(){
        Bonus draw_Action_Buy = Bonus.empty().draw(1).with(Item.ACTION, 2).with(Item.BUY, 1);
        return new Card("Worker's Village",  RegistryPrice.ProsperityPrice(4), CardType.ACTION)
                .setup(config -> config.registerSimpleAction(draw_Action_Buy));
    }
    @Dominion_Card(extension = "Prosperity")
    public static Card Contraband(){
        Bonus money_Buy = Bonus.empty().with(Item.BUY, 1).with(Item.MONEY, 3);
        return new Card("Contraband", RegistryPrice.ProsperityPrice(5), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, money_Buy);
                            nameACard(player, "contraband");
                        })
                );
    }

    private static void nameACard(Player player, String key) {
        Player left = player.getGame().onTheLeft(player);
        if(left == null)return;

        String choice = left.choose("Compute a name of a card ( pref existing), in box channel", false);
        player.getGame().getNamedCardsThisTurn(key).add(choice);
    }
    @Dominion_Card(extension = "Prosperity")
    public static Card Counting_House(){
        return new Card("Counting House",  RegistryPrice.ProsperityPrice(5), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            List<Card> discard = player.getCopyOf(Destination.DISCARD);
                            self.set("continue", true);
                            while(discard.stream().anyMatch(c -> c.hasName("Copper")) && self.getFlag("continue")){
                                player.chooseCardFromList("Choose Coppers in your discard and put them in your hand", c -> c.hasName("Copper"), discard, true)
                                        .ifPresentOrElse(card -> {
                                            discard.remove(card);
                                            player.moveTo(card, Destination.HAND);
                                        }, () -> self.set("continue", false));
                            }
                        })
                );
    }
    @Dominion_Card(extension = "Prosperity")
    public static Card Goons(){
        Bonus draw_Money = Bonus.empty().draw(1).with(Item.MONEY, 2);
        return new Card("Goons",  RegistryPrice.ProsperityPrice(6), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, draw_Money);
                            player.getGame().processMoveTo(
                                    player,
                                    self,
                                    Destination.DISCARD,
                                    3,
                                    true);
                        })
                        .onBuy((player, gained) -> player.increment(Item.VICTORY_TOKEN, 1))
                );
    }
    @Dominion_Card(extension = "Prosperity")
    public static Card Loan(){
        Bonus money = Bonus.empty().with(Item.MONEY, 1);
        return new Card("Loan", RegistryPrice.ProsperityPrice(3), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, money);
                            boolean hasTreasure = false;
                            List<Card> discard = new ArrayList<>();
                            while(!hasTreasure){
                                Card c = player.getCardFromDeck();
                                if(c == null)break;
                                c.moveTo(discard, null);
                                if(c.hasType(CardType.TREASURE)){
                                    CardUtil.executeOrOtherwise(
                                            () -> player.chooseWhatToDo("Do you want to trash this treasure", List.of(c), Button.DiscardOrTrash, true),
                                            "t"::equals,
                                            choice -> player.moveToTrash(c),
                                            () -> player.discard(c)

                                    );
                                    hasTreasure = true;
                                }
                            }

                            new ArrayList<>(discard).forEach(player::discard);
                        })
                );
    }
    @Dominion_Card(extension = "Prosperity")
    @InSet(value = {"Biggest Money"})
    public static Card Mountebank(){
        Bonus money = Bonus.empty().with(Item.MONEY, 2);
        return new Card("Mountebank", RegistryPrice.ProsperityPrice(5), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, money);
                            player.getGame().processAttack(
                                    player,
                                    self,
                                    vi -> {
                                        Optional<Card> c  = vi.chooseCardFromHand("You may discard a curse", card -> card.hasType(CardType.CURSE), true);
                                        if(c.isPresent()){
                                            vi.discard(c.get());
                                        }else{
                                            CardUtil.gainFromSupply(vi, "Curse", Destination.DISCARD, false);
                                            CardUtil.gainFromSupply(vi, "Copper", Destination.DISCARD, false);
                                        }
                                    }
                            );
                        })
                );
    }
    @Dominion_Card(extension = "Prosperity")
    public static Card Royal_Seal(){
        Bonus money =  Bonus.empty().with(Item.MONEY, 2);
        return new Card("Royal Seal",  RegistryPrice.ProsperityPrice(5), CardType.TREASURE)
                .setup(config -> config
                        .registerSimpleAction(money)
                        .onGain((owner, victim, event) ->
                            CardUtil.executeOrOtherwise(
                                    () -> owner.chooseWhatToDo("Do you want to put this card onto your deck", List.of(event.getCard()) ,Button.DeckOrDiscard, true),
                                    "deck"::equals,
                                    choice -> event.setDest(Destination.DRAW),
                                    () -> {}
                            )
                        )
                        .onCondition((event, player) -> player == event.getPlayer())
                );
    }
    @Dominion_Card(extension = "Prosperity")
    public static Card Talisman(){
        Bonus money = Bonus.empty().with(Item.MONEY, 1);
        return new Card("Talisman", RegistryPrice.ProsperityPrice(4), CardType.TREASURE)
                .setup(config -> config
                        .registerSimpleAction(money)
                        .onBuy((player, card) -> {
                            if(!card.hasType(CardType.VICTORY) && card.isAtMost(4))
                                CardUtil.gainFromSupply(player, card.getName(), Destination.DISCARD, false);
                        })
                );
    }
    @Dominion_Card(extension = "Prosperity")
    public static Card Trade_Route(){
        Bonus buy = Bonus.empty().with(Item.BUY, 1);
        return new Card("Trade Route",  RegistryPrice.ProsperityPrice(3), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, buy);
                             player.chooseCardFromHand("Trash a card", false)
                                     .ifPresent(card -> {
                                        player.moveToTrash(card);
                                        player.increment(Item.MONEY, player.getValueOf(Item.COIN_TOKEN_ROUTE));
                                    }
                            );
                        })
                );
    }
    @Dominion_Card(extension = "Prosperity")
    public static Card Venture(){
        Bonus money = Bonus.empty().with(Item.MONEY, 1);
        return new Card("Venture", RegistryPrice.ProsperityPrice(5), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, money);
                            boolean hasTreasure = false;
                            Card treasure = null;
                            List<Card> discard = new ArrayList<>();
                            while(!hasTreasure){
                                Card c = player.getCardFromDeck();
                                if(c == null)break;
                                c.moveTo(discard, null);
                                if(c.hasType(CardType.TREASURE)){
                                    treasure = c;
                                    hasTreasure = true;
                                }
                            }
                            if(treasure != null){
                                player.playCard(treasure);
                            }
                            new ArrayList<>(discard).forEach(player::discard);
                        })
                );
    }
}
