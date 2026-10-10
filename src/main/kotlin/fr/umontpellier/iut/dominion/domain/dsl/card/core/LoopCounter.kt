package fr.umontpellier.iut.dominion.domain.dsl.card.core

import fr.umontpellier.iut.dominion.domain.common.Logger
import fr.umontpellier.iut.dominion.domain.components.card.effect.BiEffect
import fr.umontpellier.iut.dominion.domain.components.card.effect.then
import fr.umontpellier.iut.dominion.domain.dsl.card.builders.ContextBuilder

class LoopCounter(var current: Int = 0, val max : Int = 0){
    val remaining get() = max - current
}

fun <U : Logger, V, X> BiEffect<U, V>.loop(
    number: Int,
    block: BiEffect<U, V>.(LoopCounter) -> ContextBuilder<BiEffect<U, V>, U, V, X>
): BiEffect<U, V> {
    return this.then { u, v ->
        val counter = LoopCounter(max = number)
        val effect = BiEffect.empty<U, V>().block(counter)
        var currentData: Any? = Unit

        for(iterator in 0 until number) {
            val ctx = effect.function(Context(u, v, currentData))
            if(ctx.data == null) break
            currentData = ctx.data
            counter.current++
        }
    }
}