package fr.umontpellier.iut.dominion.Player.PlayerComponent

import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.Tokens.JourneyFace
import fr.umontpellier.iut.dominion.Player.Tokens.Token
import fr.umontpellier.iut.dominion.Player.Tokens.TokenEffect
import fr.umontpellier.iut.dominion.Player.Skills.choose
import fr.umontpellier.iut.dominion.cards.Events.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class TokenComponent(private val self: Player)  : PlayerComponent {
    private val myTokens : MutableMap<Token.OnPile, String> = mutableMapOf()
    val tokens get() = myTokens

    private val myTokensFlag = mutableMapOf<Token.OnPlayer, MutableStateFlow<Boolean>>()

    init {
        TokenEffect.tokens.forEach {
            
            myTokens[it] = ""
        }

        myTokens[Token.OnPile.EstateToken] = ""
        myTokens[Token.OnPile.CardReductionToken] = ""
    }

    fun getTokenFlag(token : Token.OnPlayer) = myTokensFlag.getOrPut(token, { MutableStateFlow(false) })

    fun getToken(token : Token.OnPile) = myTokens.getOrPut(token, {""})
    fun setToken(token : Token.OnPile, string : String) {myTokens[token] = string}

    suspend fun checkToken(event: Event, check : (Token) -> Boolean) {
        val cardName = event.card?.name ?: return
        myTokens.forEach { (token, value) ->
            val isCorrect = value == cardName || self.game.verifyPileToken(value, cardName)
            if(isCorrect && check(token)) TokenEffect.execute(token, event, self)
        }
    }

    fun flipJourneyToken() : JourneyFace {
        val j = getTokenFlag(Token.OnPlayer.JourneyToken)
        j.value = !j.value
        return if(j.value) JourneyFace.FACE_DOWN else JourneyFace.FACE_UP
    }

    fun toJson() : String {
        val supplyTokensJson = myTokens.map { (token, supplyName) ->
            "\"${token.name}\": \"$supplyName\""
        }.joinToString(", ")

        val playerTokensJson = myTokensFlag.map { (token, flag) ->
            "\"${token.name}\": ${flag.value}"
        }.joinToString(", ")

        return """
            "tokens": {"supplyTokens": {$supplyTokensJson}, "playerTokens": {$playerTokensJson}}
            """.trimIndent()
    }

    suspend fun chooseToken(instruction: String, filter: (Token.OnPile) -> Boolean = {true}, canPass: Boolean = false ){
        val choiceTokens = tokens.keys.filter(filter).map { "TOKEN:${it.name.lowercase()}" }.toMutableList()
        val choice = self.choose(instruction, choiceTokens, canPass = canPass)

        val token = if (choice.startsWith("TOKEN:")) {
            try {
                Token.OnPile.valueOf(choice.removePrefix("TOKEN:"))
            } catch (e: IllegalArgumentException) {
                null
            }
        } else null

        if (token == null) return

        val check = self.game.actionSupplyCard
        self.chooseCardFromSupply("Select an Action supply and put your ${token.displayName} token on it",
            {it in check}, canPass)?.let { setToken(token, it.name) }

    }
}

fun Player.getTokenFlag(token : Token.OnPlayer) = getComponent<TokenComponent>()?.getTokenFlag(token)
fun Player.isAffectedBy(token : Token.OnPlayer) = getComponent<TokenComponent>()?.getTokenFlag(token)?.value ?: false
fun Player.updateTokenFlag(token: Token.OnPlayer, value: Boolean) = getComponent<TokenComponent>()?.getTokenFlag(token)?.update { value }
suspend fun Player.checkPlayToken(event : Event) = getComponent<TokenComponent>()?.checkToken(event, {it.play})
suspend fun Player.checkGainToken(event : Event) = getComponent<TokenComponent>()?.checkToken(event, {!it.play})
fun Player.askReductionToken(name: String?) = getComponent<TokenComponent>()?.tokens[Token.OnPile.CardReductionToken]?.equals(name) ?: false
fun Player.flipJourneyToken() = getComponent<TokenComponent>()?.flipJourneyToken() ?: JourneyFace.INACTIVE
fun Player.getToken(token : Token.OnPile) = getComponent<TokenComponent>()?.getToken(token) ?: ""
fun Player.setToken(token : Token.OnPile, namePile : String) = getComponent<TokenComponent>()?.setToken(token, namePile)