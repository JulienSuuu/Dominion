package fr.umontpellier.iut.dominion.cards.component;

import fr.umontpellier.iut.dominion.Player.Player;
import fr.umontpellier.iut.dominion.cards.Card;

import java.util.function.BiConsumer;


/**
 * Comportement basique d'une carte quand elle est joué
 */
@FunctionalInterface
public interface OnPlayComponent extends TriggerBiEffect<Player, Card, OnPlayComponent>, CardComponent {
    @Override
    default OnPlayComponent create(BiConsumer<Player, Card> effect) {
        return effect::accept;
    }
    @Override
    default OnPlayComponent self() {
        return this;
    }
}
