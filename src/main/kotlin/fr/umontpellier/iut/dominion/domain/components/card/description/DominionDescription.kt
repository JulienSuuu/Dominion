package fr.umontpellier.iut.dominion.domain.components.card.description

interface DominionDescription {
    fun toJson() : String
    val text : String
}