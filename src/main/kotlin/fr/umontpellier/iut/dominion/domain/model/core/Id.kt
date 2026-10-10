package fr.umontpellier.iut.dominion.domain.model.core

@JvmInline
value class Id(val id: String){
    fun contains(other : Id) = id.startsWith(other.id)
    fun cardId() = id.split("-", limit = 3).getOrNull(2) ?: ""
    override fun toString(): String {
        return id
    }
}