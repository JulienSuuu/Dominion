package fr.umontpellier.iut.dominion.domain.components.card.score

import fr.umontpellier.iut.dominion.domain.model.player.Player

class MutableScoreComponent(private var baseValue: Int, private val function : (Player) -> Int = {0}) : ScoreComponent {

    override fun invoke(player: Player): Int {
        return function.invoke(player) + baseValue;
    }

    override fun incrementValue(default : Int) { baseValue += default }
}