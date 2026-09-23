package fr.umontpellier.iut.dominion.cards.component

import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.cards.factories.Nocturne.StateComponent

/**
 * Comportement basique d'une carte Victoire
 */
@FunctionalInterface
fun interface ScoreComponent : CardComponent, (Player) -> Int?, StateComponent {
    override fun invoke(player: Player): Int?
}
