package fr.umontpellier.iut.dominion.cards.Prosperity;

import fr.umontpellier.iut.dominion.*;
import fr.umontpellier.iut.dominion.cards.*;
import fr.umontpellier.iut.dominion.cards.component.CardSelector;
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ProsperityFactory {
    public static List<Button> yesOrNo = List.of(new Button("Yes", "y"), new Button("No", "n"));

    public static Card Anvil(){
        return new Card("Anvil", RegistryPrice.ProsperityPrice(3), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 1,0,0,0, "Effect", self);
                            CardUtil.executeIfSelected(
                                    () -> player.chooseCardFromHand("Choose a treasure to trash (optional) ", card -> card.hasType(CardType.TREASURE), true),
                                    card -> {
                                        player.moveToTrash(card);
                                        CardUtil.gainFromSupply(player, "Choose a treasure costing up 4", c -> c.getCost() <= 4, Destination.DISCARD, true);
                                    }
                            );
                        })

                );
    }

    public static Card Bank(){
        return new Card("Bank", RegistryPrice.ProsperityPrice(7), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            int money = 1;
                            money += (int) player.getCopyOf(Destination.INPLAY).stream().filter(card -> card.hasType(CardType.TREASURE)).count();
                            CardUtil.TriggerEffect(player, money, 0, 0, 0, "Effect", self);
                        })
                );
    }

    public static Card Bishop(){
        return new Card("Bishop",  RegistryPrice.ProsperityPrice(4), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 1,0,0,0, "Effect", self);
                            player.increment(Item.VICTORY_TOKEN, 1);
                            CardUtil.executeIfSelected(
                                    () -> player.chooseCardFromHand("Choose a card to trash ( Victory Token )", false),
                                    card -> {
                                        player.moveToTrash(card);
                                        player.increment(Item.VICTORY_TOKEN, card.getCost()/2);
                                    }
                            );

                            player.getGame().processBenefit(
                                    player,
                                    vi -> {
                                        CardUtil.executeIfSelected(
                                                () -> vi.chooseCardFromHand("Choose a card from your hand (optional)", true),
                                                vi::moveToTrash
                                        );
                                    }
                            );

                        })
                );
    }

    public static Card Charlatan(){
        return new Card("Charlatan", RegistryPrice.ProsperityPrice(5), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 3,0,0,0, "Effect", self);
                            player.getGame().processGain(player, self, Destination.DISCARD, "Curse");
                            GameStat.charlatanPower.set(GameStat.charlatanPower.getValue() + 1);
                        })
                );
    }

    public static Card City(){
        return new Card("City", RegistryPrice.ProsperityPrice(5), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0,2,1,0, "Effect", self);
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

    public static Card Clerk(){
        return new Card("Clerk", RegistryPrice.ProsperityPrice(4), CardType.ACTION, CardType.REACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 2, 0, 0,0, "Effect", self );
                            player.getGame().processAttack(
                                    player,
                                    self,
                                    vi -> {
                                        if(vi.getCopyOf(Destination.HAND).size() >= 5){
                                            CardUtil.executeIfSelected(
                                                    () -> vi.chooseCardFromHand("Choose a card to put in your Deck", false),
                                                    card -> vi.moveTo(card, Destination.DRAW)
                                            );
                                        }
                                    }
                            );
                        })
                        .onStartTurn(player -> player.playCard(config.get()))
                );
    }

    public static Card Collection(){
        return new Card("Collection", RegistryPrice.ProsperityPrice(5), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player,2,0,0,1,"Effect", self);
                            player.addCardEffect(self);
                        })
                        .onGain((owner, victim, event) -> owner.increment(Item.VICTORY_TOKEN, 1) )
                        .onCondition((event, player) -> player == event.getPlayer() && event.getCard().hasType(CardType.ACTION))

                );
    }

    public static Card Crystal_Ball(){
        return new Card("Crystal ball", RegistryPrice.ProsperityPrice(5), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player,1,0,0,0,"Effect", self);
                            Card top = player.getCardFromDeck();
                            String canPlay = "";
                            if(top == null) return;
                            List<Button> buttons= new ArrayList<>();
                            buttons.add(new Button("Trash", "t"));
                            buttons.add(new Button("Discard", "d"));
                            if(top.hasType(CardType.ACTION) || top.hasType(CardType.TREASURE)){
                                buttons.add(new Button("play", "p"));
                                canPlay = " or Play";
                            }

                            String choice = player.chooseStringFromButtons("Choose : Trash, Discard" + canPlay, buttons, true);
                            switch(choice){
                                case "" -> {}
                                case "t" -> player.moveToTrash(top);
                                case "d" -> player.moveTo(top, Destination.DISCARD);
                                case "p" -> player.playCard(top);
                            }
                        })
                );
    }

    public static Card Expand(){
        return new Card("Expand", RegistryPrice.ProsperityPrice(7), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            if(player.getCopyOf(Destination.HAND).isEmpty())return;
                            CardUtil.executeIfSelected(
                                    () -> player.chooseCardFromHand("Choose a card to trash", false),
                                    card -> {
                                        player.moveToTrash(card);
                                        CardUtil.gainFromSupply(player,
                                                "Choose a card costing up " + (card.getCost()+3),
                                                c -> c.getCost() <= card.getCost()+3 && c.buyCondition(card.getPotion(), 0),
                                                Destination.DISCARD,
                                                false );
                                    }
                            );
                        })
                );
    }

    public static Card Forge(){
        return new Card("Forge", RegistryPrice.ProsperityPrice(7), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            self.set("continu", true);
                            self.set("money", 0);
                            while(!player.getCopyOf(Destination.HAND).isEmpty() && self.getFlag("continu")){
                                CardUtil.executeOrOtherwise(
                                        () -> player.chooseCardFromHand("Choose a card to trash ( you can pass)", true),
                                        Objects::nonNull,
                                        card ->{
                                            player.moveToTrash(card);
                                            self.set("money", self.get("money", Integer.class) + card.getCost());
                                        },
                                        () -> self.set("continu", false)
                                );
                            }
                            List<Card> available = new ArrayList<>();
                            for(Card card : player.getGame().getAvailableSupplyCards()){
                                if(card==null)continue;
                                if(card.getCost()== self.get("money", Integer.class) && card.buyCondition(0,0)){
                                    available.add(card);
                                }
                            }
                            if(available.isEmpty())return;

                            CardUtil.gainFromSupply(
                                    player,
                                    "Choose a card costing exactly " + self.get("money", Integer.class),
                                    available::contains,
                                    Destination.DISCARD,
                                    false);
                        })
                );
    }

    public static Card Grand_Market(){
        return new Card("Grand Market", RegistryPrice.ProsperityPrice(7), CardType.ACTION)
                .setup(config -> config
                        .registerSimpleAction(1,1,1,2)
                        .available(player -> !player.isFlagSet(Flags.COPPER_PLAYED))
                );
    }

    public static Card Hoard(){
        return new Card("Hoard", RegistryPrice.ProsperityPrice(6), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player,2,0,0,0,"Effect", self);
                        })
                        .onBuy((player, card) -> {
                            if(card.hasType(CardType.VICTORY)){
                                CardUtil.gainFromSupply(player, "Gold", Destination.DISCARD, false);
                            }
                        })
                );
    }

    public static Card Investment(){
        return new Card("Investment", RegistryPrice.ProsperityPrice(4), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.executeIfSelected(
                                    () -> player.chooseCardFromHand("Choose a card to trash (Hand)", true),
                                    player::moveToTrash
                            );

                            CardUtil.executeOrOtherwise(
                                    () -> player.chooseStringFromButtons("Choose: 1$ or trash this card for VT", List.of(new Button("1$", "m"), new Button("Trash", "t")), false),
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

    public static Card Kings_Court(){
        return new Card("King's Court", RegistryPrice.ProsperityPrice(7), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                                CardUtil.executeIfSelected(
                                        () -> player.chooseCardFromHand("Choose a card and play it three time", card -> card.hasType(CardType.ACTION), true),
                                        card -> {
                                            player.playCard(card);
                                            for(int i = 0; i < 2; i++){
                                                player.increment(Item.ACTION_PLAYED, 1);
                                                player.triggerEvent(TriggerComponent.OnCardPlayed.class, new Event(card, null, player));
                                                card.play(player);
                                            }
                                        }
                                );
                        })
                );
    }

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

    public static Card Mint(){
        return new Card("Mint", RegistryPrice.ProsperityPrice(5), CardType.ACTION)
                .setup(config ->{
                    config.get().set("haveSpecialEffect", true);
                    Consumer<Player> specialEffects = player->{
                        List<Card> nonDuration = player.getCopyOf(Destination.INPLAY).stream().filter(card -> !card.hasType(CardType.DURATION) && card.hasType(CardType.TREASURE)).toList();
                        new ArrayList<>(nonDuration).forEach(player::moveToTrash);
                    };
                    config.get().set("action", specialEffects);
                    config.onPlay((player, self) -> {
                            CardUtil.executeIfSelected(
                                    () -> player.chooseCardFromHand("Reveal a Treasure from your hand", card -> card.hasType(CardType.TREASURE), true),
                                    card -> {
                                        player.log("Reveals : " + card.toLog());
                                        CardUtil.gainFromSupply(player, card.getName(), Destination.DISCARD, false);
                                    }
                            );

                    });}
                );
    }

    public static Card Monument(){
        return new Card("Monument", RegistryPrice.ProsperityPrice(4), CardType.ACTION)
                .setup(config -> config
                    .onPlay((player, self) -> {
                        CardUtil.TriggerEffect(player, 2, 0,0,0, "Effect", self);
                        player.increment(Item.VICTORY_TOKEN, 1);
                    })
                );
    }

    public static Card Peddler(){
        return new Card("Peddler",  RegistryPrice.ProsperityPrice(8), CardType.ACTION)
                .setup(config ->config.registerSimpleAction(1,1,0,1));
    }

    public static Card Quarry(){
        return new Card("Quarry", RegistryPrice.ProsperityPrice(4), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 1, 0,0,0, "Effect", self);
                            var pRed = player.getProperties(Properties.quarryReduction);
                            pRed.set(pRed.get()+2);
                        }));
    }

    public static Card Rabble(){
        return new Card("Rabble", RegistryPrice.ProsperityPrice(5), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0, 0,3,0, "Effect", self);
                            player.getGame().processAttack(
                                    player,
                                    self,
                                    vi -> {
                                        List<Card> view = CardUtil.getTopCards(vi, 3);
                                        new ArrayList<>(view).stream().filter(c -> c.hasType(CardType.ACTION) || c.hasType(CardType.TREASURE)).forEach(
                                                c -> {
                                                    vi.discard(c);
                                                    view.remove(c);
                                                }
                                        );
                                        if(view.isEmpty() || view.size() == 1)return;

                                        while(!view.isEmpty()){
                                            CardUtil.executeIfSelected(
                                                    () -> vi.chooseCardFromList("Put those cards in your Draw (in the order you want )", card -> true, view, false),
                                                    card -> {
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

    public static Card Tiara(){
        return new Card("Tiara", RegistryPrice.ProsperityPrice(4), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0, 0,0,1, "Effect", self);
                            CardUtil.executeIfSelected(
                                    () -> player.chooseCardFromHand("Choose a treasure to play it two times", card -> card.hasType(CardType.TREASURE), true),
                                    card -> {
                                        player.playCard(card);
                                        player.triggerEvent(TriggerComponent.OnCardPlayed.class, new Event(card, null, player));
                                        card.play(player);
                                    }
                            );
                        })
                        .onGain((owner, victim, event) -> {
                            CardUtil.executeOrOtherwise(
                                    () -> owner.chooseStringFromButtons("Do you want to put this card " + event.getCard() + " in your draw ? ", yesOrNo, true ),
                                    "y"::equals,
                                    choice -> event.setDest(Destination.DRAW),
                                    () -> {}
                            );
                        })
                        .onCondition((event, player) -> player == event.getPlayer())



                );

    }

    public static Card Vault(){
        return new Card("Vault", RegistryPrice.ProsperityPrice(5), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0, 0,2,0, "Effect", self);
                            self.set("continue", true);
                            while (self.getFlag("continue")){
                                CardUtil.executeOrOtherwise(
                                        () -> player.chooseCardFromHand("Choose a card to discard ( gain 1$ per card discarded )", true),
                                        Objects::nonNull,
                                        card -> {
                                            player.discard(card);
                                            player.increment(Item.MONEY, 1);
                                        },
                                        () -> self.set("continue", false)
                                );
                            }

                            player.getGame().processAttack(
                                    player,
                                    self,
                                    vi-> {
                                        int i;
                                        for (i = 0; i < 2; i++){
                                            Card choice = vi.chooseCardFromHand("Choose again " + (2-i) + " cards to draw a card", true);
                                            if(choice == null)break;
                                            vi.discard(choice);
                                        }
                                        if(i == 2)vi.draw(1);
                                    }
                            );
                        })
                );
    }

    public static Card War_Chest(){
        return new Card("War Chest", RegistryPrice.ProsperityPrice(5), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            nameACard(player, "War Chest");
                            List<Card> option = player.getGame()
                                    .getAvailableSupplyCards().stream()
                                    .filter(card -> card.getCost() <= 5 && !player.getGame().getNamedCardsThisTurn("War Chest").contains(card.getName()))
                                    .toList();

                            CardUtil.executeIfSelected(
                                    () -> player.chooseCardFromList("Choose a card from this list", card -> true, option, false),
                                    card -> {
                                        player.gain(card, Destination.DISCARD);
                                    }
                            );
                        })
                );
    }

    public static Card WatchTower(){
        return new Card("Watchtower", RegistryPrice.ProsperityPrice(5), CardType.ACTION, CardType.REACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            List<Card> hand = player.getCopyOf(Destination.HAND);
                            while(hand.size() < 6){
                                player.draw(1);
                            }
                        })
                        .onGain((owner, victim, event) -> {
                                CardUtil.executeOrOtherwise(
                                        () -> owner.chooseStringFromButtons("Do you want to put " + event.getCard().getName() +" in trash or in deck ?", List.of(new Button("trash", "t"), new Button("Deck", "d")), false),
                                        "t"::equals,
                                        choice ->{
                                            event.setDest(null);
                                            owner.moveToTrash(event.getCard());
                                        },
                                        () -> event.setDest(Destination.DRAW)
                                );
                        })
                        .onCondition((event, player) -> player == event.getPlayer())
                );
    }

    public static Card Workers_Village(){
        return new Card("Worker's Village",  RegistryPrice.ProsperityPrice(4), CardType.ACTION)
                .setup(config -> config.registerSimpleAction(1,2,1,0));
    }

    public static Card Contraband(){
        return new Card("Contreband", RegistryPrice.ProsperityPrice(5), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 3, 0,0,1, "Effect", self);
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

    public static Card Counting_House(){
        return new Card("Counting House",  RegistryPrice.ProsperityPrice(5), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            List<Card> discard = player.getCopyOf(Destination.DISCARD);
                            self.set("continue", true);
                            while(discard.stream().anyMatch(c -> c.hasName("Copper")) && self.getFlag("continue")){
                                CardUtil.executeOrOtherwise(
                                        () -> player.chooseCardFromList("Choose Coppers in your disard and put them in your hand", c -> c.hasName("Copper"), discard, true),
                                        Objects::nonNull,
                                        card -> {
                                            discard.remove(card);
                                            player.moveTo(card, Destination.HAND);
                                        },
                                        () -> self.set("continue", false)
                                );
                            }
                        })
                );
    }

    public static Card Goons(){
        return new Card("Goons",  RegistryPrice.ProsperityPrice(6), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 2,0,1,0, "Effect", self);
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

    public static Card Loan(){
        return new Card("Loan", RegistryPrice.ProsperityPrice(3), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 1, 0,0,0, "Effect", self);
                            boolean hasTreasure = false;
                            List<Card> discard = new ArrayList<>();
                            while(!hasTreasure){
                                Card c = player.getCardFromDeck();
                                if(c == null)break;
                                c.moveTo(discard);
                                if(c.hasType(CardType.TREASURE)){
                                    CardUtil.executeOrOtherwise(
                                            () -> player.chooseStringFromButtons("Do you want to trash this treasure", yesOrNo, true),
                                            "y"::equals,
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

    public static Card Mountebank(){
        return new Card("Mountebank", RegistryPrice.ProsperityPrice(5), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 2, 0,0,0, "Effect", self);
                            player.getGame().processAttack(
                                    player,
                                    self,
                                    vi -> {
                                        Card c  = vi.chooseCardFromHand("You may discard a curse", card -> card.hasType(CardType.CURSE), true);
                                        if(c!= null){
                                            vi.discard(c);
                                        }else{
                                            CardUtil.gainFromSupply(vi, "Curse", Destination.DISCARD, false);
                                            CardUtil.gainFromSupply(vi, "Copper", Destination.DISCARD, false);
                                        }
                                    }
                            );
                        })
                );
    }

    public static Card Royal_Seal(){
        return new Card("Royal Seal",  RegistryPrice.ProsperityPrice(5), CardType.TREASURE)
                .setup(config -> config
                        .registerSimpleAction(0,0,0,2)
                        .onGain((owner, victim, event) ->{
                            CardUtil.executeOrOtherwise(
                                    () -> owner.chooseStringFromButtons("Do you want to put this card onto your deck", yesOrNo, true),
                                    "y"::equals,
                                    choice -> event.setDest(Destination.DRAW),
                                    () -> {}
                            );
                        })
                        .onCondition((event, player) -> player == event.getPlayer())
                );
    }

    public static Card Talisman(){
        return new Card("Talisman", RegistryPrice.ProsperityPrice(4), CardType.TREASURE)
                .setup(config -> config
                        .registerSimpleAction(0,0,0,1)
                        .onBuy((player, card) -> {
                            if(card.hasType(CardType.VICTORY) || (card.getCost() > 4 && card.buyCondition(0, 0))) return;
                            CardUtil.gainFromSupply(player, card.getName(), Destination.DISCARD, false);

                        })
                );
    }

    public static Card Trade_Route(){
        return new Card("Trade Route",  RegistryPrice.ProsperityPrice(3), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0, 0, 0, 1,  "Effect", self);
                            CardUtil.executeIfSelected(
                                    () -> player.chooseCardFromHand("Trash a card", false),
                                    card -> {
                                        player.moveToTrash(card);
                                        player.increment(Item.MONEY, player.getValueOf(Item.COIN_TOKEN_ROUTE));
                                    }
                            );
                        })
                );
    }

    public static Card Venture(){
        return new Card("Venture", RegistryPrice.ProsperityPrice(5), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 1, 0, 0, 0,  "Effect", self);
                            boolean hasTreasure = false;
                            Card treasure = null;
                            List<Card> discard = new ArrayList<>();
                            while(!hasTreasure){
                                Card c = player.getCardFromDeck();
                                if(c == null)break;
                                c.moveTo(discard);
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
