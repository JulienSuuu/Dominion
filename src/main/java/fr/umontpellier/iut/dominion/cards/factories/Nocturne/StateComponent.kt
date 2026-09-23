package fr.umontpellier.iut.dominion.cards.factories.Nocturne

import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.cards.component.BiEffect

interface StateComponent {
    fun interface OnStartBuyPhase : StateComponent, suspend (Player, State) -> Unit {
        override suspend fun invoke(player: Player, self: State)
    }
    fun interface OnStartTurnPhase : StateComponent, suspend (Player, State) -> Unit {
        override suspend fun invoke(player: Player, self: State)
    }
}