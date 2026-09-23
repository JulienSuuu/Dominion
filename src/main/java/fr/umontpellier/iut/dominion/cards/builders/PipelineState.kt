package fr.umontpellier.iut.dominion.cards.builders

import fr.umontpellier.iut.dominion.cards.component.Tuple

data class PipelineState<X, R, D>(
    val source: X,
    val current: R,
    val extraData: D
) : Tuple<PipelineState<X & Any, R & Any, D & Any>>{
    override fun contentIsNotNull(): Boolean {
        return current != null && extraData != null && source != null
    }
    override fun toNotNull() = PipelineState(source!!, current!!, extraData!!)

}