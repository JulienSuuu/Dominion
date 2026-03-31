package fr.umontpellier.iut.dominion.cards.component;


import fr.umontpellier.iut.dominion.Player;

import java.util.function.Consumer;

public interface TriggerComponent extends CardComponent {
    @FunctionalInterface
    interface OnPlayerGain extends TriggerComponent, TriggerEffect {}
    interface OnCardPlayed extends TriggerComponent, TriggerEffect {}
    interface onStartTurn extends TriggerComponent, Consumer<Player> {}
    interface Immunity extends TriggerComponent {}
    interface onEndBuy extends TriggerComponent, TriggerEffect {}
}
