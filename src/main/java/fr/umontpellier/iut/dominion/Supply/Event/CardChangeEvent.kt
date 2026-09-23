package fr.umontpellier.iut.dominion.Supply.Event

import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.escapeJson

data class CardChangeEvent(
    val actionType: String,
    val supplyName: String,
    val count: Int,
    val cardsName: List<String>,
    val topCard: Card?
) {
    fun toJson(extraJson : String = ""): String {
        val cardJson = topCard?.toJson() ?: "{}"
        val namesJson = cardsName.joinToString(", ") { "\"${it.escapeJson()}\"" }
        val extra = if (extraJson.isNotBlank()) "$extraJson," else ""
        return """
        {
            "onCardChange": {
                $extra
                "type": "CARD_${actionType}",
                "supplyName": "${supplyName.escapeJson()}",
                "count": $count,
                "cards": [$namesJson],
                "topCard": $cardJson
            }
        }
    """.trimIndent()
    }
}