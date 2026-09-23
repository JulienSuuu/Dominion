package fr.umontpellier.iut.dominion.cards.Description

import kotlin.reflect.KClass

open class InstructionDescription : DominionDescription {

    val topComponents = mutableMapOf<KClass<out DominionDescription>, DominionDescription>()

    val bottomComponents = mutableMapOf<KClass<out DominionDescription>, DominionDescription>()

    fun top(block: ComponentScope.() -> Unit): InstructionDescription {
        ComponentScope(topComponents).block()
        return this
    }

    fun bottom(block: ComponentScope.() -> Unit): InstructionDescription {
        ComponentScope(bottomComponents).block()
        return this
    }

    override val text: String
        get() = listOf(
            topComponents.values.joinToString("\n") { it.text },
            bottomComponents.values.joinToString("\n") { it.text }
        )
            .filter { it.isNotBlank() }
            .joinToString("\n")

    override fun toJson(): String {
        val topJson = topComponents.entries.joinToString(separator = ",") { (kClass, desc) ->
            "\"${kClass.simpleName}\": ${desc.toJson()}"
        }

        val bottomJson = bottomComponents.entries.joinToString(separator = ",") { (kClass, desc) ->
            "\"${kClass.simpleName}\": ${desc.toJson()}"
        }

        return """
    {
      "top": { $topJson }${if (bottomComponents.isNotEmpty()) ", \"bottom\": { $bottomJson }" else ""}
    }
    """.trimIndent()
    }

    /**
     * Scope permettant d'ajouter des composants dans l'une des deux zones.
     */
    class ComponentScope(val targetMap: MutableMap<KClass<out DominionDescription>, DominionDescription>) {

        /** Ajoute un composant dans la zone courante */
        inline fun <reified T : DominionDescription> add(component: T) {
            targetMap[T::class] = component
        }

        /** Syntaxe alternative avec l'opérateur + */
        operator fun DominionDescription.unaryPlus() {
            targetMap[this::class] = this
        }
    }

    inline fun <reified T : DominionDescription> getTop(): T? = topComponents[T::class] as? T
    inline fun <reified T : DominionDescription> getBottom(): T? = bottomComponents[T::class] as? T

    fun hasBottom(): Boolean = bottomComponents.isNotEmpty()
}