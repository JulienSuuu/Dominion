package fr.umontpellier.iut.dominion.cards.component;

import fr.umontpellier.iut.dominion.Player.Player;

/**
 * Comportement basique d'une carte Victoire
 */
@FunctionalInterface
public interface ScoreComponent extends CardComponent {
    int giveScore(Player player);
}
