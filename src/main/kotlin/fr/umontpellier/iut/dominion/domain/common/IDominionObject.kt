package fr.umontpellier.iut.dominion.domain.common

import fr.umontpellier.iut.dominion.domain.model.game.Game

interface IDominionObject {
    val game : Game?
        get() = null

    val name : String
}
