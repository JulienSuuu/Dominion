package fr.umontpellier.iut.dominion.application.service

import fr.umontpellier.iut.dominion.application.game.GameGUI
import fr.umontpellier.iut.dominion.application.game.GameInstance
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service

@Service
class GameService(
    private val gameGuiProvider: ObjectProvider<GameGUI>
) {

    fun createGame(gameId: String): GameInstance {
        val newGui = gameGuiProvider.getObject()
        return GameInstance(gameId, newGui)
    }
}