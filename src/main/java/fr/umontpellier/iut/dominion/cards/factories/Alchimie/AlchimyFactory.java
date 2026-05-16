package fr.umontpellier.iut.dominion.cards.factories.Alchimie;

import fr.umontpellier.iut.dominion.*;
import fr.umontpellier.iut.dominion.Annotation.Dominion_Card;
import fr.umontpellier.iut.dominion.Annotation.InSet;
import fr.umontpellier.iut.dominion.Annotation.PileType;
import fr.umontpellier.iut.dominion.Player.Player;
import fr.umontpellier.iut.dominion.cards.Bonus;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.RegistryPrice;
import fr.umontpellier.iut.dominion.cards.factories.FactoryUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static fr.umontpellier.iut.dominion.cards.factories.FactoryUtil.*;

public class AlchimyFactory {
    @Dominion_Card(extension = "Alchemy")
    @InSet(value = {"Potion Mixers", "Chemistry Lesson"})
    public static Card Alchemist(){
        Bonus bonus = Bonus.empty().draw(2).with(Item.ACTION, 1);
        return new Card("Alchemist", RegistryPrice.AlchimyPrice(3, 1))
                .setup(config -> config
                        .registerSimpleAction(bonus)
                        .onEndBuy((player, self) ->{
                                    boolean potionInPlay = player.getCopyOf(Destination.INPLAY)
                                            .stream().anyMatch(card -> card.hasName("Potion"));

                                    if(potionInPlay){
                                        CardUtil.executeOrOtherwise(
                                                () -> player.chooseWhatToDo("Want to move your Alchemist on top of your deck ?", List.of(self), Button.yesOrNo,true),
                                                "y"::equals,
                                                choice -> player.moveTo(self, Destination.DRAW),
                                                () -> {}
                                        );
                                    }

                                })
                );
    }
    @Dominion_Card(extension = "Alchemy")
    @InSet(value = {"Potion Mixers"})
    public static Card Apothecary() {
        Bonus play =  Bonus.empty().draw(1).with(Item.ACTION, 1);
        return new Card("Apothecary", RegistryPrice.AlchimyPrice(2, 1), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                           CardUtil.TriggerEffect(player, EFFECT, self, play);

                            List<Card> revealed = CardUtil.getTopCards(player, 4);

                            List<Card> copperAndPotion = revealed.stream()
                                    .filter(c -> c.hasName("Copper") || c.hasName("Potion"))
                                    .collect(Collectors.toList());

                            while (!copperAndPotion.isEmpty()) {
                                 Optional<Card> chosen = player.chooseCardFromList(
                                        "Select a Copper or Potion to put in your hand (Cancel to leave on deck)",
                                        card -> true,
                                        copperAndPotion,
                                        true
                                );

                                if (chosen.isEmpty()) break;

                                copperAndPotion.remove(chosen.get());
                                revealed.remove(chosen.get());
                                player.moveTo(chosen.get(), Destination.HAND);
                            }


                            while (!revealed.isEmpty()) {
                                if (revealed.size() == 1) {
                                    player.moveTo(revealed.removeFirst(), Destination.DRAW);
                                    break;
                                }

                                Optional<Card> toDeck = player.chooseCardFromList(
                                        "Choose the order to put back on deck (Last chosen = Top)",
                                        card -> true,
                                        revealed,
                                        false
                                );

                                if (toDeck.isPresent()) {
                                    revealed.remove(toDeck.get());
                                    player.moveTo(toDeck.get(), Destination.DRAW);
                                }
                            }
                        })
                );
    }
    @Dominion_Card(extension = "Alchemy")
    @InSet(value = {"Forbidden Arts"})
    public static Card Apprentice(){
        Bonus  play =  Bonus.empty().with(Item.ACTION, 1);
        return new Card("Apprentice", RegistryPrice.DominionPrice(5), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player,EFFECT, self, play);
                             player.chooseCardFromHand("Choose a card to trash", false)
                                     .ifPresent(card -> {
                                         player.trash(card);
                                         int cost =  card.getCost();
                                         int potion = card.getPotion();
                                         player.draw(cost + (potion > 0? 2:0));
                                    });
                        })
                );
    }
    @Dominion_Card(extension = "Alchemy")
    @InSet(value = {"Forbidden Arts"})
    public static Card Familiar(){
        Bonus play =  Bonus.empty().with(Item.ACTION, 1).draw(1);
        return new Card("Familiar", RegistryPrice.AlchimyPrice(3, 1), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player,EFFECT, self, play);
                            player.getGame().processGain(player, self, Destination.DISCARD, "Curse");
                        })
                );
    }
    @Dominion_Card(extension = "Alchemy")
    @InSet(value = {"Potion Mixers", "Chemistry Lesson"})
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
                                    c.moveTo(revealed, null);
                                    }
                                    else  c.moveTo(discard, null);
                                }
                            } while (c != null && numberOfAction<2);

                            while (!revealed.isEmpty()) {
                                 player.chooseCardFromList("Play those card in any order", card -> true, revealed, false)
                                         .ifPresent(card -> {
                                             player.playCard(card);
                                             linkedCard(self, card);
                                             revealed.remove(card);
                                        }
                                );
                            }

                            new ArrayList<>(discard).forEach(card -> player.moveTo(card, Destination.DISCARD));
                        })
                        .stayInPlayCondition(checkLink)
                );
    }
    @Dominion_Card(extension = "Alchemy")
    @InSet(value = {"Potion Mixers"})
    public static Card Herbalist(){
        Bonus  play =  Bonus.empty().with(Item.BUY,1).with(Item.MONEY, 1);
        return new Card("Herbalist", RegistryPrice.AlchimyPrice(2, 0), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> CardUtil.TriggerEffect(player,EFFECT, self, play))
                        .onEndBuy((player, self) -> {
                            List<Card> treasuresInPlay = player.getCopyOf(Destination.INPLAY).stream()
                                    .filter(c -> c.hasType(CardType.TREASURE))
                                    .toList();

                            if (!treasuresInPlay.isEmpty()) {
                                player.chooseCardFromList(
                                                "Herbalist: Choose a Treasure to put on top of your deck",
                                                c -> true,
                                                treasuresInPlay,
                                                true
                                        ).ifPresent(treasure -> player.moveTo(treasure, Destination.DRAW)
                                );
                            }

                        })

                );
    }
    @Dominion_Card(extension = "Alchemy")
    @InSet(value = {"Chemistry Lesson"})
    public static Card Philosopher_Stone() {
        return new Card("Philosopher's Stone", RegistryPrice.AlchimyPrice(3, 1), CardType.TREASURE)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            int deckCount = player.getCopyOf(Destination.DRAW).size();
                            int discardCount = player.getCopyOf(Destination.DISCARD).size();

                            int total = deckCount + discardCount;

                            int moneyGain = total / 5;

                            if (moneyGain > 0) {
                                Bonus bonus = Bonus.empty().with(Item.MONEY, moneyGain);
                                CardUtil.TriggerEffect(player, ACTION, self, bonus);
                            }
                        })
                );
    }
    @Dominion_Card(extension = "Alchemy")
    @InSet(value = {"Forbidden Arts"})
    public static Card Possession() {
        return new Card("Possession", RegistryPrice.AlchimyPrice(6, 1), CardType.ACTION)
                .setup(config ->
                        config.onPlay((player, self) -> {
                            Player victim = player.getGame().onTheLeft(player);
                            victim.preparePossession(player);
                        })
                );
    }
    @Dominion_Card(extension = "Alchemy")
    public static Card Scrying_Pool(){
        Bonus bonus = Bonus.empty().with(Item.ACTION, 1);
        return new Card("Scrying Pool", RegistryPrice.AlchimyPrice(2, 1), CardType.ACTION, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player,EFFECT, self, bonus);
                            player.getGame().processGlobalEffect(
                                    player,
                                    user -> {
                                        Card c = user.getCardFromDeck();
                                        if(c != null){
                                            player.log( user.getName() + " reveal " + c.getName());

                                            List<Button> choices = List.of(new Button("Discard", "d"), new Button("Keep", "k"));
                                            String choice = player.chooseWhatToDo(
                                                    "Discard " + c.getName() + " from " + user.getName() + " ?",
                                                    List.of(c),
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
                                c.moveTo(revealed, null);
                            }

                            revealed.forEach(card -> player.moveTo(card, Destination.HAND));

                        })
                );
    }
    @Dominion_Card(extension = "Alchemy")
    @InSet(value = {"Potion Mixers"})
    public static Card Transmute(){
        return new Card("Transmute", RegistryPrice.AlchimyPrice(0, 1), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> player.chooseCardFromHand("Choose a card to trash", false)
                                .ifPresent(card -> {
                                    player.trash(card);
                                    if(card.hasType(CardType.ACTION)) CardUtil.gainFromSupply(player, "Duchy", Destination.DISCARD, false);
                                    if(card.hasType(CardType.TREASURE))CardUtil.gainFromSupply(player, "Transmute", Destination.DISCARD, false);
                                    if(card.hasType(CardType.VICTORY))CardUtil.gainFromSupply(player, "Gold", Destination.DISCARD, false);
                                }
                        ))
                );
    }
    @Dominion_Card(extension = "Alchemy")
    @InSet(value = {"Forbidden Arts", "Chemistry Lesson"})
    public static Card University(){
        Bonus bonus = Bonus.empty().with(Item.ACTION, 2);
        return new Card("University", RegistryPrice.AlchimyPrice(2, 1), CardType.ACTION)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player,EFFECT, self, bonus);
                            CardUtil.gainFromSupply(player, "Choose an Action card from supply cost up max", card -> card.isAtMost(5) && card.hasType(CardType.ACTION), Destination.DISCARD, false);
                        })
                );
    }
    @Dominion_Card(extension = "Alchemy", pileType = PileType.VICTORY)
    public static Card Vineyard(){
        return new Card("Vineyard", RegistryPrice.AlchimyPrice(0, 1), CardType.VICTORY)
                .setup(config -> config
                        .score(player -> (int) player.getCopyOf(Destination.HAND).stream().filter(card ->card.hasType(CardType.ACTION)).count() / 3));
    }
}
