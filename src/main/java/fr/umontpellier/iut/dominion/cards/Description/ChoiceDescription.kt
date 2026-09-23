package fr.umontpellier.iut.dominion.cards.Description

import fr.umontpellier.iut.dominion.Button
import java.awt.SystemColor.text

class ChoiceDescription : DominionDescription {
    var buttons = emptyList<Button>()
    override var text: String = ""
        private set
    var selectCount: Int = 1
        private set

    fun choose(count: Int): ChoiceDescription {
        this.selectCount = count
        return this
    }

    fun text(text: String): ChoiceDescription {
        this.text = text
        return this
    }

    fun addChoice(vararg button: Button): ChoiceDescription {
        buttons = button.toList()
        return this
    }

    override fun toJson() = """
        {
        "text": "$text",
        "selectCount": $selectCount,
        "buttons": ${buttons.joinToString { it.toJson() }}
        }
    """.trimIndent()


}

fun Button.toJson() = """
    "label": "$label",
    "value": $value
""".trimIndent()