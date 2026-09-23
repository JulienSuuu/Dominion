package fr.umontpellier.iut.dominion.cards.Description

import fr.umontpellier.iut.dominion.cards.component.CardComponent
import kotlin.reflect.KClass

object CardCatalog {
    val registry = java.util.concurrent.ConcurrentHashMap<String, MutableMap<KClass<out DominionDescription>, DominionDescription>>()

    /**
     * Enregistre un composant pour un modèle de carte spécifique
     */
    fun <T : DominionDescription> registerDescription(cardName: String, component: T) {
        val cardNameLower = cardName.lowercase()
        val componentsMap = registry.computeIfAbsent(cardNameLower) { java.util.concurrent.ConcurrentHashMap() }

        val componentClass = component::class
        if (componentsMap.containsKey(componentClass)) return
        componentsMap[componentClass] = component
    }



    fun registerAll(vararg names: String, description: BonusDescription) {
        names.forEach { registerDescription(it, description) }
    }

    inline fun <reified T: DominionDescription> getComponent(cardName: String): T? {
        val componentsMap = registry[cardName.lowercase()] ?: return null
        return componentsMap[T::class] as? T
    }



    class CardScope(val cardName: String) {
        inline fun <reified T : DominionDescription> add(component: T): CardScope {
            registerDescription(cardName, component)
            return this
        }

        operator fun DominionDescription.unaryPlus() {
            registerDescription(cardName, this)
        }
    }

    /**
     * Permet de configurer plusieurs composants pour une même carte.
     */
    fun configure(cardName: String, block: CardScope.() -> Unit) {
        CardScope(cardName).block()
    }

    fun toText(key : String) : String {
        val components = registry[key.lowercase()] ?: return ""
        return components.values.joinToString("\n"){ it.text }
    }

    fun toJson(cardName: String): String {
        val componentsMap = registry[cardName.lowercase()] ?: return "{}"

        val componentsJson = componentsMap.entries.joinToString(separator = ",") { (kClass, description) ->
            "\"${kClass.simpleName}\": ${description.toJson()}"
        }

        return "{$componentsJson}"
    }
}