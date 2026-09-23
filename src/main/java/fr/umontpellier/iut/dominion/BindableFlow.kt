package fr.umontpellier.iut.dominion

import fr.umontpellier.iut.dominion.Supply.Event.ListChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.CopyOnWriteArrayList

private val activeBindings = Collections.synchronizedMap(WeakHashMap<MutableStateFlow<*>, Job>())
private val activeListeners = Collections.synchronizedMap(WeakHashMap<MutableStateFlow<*>, CopyOnWriteArrayList<Job>>())

val <T> MutableStateFlow<T>.isBound: Boolean
    get() = activeBindings[this]?.isActive == true

val <T> MutableStateFlow<T>.hasListeners: Boolean
    get() = activeListeners[this]?.any { it.isActive } == true

fun <T> MutableStateFlow<T>.bind(scope: CoroutineScope, source: Flow<T>): Job {
    this.unbind()

    val job = scope.launch {
        source.collect { newValue ->
            this@bind.value = newValue
        }
    }

    job.invokeOnCompletion {
        activeBindings.remove(this, job)
    }

    activeBindings[this] = job
    return job
}

fun <T, R> MutableStateFlow<R>.bindComputed(
    scope: CoroutineScope,
    dependencies: Flow<T>,
    compute: (T) -> R
): Job {
    this.unbind()

    val computedFlow = dependencies.map { compute(it) }

    val job = scope.launch {
        computedFlow.collect { newValue ->
            this@bindComputed.value = newValue
        }
    }

    job.invokeOnCompletion {
        activeBindings.remove(this, job)
    }

    activeBindings[this] = job
    return job
}

fun <R> MutableStateFlow<R>.bindCombined(
    scope: CoroutineScope,
    vararg dependencies: Flow<*>,
    compute: () -> R
): Job {
    this.unbind()
    val combinedFlow = combine(dependencies.toList()) { _ ->
        compute()
    }

    val job = scope.launch {
        combinedFlow.collect { newValue ->
            this@bindCombined.value = newValue
        }
    }

    job.invokeOnCompletion {
        activeBindings.remove(this, job)
    }

    activeBindings[this] = job
    return job
}

fun <T1, T2, R> MutableStateFlow<R>.bindCombined(
    scope: CoroutineScope,
    dep1: Flow<T1>,
    dep2: Flow<T2>,
    compute: (T1, T2) -> R
): Job {
    this.unbind()

    val combinedFlow = combine(dep1, dep2) { v1, v2 -> compute(v1, v2) }

    val job = scope.launch {
        combinedFlow.collect { newValue ->
            this@bindCombined.value = newValue
        }
    }

    job.invokeOnCompletion {
        activeBindings.remove(this, job)
    }

    activeBindings[this] = job
    return job
}

fun <T> MutableStateFlow<T>.addListener(
    scope: CoroutineScope,
    block: (oldValue: T, newValue: T) -> Unit
): Job {
    val job = scope.launch {
        var previousValue = value

        this@addListener
            .drop(1)
            .collect { newValue ->
                val oldValue = previousValue
                previousValue = newValue

                block(oldValue, newValue)
            }
    }

    job.invokeOnCompletion {
        val list = activeListeners[this]
        list?.remove(job)
        if (list?.isEmpty() == true) {
            activeListeners.remove(this)
        }
    }
    activeListeners.computeIfAbsent(this) { CopyOnWriteArrayList() }.add(job)
    return job
}

fun <T> MutableStateFlow<List<T>>.addListListener(
    scope: CoroutineScope,
    block: (ListChange<T>) -> Unit
): Job {
    var previousList = this.value

    val job = scope.launch {
        this@addListListener.collect { newList ->
            if (newList != previousList) {
                block(ListChange(previousList, newList))
                previousList = newList
            }
        }
    }

    job.invokeOnCompletion {
        val list = activeListeners[this]
        list?.remove(job)
        if (list?.isEmpty() == true) {
            activeListeners.remove(this)
        }
    }

    activeListeners.computeIfAbsent(this) { CopyOnWriteArrayList() }.add(job)
    return job
}

fun <T> MutableStateFlow<T>.clearListeners() {
    activeListeners.remove(this)?.forEach { it.cancel() }
}

fun <T> MutableStateFlow<T>.unbind() {
    activeBindings.remove(this)?.cancel()
}