package fr.umontpellier.iut.dominion.domain.model.supply

data class ListChange<T>(
    val oldList: List<T>,
    val newList: List<T>
) {
    fun wasRemoved(): Boolean = oldList.size > newList.size
    fun wasAdded(): Boolean = newList.size > oldList.size

    val removed: List<T>
        get() = oldList.filter { it !in newList }

    val added: List<T>
        get() = newList.filter { it !in oldList }
}