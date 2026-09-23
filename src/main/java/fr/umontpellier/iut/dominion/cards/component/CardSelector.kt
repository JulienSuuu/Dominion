package fr.umontpellier.iut.dominion.cards.component

import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.cards.Card

/**
 * Interface fonctionnelle de selection d'une carte
 */
fun interface CardSelector : suspend (Player, Player, MutableList<Card>) -> Card? {
    override suspend fun invoke(chooser: Player, victim: Player, options: MutableList<Card>): Card?
}
