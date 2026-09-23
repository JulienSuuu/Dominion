package fr.umontpellier.iut.dominion.cards.component

import java.util.*
import java.util.function.Consumer

/**
 * Interface fonctionnelle représentant un effet de jeu agissant sur une seule entité (T).
 */
fun interface Effect<in T> : CardComponent, suspend (T) -> Unit {
    override suspend fun invoke(t: T)
}

/**
 * Enchaîne une action après l'effet actuel.
 */
fun <T, E : Effect<T>> E.then(after: suspend (T) -> Unit): Effect<T> {
    return Effect { t ->
        this(t)
        after(t)
    }
}

/**
 * Répète l'effet actuel un nombre fixe de fois.
 */
fun <T, E : Effect<T>> E.repeat(times: Int): Effect<T> {
    return Effect { t ->
        repeat(times) { this(t) }
    }
}

/**
 * Insère une action avant l'effet actuel.
 */
fun <T, E : Effect<T>> E.compose(before: suspend (T) -> Unit): Effect<T> {
    return Effect { t ->
        before(t)
        this(t)
    }
}