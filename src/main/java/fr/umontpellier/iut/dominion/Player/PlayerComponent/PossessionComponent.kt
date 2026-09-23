package fr.umontpellier.iut.dominion.Player.PlayerComponent

import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.displayCoin

class PossessionComponent(val self : Player) : PlayerComponent {
    var controller: Player = self
    var mustConductPossessedTurn: Boolean = false
    var nextPossessor: Player? = null
    val underPossession get() = self != controller
    val mustBeDiscarded = mutableListOf<Card>()

    fun preparePossession(possessor: Player) {
        this.mustConductPossessedTurn = true
        this.nextPossessor = possessor
    }

    fun updatePossession() : Boolean {
        nextPossessor?.let {controller = it}
        mustConductPossessedTurn = false
        self.isSecondTurn = true
        return true
    }

    fun toJson() = """
        "possession": {
            "underPossession": $underPossession,
            "mustConductPossessedTurn": $mustConductPossessedTurn
        }
    """.trimIndent()
}

var Player.controller get() = getComponent<PossessionComponent>()?.controller ?: self
    set(value) {getComponent<PossessionComponent>()?.controller = value }
val Player.underPossession get() = getComponent<PossessionComponent>()?.underPossession ?: false
val Player.mustBeDiscarded get() = getComponent<PossessionComponent>()?.mustBeDiscarded ?: emptyList()
fun Player.addToDiscarded(card : Card) { getComponent<PossessionComponent>()?.mustBeDiscarded += card }
fun Player.updatePossession() = getComponent<PossessionComponent>()?.updatePossession() ?: false
val Player.mustConductPossessedTurn get() = getComponent<PossessionComponent>()?.mustConductPossessedTurn ?: false

fun Player.preparePossession(possessor: Player) = getComponent<PossessionComponent>()?.preparePossession(possessor)