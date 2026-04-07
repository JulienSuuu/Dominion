package fr.umontpellier.iut.dominion.cards.component;

import fr.umontpellier.iut.dominion.Player;

public interface ScoreComponent extends CardComponent {
    int giveScore(Player player);
}
