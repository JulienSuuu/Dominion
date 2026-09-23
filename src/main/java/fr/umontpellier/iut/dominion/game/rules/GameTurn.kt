package fr.umontpellier.iut.dominion.game.rules

import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.game.Game

class GameTurn(val game: Game) : GameComponent {
    private val cardsToResetAtEndOfTurn = mutableListOf<Card>()

    fun flipCardFaceDown(card: Card) {
        card.faceDown.hide()
        cardsToResetAtEndOfTurn.add(card)
    }


    fun cleanupTurn() {
        if (cardsToResetAtEndOfTurn.isEmpty()) return
        cardsToResetAtEndOfTurn.forEach { card ->
            card.faceDown.reveal()
        }

        cardsToResetAtEndOfTurn.clear()
    }
}

fun Game.flipCardFaceDown(card: Card) { getRule<GameTurn>()?.flipCardFaceDown(card) }
fun Game.cleanupTurnCard() = getRule<GameTurn>()?.cleanupTurn()