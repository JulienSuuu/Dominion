package fr.umontpellier.iut.dominion.domain.components.card.score

import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.setup.card.factories.nocturne.StateComponent
import fr.umontpellier.iut.dominion.domain.components.card.CardComponent

/**
 * Comportement basique d'une carte Victoire
 */
@FunctionalInterface
fun interface ScoreComponent : CardComponent, (Player) -> Int?, StateComponent {
    override fun invoke(player: Player): Int?
    fun incrementValue(default: Int = 1){}
}
