package fr.umontpellier.iut.dominion.cards.component;

import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface OnPlayComponent extends BiConsumer<Player, Card>, CardComponent {
}
