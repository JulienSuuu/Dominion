package fr.umontpellier.iut.dominion.cards.Description

class BasicScoreDescription(val score: Int) : ScoreDescription() {
    override fun getSimpleAttribute() = score
    override fun toJson(): String = """{"score": $score}"""
}