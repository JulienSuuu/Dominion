package fr.umontpellier.iut.dominion.domain.components.card

import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.event.Event
import kotlin.reflect.KClass

class ComponentRegistry {
    val storage = mutableMapOf<KClass<out CardComponent>, ComponentsList<*>>()

    @Suppress("UNCHECKED_CAST")
    private fun <T : CardComponent> getOrCreateList(key: KClass<T>): ComponentsList<T> {
        return storage.getOrPut(key) { ComponentsList<T>() } as ComponentsList<T>
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : CardComponent> getList(key: KClass<T>): ComponentsList<T>? {
        return storage[key] as? ComponentsList<T>
    }

    fun <T : CardComponent> register(
        key: KClass<T>,
        component: T,
        condition: (Event, Player) -> Boolean
    ) {
        getOrCreateList(key).register(component, condition)
    }

    fun <T : CardComponent> getExecutableComponents(
        key: KClass<T>,
        event: Event,
        player: Player
    ): List<T> {
        return getList(key)?.getExecutableComponents(event, player) ?: emptyList()
    }

    /**
     * Supprime tous les composants du type T spécifié.
     */
    fun <T : CardComponent> remove(key: KClass<T>) {
        storage.remove(key)
    }

    inline fun <reified T : CardComponent> remove() {
        remove(T::class)
    }

    /**
     * Vérifie s'il existe au moins un composant enregistré pour le type T.
     */
    inline fun <reified T : CardComponent> hasComponent(): Boolean {
        return getList(T::class)?.isNotEmpty() == true
    }

    /**
     * Raccourci pour isNotEmpty.
     */
    inline fun <reified T : CardComponent> isNotEmpty(): Boolean = hasComponent<T>()

    /**
     * Récupère le premier composant du type T (utile pour les SingleComponent).
     */
    inline fun <reified T : CardComponent> getComponent(): T? {
        return getList(T::class)?.components?.firstOrNull()?.component
    }

    inline fun <reified T : CardComponent> getEntry() = getList(T::class)?.getEntry()

    /**
     * Duplique le registre de manière isolée pour la méthode Card.copy().
     */
    fun copy(): ComponentRegistry = ComponentRegistry().apply {
        this@ComponentRegistry.storage.forEach { (key, list) ->
            this.storage[key] = list.copy()
        }
    }


}


class ComponentsList<T: CardComponent>(
    val components : MutableList<ComponentEntry<T>> = mutableListOf<ComponentEntry<T>>()
){

    fun register(
        component: T,
        condition: (Event, Player) -> Boolean = { _, _ -> true }
    ) {
        if (component is SingleCardComponent) {
            components.clear()
        }

        components.add(ComponentEntry(component, condition))
    }

    fun isNotEmpty(): Boolean = components.isNotEmpty()

    fun copy(): ComponentsList<T> = ComponentsList(components.toMutableList())

    fun getExecutableComponents(event: Event, player: Player): List<T> {
        return components
            .filter { it.canExecute(event, player) }
            .map { it.component }
    }

    fun getEntry() = components.firstOrNull()


    fun remove(component: T): Boolean {
        components.removeAll{ it.component == component }
        return components.isEmpty()
    }

}


class ComponentEntry<T : CardComponent>(
   val component: T,
   private val condition: (Event, Player) -> Boolean = { _, _ -> true }
){
    fun canExecute(event: Event, player: Player): Boolean {
        return condition(event, player)
    }
}