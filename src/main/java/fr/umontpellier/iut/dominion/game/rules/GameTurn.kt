package fr.umontpellier.iut.dominion.game.rules

import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.game.Game

class GameTurn(val game: Game) : TurnEndListener {
    private val cardsToResetAtEndOfTurn = mutableListOf<Card>()

    fun getPhasesForGame(): List<TurnPhase> {
        val phases = mutableListOf(StartTurnPhase, MainPlayPhase)
        if (game.hasExpansion("Nocturne", 1)) {
            phases.add(NightPhase)
        }

        phases.add(CleanupPhase)
        return phases
    }


    suspend fun runTurn(player: Player) {
        val activePhases = getPhasesForGame()

        for (phase in activePhases) {
                phase.execute(game, player)
        }
    }





    fun flipCardFaceDown(card: Card) {
        card.faceDown.hide()
        cardsToResetAtEndOfTurn.add(card)
    }

    override suspend fun onTurnEnded(game: Game) {
        if (cardsToResetAtEndOfTurn.isEmpty()) return
        cardsToResetAtEndOfTurn.forEach { card ->
            card.faceDown.reveal()
        }

        cardsToResetAtEndOfTurn.clear()
    }
}

fun Game.flipCardFaceDown(card: Card) { getRule<GameTurn>()?.flipCardFaceDown(card) }