package fr.umontpellier.iut.dominion.cards.component;


import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.Event;

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
    interface onCardDiscarded extends TriggerComponent, BiConsumer<Player, Event> {}
    interface onCardTrashed extends TriggerComponent, BiConsumer<Player, Event> {}
    interface onBuy extends TriggerComponent, BiConsumer<Player, Card> {}
}
