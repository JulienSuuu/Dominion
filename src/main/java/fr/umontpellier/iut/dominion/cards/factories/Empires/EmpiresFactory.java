package fr.umontpellier.iut.dominion.cards.factories.Empires;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Item;
import fr.umontpellier.iut.dominion.Player.Player;
import fr.umontpellier.iut.dominion.cards.Bonus;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.RegistryPrice;
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent;
import fr.umontpellier.iut.dominion.cards.component.Triplet;
import fr.umontpellier.iut.dominion.cards.factories.FactoryUtil;

import java.util.ArrayList;
import java.util.List;

import static fr.umontpellier.iut.dominion.cards.CardConfigurator.bonus;
import static fr.umontpellier.iut.dominion.cards.CardConfigurator.empty;
import static fr.umontpellier.iut.dominion.cards.factories.FactoryUtil.lessThan;

public class EmpiresFactory {
    public static Card Archive(){
        Bonus action = Bonus.empty().with(Item.ACTION, 1);
        return new Card("Archive", RegistryPrice.Empires(5, 0), CardType.ACTION, CardType.DURATION)
                .setup(config -> config
                        .onPlay(bonus(action)
                                .then((player, card) -> {
                                    List<Card> toAside = CardUtil.getTopCards(player, 3);
                                    card.set("Aside",  toAside);
                                    player.moveAll(toAside, Destination.ASIDE);
                                    player.chooseCardFromList("Put one into your hand", c -> true, toAside, false)
                                            .ifPresent(c ->{
                                                        player.moveTo(c, Destination.HAND);
                                                        toAside.remove(c);
                                                    });

                                })
                        ).onDurationWithTime((player, self) ->{
                            List<Card> aside = new ArrayList<>(self.getCollection("Aside"));
                            player.chooseCardFromList("Put one into your hand", c -> true, aside, false)
                                    .ifPresent(c ->{
                                        player.moveTo(c, Destination.HAND);
                                        self.getCollection("Aside").remove(c);
                                    });
                        }, 2)
                        .stayInPlayCondition(card -> FactoryUtil.checkDuration.and(self -> !self.getCollection("Aside").isEmpty()).test(card))

                );
    }

    public static Card Capital(){
        Bonus buy_money =  Bonus.empty().with(Item.BUY, 1).with(Item.MONEY, 6);
        Bonus debt = Bonus.empty().with(Item.DEBT, 6);

        return Card.treasure("Capital", RegistryPrice.Empires(5, 0))
                .setup(config -> config
                        .registerSimpleAction(buy_money)
                        .checkItselfDiscard(empty(TriggerComponent.checkItselfDiscarded.class)
                                .lookingAt((event, card) -> event.getPlayer())
                                .thenDo((player, card) -> CardUtil.TriggerEffect(player, "Discard Effect", card, debt))
                                .end()
                        )
                        .itselfDiscardCondition((event, player) -> event.initialCameFrom(Destination.INPLAY))
                );
    }

    public static Card Chariot_race(){
        Bonus action =  Bonus.empty().with(Item.ACTION, 1);
        return Card.action("Chariot Race",  RegistryPrice.Empires(3, 0))
                .setup(config -> config
                        .onPlay(bonus(action)
                                .lookingAt((player, card) -> {
                                    Player left = player.getGame().onTheLeft(player);
                                    return new Triplet<>(player.getCardFromDeck(), left, left.getCardFromDeck());

                                })
                                .thenWith((player, triplet) -> {
                                    player.reveals(triplet.first());
                                    player.draw(1);
                                })
                                .filter(triplet -> triplet.first()!= null && triplet.third() != null)
                                .thenWith((player, triplet) -> {
                                    triplet.second().reveals(triplet.third());
                                    if(lessThan.test(triplet.third(), triplet.first())){
                                        player.increment(Item.MONEY, 1);
                                        player.increment(Item.VICTORY_TOKEN, 1);
                                    }
                                })
                                .end()
                        )
                );
    }





    public static Card Castles(){
        return TCastles("Castles").addType(CardType.TEMPLATE);
    }

    public static Card TCastles(String name){
        return new Card(name, RegistryPrice.Empires(3, 0), CardType.VICTORY, CardType.CASTLE);
    }

    public static Card Catapults_Rocks(){
        return new Card("Catapult Rocks", RegistryPrice.Empires(3, 0), CardType.ActionAndAttack).addType(CardType.TEMPLATE);
    }




}
