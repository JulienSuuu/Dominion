package fr.umontpellier.iut.dominion.domain.common

import fr.umontpellier.iut.dominion.domain.model.ui.Button
import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.player.PlayerMessage
import fr.umontpellier.iut.dominion.domain.model.card.Card
import fr.umontpellier.iut.dominion.domain.dsl.card.core.InteractionRequest

interface Logger : IDominionObject {
    fun log(message: String?) {
        game?.log(message?:"")
    }

    fun sendMessage(message: PlayerMessage, card: Card? = null)

    suspend fun chooseWhatToDo(
        instruction: String = "Choose what to do",
        list: List<Card> = emptyList(),
        filter: (Card) -> Boolean = { false },
        buttons: List<Button> = emptyList(),
        canPass: Boolean = false,
    ): String

    suspend fun chooseCardFromSupply(
        instruction: String = "Choose card from Supply",
        filter: (Card) -> Boolean = { true },
        canPass: Boolean = false,
    ) : Card?

    suspend fun chooseCardFromHand(
        instruction: String = "Choose card from hand",
        canPass: Boolean = false,
        predicate: (Card) -> Boolean = { true }
    ): Card?

    suspend fun chooseCardFromList(
        instruction: String = "Choose card from list",
        cards: List<Card>,
        canPass: Boolean = false,
        predicate: (Card) -> Boolean = { true }
    ): Card?

    fun toPlayer() : Player

}

    suspend fun <X> Logger.chooseWhatToDo(request: InteractionRequest<X>): String {
        return chooseWhatToDo(
            instruction = request.instruction,
            list = request.cards,
            filter = request.chooseFilter,
            buttons = request.buttons,
            canPass = request.canPass
        )
    }

    suspend fun <X> Logger.chooseCardFromHand(request: InteractionRequest<X>): Card? {
        return chooseCardFromHand(
            instruction = request.instruction,
            canPass = request.canPass,
            predicate = request.filter
        )
    }

    suspend fun <X> Logger.chooseCardFromList(request: InteractionRequest<X>): Card? {
        return chooseCardFromList(
            instruction = request.instruction,
            cards = request.cards,
            canPass = request.canPass,
            predicate = request.filter
        )
    }

suspend fun <X> Logger.chooseCardFromSupply(request: InteractionRequest<X>): Card? {
    return chooseCardFromSupply(
        instruction = request.instruction,
        filter = request.filter,
        canPass = request.canPass
    )
}

