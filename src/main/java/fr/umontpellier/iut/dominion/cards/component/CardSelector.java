package fr.umontpellier.iut.dominion.cards.component;

import fr.umontpellier.iut.dominion.Player.Player;
import fr.umontpellier.iut.dominion.cards.Card;

import java.util.List;

/**
 *Interface fonctionnelle de selection d'une carte
 */
@FunctionalInterface
public interface CardSelector {
    Card select(Player attacker, Player victim, List<Card> options);
}
