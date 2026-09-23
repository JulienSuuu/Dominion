package fr.umontpellier.iut.dominion.cards

import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination

class CardFunction

infix fun Card.isNotIn(destination: Destination?): Boolean = !hasForLocation(destination)
infix fun Card.hasNotType(type: CardType): Boolean = !hasType(type)

fun Card.testTypes(configure: CardTypeChecker.() -> Unit): Boolean {
    val checker = CardTypeChecker(this)
    checker.configure()
    return checker.matches()
}

class CardTypeChecker(private val card: Card) {
    private val conditions = mutableListOf<() -> Boolean>()

    fun any(vararg types: CardType) {
        conditions.add { types.any { card.hasType(it) } }
    }

    fun any(firstType: CardType){
        conditions.add { card.hasType(firstType) }
    }

    fun any(types : List<CardType>){
        conditions.add{ if(types.isEmpty()) true else types.any { card.hasType(it) } }
    }

    fun none(types : List<CardType>){
        conditions.add { if (types.isEmpty()) true else types.none { card.hasType(it) } }
    }

    fun any(first : CardType, second: CardType){
        conditions.add { card.hasType(first) || card.hasType(second) }
    }

    fun all(vararg types: CardType) {
        conditions.add { types.all { card.hasType(it) } }
    }

    fun none(vararg types: CardType) {
        conditions.add { types.none { card.hasType(it) } }
    }

    fun matches(): Boolean {
        if (conditions.isEmpty()) return true
        return conditions.all { it() }
    }



}
