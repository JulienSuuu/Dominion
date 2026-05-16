package fr.umontpellier.iut.dominion.cards.factories.Adventures;

import fr.umontpellier.iut.dominion.*;
import fr.umontpellier.iut.dominion.Annotation.Dominion_Card;
import fr.umontpellier.iut.dominion.Annotation.PileType;
import fr.umontpellier.iut.dominion.Button;
import fr.umontpellier.iut.dominion.Player.Player;
import fr.umontpellier.iut.dominion.Player.Tokens.JourneyFace;
import fr.umontpellier.iut.dominion.Player.Tokens.Token;
import fr.umontpellier.iut.dominion.Properties;
import fr.umontpellier.iut.dominion.cards.*;
import fr.umontpellier.iut.dominion.cards.Events.Discard_Type;
import fr.umontpellier.iut.dominion.cards.Events.Event;
import fr.umontpellier.iut.dominion.cards.component.*;
import fr.umontpellier.iut.dominion.cards.factories.FactorySupplyPile;
import fr.umontpellier.iut.dominion.cards.factories.FactoryUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;

import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static fr.umontpellier.iut.dominion.cards.CardConfigurator.*;
import static fr.umontpellier.iut.dominion.cards.factories.FactoryUtil.*;

public class AdventuresFactory {

    @Dominion_Card(extension = "Adventures")
    public static Card Amulet(){
        Bonus money = Bonus.empty().with(Item.MONEY, 1);
        List<Button> buttons = new ArrayList<>();
        buttons.add(new Button("+1$", "m"));
        buttons.add(new Button("trash", "t"));
        buttons.add(new Button("+ Silver", "s"));
        BiConsumer<Player, Card> action = (player, card) -> {
            String choice = player.chooseWhatToDo("Choose : +1$, or trash a card, or gain a silver", List.of(card), buttons, false);
            switch (choice) {
                case "m" -> CardUtil.TriggerEffect(player, ACTION, card, money);
                case "t" -> player.trash(1);
                case "s" -> CardUtil.gainFromSupply(player, "Silver", Destination.DISCARD, false);
            }
        };

        return new Card("Amulet", RegistryPrice.Adventure(3), CardType.ACTION, CardType.DURATION)
                .setup(config -> config
                        .onPlay(action::accept)
                        .onDuration(action::accept)
                );
    }

