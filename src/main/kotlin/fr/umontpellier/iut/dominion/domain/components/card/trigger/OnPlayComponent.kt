package fr.umontpellier.iut.dominion.domain.components.card.trigger

import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.card.Card
import fr.umontpellier.iut.dominion.domain.components.card.MultiCardComponent
import fr.umontpellier.iut.dominion.domain.components.card.effect.BiEffect

/**
 * Comportement basique d'une carte quand elle est joué
 */
@FunctionalInterface
fun interface OnPlayComponent : BiEffect<Player, Card>, MultiCardComponent