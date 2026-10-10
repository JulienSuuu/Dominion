package fr.umontpellier.iut.dominion.domain.components.card.description

import fr.umontpellier.iut.dominion.domain.model.token.Token
import fr.umontpellier.iut.dominion.domain.model.token.TokenEffect.tokens

class TokenUser : DominionDescription {
    override var text: String = ""
        private set

    var token = listOf<Token.OnPile>()
    var amount: Int = 1

    fun text(raw: String) = apply { text = raw }
    fun tokens(vararg tokens: Token.OnPile) = apply { this.token = tokens.toList() }
    fun amount(amount: Int) = apply { this.amount = amount }


    override fun toJson()=  """
        {
            "text": "$text",
            "tokens": [${tokens.joinToString(",\n")}],
            "amount": $amount
        }
    """.trimIndent()
}