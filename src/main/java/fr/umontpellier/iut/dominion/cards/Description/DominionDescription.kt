package fr.umontpellier.iut.dominion.cards.Description

interface DominionDescription {
    fun toJson() : String
    val text : String
}