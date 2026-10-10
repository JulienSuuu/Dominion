package fr.umontpellier.iut.dominion.domain.dsl.card.extensions

import fr.umontpellier.iut.dominion.domain.common.Logger
import fr.umontpellier.iut.dominion.domain.components.card.effect.BiEffect
import fr.umontpellier.iut.dominion.domain.dsl.card.core.InteractionRequest
import fr.umontpellier.iut.dominion.domain.dsl.card.builders.ContextBuilder
import fr.umontpellier.iut.dominion.domain.dsl.card.core.Context
import fr.umontpellier.iut.dominion.domain.dsl.card.core.PipelineState


fun <T : BiEffect<U, V>, U : Logger, V, X, R, D> ContextBuilder<T, U, V, PipelineState<X, R, D>>.filterPipe(condition : suspend (R) -> Boolean) : ContextBuilder<T, U, V, PipelineState<X, R, D>> {
    return this.filterPipe { _, _ -> condition(current) }
}

fun <T : BiEffect<U, V>, U : Logger, V, X, D, R> ContextBuilder<T, U, V, PipelineState<X, R, D>>.filterPipe(condition : suspend (U, V, X, R) -> Boolean) : ContextBuilder<T, U, V, PipelineState<X, R, D>> {
    return this.filterPipe { u, v -> condition(u, v, source, current) }
}

fun <T : BiEffect<U, V>, U : Logger, V, X, D, R> ContextBuilder<T, U, V, PipelineState<X, R, D>>.filterPipe(condition : suspend PipelineState<X, R, D>.(U, V) -> Boolean) : ContextBuilder<T, U, V, PipelineState<X, R, D>> {
    return ContextBuilder(parent) {
        val ctx = function()
        val data = ctx.data
        val x = ctx.data?.source
        val r = ctx.data?.current
        val d = ctx.data?.extraData
        if (x != null && r!= null && d!=null && data.condition(right, left)) ctx else Context(right, left, null)
    }
}

fun <T : BiEffect<U, V>, U : Logger, V, X, R, next, D> ContextBuilder<T, U, V, PipelineState<X, R, D>>.mapPipe(
    transformation: suspend PipelineState<X, R, D>.(U, V) -> next
): ContextBuilder<T, U, V, PipelineState<X, next, D>> {
    return ContextBuilder<T, U, V, PipelineState<X, next, D>>(parent) {
        val oldCtx = this.function()
        val state = oldCtx.data

        if (state == null) {
            Context(oldCtx.right, oldCtx.left, null)
        } else {
            val newData = state.transformation(oldCtx.right, oldCtx.left)
            Context(oldCtx.right, oldCtx.left, PipelineState(state.source, newData, state.extraData))
        }
    }
}


fun <T : BiEffect<U, V>, U : Logger, V, X, R, D> ContextBuilder<T, U, V, PipelineState<X, R, D>>.filterPipeNotNull(
    predicate: PipelineState<X, R, D>.(U, V) -> Boolean = { _, _ -> true}
): ContextBuilder<T, U, V, PipelineState<X, R, D>> {

    return ContextBuilder(parent) {
        val ctx = this.function()
        val state = ctx.data

        if (state != null && state.predicate(ctx.right, ctx.left)) {
            Context(ctx.right, ctx.left, state)
        } else {
            Context(ctx.right, ctx.left, null)
        }
    }
}

fun <T : BiEffect<U, V>, U : Logger, V, X, R, D> ContextBuilder<T, U, V, PipelineState<X, R, D>>.thenWithPipe(
    nextAction : suspend PipelineState<X, R, D>.(U, V) -> Unit
) : ContextBuilder<T, U, V, PipelineState<X, R, D>>{
    return this.thenDoPipe (nextAction)
}

fun <T : BiEffect<U, V>, U : Logger, V, X, R, D> ContextBuilder<T, U, V, PipelineState<X, R, D>>.thenIfFail(
    nextAction : suspend (U, V) -> Unit
) : ContextBuilder<T, U, V, PipelineState<X, R, D>>{
    return ContextBuilder(parent){
        val ctx = this.function()
        val stat = ctx.data

        if(stat == null){
            nextAction(right, left)
        }

        ctx
    }
}

fun <T : BiEffect<U, V>, U : Logger, V, X, R, D> ContextBuilder<T, U, V, PipelineState<X, R, D>>.thenDoPipe(
    nextAction : suspend PipelineState<X, R, D>.(U, V) -> Unit
) : ContextBuilder<T, U, V, PipelineState<X, R, D>>{
    return ContextBuilder(parent){
        val ctx = function()
        val stat = ctx.data

        if(stat == null) ctx
        else {
            stat.nextAction(right, left)
            Context(right, left, stat)
        }
    }
}


fun <T : BiEffect<U, V>, U : Logger, V, X, R, next, D> ContextBuilder<T, U, V, PipelineState<X, R, D>>.internalChooseWithData(
    config: (U, V, X, R) -> InteractionRequest<X>,
    target: suspend (U, V, X) -> Logger,
    playerMethod: suspend (Logger, InteractionRequest<X>) -> next?
) : ContextBuilder<T, U, V, PipelineState<X, next, D>> {
    return ContextBuilder(parent) {
        val ctx = this.function()
        val state = ctx.data

        if (state != null) {
            val req = config(ctx.right, ctx.left, state.source, state.current)
            val logger = target(ctx.right, ctx.left, state.source)
            val nextResult = playerMethod(logger, req)

            if (nextResult != null) {
                Context(ctx.right, ctx.left, PipelineState(state.source, nextResult, state.extraData))
            } else {
                Context(ctx.right, ctx.left, null)
            }
        } else {
            Context(ctx.right, ctx.left, null)
        }
    }
}

fun <T : BiEffect<U, V>, U : Logger, V, X, R, next, D> ContextBuilder<T, U, V, PipelineState<X, R, D>>.internalChooseWithPipe(
    config: (U, V, X, R) -> InteractionRequest<X>,
    target: suspend (U, V, X) -> Logger,
    playerMethod: suspend (Logger, InteractionRequest<X>) -> next?
) : ContextBuilder<T, U, V, PipelineState<X, R, next>> {
    return ContextBuilder(parent) {
        val ctx = this.function()
        val state = ctx.data

        if (state != null) {
            val req = config(ctx.right, ctx.left, state.source, state.current)
            val logger = target(ctx.right, ctx.left, state.source)
            val nextResult = playerMethod(logger, req)

            if (nextResult != null) {
                Context(ctx.right, ctx.left, PipelineState(state.source, state.current, nextResult))
            } else {
                Context(ctx.right, ctx.left, null)
            }
        } else {
            Context(ctx.right, ctx.left, null)
        }
    }
}
