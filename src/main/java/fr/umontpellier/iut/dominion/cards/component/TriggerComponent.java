package fr.umontpellier.iut.dominion.cards.component;


import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.Events.Event;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface TriggerComponent extends CardComponent {
    @FunctionalInterface
    interface OnPlayerGain extends TriggerComponent, TriggerEffect {}
    interface OnCardPlayed extends TriggerComponent, TriggerEffect {}
    interface onStartTurn extends TriggerComponent, Consumer<Player> {}
    interface Immunity extends TriggerComponent {
        default boolean revealed(Player player, Card self) {return false;}
        default boolean immune(Card self) {return false;}
    }
    interface onEndBuy extends TriggerComponent, BiConsumer<Player, Card> {}
    interface onCardDiscarded extends TriggerComponent, BiConsumer<Event, Card> {}
    interface onCardTrashed extends TriggerComponent, BiConsumer<Event, Card> {}
    interface onBuy extends TriggerComponent, BiConsumer<Player, Card> {}
    interface checkItSelfBuy extends TriggerComponent, BiConsumer<Event, Card> {}
    interface checkItselfGain extends TriggerComponent, BiConsumer<Event, Card> {}
    interface overPaidCard extends TriggerComponent, BiConsumer<Player, Card> {}
    interface discardHook extends TriggerComponent, Consumer<Event> {}
}
