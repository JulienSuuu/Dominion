package fr.umontpellier.iut.dominion.gui

import fr.umontpellier.iut.dominion.cards.Card
import java.util.Map
import java.util.stream.Collectors

object Utils {
    fun toLog(list: List<Card?>): String {
        return list.filterNotNull()
            .groupingBy { it.name }
            .eachCount()
            .toSortedMap()
            .entries
            .joinToString(", ") { (name, count) ->
                if (count > 1) "$name x$count" else name
            }
    }

    fun toString(list: List<Card?>): String {
        return list.filterNotNull()
            .joinToString(", ")
    }

    fun toJSON(list: List<Card?>): String {
        return list.filterNotNull()
            .joinToString(prefix = "[", postfix = "]") { it.toJsonPlayer() }
    }
}
