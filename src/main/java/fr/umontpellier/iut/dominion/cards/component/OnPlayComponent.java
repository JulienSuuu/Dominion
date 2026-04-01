package fr.umontpellier.iut.dominion.cards.component;

import fr.umontpellier.iut.dominion.Player;

import java.util.function.Consumer;

public interface OnPlayComponent extends Consumer<Player>, CardComponent {
}
