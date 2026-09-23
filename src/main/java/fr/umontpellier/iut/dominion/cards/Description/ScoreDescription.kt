package fr.umontpellier.iut.dominion.cards.Description

open class ScoreDescription : DominionDescription{
    override var text: String = ""

    var type : ScoreType? = null

    fun text(text: String) : ScoreDescription {
        this@ScoreDescription.text = text
        return this
    }

    fun type(type: ScoreType) : ScoreDescription {
        this.type = type
        return this
    }

    open fun getSimpleAttribute() = 0

    override fun toJson(): String {
        val scoreTypeJson = type?.toJson() ?: "null"
        val safeInstruction = text.replace("\"", "\\\"")

        return """
    {
      "score": "$safeInstruction",
      "type": $scoreTypeJson
    }
    """.trimIndent()
    }

}


sealed interface ScoreType {
    fun toJson(): String

    data class PerCardsCount(val vp: Int = 1, val cardsStep: Int = 10) : ScoreType {
        override fun toJson(): String = """{"type": "PER_CARDS_COUNT", "vp": $vp, "cardsStep": $cardsStep}"""
    }

    data class Fixed(val vp: Int) : ScoreType {
        override fun toJson(): String = """{"type": "FIXED", "vp": $vp}"""
    }
}
