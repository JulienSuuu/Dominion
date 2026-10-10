package fr.umontpellier.iut.dominion.domain.components.card.bonus

import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.card.Card
import fr.umontpellier.iut.dominion.domain.components.card.trigger.OnPlayComponent
import fr.umontpellier.iut.dominion.domain.setup.card.factories.EFFECT
import fr.umontpellier.iut.dominion.domain.utils.triggerEffect

interface DominionBonus {
    fun apply(player: Player, card: Card)
    fun onPlay() : OnPlayComponent = OnPlayComponent{player, c -> player.triggerEffect(EFFECT, c, this)}
    fun toLog() : List<String> = logs
    val logNotEmpty : Boolean get() = logs.isNotEmpty()
    val logs : List<String>
}