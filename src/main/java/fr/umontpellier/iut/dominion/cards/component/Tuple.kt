package fr.umontpellier.iut.dominion.cards.component

import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.cards.Card


/**
 * Un triplet générique pour transporter trois données liées.
 */
interface Tuple<out NOT_NULL_TUPLE> {
    fun contentIsNotNull(): Boolean
    fun toNotNull(): NOT_NULL_TUPLE
}

data class NonNullPair<out X : Any, out Y : Any>(val first: X, val second: Y)
data class NonNullChoiceMade<out X : Any, out Y : Any>(val content: X, val choice: Y)

data class Pair<out X, out Y>(val first: X?, val second: Y?) : Tuple<NonNullPair<X & Any, Y & Any>> {
    override fun contentIsNotNull() = first != null && second != null
    override fun toNotNull() = NonNullPair(first!!, second!!)
}

data class ChoiceMade<out X, out Y>(val content : X?, val choice : Y?) : Tuple<NonNullChoiceMade<X & Any, Y & Any>> {
    override fun contentIsNotNull() = content != null && choice != null
    override fun toNotNull() = NonNullChoiceMade(content!!, choice!!)
}

data class Triplet<out U, out V, out X>(
    val first: U,
    val second: V,
    val third: X
) : Tuple<Triplet<U & Any, V & Any, X & Any>> {
    override fun contentIsNotNull() : Boolean {
        return first != null && second != null && third != null
    }
    override fun toNotNull() = Triplet(first!!, second!!, third!!)
}

data class Transformation<out X, out Y>(val self : X?, val result : Y?) : Tuple<Transformation<X & Any, Y & Any>> {
    override fun contentIsNotNull() : Boolean {
        return result != null && self != null
    }
    override fun toNotNull() = Transformation(self!!, result!!)
}

data class RaceCompetitors(
    val myCard: Card?,
    val opponent: Player?,
    val opponentCard: Card?
) : Tuple<NonNullRaceCompetitors> {
    override fun contentIsNotNull() : Boolean {
        return myCard != null && opponentCard != null && opponent != null
    }
    override fun toNotNull() = NonNullRaceCompetitors(myCard!!, opponent!!, opponentCard!!)
}

data class NonNullRaceCompetitors(
    val myCard: Card,
    val opponent: Player,
    val opponentCard: Card
)




