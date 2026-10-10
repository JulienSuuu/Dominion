package fr.umontpellier.iut.dominion.domain.dsl.card.core

import fr.umontpellier.iut.dominion.domain.common.Logger

@JvmRecord
data class Context<U : Logger, V, X>(val right: U, val left: V, val data: X?) : ContextData {
    override fun contentIsNotNull() : Boolean {
        return data != null
    }
}