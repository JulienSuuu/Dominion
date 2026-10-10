package fr.umontpellier.iut.dominion.domain.dsl.card.builders

import fr.umontpellier.iut.dominion.domain.common.Logger
import fr.umontpellier.iut.dominion.domain.components.card.effect.BiEffect
import fr.umontpellier.iut.dominion.domain.dsl.card.core.ChooseDecisionContext

class ChooseBranchBuilder<U : Logger, V, X : Any, R : Any, D : Any>(
    val context: ChooseDecisionContext<U, V, X, R, D>
) {
    val cases = mutableListOf<Pair<ChooseDecisionContext<U, V, X, R, D>.() -> Boolean, suspend R.(U, V, X) -> Unit>>()

    infix fun (ChooseDecisionContext<U, V, X, R, D>.() -> Boolean).then(action: suspend R.(U, V, X) -> Unit) {
        cases.add(this to action)
    }


    infix fun otherwise(action: suspend R.(U, V, X) -> Unit){
        cases.add(on {true} to action)
    }

    infix fun on(condition: ChooseDecisionContext<U, V, X, R, D>.() -> Boolean) = condition
}

fun <T : BiEffect<U, V>, U : Logger, V, X : Any, R : Any, D : Any> ChooseBuilder<T, U, V, X, R, D>.branchDecision(
    block: ChooseBranchBuilder<U, V, X, R, D>.() -> Unit
): ChooseBuilder<T, U, V, X, R, D> {
    val choiceBuilder = this.result

    val next = ContextBuilder(choiceBuilder.parent) {
        val choiceCtx = choiceBuilder.function(this)
        val dataR = choiceCtx.data?.current
        val dataX = choiceCtx.data?.source
        val dataD = choiceCtx.data?.extraData

        if (dataR != null && dataX != null && dataD != null) {
            val combinedContext = ChooseDecisionContext(right, left, dataX, dataR, dataD)
            val builder = ChooseBranchBuilder(combinedContext)
            builder.block()

            val matchingCase = builder.cases.firstOrNull { (condition, _) ->
                combinedContext.condition()
            }
            matchingCase?.second?.invoke(dataR, choiceCtx.right, choiceCtx.left, dataX)
        }
        choiceCtx
    }
    return ChooseBuilder(parent, next, targetPicker)
}

fun <T : BiEffect<U, V>, U : Logger, V, X : Any, R : Any, D : Any> ChooseBuilder<T, U, V, X, R, D>.matchAll(
    block: ChooseBranchBuilder<U, V, X, R, D>.() -> Unit
): ChooseBuilder<T, U, V, X, R, D> {
    val choiceBuilder = this.result

    val next = ContextBuilder(choiceBuilder.parent) {
        val choiceCtx = choiceBuilder.function(this)
        val dataR = choiceCtx.data?.current
        val dataX = choiceCtx.data?.source
        val dataD = choiceCtx.data?.extraData


        if (dataX != null && dataR != null && dataD != null) {
            val combinedContext = ChooseDecisionContext(right, left, dataX, dataR, dataD)
            val builder = ChooseBranchBuilder(combinedContext)
            builder.block()

            builder.cases.forEach { (condition, action) ->
                if (combinedContext.condition()) {
                    dataR.action(choiceCtx.right, choiceCtx.left, dataX)
                }
            }
        }
        choiceCtx
    }
    return ChooseBuilder(parent, next, targetPicker)
}