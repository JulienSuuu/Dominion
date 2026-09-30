package fr.umontpellier.iut.dominion.game.rules

import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.PlayerComponent.PlayerTurnPhase
import fr.umontpellier.iut.dominion.Player.PlayerComponent.startNightPhase
import fr.umontpellier.iut.dominion.Player.Skills.cleanup
import fr.umontpellier.iut.dominion.Player.Skills.playTurn
import fr.umontpellier.iut.dominion.game.Game
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update


sealed interface TurnPhase {
    val name: String
    suspend fun execute(game: Game, player: Player)
}

object CleanupPhase : TurnPhase {
    override val name = "CLEANUP"

    override suspend fun execute(game: Game, player: Player) {
        player.cleanup()
        game.generalCleanUp()
        player.state.update { it.reset() }
    }
}

object NightPhase : TurnPhase {
    override val name = "NIGHT_PHASE"
    override suspend fun execute(game: Game, player: Player) { player.startNightPhase() }
}

object StartTurnPhase : TurnPhase {
    override val name = "START_TURN"
    override suspend fun execute(game: Game, player: Player) {
        player.state.update { it.changeTurnState(PlayerTurnPhase.StartTurn) }
        player.state.first { it.turnPhase == PlayerTurnPhase.ActionPhase }
    }
}

object MainPlayPhase : TurnPhase {
    override val name = "PLAY_PHASE"
    override suspend fun execute(game: Game, player: Player) {
        player.playTurn()
    }
}