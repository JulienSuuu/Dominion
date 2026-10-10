package fr.umontpellier.iut.dominion.domain.dsl.card.extensions

import fr.umontpellier.iut.dominion.domain.common.Logger
import fr.umontpellier.iut.dominion.domain.model.card.Card
import fr.umontpellier.iut.dominion.domain.components.card.effect.BiEffect
import fr.umontpellier.iut.dominion.domain.dsl.card.builders.ContextBuilder
import fr.umontpellier.iut.dominion.domain.dsl.card.builders.lookingAt
import fr.umontpellier.iut.dominion.domain.dsl.card.core.Context
import fr.umontpellier.iut.dominion.domain.dsl.card.core.PipelineState
import fr.umontpellier.iut.dominion.domain.dsl.card.core.SuspendTriHandler
import kotlin.collections.filterTo
import kotlin.collections.partition

/**
 * Filtre les éléments d'une collection contenue dans le pipeline.
 * Modifie le type de transport d'un [Iterable] vers une [MutableList].
 *
 * @param E Le type des éléments contenus dans l'itérable.
 * @param predicate Le critère de sélection à appliquer sur chaque élément.
 */
fun <T : BiEffect<U, V>, U : Logger, V, E : Any> ContextBuilder<T, U, V, out Iterable<E>>.filterList(
    predicate: (E) -> Boolean
): ContextBuilder<T, U, V, MutableList<E>> {
    return ContextBuilder(this.parent) {
        val ctx = this.function()
        val list = ctx.data
        if (list != null) {
            val destination = ArrayList<E>()
            list.filterTo(destination, predicate)
            Context(right, left, destination)
        } else {
            Context(right, left, null)
        }
    }
}

/**
 * Filtre les éléments d'une collection et s'assure que la liste résultante n'est pas vide.
 * Si aucun élément ne valide le filtre ou si la source est vide, le contexte du pipeline devient immédiatement nul.
 */
fun <T : BiEffect<U, V>, U : Logger, V, E : Any> ContextBuilder<T, U, V, out Iterable<E>>.filterListAndNotEmpty(
    predicate: (E) -> Boolean = {true}
): ContextBuilder<T, U, V, MutableList<E>> {
    return this.filterList(predicate).listIsNotEmpty()
}

fun <T : BiEffect<U, V>, U : Logger, V, E : Any, K> ContextBuilder<T, U, V, out Iterable<E>>.distinctList(
    selector: (E) -> K
): ContextBuilder<T, U, V, PipelineState<MutableList<E>, List<E>, Unit>> {
    return ContextBuilder(parent) {
        val ctx = this.function()
        val currentCollection = ctx.data
        if (currentCollection != null) {
            val uniqueList = currentCollection.distinctBy(selector)
            Context(ctx.right, ctx.left, PipelineState(currentCollection.toMutableList(), uniqueList, Unit))
        } else {
            Context(ctx.right, ctx.left, null)
        }
    }
}

fun <T : BiEffect<U, V>, U : Logger, V, E : Any, K> ContextBuilder<T, U, V, out Iterable<E>>.distinctListAndNotEmpty(
    selector: (E) -> K
): ContextBuilder<T, U, V, PipelineState<MutableList<E>, List<E>, Unit>> {

    return ContextBuilder(parent) {
        val ctx = this.function()
        val currentCollection = ctx.data
        if (currentCollection != null && currentCollection.any()) {
            val uniqueList = currentCollection.distinctBy(selector)
            Context(ctx.right, ctx.left, PipelineState(currentCollection.toMutableList(), uniqueList, Unit))
        } else {
            Context(ctx.right, ctx.left, null)
        }
    }
}



/**
 * Transforme chaque élément d'une collection via une fonction de mapping.
 * Convertit le pipeline d'un [Iterable] vers une [MutableList] contenant les éléments transformés.
 */
fun <T : BiEffect<U, V>, U : Logger, V, E : Any, R> ContextBuilder<T, U, V, out Iterable<E>>.mapList(
    transform: (E) -> R
): ContextBuilder<T, U, V, MutableList<R>> {
    return ContextBuilder(this.parent) {
        val ctx = this.function()
        val list = ctx.data
        if (list != null) {
            val destination = ArrayList<R>()
            for (element in list) {
                destination.add(transform(element))
            }
            Context(right, left, destination)
        } else {
            Context(right, left, null)
        }
    }
}

