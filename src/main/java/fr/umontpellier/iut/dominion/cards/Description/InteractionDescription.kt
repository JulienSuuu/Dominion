package fr.umontpellier.iut.dominion.cards.Description

import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.testTypes

class InteractionDescription : DominionDescription {
    override var text: String = ""
        private set

    var targetPlayer: TargetPlayer = TargetPlayer.SELF
    var targetCard: String = ""
    var fromZone: Destination = Destination.PlayerZone.Hand
    var amount: Int? = null
    var isOptional: Boolean = false

    var filterTypes: MutableList<CardType> = mutableListOf()
    var excludedTypes: MutableList<CardType> = mutableListOf()
    var excludedCards: MutableList<String> = mutableListOf()

    var primaryDestination: Destination? = null

    var secondaryEffect: SecondaryEffect? = null

    fun text(text: String) = apply { this.text = text }
    fun target(target: TargetPlayer) = apply { this.targetPlayer = target }
    fun from(zone: Destination) = apply { this.fromZone = zone }

    fun amount(count: Int?) = apply { this.amount = count }
    fun anyAmount() = apply { this.amount = null }
    fun optional(value: Boolean = true) = apply { this.isOptional = value }

    fun card(card: String) = apply { this.targetCard = card }

    fun filter(vararg types: CardType) = apply { this.filterTypes.addAll(types) }
    fun exclude(vararg names: String) = apply { this.excludedCards.addAll(names) }

    fun applyFilter(): (Card) -> Boolean = { card ->
        val matchesTargetCard = targetCard.isEmpty() || card.hasName(targetCard)
        val exclude = excludedCards.contains(card.name)
        val matchesTypes = card.testTypes {
            any(filterTypes)
            none(excludedTypes)
        }

        matchesTargetCard && !exclude && matchesTypes
    }


    fun moveTo(destination: Destination) = apply { this.primaryDestination = destination }

    fun thenDrawRatio(ratio: Int = 1) = apply {
        this.secondaryEffect = SecondaryEffect.DrawMatchingAmount(ratio)
    }
    fun secondaryEffect(effect: SecondaryEffect) = apply { this.secondaryEffect = effect }

    override fun toJson(): String {
        val amountJson = amount?.toString() ?: "null"
        val filterTypesJson = filterTypes.joinToString(prefix = "[", postfix = "]") { "\"${it.name}\"" }
        val excludeType = excludedTypes.joinToString(prefix = "[", postfix = "]") { "\"${it.name}\"" }
        val excludedJson = excludedCards.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        val primaryDestJson = primaryDestination?.let { "\"$it\"" } ?: "null"
        val secondaryEffectJson = secondaryEffect?.toJson() ?: "null"

        return """
    {
      "text": "$text",
      "targetPlayer": "${targetPlayer.name}",
      "targetCard": "$targetCard",
      "fromZone": "$fromZone",
      "amount": $amountJson,
      "isOptional": $isOptional,
      "filterTypes": $filterTypesJson,
      "excludeTypes": $excludeType,"
      "excludedCards": $excludedJson,
      "primaryDestination": $primaryDestJson,
      "secondaryEffect": $secondaryEffectJson
    }
    """.trimIndent()
    }
}

sealed interface SecondaryEffect {
    fun toJson(): String

    data class DrawMatchingAmount(val ratio: Int = 1) : SecondaryEffect {
        override fun toJson(): String = """{"type": "DRAW_MATCHING", "ratio": $ratio}"""
    }

    data class RevealHandIfNoMatch(val value: Boolean = true) : SecondaryEffect {
        override fun toJson(): String = """{"type": "REVEAL_HAND", "value": $value}"""
    }

    object DiscardRemaining : SecondaryEffect {
        override fun toJson(): String = """{"type": "DISCARD_REMAINING"}"""

    }

    data class DrawCards(val count: Int = 1) : SecondaryEffect {
        override fun toJson(): String = """{"type": "DRAW", "count": $count}"""
    }

    data class DrawUntil(
        val targetHandSize: Int,
        val skippableType: CardType? = null,
        val skippedDestination: Destination = Destination.PlayerZone.Discard
    ) : SecondaryEffect {
        override fun toJson(): String {
            val typeJson = skippableType?.let { "\"${it.name}\"" } ?: "null"
            return """
            {
              "type": "DRAW_UNTIL",
              "targetHandSize": $targetHandSize,
              "skippableType": $typeJson,
              "skippedDestination": "$skippedDestination"
            }
            """.trimIndent()
        }
    }

    data class DiscardDownTo(val targetHandSize: Int = 3) : SecondaryEffect {
        override fun toJson(): String = """{"type": "DISCARD_DOWN_TO", "targetHandSize": $targetHandSize}"""
    }
}