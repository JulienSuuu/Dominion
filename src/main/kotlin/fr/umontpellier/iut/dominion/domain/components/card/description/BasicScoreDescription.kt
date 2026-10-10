package fr.umontpellier.iut.dominion.domain.components.card.description

class BasicScoreDescription(val score: Int) : ScoreDescription() {
    override fun getSimpleAttribute() = score
    override fun toJson(): String = """{"score": $score}"""
}