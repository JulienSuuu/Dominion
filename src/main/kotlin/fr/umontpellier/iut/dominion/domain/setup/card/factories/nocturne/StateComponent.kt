package fr.umontpellier.iut.dominion.domain.setup.card.factories.nocturne

import fr.umontpellier.iut.dominion.domain.model.player.Player

interface StateComponent {
    fun interface OnStartBuyPhase : StateComponent, suspend (Player, State) -> Unit {
        override suspend fun invoke(player: Player, self: State)
    }
    fun interface OnStartTurnPhase : StateComponent, suspend (Player, State) -> Unit {
        override suspend fun invoke(player: Player, self: State)
    }
}