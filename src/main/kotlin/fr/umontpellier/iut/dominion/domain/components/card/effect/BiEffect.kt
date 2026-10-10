package fr.umontpellier.iut.dominion.domain.components.card.effect

import fr.umontpellier.iut.dominion.domain.common.Logger
import fr.umontpellier.iut.dominion.domain.components.card.CardComponent


fun interface BiEffect<in U : Logger, in V> : CardComponent, suspend (U, V) -> Unit {
    override suspend fun invoke(u: U, v: V)
    companion object {
        fun <U : Logger, V> empty() : BiEffect<U, V> {
            return BiEffect{_, _ -> }
        }
    }
}


fun <T, U : Logger, V> empty() : BiEffect<U, V> where T : CardComponent, T : BiEffect<U, V> {
    return BiEffect{ _, _ -> }
}
/**
 * Enchaîne une action après l'effet actuel (Alias de then)
 */
infix fun <U : Logger, V, T : BiEffect<U, V>> T.first(after: suspend (U, V) -> Unit): BiEffect<U, V> = then(after)

/**
 * Enchaîne une action après l'effet actuel.
 */
infix fun <U : Logger, V, T : BiEffect<U, V>> T.then(after: suspend (U, V) -> Unit): BiEffect<U, V> {
    return BiEffect { u, v ->
        this(u, v)
        after(u, v)
    }
}

infix fun < U : Logger, V, T : BiEffect<U, V>> T.then(after: suspend U.() -> Unit): BiEffect<U, V> {
    return then { u, _ -> u.after() }
}

/**
 * Répète l'effet actuel un nombre fixe de fois.
 */
infix fun <U : Logger, V, T : BiEffect<U, V>> T.repeat(times: Int): BiEffect<U, V> {
    return BiEffect { u, v ->
        repeat(times) { this(u, v) }
    }
}

/**
 * Conditionne l'exécution de l'effet actuel par un prédicat.
 */
infix fun <U : Logger, V, T : BiEffect<U, V>> T.whenCondition(check: (U, V) -> Boolean): BiEffect<U, V> {
    return BiEffect { u, v ->
        if (check(u, v)) {
            this(u, v)
        }
    }
}

/**
 * Insère une action avant l'effet actuel.
 */
infix fun <U : Logger, V, T : BiEffect<U, V>> T.compose(before: (U, V) -> Unit): BiEffect<U, V> {
    return BiEffect { u, v ->
        before(u, v)
        this(u, v)
    }
}

/**
 * Répète l'effet tant qu'une condition est remplie ou que le nombre maximum est atteint.
 */
fun <U : Logger, V, T : BiEffect<U, V>> T.repeatWhile(check: (U, V) -> Boolean, times: Int): BiEffect<U, V> {
    return BiEffect { u, v ->
        for (i in 0 until times) {
            if (!check(u, v)) break
            this(u, v)
        }
    }
}

