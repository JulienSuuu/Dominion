package fr.umontpellier.iut.dominion.service

import fr.umontpellier.iut.dominion.game.GameInstance
import fr.umontpellier.iut.dominion.gui.game.GameGUI
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