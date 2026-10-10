package fr.umontpellier.iut.dominion.domain.dsl.card.builders

import fr.umontpellier.iut.dominion.domain.common.Logger
import fr.umontpellier.iut.dominion.domain.common.chooseCardFromHand
import fr.umontpellier.iut.dominion.domain.common.chooseCardFromList
import fr.umontpellier.iut.dominion.domain.common.chooseCardFromSupply
import fr.umontpellier.iut.dominion.domain.common.chooseWhatToDo
import fr.umontpellier.iut.dominion.domain.model.card.Card
import fr.umontpellier.iut.dominion.domain.components.card.effect.BiEffect
import fr.umontpellier.iut.dominion.domain.dsl.card.core.InteractionRequest
import fr.umontpellier.iut.dominion.domain.dsl.card.core.Context
import fr.umontpellier.iut.dominion.domain.dsl.card.core.PipelineState

typealias ChoiceAction<U, V, X, R> = suspend (Logger, U, V, X) -> R?



class TargetedBuilder<T : BiEffect<U, V>, U : Logger, V, X>(
    private val parent: ContextBuilder<T, U, V, X>,
    private val targetPicker: suspend (U, V, X) -> Logger
) {

    private fun <R> makeChoice(
        askLogger: ChoiceAction<U, V, X, R>
    ): ChooseBuilder<T, U, V, X, R, Unit> {
        val next : ContextBuilder<T, U, V, PipelineState<X, R, Unit>> = ContextBuilder(parent.parent) {
            val ctx = parent.function(this)
            val dataX = ctx.data

            if (dataX != null) {
                val logger = targetPicker(ctx.right, ctx.left, dataX)
                val choice = askLogger(logger, ctx.right, ctx.left, dataX)
                if (choice == null) {
                    Context(ctx.right, ctx.left, null)
                } else Context(ctx.right, ctx.left, PipelineState(dataX, choice, Unit))
            } else {
                Context(ctx.right, ctx.left, null)
            }
        }

        return ChooseBuilder(parent, next, targetPicker)
    }

    // --- Fonctions classiques (U, V) -> Request ---

    fun chooseCardFromHand(config: (U, V) -> InteractionRequest<X?>): ChooseBuilder<T, U, V, X, Card, Unit> =
        makeChoice { logger, u, v, _ ->
            logger.chooseCardFromHand(config(u, v) )
        }

    fun chooseWhatToDo(config: (U, V) -> InteractionRequest<X?>): ChooseBuilder<T, U, V, X, String, Unit> =
        makeChoice { logger, u, v, _ ->

            logger.chooseWhatToDo(config(u, v))
        }

    fun chooseCardFromList(config: (U, V) -> InteractionRequest<X?>): ChooseBuilder<T, U, V, X, Card, Unit> =
        makeChoice { logger, u, v, _ ->

            logger.chooseCardFromList(config(u, v))
        }

    fun chooseCardFromSupply(config : (U, V) -> InteractionRequest<X?>): ChooseBuilder<T, U, V, X, Card, Unit> =
        makeChoice { logger, u, v, _ -> logger.chooseCardFromSupply(config(u, v)) }

    fun chooseCardFromSupply(config : (U, V, X) -> InteractionRequest<X?>): ChooseBuilder<T, U, V, X, Card, Unit> =
        makeChoice { logger, u, v, x -> logger.chooseCardFromSupply(config(u, v, x)) }

    // --- Fonctions avec données injectées (U, V, X) -> Request ---

    fun chooseWhatToDo(config: (U, V, X) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, String, Unit> =
        makeChoice { logger, u, v, x ->
            logger.chooseWhatToDo(config(u, v, x))
        }

    fun chooseCardFromHand(config: (U, V, X) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, Card, Unit> =
        makeChoice { logger, u, v, x ->
            logger.chooseCardFromHand(config(u, v, x))
        }

    fun chooseCardFromList(config: (U, V, X) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, Card, Unit> =
        makeChoice { logger, u, v, x ->
            logger.chooseCardFromList(config(u, v, x))
        }
}

fun <U : Logger, V, T : BiEffect<U, V>> T.choose(): TargetedBuilder<T, U, V, Boolean> {
    return lookingAt { _, _ -> true }.choose()
}

infix fun <U : Logger, V, T : BiEffect<U, V>> T.choose(target: (U, V) -> Logger): TargetedBuilder<T, U, V, Boolean> {
    return lookingAt { _, _ -> true }.choose(target)
}
