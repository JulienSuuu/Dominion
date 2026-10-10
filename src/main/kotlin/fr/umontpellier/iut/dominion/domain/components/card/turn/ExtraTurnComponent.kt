package fr.umontpellier.iut.dominion.domain.components.card.turn

import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.components.card.CardComponent
import java.util.concurrent.atomic.AtomicBoolean

class ExtraTurnComponent(
    private val used: AtomicBoolean = AtomicBoolean(false)
) : CardComponent {

    /**
     * Vérifie si le tour supplémentaire est disponible.
     * @return L'effet lui-même s'il est disponible, ou `null` s'il a déjà été utilisé.
     */
    fun canUseExtraTurn(): ExtraTurnComponent? {
        return if (used.get()) null else this
    }

    /**
     * Consomme le tour supplémentaire en marquant le composant comme utilisé.
     */
    fun consume(player: Player) {
        used.set(true)
    }
}