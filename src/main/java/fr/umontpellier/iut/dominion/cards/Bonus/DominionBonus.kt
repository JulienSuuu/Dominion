package fr.umontpellier.iut.dominion.cards.Bonus

import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.component.OnPlayComponent
import fr.umontpellier.iut.dominion.cards.factories.EFFECT
import fr.umontpellier.iut.dominion.cards.triggerEffect

interface DominionBonus {
    fun apply(player: Player, card: Card)
    fun onPlay() : OnPlayComponent = OnPlayComponent{player, c -> player.triggerEffect(EFFECT, c, this)}
    fun toLog() : List<String> = logs
    val logNotEmpty : Boolean get() = logs.isNotEmpty()
    val logs : List<String>
}