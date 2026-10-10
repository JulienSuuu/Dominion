package fr.umontpellier.iut.dominion.domain.model.core

import java.util.Collections
import java.util.WeakHashMap

class ShadowKey private constructor(
    val shadowId: Id,
    val activationIndex: Int = 0
) {
    companion object {
        private val cache = Collections.synchronizedMap(WeakHashMap<String, ShadowKey>())


        fun get(shadowId: Id, activationIndex: Int = 0): ShadowKey {
            val lookupKey = "${shadowId.id}_$activationIndex"
            return cache.computeIfAbsent(lookupKey) {
                ShadowKey(shadowId, activationIndex)
            }
        }
    }

    override fun toString(): String = "$shadowId, $activationIndex"

    operator fun plus(other: ShadowKey): ShadowKey {
        return get(Id("${this.shadowId.id}-${other.shadowId.id}"), other.activationIndex)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ShadowKey
        return shadowId.id == other.shadowId.id && activationIndex == other.activationIndex
    }

    fun contains(other: Id) = shadowId.contains(other)

    override fun hashCode(): Int {
        var result = shadowId.id.hashCode()
        result = 31 * result + activationIndex
        return result
    }
}