/**
 * Compte combien d'éléments de la liste valident le critère spécifié.
 * Transforme la donnée du pipeline d'un [Iterable] vers un entier ([Int]).
 */
fun <T : BiEffect<U, V>, U : Logger, V, E : Any> ContextBuilder<T, U, V, out Iterable<E>>.countList(
    predicate: (E) -> Boolean
): ContextBuilder<T, U, V, Int> {
    return ContextBuilder(this.parent) {
        val ctx = this.function()
        val list = ctx.data
        if (list != null) {
            val count = list.count (predicate)
            Context(right, left, count)
        } else {
            Context(right, left, 0)
        }
    }
}

/**
 * Extrait le tout premier élément de la collection validant le prédicat (ou le premier élément par défaut).
 * Si aucun élément ne correspond, retourne un contexte contenant une valeur nulle.
 */
fun <T : BiEffect<U, V>, U : Logger, V, E : Any> ContextBuilder<T, U, V, out Iterable<E>>.firstOrNull(
    predicate: (E) -> Boolean = {true}
): ContextBuilder<T, U, V, E?> {
    return ContextBuilder(this.parent) {
        val ctx = this.function()
        val list = ctx.data
        if (list != null) {
            var result: E? = null
            for (element in list) {
                if (predicate(element)) {
                    result = element
                    break
                }
            }
            Context(right, left, result)
        } else {
            Context(right, left, null)
        }
    }
}

/**
 * Applique une boucle d'effets de bord sur chacun des éléments de la collection,
 * puis retourne la liste originale totalement intacte pour la suite du pipeline.
 */
fun <T : BiEffect<U, V>, U : Logger, V, E : Any> ContextBuilder<T, U, V, out Iterable<E>>.flatWith(
    action : SuspendTriHandler<U, V, E>
): ContextBuilder<T, U, V, out Iterable<E>> {
    return ContextBuilder(this.parent) {
        val ctx = this.function()
        val list = ctx.data
        if (list != null) {
            for (element in list) {
                action(right, left, element)
            }
        }
        Context(right, left, list)
    }
}


fun <T : BiEffect<U, V>, U : Logger, V, X : List<Card>> ContextBuilder<T, U, V, X>.partition(
    predicate: (Card) -> Boolean
): ContextBuilder<T, U, V, Pair<List<Card>, List<Card>>> {
    return ContextBuilder(parent) {
        val ctx = this.function()
        val cardList = ctx.data

        if (cardList == null) {
            Context(right, left, null)
        } else {
            val (matching, remaining) = cardList.partition(predicate)
            Context(right, left, Pair(matching, remaining))
        }
    }
}

fun <T : BiEffect<U, V>, U : Logger, V, X : List<Card>> ContextBuilder<T, U, V, X>.partitionWithContext(
    predicate: (U, Card) -> Boolean
): ContextBuilder<T, U, V, Pair<List<Card>, List<Card>>> {
    return ContextBuilder(parent) {
        val ctx = this.function()
        val cardList = ctx.data

        if (cardList == null) {
            Context(right, left, Pair(emptyList(), emptyList()))
        } else {
            val matching = mutableListOf<Card>()
            val remaining = mutableListOf<Card>()

            for (card in cardList) {
                if (predicate(right, card)) matching.add(card) else remaining.add(card)
            }

            Context(right, left, Pair(matching, remaining))
        }
    }
}


inline fun <T : BiEffect<U, V>, U : Logger, V, X> ContextBuilder<T, U, V, X>.forEachCard(
    crossinline elements: (U, X) -> Iterable<Card>,
    crossinline block: suspend ContextBuilder<BiEffect<U, V>, U, V, X>.(X, Card) -> ContextBuilder<BiEffect<U, V>, U, V, X>
): ContextBuilder<BiEffect<U, V>, U, V, X> {
    return ContextBuilder(this.parent) {
        val parentCtx = this.function()
        val currentX = parentCtx.data
        var lastCtx: Context<U, V, X?> = parentCtx

        if (currentX != null) {
            elements(right, currentX).forEach { card ->
                val emptyBuilder : ContextBuilder<BiEffect<U, V>, U, V, X> = BiEffect.empty<U, V>().lookingAt { u, v -> currentX }
                val subBuilder = emptyBuilder.block(currentX, card)
                val res = subBuilder.function(this)
                lastCtx = res
            }
        }

        lastCtx
    }
}

