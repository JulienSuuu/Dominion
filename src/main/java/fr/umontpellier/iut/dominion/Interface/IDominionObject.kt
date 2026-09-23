package fr.umontpellier.iut.dominion.Interface

import fr.umontpellier.iut.dominion.game.Game

interface IDominionObject {
    val game : Game?
        get() = null

    val name : String
}
