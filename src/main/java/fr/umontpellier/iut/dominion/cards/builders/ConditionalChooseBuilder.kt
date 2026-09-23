package fr.umontpellier.iut.dominion.cards.builders

import fr.umontpellier.iut.dominion.Interface.Logger
import fr.umontpellier.iut.dominion.cards.component.BiEffect

class ConditionalChooseBuilder<T : BiEffect<U, V>, U : Logger, V, X, R, D>(
    private val parent: ContextBuilder<T, U, V, X>,
    private val result: ContextBuilder<T, U, V, PipelineState<X, R, D>>,
    private val targetPicker: suspend (U, V, X) -> Logger,
    private val predicate: suspend (U, V, X, R) -> Boolean,
    private val onSuccess: suspend (U, V, X, R) -> Unit = { _, _, _, _ -> }
) {

    fun thenDo(action: suspend (U, V, X, R) -> Unit): ConditionalChooseBuilder<T, U, V, X, R, D> {
        return ConditionalChooseBuilder(parent, result, targetPicker, predicate, action)
    }
    /**
     * Clôture le bloc conditionnel
     */
    fun otherwise(alternativeAction: suspend (U, V, X) -> Unit): ChooseBuilder<T, U, V, X, R, D> {
        val nextResult = ContextBuilder(result.parent) {
            val ctxR = result.function(this)

            val x = ctxR.data?.source
            val r = ctxR.data?.current

            if (x != null && r != null) {
                if (predicate(right, left, x, r)) {
                    onSuccess(right, left, x, r)
                    ctxR
                } else {
                    alternativeAction(right, left, x)
                    Context(right, left, null)
                }
            } else if (x != null) {
                alternativeAction(right, left, x)
                Context(right, left, null)
            } else {
                ctxR
            }
        }
        return ChooseBuilder(parent, nextResult, targetPicker)
    }
}