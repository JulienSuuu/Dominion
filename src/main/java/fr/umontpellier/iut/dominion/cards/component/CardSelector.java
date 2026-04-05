package fr.umontpellier.iut.dominion.cards.component;

import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;

import java.util.List;

public interface CardSelector {
    Card select(Player attacker, Player victim, List<Card> options);
}
