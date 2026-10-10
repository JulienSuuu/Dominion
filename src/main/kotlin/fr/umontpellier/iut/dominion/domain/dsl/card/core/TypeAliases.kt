package fr.umontpellier.iut.dominion.domain.dsl.card.core

import fr.umontpellier.iut.dominion.domain.components.card.effect.BiEffect
import fr.umontpellier.iut.dominion.domain.dsl.card.builders.ContextBuilder

typealias ContextAction<U, V, X> = suspend Context<U, V, *>.() -> Context<U, V, X?>
typealias LoopAction<U, V, X, C> = suspend BiEffect<U, V>.(X, LoopCounter) -> ContextBuilder<BiEffect<U, V>, U, V, C>
typealias SuspendTriHandler<U, V, X> = suspend (U, V, X) -> Unit
typealias SuspendBiHandler<U, V> = suspend (U, V) -> Unit
typealias SuspendBiFunction<U, V, R> = suspend (U, V) -> R
typealias SuspendTriFunction<U, V, X, R> = suspend (U, V, X) -> R
typealias SuspendBiOperation<U, V, X, R> = suspend X.(U, V) -> R