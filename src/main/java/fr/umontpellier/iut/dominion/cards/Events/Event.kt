package fr.umontpellier.iut.dominion.cards.Events

import fr.umontpellier.iut.dominion.Button
import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Interface.Logger
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.PlayerMessage
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.factories.Futaba.PokerHand


open class Event (
    var card: Card?,
    var destination: Destination?,
    var player: Player,
    val isBuy: Boolean = false,
    val discard: Discard_Type = Discard_Type.ACTION,
) : Logger {
    override val name: String = "Event"
    companion object {
        private var idCounter = 0
    }

    val id: Int = idCounter++
    val initialOrigin: Destination? = card?.loc?.value
    val origin: Destination? = destination
    var cardOrigin: Destination? = card?.loc?.value
        private set

    val originalCard: Card? = card

    constructor(player: Player) : this(
        card = null,
        destination = null,
        player = player,
        isBuy = false,
        discard = Discard_Type.ACTION
    )

    open fun isPokerHand(type : PokerHand) : Boolean = false
    open fun getEvaluatedHand() : List<Card> = emptyList()

    open val hasMoved: Boolean
        get() = destination != origin

    open val notMoved: Boolean
        get() = destination == origin

    open val isSameCard: Boolean
        get() = card == originalCard

    val isActionDiscard: Boolean
        get() = discard == Discard_Type.ACTION

    open fun updateGainType(gainType: GainType) {}

    open fun hasGainType(t : GainType) = false

    open fun cameFrom(destination: Destination): Boolean = cardOrigin == destination

    open fun goTo(destination: Destination): Boolean = destination == this.destination

    open fun isCardIn(destination: Destination): Boolean = card?.hasForLocation(destination) == true

    open fun initialCameFrom(destination: Destination): Boolean = initialOrigin == destination

    open fun isSamePlayer(player: Player): Boolean = player == this.player

    open fun isSameId(id: Int): Boolean = this.id == id

    fun updateCard(newCard: Card) {
        this.card = newCard
        newCard.loc.value?.let { cardOrigin = it}
    }

    fun updateDest(newDestination: Destination) {
        this.destination = newDestination
    }


    infix fun cardHasName(name: String): Boolean {
        return card?.hasName(name)?:false
    }

    infix fun cardHasType(type : CardType): Boolean {
        return card?.hasType(type)?:false
    }

    fun cardHasAllType(vararg type : CardType) : Boolean {
        return type.all { cardHasType(it) }
    }

    fun testCard(predicate: (Card) -> Boolean) : Boolean = card?.let { predicate(it) } ?: false


    override suspend fun chooseWhatToDo(instruction: String, list: List<Card>, filter : (Card) -> Boolean, buttons: List<Button>, canPass: Boolean): String {
        return player.chooseWhatToDo(instruction, list, filter,  buttons, canPass)
    }

    override suspend fun chooseCardFromHand(instruction: String, canPass: Boolean, predicate: (Card) -> Boolean): Card? {
        return player.chooseCardFromHand(instruction, canPass, predicate)
    }

    override suspend fun chooseCardFromList(
        instruction: String,
        cards: List<Card>,
        canPass: Boolean,
        predicate: (Card) -> Boolean
    ): Card? {
        return player.chooseCardFromList(instruction, cards, canPass, predicate)
    }

    override suspend fun chooseCardFromSupply(instruction: String, filter: (Card) -> Boolean, canPass: Boolean): Card? {
        return player.chooseCardFromSupply(instruction, filter, canPass)
    }

    override fun sendMessage(message: PlayerMessage, card: Card?) {
        player.sendMessage(message, card)
    }

    override fun toPlayer() = player

    operator fun component1() = player
    operator fun component2() = card
    operator fun component3() = destination
}