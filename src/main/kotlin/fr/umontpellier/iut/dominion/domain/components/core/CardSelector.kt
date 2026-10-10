package fr.umontpellier.iut.dominion.domain.components.core

import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.card.Card

/**
 * Interface fonctionnelle de selection d'une carte
 */
fun interface CardSelector : suspend (Player, Player, MutableList<Card>) -> Card? {
    override suspend fun invoke(chooser: Player, victim: Player, options: MutableList<Card>): Card?
}