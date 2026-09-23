package fr.umontpellier.iut.dominion.cards.Description

import fr.umontpellier.iut.dominion.cards.Bonus.Bonus
import fr.umontpellier.iut.dominion.cards.displayCoin

class BonusDescription : DominionDescription {
    val attributes : MutableMap<String, Int> = HashMap()

    fun getAttribute(key: String) = attributes[key.lowercase()]?.let { it > 0 } ?: false
    fun giveAttributes(bonus : Bonus) : BonusDescription { attributes.putAll(bonus.toDescription()); return this }
    fun setManualAttribute(key: String, value: Int) : BonusDescription { attributes[key.lowercase()] = value; return this }


    override fun toJson() = attributes.entries.joinToString(prefix = "{", postfix = "}") {
        "\"${it.key}\": ${it.value}"
    }

    fun cards(amount: Int) = setManualAttribute("cards", amount)
    fun buys(amount: Int) = setManualAttribute("buy", amount)
    fun actions(amount: Int) = setManualAttribute("action", amount)
    fun money(amount: Int) = setManualAttribute("money", amount)

    override val text get() = attributes.entries.joinToString {
        when(it.key) {
            "money" -> "+${displayCoin(it.value)}"
            else -> "+ $it.value ${it.key}"
        }

    }

    companion object {
        val Money = BonusDescription().giveAttributes(Bonus.Money)
        val Action = BonusDescription().giveAttributes(Bonus.Action)
        val Buy = BonusDescription().giveAttributes(Bonus.Buy)
        val Draw = BonusDescription().giveAttributes(Bonus.draw)
        val ActionAndDraw = BonusDescription().giveAttributes(Bonus.ActionAndDraw)
    }
}