package fr.umontpellier.iut.dominion.Player.Tokens

import fr.umontpellier.iut.dominion.cards.displayCoin
import java.awt.Choice

sealed class Token(val name: String, val displayName : String, val play: Boolean) {

    sealed class OnPile(name : String, displayName : String, play : Boolean = true) : Token(name, displayName, play) {
        companion object {
            val set: List<OnPile> by lazy {
                listOf(
                    OneMoneyToken,
                    OneActionToken,
                    OneBuyToken,
                    OneCardToken,
                    TrashingToken,
                    EstateToken,
                    CardReductionToken
                )
            }
            fun valueOf(choice: String): OnPile? = set.find { it.name.equals(choice, ignoreCase = true) }
        }

        data object OneMoneyToken : OnPile("one_money_token", "+${displayCoin(1)}")
        data object OneActionToken : OnPile("one_action_token", "+1 action")
        data object OneBuyToken : OnPile("one_buy_token", "+1 buy")
        data object OneCardToken : OnPile("one_card_token", "+1 card")
        data object TrashingToken : OnPile("trashing_token", "trash", false)
        data object EstateToken : OnPile("estate_token", "estate", false)
        data object CardReductionToken : OnPile("card_reduction_token", "card_reduction", false)
    }

    sealed class OnPlayer(name : String, displayName : String) : Token(name, displayName, false) {
        data object TaxToken : OnPlayer("MinusOneCoinToken", "-${displayCoin(1)}")
        data object JourneyToken : OnPlayer("JourneyToken", "Journey")
        data object MinusOneCardToken : OnPlayer("MinusOneCardToken", "-1 card")
    }

    fun toJson() = """
        "name": "$name",
        "displayName": "$displayName",
        "isBasicBonus": $play
    """.trimIndent()
}
