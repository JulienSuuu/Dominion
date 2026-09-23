package fr.umontpellier.iut.dominion.cards.component

import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.cards.Card

/**
 * Comportement basique d'une carte quand elle est joué
 */
@FunctionalInterface
fun interface OnPlayComponent : BiEffect<Player, Card>, MultiCardComponent
