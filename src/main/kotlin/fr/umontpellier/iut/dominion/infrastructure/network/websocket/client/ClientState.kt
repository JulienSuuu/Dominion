package fr.umontpellier.iut.dominion.Client

import kotlinx.serialization.Serializable

// ==========================================
// 1. ÉTAT DE LA CONNEXION / SESSION CLIENT
// ==========================================
@Serializable
sealed interface ClientConnectionState {
    @Serializable
    data object Disconnected : ClientConnectionState

    @Serializable
    data class Connecting(val pseudo: String) : ClientConnectionState

    @Serializable
    data class Connected(val playerId: String, val playerName: String) : ClientConnectionState

    @Serializable
    data class Error(val message: String) : ClientConnectionState
}


@Serializable
sealed interface ClientView {
    @Serializable
    data object Home : ClientView

    @Serializable
    data class Lobby(
        val roomCode: String,
        val players: List<String>,
        val isHost: Boolean
    ) : ClientView

    @Serializable
    data object CardSelection : ClientView

    @Serializable
    data class Game(
        val gameId: String,
    ) : ClientView

    @Serializable
    data class GameOver(
        val winnerName: String,
        val finalScores: Map<String, Int>
    ) : ClientView
}

@Serializable
sealed interface ClientGameUIState {
    @Serializable
    data class Spectating(val currentTurnPlayer: String) : ClientGameUIState

    @Serializable
    data class PlayingTurn(
        val canPlayActions: Boolean,
        val canBuy: Boolean
    ) : ClientGameUIState

    @Serializable
    data class AwaitingInput(
        val instruction: String,
        val choices: List<String>,
    ) : ClientGameUIState
}