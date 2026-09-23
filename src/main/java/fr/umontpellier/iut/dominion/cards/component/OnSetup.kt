package fr.umontpellier.iut.dominion.cards.component

import fr.umontpellier.iut.dominion.game.Game

class OnSetup(val block : Game.() -> Unit): CardComponent {
    var neededPlayer = false
    fun execute(game : Game) { game.block() }
}