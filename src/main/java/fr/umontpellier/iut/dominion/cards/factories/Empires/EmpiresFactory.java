//package fr.umontpellier.iut.dominion.cards.factories.Empires;
//
//import fr.umontpellier.iut.dominion.*;
//import fr.umontpellier.iut.dominion.Player.Player;
//import fr.umontpellier.iut.dominion.cards.Bonus.Bonus;
//import fr.umontpellier.iut.dominion.cards.Card;
//import fr.umontpellier.iut.dominion.cards.CardUtil;
//import fr.umontpellier.iut.dominion.cards.RegistryPrice;
//import fr.umontpellier.iut.dominion.cards.component.*;
//import org.apache.logging.log4j.util.TriConsumer;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.function.BiPredicate;
//
//import static fr.umontpellier.iut.dominion.cards.CardConfigurator.bonus;
//import static fr.umontpellier.iut.dominion.cards.CardConfigurator.empty;
//import static fr.umontpellier.iut.dominion.cards.factories.FactoryUtil.*;
//import static fr.umontpellier.iut.dominion.cards.factories.FactoryUtil.EFFECT;
//import static fr.umontpellier.iut.dominion.cards.factories.FactoryUtil.lessThan;
//
//public class EmpiresFactory {
//    public static Card Archive(){
//        Bonus action = Bonus.empty().with(Item.ACTION, 1);
//        return new Card("Archive", RegistryPrice.Empires(5, 0), CardType.ACTION, CardType.DURATION)
//                .setup(config -> config
//                        .onPlay(bonus(action)
//                                .then((player, card) -> {
//                                    List<Card> toAside = CardUtil.getTopCards(player, 3);
//                                    card.set("Aside",  toAside);
//                                    player.moveAll(toAside, Destination.ASIDE);
//                                    player.chooseCardFromList("Put one into your hand", c -> true, toAside, false)
//                                            .ifPresent(c ->{
//                                                        player.moveTo(c, Destination.HAND);
//                                                        toAside.remove(c);
//                                                    });
//
//                                })
//                        ).onDurationWithTime((player, self) ->{
//                            List<Card> aside = new ArrayList<>(self.getCollection("Aside"));
//                            player.chooseCardFromList("Put one into your hand", c -> true, aside, false)
//                                    .ifPresent(c ->{
//                                        player.moveTo(c, Destination.HAND);
//                                        self.getCollection("Aside").remove(c);
//                                    });
//                        }, 2)
//                        .stayInPlayCondition(card -> checkDuration.and(self -> !self.getCollection("Aside").isEmpty()).test(card))
//
//                );
//    }
//
//    public static Card Capital(){
//        Bonus buy_money =  Bonus.empty().with(Item.BUY, 1).with(Item.MONEY, 6);
//        Bonus debt = Bonus.empty().with(Item.DEBT, 6);
//
//        return Card.treasure("Capital", RegistryPrice.Empires(5, 0))
//                .setup(config -> config
//                        .registerSimpleAction(buy_money)
//                        .checkItselfDiscard(empty(TriggerComponent.CheckItselfDiscarded.class)
//                                .lookingAt((event, card) -> event.getPlayer())
//                                .thenDo((player, card) -> CardUtil.TriggerEffect(player, "Discard Effect", card, debt))
//                                .end()
//                        )
//                        .itselfDiscardCondition((event, player) -> event.initialCameFrom(Destination.INPLAY))
//                );
//    }
//
//    public static Card Chariot_race(){
//        Bonus action =  Bonus.empty().with(Item.ACTION, 1);
//        return Card.action("Chariot Race",  RegistryPrice.Empires(3, 0))
//                .setup(config -> config
//                        .onPlay(bonus(action)
//                                .lookingAt((player, card) -> {
//                                    Player left = player.getGame().onTheLeft(player);
//                                    return new Tuple.RaceCompetitors(player.drawToHand(), left, left.getCardFromDeck());
//                                })
//                                .thenWith((player, triplet) -> player.reveals(triplet.myCard()))
//                                .filter(triplet -> triplet.myCard() != null && triplet.opponentCard() != null)
//                                .thenWith((player, triplet) -> {
//                                    triplet.opponent().reveals(triplet.opponentCard());
//                                    if(lessThan.test(triplet.opponentCard(), triplet.myCard())){
//                                        player.increment(Item.MONEY, 1);
//                                        player.increment(Item.VICTORY_TOKEN, 1);
//                                    }
//                                })
//                                .end()
//                        )
//                );
//    }
//
//    public static Card Charm(){
//        Bonus buy_money = Bonus.empty().with(Item.BUY, 1).with(Item.MONEY, 2);
//        return Card.treasure("Charm",  RegistryPrice.Empires(5, 0))
//                .setup(config -> config
//                        .onPlay(empty(OnPlayComponent.class)
//                                .choose()
//                                .chooseWhatToDo((player, card) -> new InteractionRequest.Builder<Boolean>()
//                                        .instruction("Choose :  1 buy and 2$, or a next time side effect")
//                                        .cards(List.of(card))
//                                        .buttons(List.of(new Button("buy and $", "bonus"), new Button("side effect", "effect")))
//                                        .build())
//                                .thenDo((player, card, string) -> {
//                                    switch (string){
//                                        case "bonus" -> CardUtil.TriggerEffect(player, EFFECT, card, buy_money);
//                                        case "effect" -> player.addCardEffect(card);
//                                        default -> {}
//                                    }
//                                })
//                                .end()
//                        )
//                        .onGain((event, player) -> {
//                            Card c = CardUtil.gainFromSupply(player, "Gain a card costing exactly the same amount as " + event.getCard(), card -> !card.hasSameNameAs(event.getCard()) && card.isEqualWithBonus(event.getCard(), 0) , Destination.DISCARD, true);
//                            if( c != null) player.removeCardEffect(config.get());
//                        })
//                        .duringGainCondition((event, player) -> event.player == player)
//
//                );
//    }
//
//    public static Card City_Quarter(){
//        Bonus actions =  Bonus.empty().with(Item.ACTION, 2);
//        return Card.action("City Quarter",  RegistryPrice.Empires(0, 8))
//                .setup(config -> config
//                        .onPlay(bonus(actions)
//                                .lookingAt((player, card) -> player.getCopyOf(Destination.HAND))
//                                .thenWith(Player::reveals)
//                                .map((player, list) -> (int) list.stream().filter(c -> c.hasType(CardType.ACTION)).count())
//                                .thenWith(Player::draw)
//                                .end()
//                        )
//                );
//    }
//
//    public static Card Crown(){
//        TriConsumer<Player, Card, Card> play = (player, card, self) -> {
//            player.playCard(card, 2);
//            linkedCard(self, card);
//        };
//
//        BiPredicate<Card, Boolean> check = (card, b) -> b ? card.hasType(CardType.ACTION) : card.hasType(CardType.TREASURE);
//
//        return new Card("Crown", RegistryPrice.Empires(5, 0), CardType.ACTION, CardType.TREASURE)
//                .setup(config -> config
//                        .onPlay((player, card) -> {
//                            boolean isAction = player.getFlag("Action").get();
//                            player.chooseCardFromHand("Play a card twice from your hand",c -> check.test(c, isAction) ,true)
//                                    .ifPresent(c -> play.accept(player, c, card));
//
//                        })
//                        .stayInPlayCondition(checkLink)
//                );
//    }
//
//    public static Card Enchantress(){
//        return new Card("Enchantress", RegistryPrice.Empires(5, 0), CardType.ActionAndAttack).addType(CardType.DURATION)
//                .setup(config -> config
//                        .onPlay(empty(OnPlayComponent.class)
//                                .first((player, self) -> self.set("Players", player.getGame().scanImmunity(player)))
//                                .then((player, card) ->
//                                        player.getGame().processAttack(player, card, vi -> vi.getFlag(Flags.enchantress).set(true)))
//                        )
//                        .duringGainCondition((event, player) -> !event.isSamePlayer(player) && event.player.getFlag(Flags.enchantress).get())
//
//                );
//    }
//
//
//
//    public static Card Castles(){
//        return TCastles("Castles").addType(CardType.TEMPLATE);
//    }
//
//    public static Card TCastles(String name){
//        return new Card(name, RegistryPrice.Empires(3, 0), CardType.VICTORY, CardType.CASTLE);
//    }
//
//    public static Card Catapults_Rocks(){
//        return new Card("Catapult Rocks", RegistryPrice.Empires(3, 0), CardType.ActionAndAttack).addType(CardType.TEMPLATE);
//    }
//
//    public static Card Encampment_Plunder(){
//        return new Card("Encampment Plunder", RegistryPrice.Empires(2, 0), CardType.ACTION, CardType.TEMPLATE);
//    }
//
//
//
//
//}
