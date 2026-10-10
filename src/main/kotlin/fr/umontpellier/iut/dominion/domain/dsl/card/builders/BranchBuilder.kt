package fr.umontpellier.iut.dominion.domain.dsl.card.builders

import fr.umontpellier.iut.dominion.domain.common.Logger
import fr.umontpellier.iut.dominion.domain.components.card.effect.BiEffect
import fr.umontpellier.iut.dominion.domain.dsl.card.core.DecisionContext

class BranchBuilder<U, V, X>(val context: DecisionContext<U, V, X>) {
    internal val cases = mutableListOf<Pair<DecisionContext<U, V, X>.() -> Boolean, suspend (U, V, X) -> Unit>>()

    infix fun (DecisionContext<U, V, X>.() -> Boolean).then(action: suspend (U, V, X) -> Unit){
        cases.add(this to action)
    }

    val data get() =  context.data


    infix fun otherwise(action: suspend (U, V, X) -> Unit){
        cases.add(on {true} to action)
    }

    infix fun on(condition: DecisionContext<U, V, X>.() -> Boolean) = condition

}

fun <T : BiEffect<U, V>, U : Logger, V, X : Any> ContextBuilder<T, U, V, X>.branchDecision(
    block: BranchBuilder<U, V, X>.() -> Unit
): ContextBuilder<T, U, V, X> {
    return ContextBuilder(this.parent) {
        val ctx = this.function()
        val data = ctx.data

        if (data != null) {
            val decisionContext = DecisionContext(right, left, data)
            val builder = BranchBuilder(decisionContext)
            builder.block()

            val matchingCase = builder.cases.firstOrNull { (condition, _) -> decisionContext.condition() }
            matchingCase?.second?.invoke(right, left, data)
        }
        ctx
    }
}

fun <T : BiEffect<U, V>, U : Logger, V, X : Any> ContextBuilder<T, U, V, X>.match(
    block: BranchBuilder<U, V, X>.() -> Unit)
        : ContextBuilder<T, U, V, X> {
    return ContextBuilder(parent) {
        val ctx = this.function()
        val data = ctx.data

        if (data != null) {
            val decisionContext = DecisionContext(right, left, data)
            val builder = BranchBuilder(decisionContext)
            builder.block()

            builder.cases.forEach { (condition, action) ->
                if (decisionContext.condition()) {
                    action(right, left, ctx.data)
                }
            }
        }

        ctx
    }
}

