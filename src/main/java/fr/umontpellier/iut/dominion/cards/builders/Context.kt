package fr.umontpellier.iut.dominion.cards.builders

import fr.umontpellier.iut.dominion.Interface.ContextData
import fr.umontpellier.iut.dominion.Interface.Logger

@JvmRecord
data class Context<U : Logger, V, X>(val right: U, val left: V, val data: X?) : ContextData {
    override fun contentIsNotNull() : Boolean {
        return data != null
    }
}