    @Dominion_Card(extension = "Adventures")
    public static Card Artificer(){
        Bonus card_action_money =  Bonus.empty().draw(1).with(Item.ACTION, 1).with(Item.MONEY, 1);
        return Card.action("Artificer", RegistryPrice.Adventure(5))
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, card_action_money);
                            player.discardUntilYouStop(Destination.HAND, integer ->
                                    CardUtil.gainFromSupply(player,
                                            "Gain a card costing exactly " + integer + "$",
                                            card -> card.isEqual(integer), Destination.DRAW, true));
                        })
                );
    }

    @Dominion_Card(extension = "Adventures")
    public static Card Bridge_Troll(){
        Bonus buy = Bonus.empty().with(Item.BUY, 1);

        BiConsumer<Player, Card> action = (player, card) -> {
            CardUtil.TriggerEffect(player, ACTION, card, buy);
            IntegerProperty red = GameStat.reduction;
            red.set(red.get() + 1);
        };

        return new Card("Bridge Troll", RegistryPrice.Adventure(5), CardType.ACTION, CardType.ATTACK, CardType.DURATION)
                .setup(config -> config
                        .onPlay((player, self) ->{
                            player.getGame().processAttack(player, self, vi -> vi.getPersistentFlag(Token.TAX_TOKEN.name()).set(true));
                            action.accept(player, self);
                        })
                        .onDuration(action::accept)
                );
    }

    @Dominion_Card(extension = "Adventures")
    public static Card Caravan_Guard(){
        Bonus card_action = Bonus.empty().draw(1).with(Item.ACTION, 1);
        Bonus money = Bonus.empty().with(Item.MONEY, 1);
        return new Card("Caravan_Guard", RegistryPrice.Adventure(3), CardType.ACTION, CardType.DURATION, CardType.REACTION)
                .setup(config -> config
                        .registerSimplePlayAndDuration(card_action, money)
                        .beforeCardPlayed((event, owner) -> {
                            owner.chooseWhatToDoOpt("Do you want to play this ?", List.of(config.get()), Button.yesOrNo, true)
                                    .filter("y"::equals)
                                    .ifPresent(c -> owner.playCard(config.get()));
                        })
                        .beforeCardPlayedCondition((event, player) -> event.getPlayer() != player && event.getCard().hasType(CardType.ATTACK))
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Coin_Of_The_Realm(){
        Bonus money =  Bonus.empty().with(Item.MONEY, 1);
        return new Card("Coin of the Realm", RegistryPrice.Adventure(2), CardType.TREASURE, CardType.RESERVE)
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, money);
                        })
                        .afterCardPlayed((event, player) -> player.increment(Item.ACTION, 2))
                        .afterCardPlayedCondition(reserveCondition(config.get(), (event, player)-> event.getCard().hasType(CardType.ACTION)))
                );
    }
    @Dominion_Card(extension = "Adventures", pileType = PileType.VICTORY)
    public static Card Distant_Lands(){
        return new Card("Distant Lands", RegistryPrice.Adventure(5), CardType.ACTION, CardType.VICTORY, CardType.RESERVE)
                .setup(config -> config
                        .score(player ->  config.get().hasForLocation(Destination.TAVERN)? 4 : 0)
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Dungeon(){
        Bonus action = Bonus.empty().with(Item.ACTION, 1);
        Consumer<Player> drawAndDiscard = player -> {
            player.draw(2);
            player.discardFromHand(2);
        };


        return new Card("Dungeon", RegistryPrice.Adventure(3), CardType.ACTION, CardType.DURATION)
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, action);
                            drawAndDiscard.accept(player);
                        })
                        .onDuration((player, card) -> drawAndDiscard.accept(player))
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Duplicate(){
        return new Card("Duplicate", RegistryPrice.Adventure(4), CardType.ACTION, CardType.RESERVE)
                .setup(config -> config
                        .afterGain((event, player) -> {
                            Card c = event.getCard();
                            CardUtil.gainFromSupply(player, c.getName(), Destination.DISCARD, false);
                        })
                        .afterGainCondition(reserveCondition(config.get(), (event, player) -> event.getCard().isAtMost(6)))
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Gear(){
        Bonus cards = Bonus.empty().draw(2);
        return new Card("Gear",  RegistryPrice.Adventure(3), CardType.ACTION, CardType.DURATION)
                .setup(config -> config
                        .onPlay((player, self) ->{
                            CardUtil.TriggerEffect(player, EFFECT, self, cards);
                            List<Card> aside = new ArrayList<>();
                            for(int i = 0; i<2; i++){
                                player.chooseCardFromHand("Set aside " + (2-i) + "card from your hand", true)
                                        .ifPresent(card -> {
                                            player.moveTo(card, Destination.ASIDE);
                                            aside.add(card);
                                        });
                            }
                            self.set("cards", aside);
                        })
                        .onDurationWithTrigger((player, self) -> {
                            Collection<Card> aside = self.getCollection("cards");
                            if(aside==null || aside.isEmpty())return;
                            aside.forEach(card -> {
                                player.moveTo(card, Destination.HAND);
                            });
                        }, card -> {
                            Collection<Card> aside = config.get().getCollection("cards");
                            return aside != null && !aside.isEmpty();
                        })
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Giant(){
        Bonus face_down = Bonus.empty().with(Item.MONEY, 1);
        Bonus face_Up = Bonus.empty().with(Item.MONEY, 5);
        return new Card("Giant", RegistryPrice.Adventure(5), CardType.ActionAndAttack)
                .setup(config -> config
                        .onPlay((player, self) ->{
                            JourneyFace face = player.flipJourneyToken();
                            if(face == JourneyFace.FACE_DOWN){
                                CardUtil.TriggerEffect(player, EFFECT, self, face_down);
                            }else if(face == JourneyFace.FACE_UP){
                                CardUtil.TriggerEffect(player, EFFECT, self, face_Up);
                                player.getGame().processAttack(player, self, vi -> {
                                    Card c = vi.getCardFromDeck();

                                    if(c!= null && c.isBetween(3, 6)) vi.trash(c);
                                    else {
                                        vi.discard(c);
                                        CardUtil.gainFromSupply(vi, "Curse",  Destination.DISCARD, false);
                                    }
                                });
                            }
                        })
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Guide(){
        Bonus card_action =  Bonus.empty().draw(1).with(Item.ACTION, 1);
        return new Card("Guide", RegistryPrice.Adventure(3), CardType.ACTION, CardType.RESERVE)
                .setup(config -> config
                        .registerSimpleAction(card_action)
                        .onStartTurn(player -> {
                            player.discardAll(Destination.HAND);
                            player.draw(5);
                        })
                        .onStartTurnCondition(reserveCondition(config.get(), (event, player) -> true ))
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Haunted_Woods(){
        Bonus cards = Bonus.empty().draw(3);
        return new Card("Haunted Woods", RegistryPrice.Adventure(5), CardType.ACTION, CardType.ATTACK,  CardType.DURATION)
                .setup(config -> config
                        .registerSimpleDuration(cards)
                        .onGain((event, self) -> {
                            Player vi = event.getPlayer();
                            vi.moveAllAndChooseTheOrder(Destination.HAND, Destination.DRAW);
                        })
                        .duringGainCondition((event, player) ->
                                event.getPlayer() != player
                                && event.getPlayer().getFlag(Flags.onBuyPhase).get()
                                && activate.test(config.get())
                                && event.isBuy()
                        )
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Hireling(){
        Bonus draw = Bonus.empty().draw(1);
        return new Card("Hireling", RegistryPrice.Adventure(6), CardType.ACTION, CardType.DURATION)
                .setup(config -> config
                        .registerSimpleAction(draw)
                        .onInfiniteDuration((player, self) -> {player.draw(1);})
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Lost_City(){
        Bonus draw_action = Bonus.empty().draw(2).with(Item.ACTION, 2);
        return Card.action("Lost City", RegistryPrice.Adventure(5))
                .setup(config -> config
                        .registerSimpleAction(draw_action)
                        .checkGain((event, self) -> {
                            Player player = event.getPlayer();
                            player.getGame().processBenefit(player, vi -> {vi.draw(1);});
                        })
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Magpie(){
        Bonus draw_action =   Bonus.empty().draw(1).with(Item.ACTION, 1);
        return Card.action("Magpie", RegistryPrice.Adventure(4))
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, draw_action);
                            Card c = player.getCardFromDeck();
                            if(c == null) return;
                            if(c.hasType(CardType.TREASURE)) player.moveTo(c, Destination.HAND);
                            if(c.hasType(CardType.ACTION) || c.hasType(CardType.VICTORY)) CardUtil.gainFromSupply(player, "Magpie", Destination.DISCARD, false);
                        })
                );

    }
    @Dominion_Card(extension = "Adventures")
    public static Card Messenger(){
        Bonus buy_Money = Bonus.empty().with(Item.BUY, 1).with(Item.MONEY, 2);
        return Card.action("Messenger", RegistryPrice.Adventure(4))
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, buy_Money);
                            player.chooseWhatToDoOpt("Do you want to put your deck into your discard ?", List.of(self) ,Button.yesOrNo, true)
                                    .filter("y"::equals)
                                    .ifPresent( choice -> player.moveAll(Destination.DRAW, Destination.DISCARD));
                        })
                        .checkGain((event, self) -> {
                            Player player = event.getPlayer();
                            Card c = CardUtil.gainFromSupply(player, "Gain a card costing up to 4$", card -> card.isAtMost(4), Destination.DISCARD, false);
                            if (c == null) return;
                            player.getGame().processBenefit(player, vi -> CardUtil.gainFromSupply(vi, c.getName(), Destination.DISCARD, false));
                        })
                        .itselfGainCondition((event, player) -> player.getFlag(Flags.onBuyPhase).get() && player.getProperties(Properties.Cards_Bought).get() == 0)
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Miser(){
        return Card.action("Miser", RegistryPrice.Adventure(4))
                .setup(config -> config
                        .onPlay((player, self) -> {
                            String choice = player.chooseWhatToDo("Choose : put a copper in your Tavern, or take 1$ per Copper into",  List.of(self) ,List.of(new Button("Add Copper", "a"), new Button("Take money", "t")), false);
                            switch (choice) {
                                case "a" -> player.chooseCardFromHand("Take a copper and put it on your tavern", c -> c.hasName("Copper"), false).ifPresent(card -> player.moveTo(card, Destination.TAVERN));
                                case "t" -> {
                                    Number n = player.getCopyOf(Destination.TAVERN).stream().filter(c -> c.hasName("Copper")).count();
                                    player.increment(Item.MONEY, n.intValue());
                                }
                            }
                        })

                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Page(){
        Bonus card_action = Bonus.empty().with(Item.ACTION, 1).draw(1);
        return new Card("Page", RegistryPrice.Adventure(4), CardType.ACTION, CardType.TRAVELLER)
                .setup(config -> config
                        .registerSimpleAction(card_action)
                        .checkItselfDiscard((event, self) -> travellerUpgrade(event, self, "Treasure Hunter", true))
                        .itselfDiscardCondition((event, player) -> event.initialCameFrom(Destination.INPLAY))
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Peasant(){
        Bonus buy_money =  Bonus.empty().with(Item.BUY, 1).with(Item.MONEY, 1);
        return new Card("Peasant", RegistryPrice.Adventure(2), CardType.ACTION, CardType.TRAVELLER)
                .setup(config -> config
                        .registerSimpleAction(buy_money)
                        .checkItselfDiscard((event, card) ->  travellerUpgrade(event, card, "Soldier", true))
                        .itselfDiscardCondition((event, player) -> event.initialCameFrom(Destination.INPLAY))
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Port(){
        Bonus card_Action = Bonus.empty().with(Item.ACTION, 2).draw(1);
        return Card.action("Port", RegistryPrice.Adventure(4))
                .setup(config -> config
                        .registerSimpleAction(card_Action)
                        .checkGain((event, self) ->{
                            Player player = event.getPlayer();
                            BooleanProperty p = player.getFlag(self.getName());
                            p.set(true);
                            CardUtil.gainFromSupply(player, self.getName(), Destination.DISCARD, false);
                            p.set(false);
                        })
                        .itselfGainCondition((event, player) -> !player.getFlag(event.getCard().getName()).get())
                );

    }
    @Dominion_Card(extension = "Adventures")
    public static Card Ranger(){
        Bonus buy = Bonus.empty().with(Item.BUY, 1);
        return Card.action("Ranger", RegistryPrice.Adventure(4))
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, buy);
                            JourneyFace j = player.flipJourneyToken();
                            if(j == JourneyFace.FACE_UP) player.draw(5);
                        })
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Ratcatcher(){
        Bonus card_action = Bonus.empty().with(Item.ACTION, 1).draw(1);
        return new Card("Ratcatcher", RegistryPrice.Adventure(2), CardType.RESERVE, CardType.ACTION)
                .setup(config -> config
                        .registerSimpleAction(card_action)
                        .onStartTurn(player -> player.trash(1))
                        .onStartTurnCondition(FactoryUtil.reserveCondition(config.get(), (event, player) -> true))
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Raze(){
        Bonus action = Bonus.empty().with(Item.ACTION, 1);
        return Card.action("Raze", RegistryPrice.Adventure(2))
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, action);
                            Card toTrash = null;
                            String choice = player.chooseWhatToDo("Trash this or a card from your hand", List.of(self), List.of(new Button(self.getName(), "self"), new Button("hand", "h")), false);
                            if("self".equals(choice)) {
                                toTrash = self;
                            }else if("hand".equals(choice)) {
                                Optional<Card> card = player.chooseCardFromHand("Trash a card", false);
                                if(card.isPresent()) {
                                    toTrash = card.get();
                                }
                            }
                            if(toTrash == null ) return;
                            if(!player.trash(toTrash)) return;

                            List<Card> reveals = CardUtil.getTopCards(player, toTrash.getCost());

                            if(reveals.isEmpty()) return;

                            player.chooseCardFromList("Take a card to your hand", card -> true, reveals, false)
                                    .ifPresent(hand ->{
                                        reveals.remove(hand);
                                        player.moveTo(hand, Destination.HAND);}
                                    );

                            reveals.forEach(player::discard);
                        })
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Relic(){
        Bonus money = Bonus.empty().with(Item.MONEY, 2);
        return new Card("Relic", RegistryPrice.Adventure(5), CardType.TREASURE, CardType.ATTACK)
                .setup(config -> config
                        .onPlay((player, self) -> {
                            CardUtil.TriggerEffect(player, EFFECT, self, money);
                            player.getGame().processAttack(player, self, vi -> vi.getPersistentFlag(Token.MINUS_ONE_CARD_TOKEN.name()).set(true));
                        })
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Royal_Carriage(){
        Bonus action = Bonus.empty().with(Item.ACTION, 1);
        return new Card("Royal Carriage", RegistryPrice.Adventure(5),  CardType.RESERVE, CardType.ACTION)
                .setup(config -> config
                        .registerSimpleAction(action)
                        .afterCardPlayed(empty(TriggerComponent.afterCardPlayed.class)
                                .lookingAt((event, player) -> event.getCard())
                                .thenDo(Card::play)
                                .thenDo(card -> linkedCard(config.get(), card))
                                .end()
                        )
                        .stayInPlayCondition(checkLink)
                        .afterCardPlayedCondition(
                                reserveCondition(config.get(), (event, player) ->
                                event.getCard().hasType(CardType.ACTION)
                                && event.isCardIn(Destination.INPLAY)
                        ))

                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Storyteller(){
        Bonus action = Bonus.empty().with(Item.ACTION, 1);

        return Card.action("Storyteller", RegistryPrice.Adventure(5))
                .setup(config -> config
                        .onPlay(bonus(action)
                                .choose()
                                .chooseCardFromHand((player, card) ->
                                        new InteractionRequest.Builder<Boolean>()
                                        .instruction("Play a Treasure from your Hand")
                                        .filter(c -> c.hasType(CardType.TREASURE))
                                        .canPass(true)
                                        .build()

                                )
                                .thenDo((player, card, gained) -> {
                                    player.playCard(gained);
                                    linkedCard(card, gained);
                                })
                                .repeat(3)
                                .end()
                                .then((player, self) -> {
                                    player.draw(1);
                                    int money = player.getValueOf(Item.MONEY);
                                    player.decrement(Item.MONEY, money);
                                    player.draw(money);
                        }))
                        .stayInPlayCondition(checkLink)
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Swamp_Hag(){
        Bonus money =  Bonus.empty().with(Item.MONEY, 3);
        return new Card("Swamp Hag", RegistryPrice.Adventure(5), CardType.ACTION, CardType.ATTACK, CardType.DURATION)
                .setup(config -> config
                        .registerSimpleDuration(money)
                        .onGain((event, player) -> {
                            Player vi = event.getPlayer();
                            CardUtil.gainFromSupply(vi, "Curse", Destination.DISCARD, false);
                        })
                        .duringGainCondition((event, player) -> event.getPlayer() != player && event.isBuy() && activate.test(config.get()))
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Transmogrify(){
        Bonus action = Bonus.empty().with(Item.ACTION, 1);
        return new Card("Transmogrify", RegistryPrice.Adventure(4), CardType.ACTION, CardType.RESERVE)
                .setup(config -> config
                        .registerSimpleAction(action)
                        .onStartTurn(player ->
                            player.chooseCardFromHand("Trash a card from your hand", true)
                                    .ifPresent(trash -> {
                                        player.trash(trash);
                                        CardUtil.gainFromSupply(player, "Choose a card costing up to " + (trash.getCost() + 1 ) + "$",card -> card.isAtMostWithBonus(trash, 1) , Destination.HAND, false);
                                    })
                        )
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Treasure_Trove(){
        Bonus money = Bonus.empty().with(Item.MONEY, 2);
        return Card.treasure("Treasure Trove", RegistryPrice.Adventure(5))
                .setup(config -> config
                        .onPlay(bonus(money)
                                .then((player, card) -> {
                                    CardUtil.gainFromSupply(player, "Gold", Destination.DISCARD, false);
                                    CardUtil.gainFromSupply(player, "Copper", Destination.DISCARD, false);
                                }))
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Wine_Merchant(){
        Bonus money_buy = Bonus.empty().with(Item.MONEY, 4).with(Item.BUY, 1);
        return new Card("Wine Merchant", RegistryPrice.Adventure(5), CardType.ACTION, CardType.RESERVE)
                .setup(config -> config
                        .registerSimpleAction(money_buy)
                        .onEndBuy(Player::discard)
                        .onEndBuyCondition((event, player) -> player.getValueOf(Item.MONEY) >= 2)
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Treasure_Hunter(){
        Bonus action_money = Bonus.empty().with(Item.ACTION, 1).with(Item.MONEY, 1);
        return new Card("Treasure Hunter", RegistryPrice.Adventure(3), CardType.ACTION, CardType.TRAVELLER, CardType.ASIDE)
                .setup(config -> config
                        .onPlay(bonus(action_money)
                                .then((player, self) -> {
                                    int number = player.getGame().onTheRight(player).getCardGainedLastTurn().size();
                                    CardUtil.gainMultiplyCardFromSupply(player, "Silver", Destination.DISCARD, number);
                                })
                        )
                        .checkItselfDiscard((event, self) -> travellerUpgrade(event, self, "Warrior", false))
                        .itselfDiscardCondition((event, player) -> event.initialCameFrom(Destination.INPLAY))

                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Warrior(){
        Bonus cards = Bonus.empty().draw(2);
        return new Card("Warrior",  RegistryPrice.Adventure(4), CardType.ACTION, CardType.TRAVELLER, CardType.ASIDE)
                .setup(config -> config
                        .onPlay(bonus(cards)
                                .then((player, self) ->
                                    player.getCopyOf(Destination.INPLAY).stream().filter(c -> c.hasType(CardType.TRAVELLER))
                                            .forEach(c ->
                                                player.getGame().processAttack(player, self, vi -> {
                                                    Card card = vi.getCardFromDeck();
                                                    if(card.isBetween(3, 4)) vi.trash(card);
                                                    else vi.discard(card);
                                                })
                                            )
                                )
                        )
                        .checkItselfDiscard((event, self) -> travellerUpgrade(event, self, "Hero", false) )
                        .itselfDiscardCondition((event, player) -> event.initialCameFrom(Destination.INPLAY))
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Hero(){
        Bonus money =  Bonus.empty().with(Item.MONEY, 2);
        return new Card("Hero", RegistryPrice.Adventure(5), CardType.ACTION, CardType.TRAVELLER, CardType.ASIDE)
                .setup(config -> config
                        .onPlay(bonus(money)
                                .then((player, self) -> CardUtil.gainFromSupply(player, "Gain a Treasure", card -> card.hasType(CardType.TREASURE), Destination.DISCARD, false))
                        )
                        .checkItselfDiscard((event, self) -> travellerUpgrade(event, self, "Champion", false) )
                        .itselfDiscardCondition((event, player) -> event.initialCameFrom(Destination.INPLAY))
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Champion(){
        Bonus action = Bonus.empty().with(Item.ACTION, 1);
        return new Card("Champion", RegistryPrice.Adventure(6), CardType.ACTION, CardType.DURATION, CardType.TRAVELLER, CardType.ASIDE)
                .setup(config -> config
                        .onPlay(bonus(action)
                                .then((player, self) ->
                                    player.getGame().processBenefit(player, vi -> {
                                        List<Card> previous = vi.getCopyOf(Destination.INPLAY).stream().filter(c -> c.hasType(CardType.ATTACK)).toList();
                                        self.getCollection("Stock").addAll(previous);
                                    })
                                )
                        )
                        .checkGain((event, self) -> self.removeType(CardType.TRAVELLER))
                        .beforeCardPlayed((event, player) -> CardUtil.TriggerEffect(player, ACTION, config.get(), action))
                        .beforeCardPlayedCondition((event, player) -> player == event.getPlayer() && event.getCard().hasType(CardType.ACTION))
                        .onInfiniteDuration((player, self) -> self.clear())
                        .immunity(new TriggerComponent.Immunity() {
                            public boolean isImmuneAgainst(Card self, Card attack) {return !self.getCollection("Stock").contains(attack);}
                        })
                        .itselfDiscardCondition((event, player) -> event.initialCameFrom(Destination.INPLAY))
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Soldier(){
        Bonus money =  Bonus.empty().with(Item.MONEY, 2);
        return new Card("Soldier", RegistryPrice.Adventure(3), CardType.ACTION,CardType.ATTACK , CardType.TRAVELLER, CardType.ASIDE)
                .setup(config -> config
                        .onPlay(bonus(money)
                                .then((player, self) ->{
                                    int attack = player.getCopyOf(Destination.INPLAY).stream().filter(c -> c.hasType(CardType.ATTACK)).toList().size();
                                    player.increment(Item.MONEY, attack);
                                    player.getGame().processAttack(player, self, vi -> {
                                        if(vi.getCopyOf(Destination.HAND).size() >= 4){
                                            vi.discardFromHand(1);
                                        }
                                    });
                                })
                        )
                        .checkItselfDiscard((event, card) ->  travellerUpgrade(event, card, "Fugitive", false) )
                        .itselfDiscardCondition((event, player) -> event.initialCameFrom(Destination.INPLAY))
                );

    }
    @Dominion_Card(extension = "Adventures")
    public static Card Fugitive(){
        Bonus cards_action =   Bonus.empty().with(Item.ACTION, 1).draw(2);
        return new Card("Fugitive", RegistryPrice.Adventure(4), CardType.ACTION,CardType.TRAVELLER, CardType.ASIDE)
                .setup(config -> config
                        .onPlay(bonus(cards_action)
                                .then((player, self) -> player.discardFromHand(1))
                        )
                        .checkItselfDiscard((event, card) ->   travellerUpgrade(event, card, "Disciple", false) )
                        .itselfDiscardCondition((event, player) -> event.initialCameFrom(Destination.INPLAY))
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Disciple(){
        return new Card("Disciple", RegistryPrice.Adventure(4),CardType.ACTION, CardType.TRAVELLER, CardType.ASIDE)
                .setup(config -> config
                        .onPlay((player, self) ->
                            player.chooseCardFromHand("You may play an Action card twice", card -> card.hasType(CardType.ACTION), true)
                                    .ifPresent(card -> {
                                        player.playCard(card, 2);
                                        linkedCard(self, card);
                                    })
                        )
                        .checkItselfDiscard((event, card) ->  travellerUpgrade(event, card, "Teacher", false) )
                        .itselfDiscardCondition((event, player) -> event.initialCameFrom(Destination.INPLAY))
                        .stayInPlayCondition(checkLink)
                );
    }
    @Dominion_Card(extension = "Adventures")
    public static Card Teacher(){
        Set<Token> tokens = Set.of(Token.ONE_ACTION_TOKEN, Token.ONE_BUY_TOKEN, Token.ONE_CARD_TOKEN, Token.ONE_MONEY_TOKEN);

        return new Card("Teacher", RegistryPrice.Adventure(6),CardType.ACTION, CardType.TRAVELLER, CardType.RESERVE, CardType.ASIDE)
                .setup(config -> config
                        .checkGain((player, self) -> self.removeType(CardType.TRAVELLER))
                        .onStartTurn(player -> player.chooseToken("Put your +Card or +Action or +Buy or +Money on a supply", t -> tokens.contains(t) && player.getToken(t).isEmpty(), false))
                        .itselfDiscardCondition((event, player) -> event.initialCameFrom(Destination.INPLAY))
                );
    }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    public static Card Alms(){
        return Card.event("Alms", RegistryPrice.Adventure(0))
                .setup(config -> config
                        .checkItselfBuy((event, card) -> {
                            Player player = event.getPlayer();
                            CardUtil.gainFromSupply(player, "Gain a card costing up to 4$", card1 -> card1.isAtMost(4), Destination.DISCARD, false);
                            player.getFlag(card.getName()).set(true);
                        })
                        .checkItselfBuyCondition((event, player) -> player.getCopyOf(Destination.INPLAY).stream().noneMatch(c -> c.hasType(CardType.TREASURE)))
                        .available(player -> !player.isFlagSet(config.get().getName()))
                );
    }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    public static Card Ball(){
        return Card.event("Ball", RegistryPrice.Adventure(5))
                .setup(config -> config
                        .checkItselfBuy((event, card) -> {
                            Player player = event.getPlayer();
                            player.getPersistentFlag(Token.TAX_TOKEN.name()).set(true);
                            CardUtil.gainMultipleCardFromSupply(player,"Gain 2 cards costing up to 4$", card1 -> card1.isAtMost(4), Destination.DISCARD, 2 );
                        })
                );
    }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    public static Card Bonfire(){
        return Card.event("Bonfire", RegistryPrice.Adventure(3))
                .setup(config -> config
                        .checkItselfBuy((event, card) -> {
                            Player player = event.getPlayer();
                            player.trashWithCondition(2, card1 -> card.hasName("Copper"), Destination.INPLAY);
                        })
                );
    }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    public static Card Borrow(){
        Bonus buy = Bonus.empty().with(Item.BUY, 1);

        return Card.event("Borrow", RegistryPrice.Adventure(0))
                .setup(config -> config
                        .checkItselfBuy(buyBonus(buy)
                                .setFlag(config.get().getName())
                                .then(run(TriggerComponent.checkItSelfBuy.class, (event, card) -> {
                                    Player player = event.getPlayer();
                                    player.getPersistentFlag(Token.MINUS_ONE_CARD_TOKEN.name()).set(true);
                                    player.increment(Item.MONEY, 1);
                                }).when((event, card) -> !event.getPlayer().getPersistentFlag(Token.MINUS_ONE_CARD_TOKEN.name()).get()))
                        )
                        .available(player -> !player.isUsed(config.get()))
                );
    }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    public static Card Expedition(){
        return Card.event("Expedition", RegistryPrice.Adventure(3))
                .setup(config -> config
                        .checkItselfBuy((event, card) -> event.getPlayer().updateDrawBonusValue(2))
                );
    }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    public static Card Ferry(){
        return Card.event("Ferry", RegistryPrice.Adventure(3))
                .setup(config -> config
                        .checkItselfBuy((event, card) -> {
                            Player player = event.getPlayer();
                            player.chooseToken("Put your -2Cost onto an Action supply", t -> t == Token.CARD_REDUCTION_TOKEN, false);
                        })
                );
    }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    public static Card Inheritance(){
        return Card.event("Inheritance", RegistryPrice.Adventure(7))
                .setup(config -> config
                        .checkItselfBuy((event, card) -> {
                            Player player = event.getPlayer();
                            Optional<Card> set = player.chooseCardFromSupply(
                                    "Set aside a non duration non Command action card Supply costing up to 4$",
                                    c -> c.isAtMost(4)
                                            && c.hasType(CardType.ACTION)
                                            &&!c.hasType(CardType.DURATION)
                                            && !c.hasType(CardType.COMMAND), false);
                            if(set.isPresent()) {
                                player.moveTo(set.get(), Destination.ASIDE);
                                set.get().set("unable", true);
                                player.getPersistentFlag(card.getName()).set(true);
                                player.setToken(Token.ESTATE_TOKEN, card.getName());
                            }
                        })
                        .available(player -> !player.getPersistentFlag(config.get().getName()).get())
                );
    }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    public static Card Lost_Arts(){
        return Card.event("Lost Arts", RegistryPrice.Adventure(6))
                .setup(config -> config
                        .checkItselfBuy((event, card) -> {
                            Player player = event.getPlayer();
                            player.chooseToken("Move your +1 Action to an Action pile", t -> t == Token.ONE_ACTION_TOKEN, false);
                        })
                );
    }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    public static Card Mission(){
        return Card.event("Mission", RegistryPrice.Adventure(4))
                .setup(config -> config
                        .checkItselfBuy((event, card) -> {
                            Player player = event.getPlayer();
                            player.getFlag(Flags.expedition).set(true);
                        })
                );
    }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    public static Card Pathfinding(){
        return Card.event("Pathfinding", RegistryPrice.Adventure(8))
                .setup(config -> config
                        .checkItselfBuy((event, card) -> {
                            Player player = event.getPlayer();
                            player.chooseToken("Move your +1 Card onto a Action supply",  t -> t == Token.ONE_CARD_TOKEN, false);
                        })
                );
    }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    public static Card Pilgrimage(){
        return Card.event("Pilgrimage", RegistryPrice.Adventure(4))
                .setup(config -> config
                        .checkItselfBuy((event, card) -> {
                            Player player = event.getPlayer();
                            JourneyFace face = player.flipJourneyToken();
                            if(face == JourneyFace.FACE_UP){
                                List<Card> play = player.getDistinctCards(Destination.INPLAY);
                                List<Card> toCopy = new ArrayList<>();
                                for(int i = 0; i < 3 && !play.isEmpty(); i++){
                                    player.chooseCardFromList("Take 3 cards and gain a copy of it", c -> true, play, true)
                                            .ifPresent(c -> {
                                                play.remove(c);
                                                toCopy.add(c);
                                            });

                                }

                                for(Card c : toCopy){
                                    CardUtil.gainFromSupply(player, c.getName(), Destination.DISCARD, false);
                                }
                            }
                        })
                );
    }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    public static Card Plan(){
        return Card.event("Plan", RegistryPrice.Adventure(3))
                .setup(config -> config
                        .checkItselfBuy((event, card) -> {
                            Player player = event.getPlayer();
                            player.chooseToken("Move your trash Token to an Action Supply", t -> t == Token.TRASHING_TOKEN, false);
                        })
                );
    }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    public static Card Quest(){
        List<Button> buttons = new ArrayList<>();
        buttons.add(new Button("Attack", "a"));
        buttons.add(new Button("Curses", "c"));
        buttons.add(new Button("6 cards", "d"));

        return Card.event("Quest", RegistryPrice.Adventure(0))
                .setup(config -> config
                        .checkItselfBuy(run(TriggerComponent.checkItSelfBuy.class, (event, card) -> {
                            Player player = event.getPlayer();
                            int handSize = player.getCopyOf(Destination.HAND).size();
                            int numberOfCurses = player.getCopyOf(Destination.HAND).stream().filter(c -> c.hasType(CardType.CURSE)).toList().size();
                            String choice = player.chooseWhatToDo("Choose: You may discard an Attack card, 2 curses or 6 card ", List.of(card), buttons, true);
                            switch (choice) {
                                case "a" -> {
                                        player.chooseCardFromHand("Discard an attack card", card1 -> card1.hasType(CardType.ATTACK), false)
                                        .ifPresent(c -> {
                                            if(player.discard(c)) CardUtil.gainFromSupply(player, "Gold", Destination.DISCARD, false);
                                        });
                                }
                                case "d" -> {
                                    player.discardFromHand(6);
                                    if(handSize > 6) CardUtil.gainFromSupply(player, "Gold", Destination.DISCARD, false);
                                }
                                case "c" -> {
                                    for(int i = 0; i < 2; i++){
                                        player.chooseCardFromHand("Discard 2 Curses", c -> c.hasType(CardType.CURSE) , false)
                                                .ifPresent(player::discard);
                                    }
                                    if(numberOfCurses>2) CardUtil.gainFromSupply(player, "Gold", Destination.DISCARD, false);
                                }
                            }
                        }))
                );
    }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    public static Card Raid(){
        return Card.event("Raid", RegistryPrice.Adventure(5))
                .setup(config -> config
                        .checkItselfBuy(empty(TriggerComponent.checkItSelfBuy.class)
                                .lookingAt((event, card) -> event.getPlayer())
                                .thenDo((player, card) ->{
                                    Number silver = player.getCopyOf(Destination.INPLAY).stream().filter(c -> c.hasName("Silver")).count();
                                    CardUtil.gainMultiplyCardFromSupply(player, "Silver", Destination.DISCARD, silver.intValue());
                                })
                                .thenDo((player, card) -> player.getGame().processAttack(player, card, vi ->
                                    vi.getPersistentFlag(Token.MINUS_ONE_CARD_TOKEN.name()).set(true))
                                )
                                .end()

                        )
                );
    }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    public static Card Save(){
        Bonus buy = Bonus.empty().with(Item.BUY, 1);
        return Card.event("Save", RegistryPrice.Adventure(1))
                .setup(config -> config
                        .checkItselfBuy(buyBonus(buy)
                                .setFlag(config.get().getName())
                                .lookingAt((event, self) -> event.getPlayer())
                                .thenDo((player, self) ->
                                    player.chooseCardFromHand("Set Aside a Card from your hand",  false)
                                            .ifPresent(c -> {
                                                player.moveTo(c, Destination.ASIDE);
                                                self.getCollection("Aside").add(c);
                                                player.addCardEffect(self); //sauvegarde l'effet de onEndBuy
                                            })
                                )
                                .end()
                        )
                        .onEndBuy((player, self) ->{
                            Collection<Card> cards = self.getCollection("Aside");
                            cards.forEach(card -> player.moveTo(card, Destination.HAND));
                        })
                        .checkItselfBuyCondition((event, player) -> !player.isUsed(config.get()))
                );
    }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    public static Card Scouting_Party(){
        Bonus buy = Bonus.empty().with(Item.BUY, 1);
        return Card.event("Scouting Party", RegistryPrice.Adventure(2))
                .setup(config -> config
                        .checkItselfBuy(buyBonus(buy)
                                .lookingAtPair((event, card) -> new Pair<>(event.getPlayer(), event.getPlayer().discardAList(CardUtil.getTopCards(event.getPlayer(), 5), 3)))
                                .thenDo((pair) -> {
                                    if (pair.second().isEmpty()) return;
                                    Player player = pair.first();
                                    player.moveAllAndChooseTheOrder(pair.second(), Destination.DRAW, Destination.DRAW);
                                })
                                .end()
                        )
                );
    }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    public static Card Seaway(){
        return Card.event("Seaway", RegistryPrice.Adventure(5))
                .setup(config -> config
                        .checkItselfBuy(empty(TriggerComponent.checkItSelfBuy.class)
                                .lookingAt((event, card) -> event.getPlayer())
                                .thenDo((player) ->{
                                    Card c = CardUtil.gainFromSupply(player, "Gain a card costing up to 4$", card -> card.isAtMost(4) && card.hasType(CardType.ACTION), Destination.DISCARD, false);
                                    if(c!=null){
                                        player.setToken(Token.ONE_BUY_TOKEN, c.getName());
                                    }
                                })
                                .end()
                        )
                );
    }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    public static Card Trade(){
        return Card.event("Trade", RegistryPrice.Adventure(5))
                .setup(config -> config
                        .checkItselfBuy(empty(TriggerComponent.checkItSelfBuy.class)
                                .lookingAt((event, card) -> event.getPlayer())
                                .map((event, player) -> new Pair<>(player, player.trash()))
                                .thenDo((pair) -> CardUtil.gainFromSupply(pair.first(), "Silver", Destination.DISCARD, false))
                                .repeat(2)
                                .end()
                        )
                );
    }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    public static Card Training(){
        return  Card.event("Training", RegistryPrice.Adventure(6))
                .setup(config -> config
                        .checkItselfBuy((event, card) -> {
                                    Player player = event.getPlayer();
                                    player.chooseToken("Move your Action token", t -> t == Token.ONE_ACTION_TOKEN, false);
                                })
                        );
    }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    public static Card Travelling_Fair(){
        Bonus bonus = Bonus.empty().with(Item.BUY, 2);
        return Card.event("Travelling Fair", RegistryPrice.Adventure(2))
                .setup(config -> config
                        .checkItselfBuy(buyBonus(bonus).then((event, card) -> event.getPlayer().addCardEffect(card)))
                        .onGain((event, player) -> {
                            player.chooseWhatToDoOpt("Do you want to put this card in your deck ? ", List.of(event.getCard()), Button.yesOrNo, true)
                                    .filter("y"::equals).ifPresent(c -> event.setDest(Destination.DRAW));
                        })
                        .duringGainCondition((event, player) -> event.getPlayer() == player && event.notMoved() && event.isSameCard())

                );
    }


    public static void travellerUpgrade(Event event, Card self, String nextCard, boolean fromSupply){
        Player player = event.getPlayer();
        String pile = page.contains(nextCard)? "Page upgrade": "Peasant upgrade";
        Card next = player.getGame().getSpecificAsideCard(nextCard, pile);

        if(next == null) return;
        player.chooseWhatToDoOpt("Do you want upgrade " + self.getName() + " ?", List.of(self, next), Button.yesOrNo, true)
                .filter("y"::equals).ifPresent(choice -> {
                    if(fromSupply) player.getGame().replaceCardInSupply(self);
                    else player.getGame().replaceCardInAsideSupply(self, pile);
                    if(self.hasForLocation(Destination.SUPPLY)){
                        event.setCard(next);
                    }
                });
    }

    private static Set<String> page = Set.of("Treasure Hunter", "Warrior", "Hero", "Champion");
    private static Set<String> peasant = Set.of("Soldier", "Fugitive", "Disciple", "Teacher");


    public static List<String> getTraveller(String name){
        if("Page".equals(name)){
            return FactorySupplyPile.getMixedCards(CardType.ASIDE).stream().filter(
                    c -> !c.equals(name) && page.contains(c)).toList();
        }
        if("Peasant".equals(name)){
            return FactorySupplyPile.getMixedCards(CardType.ASIDE).stream().filter(
                    c -> !c.equals(name) && peasant.contains(c)).toList();
        }
        return List.of();
    }



}
