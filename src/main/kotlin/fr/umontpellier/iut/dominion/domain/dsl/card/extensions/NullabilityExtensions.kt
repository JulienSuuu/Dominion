package fr.umontpellier.iut.dominion.domain.dsl.card.extensions

import fr.umontpellier.iut.dominion.domain.common.Logger
import fr.umontpellier.iut.dominion.domain.components.card.effect.BiEffect
import fr.umontpellier.iut.dominion.domain.dsl.card.core.ChoiceMade
import fr.umontpellier.iut.dominion.domain.dsl.card.core.NonNullChoiceMade
import fr.umontpellier.iut.dominion.domain.dsl.card.core.NonNullPair
import fr.umontpellier.iut.dominion.domain.dsl.card.core.Tuple
import fr.umontpellier.iut.dominion.domain.dsl.card.builders.ContextBuilder
import fr.umontpellier.iut.dominion.domain.dsl.card.core.Context
import fr.umontpellier.iut.dominion.domain.dsl.card.core.Pair

fun <T : BiEffect<U, V>, U : Logger, V, E : Any, C : Collection<E>> ContextBuilder<T, U, V, C>.listIsNotEmpty(): ContextBuilder<T, U, V, C> {
    return ContextBuilder(this.parent) {
        val ctx = this.function()
        val list = ctx.data

        if (!list.isNullOrEmpty()) {
            Context(right, left, list)
        } else {
            Context(right, left, null)
        }
    }
}

/**
 * Filtre les données optionnelles et garantit que la donnée [X] extraite n'est pas nulle.
 * Si le critère ou la présence de donnée échoue, le contexte bascule sur une valeur nulle.
 */
fun <T : BiEffect<U, V>, U : Logger, V, X : Any> ContextBuilder<T, U, V, X?>.filterNotNull(predicate :suspend (X) -> Boolean = {true}): ContextBuilder<T, U, V, X> {
    return filterNotNullWithContext { _, _, x -> predicate(x)  }
}


fun <T : BiEffect<U, V>, U : Logger, V, X : Any> ContextBuilder<T, U, V, X?>.filterNotNullWithContext(predicate : suspend (U, V, X) -> Boolean = {_, _, _ ->true}): ContextBuilder<T, U, V, X> {
    return ContextBuilder(parent){
        val ctx = this.function()
        val data = ctx.data

        if(data != null && predicate(right, left, data)){
            Context(right, left, data)
        }else Context(right, left, null)
    }
}

/**
 * Spécifique aux objets composites de type [Tuple] : s'assure que le conteneur et que
 * son contenu sous-jacent ne contiennent aucune valeur nulle avant de poursuivre.
 */

fun <T : BiEffect<U, V>, U : Logger, V, X : Tuple<OUT>, OUT : Any> ContextBuilder<T, U, V, X>.filterTupleNotNull(
): ContextBuilder<T, U, V, OUT> {
    return ContextBuilder(this.parent) {
        val ctx = this.function()
        val tuple = ctx.data

        if (tuple != null && tuple.contentIsNotNull()) {
            Context(ctx.right, ctx.left, tuple.toNotNull())
        } else {
            Context(ctx.right, ctx.left, null)
        }
    }
}


fun <T : BiEffect<U, V>, U : Logger, V, X, Y> ContextBuilder<T, U, V, ChoiceMade<X, Y>>.filterChoiceNotNull(
): ContextBuilder<T, U, V, NonNullChoiceMade<X & Any, Y & Any>> {
    return ContextBuilder(this.parent) {
        val ctx = this.function()
        val choiceMade = ctx.data

        if(choiceMade != null && choiceMade.contentIsNotNull()){
            Context(right, left, choiceMade.toNotNull())
        } else Context(right, left, null)
    }
}


fun <T : BiEffect<U, V>, U : Logger, V, X, Y> ContextBuilder<T, U, V, Pair<X, Y>>.filterPairNotNull(
): ContextBuilder<T, U, V, NonNullPair<X & Any, Y & Any>> {
    return ContextBuilder(this.parent) {
        val ctx = this.function()
        val choiceMade = ctx.data

        if(choiceMade != null && choiceMade.contentIsNotNull()){
            Context(right, left, choiceMade.toNotNull())
        } else Context(right, left, null)
    }
}









