package fr.umontpellier.iut.dominion.cards.Alchimie;

import fr.umontpellier.iut.dominion.*;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.RegistryPrice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class AlchimyFactory {
    public static List<Button> yesOrNo = List.of(new Button("Yes", "y"), new Button("No", "n"));




    public static Card Alchemist(){
        return new Card("Alchemist", RegistryPrice.AlchimyPrice(3, 1))
                .setup(config -> config
                        .registerSimpleAction(2, 1, 0, 0)
                        .onEndBuy((player, self) ->{
                                    boolean potionInPlay = player.getCopyOf(Destination.INPLAY)
                                            .stream().anyMatch(card -> card.hasName("Potion"));

                                    if(potionInPlay){
                                        CardUtil.executeOrOtherWise(
                                                () -> player.chooseStringFromButtons("Want to move your Alchemist on top of your deck ?",yesOrNo,true  ),
                                                "y"::equals,
                                                choice -> player.moveTo(self, Destination.DRAW),
                                                () -> {}
                                        );
                                    }

                                })
                );
    }

    public static Card Apothecary() {
        return new Card("Apothecary", RegistryPrice.AlchimyPrice(2, 1), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                           CardUtil.TriggerEffect(player, 0, 1, 1, 0, "Effect", self);

                            List<Card> revealed = CardUtil.getTopCards(player, 4);

                            List<Card> copperAndPotion = revealed.stream()
                                    .filter(c -> c.hasName("Copper") || c.hasName("Potion"))
                                    .collect(Collectors.toList());

                            while (!copperAndPotion.isEmpty()) {
                                Card chosen = player.chooseCardFromList(
                                        "Select a Copper or Potion to put in your hand (Cancel to leave on deck)",
                                        card -> true,
                                        copperAndPotion,
                                        false
                                );

                                if (chosen == null) break;

                                copperAndPotion.remove(chosen);
                                revealed.remove(chosen);
                                player.moveTo(chosen, Destination.HAND);
                            }


                            while (!revealed.isEmpty()) {
                                if (revealed.size() == 1) {
                                    player.moveTo(revealed.removeFirst(), Destination.DRAW);
                                    break;
                                }

                                Card toDeck = player.chooseCardFromList(
                                        "Choose the order to put back on deck (Last chosen = Top)",
                                        card -> true,
                                        revealed,
                                        false
                                );

                                if (toDeck != null) {
                                    revealed.remove(toDeck);
                                    player.moveTo(toDeck, Destination.DRAW);
                                }
                            }
                        })
                );
    }

    public static Card Apprentice(){
        return new Card("Apprentice", RegistryPrice.DominionPrice(5), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0, 1, 0, 0, "Effect", self);
                            CardUtil.executeIfSelected(
                                    () -> player.chooseCardFromHand("Choose a card to trash", false),
                                    card -> {
                                        player.moveToTrash(card);
                                        int cost =  card.getCost();
                                        int potion = card.getPotion();
                                        player.draw(cost + (potion > 0? 2:0));
                                    });
                        })
                );
    }

    public static Card Familiar(){
        return new Card("Familiar", RegistryPrice.AlchimyPrice(3, 1), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0, 1, 1, 0, "Effect", self);
                            player.getGame().processGain(player, self, Destination.DISCARD, "Curse");
                        })
                );
    }

    public static Card Golem(){
        return new Card("Golem", RegistryPrice.AlchimyPrice(4, 1), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            List<Card> revealed = new ArrayList<>();
                            List<Card> discard = new ArrayList<>();
                            int numberOfAction = 0;
                            Card c;
                            do {
                                c = player.getCardFromDeck();
                                if (c!= null)
                                {
                                    if (c.hasType(CardType.ACTION) && !c.hasName("Golem")) {
                                    numberOfAction++;
                                    c.moveTo(revealed);
                                    }
                                    else  c.moveTo(discard);
                                }
                            } while (c != null && numberOfAction<2);

                            while (!revealed.isEmpty()) {
                                CardUtil.executeIfSelected(
                                        () -> player.chooseCardFromList("Play those card in any order", card -> true, revealed, false),
                                        card -> {
                                            player.playCard(card);
                                            revealed.remove(card);
                                        }
                                );
                            }

                            new ArrayList<>(discard).forEach(card -> player.moveTo(card, Destination.DISCARD));                        })
                );
    }

    public static Card Herbalist(){
        return new Card("Herbalist", RegistryPrice.AlchimyPrice(2, 0), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 1, 0, 0, 1, "Effect", self);
                        })
                        .onEndBuy((player, self) -> {
                            List<Card> treasuresInPlay = player.getCopyOf(Destination.INPLAY).stream()
                                    .filter(c -> c.hasType(CardType.TREASURE))
                                    .toList();

                            if (!treasuresInPlay.isEmpty()) {
                                CardUtil.executeIfSelected(
                                        () -> player.chooseCardFromList(
                                                "Herbalist: Choose a Treasure to put on top of your deck",
                                                c -> true,
                                                treasuresInPlay,
                                                true
                                        ),
                                        treasure -> player.moveTo(treasure, Destination.DRAW)
                                );
                            }

                        })

                );
    }

    public static Card Philosopher_Stone() {
        return new Card("Philosopher's Stone", RegistryPrice.AlchimyPrice(3, 1), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            int deckCount = player.getCopyOf(Destination.DRAW).size();
                            int discardCount = player.getCopyOf(Destination.DISCARD).size();

                            int total = deckCount + discardCount;

                            int moneyGain = total / 5;

                            if (moneyGain > 0) {
                                CardUtil.TriggerEffect(player, moneyGain, 0, 0, 0, "Effect", self);
                            }
                        })
                );
    }

    public static Card Possession() {
        return new Card("Possession", RegistryPrice.AlchimyPrice(6, 1), CardType.ACTION)
                .setup(config ->
                        config.onPlay((player, self) -> {
                            Player victim = player.getGame().onTheLeft(player);
                            victim.preparePossession(player);
                        })
                );
    }

    public static Card Scrying_Pool(){
        return new Card("Scrying Pool", RegistryPrice.AlchimyPrice(2, 1), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0, 1, 0, 0, "Effect", self);
                            player.getGame().processGlobalEffect(
                                    player,
                                    user -> {
                                        Card c = user.getCardFromDeck();
                                        if(c != null){
                                            player.log( user.getName() + " reveal " + c.getName());

                                            List<Button> choices = List.of(new Button("Discard", "d"), new Button("Keep", "k"));
                                            String choice = player.getController().chooseStringFromButtons(
                                                    "Discard " + c.getName() + " from " + user.getName() + " ?",
                                                    choices,
                                                    false
                                            );

                                            if ("d".equals(choice)) {
                                                user.moveTo(c, Destination.DISCARD);
                                            }
                                        }
                                    }
                            );

                            List<Card> revealed = new ArrayList<>();
                            while (true){
                                Card c = player.getCardFromDeck();
                                if(c == null)break;
                                if(!c.hasType(CardType.ACTION))break;
                                c.moveTo(revealed);
                            }

                            revealed.forEach(card -> player.moveTo(card, Destination.HAND));

                        })
                );
    }

    public static Card Transmute(){
        return new Card("Transmute", RegistryPrice.AlchimyPrice(0, 1), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.executeIfSelected(
                                    () -> player.chooseCardFromHand("Choose a card to trash", false),
                                    card -> {
                                        player.moveToTrash(card);
                                        if(card.hasType(CardType.ACTION)) CardUtil.gainFromSupply(player, "Duchy", Destination.DISCARD, false);
                                        if(card.hasType(CardType.TREASURE))CardUtil.gainFromSupply(player, "Transmute", Destination.DISCARD, false);
                                        if(card.hasType(CardType.VICTORY))CardUtil.gainFromSupply(player, "Gold", Destination.DISCARD, false);
                                    }
                            );
                        })
                );
    }

    public static Card University(){
        return new Card("University", RegistryPrice.AlchimyPrice(2, 1), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, 0, 2, 0, 0, "Effect", self);
                            CardUtil.gainFromSupply(player, "Choose an Action card from supply cost up max", card -> card.getCost() <= 5 && card.hasType(CardType.ACTION), Destination.DISCARD, false);
                        })
                );
    }

    public static Card Vineyard(){
        return new Card("Vineyard", RegistryPrice.AlchimyPrice(0, 1), CardType.VICTORY)
                .setup(config -> config
                        .score(player -> (int) player.getCopyOf(Destination.HAND).stream().filter(card ->card.hasType(CardType.ACTION)).count() / 3));
    }
}
