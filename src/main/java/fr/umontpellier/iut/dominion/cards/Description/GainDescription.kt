package fr.umontpellier.iut.dominion.cards.Description

import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.testTypes

class GainDescription : DominionDescription {
    override var text: String = ""
        private set

    var targetCard: String? = null
    var allowedTypes: MutableList<CardType> = mutableListOf()
    var excludedTypes: MutableList<CardType> = mutableListOf()
    var amount: Int = 1

    var maxCostCoins: Int? = null
    var maxCostPotions: Int? = null
    var isRelativeCost: Boolean = false

    var fromZone: Destination = Destination.Supply
    var destination: Destination.PlayerZone = Destination.PlayerZone.Discard

    var optional : Boolean = false


    fun text(rawText: String) = apply { this.text = rawText }

    fun card(name: String) = apply { this.targetCard = name }
    fun count(n: Int) = apply { this.amount = n }

    fun upToCost(coins: Int, potions: Int = 0) = apply {
        this.maxCostCoins = coins
        if (potions > 0) this.maxCostPotions = potions
    }

    fun applyFilter(costCheck: (card: Card, maxCost: Int) -> Boolean): (Card) -> Boolean = { card ->
        val matchesTargetCard = targetCard == null || card.name == targetCard
        val matchesTypes = card.testTypes {
            any(allowedTypes)
            none(excludedTypes)
        }
        val maxCost = maxCostCoins ?: card.costValue

        matchesTargetCard && matchesTypes && costCheck(card, maxCost)
    }

    fun applyFilter(): (Card) -> Boolean = applyFilter { card, maxCost ->
        card.isAtMost(maxCost)
    }

    fun applyFilter(trashed: Card): (Card) -> Boolean = applyFilter { card, maxCost ->
        card.isAtMostWithBonus(trashed, maxCost)
    }

    fun relativeCost(extraCoins: Int) = apply {
        this.maxCostCoins = extraCoins
        this.isRelativeCost = true
    }

    fun ofType(vararg types: CardType) = apply {
        this.allowedTypes.addAll(types)
    }

    fun from(zone: Destination) = apply { this.fromZone = zone }
    fun to(zone: Destination.PlayerZone) = apply { this.destination = zone }

    override fun toJson() = """
        {
            "text": "$text",
            "targetCard": ${targetCard?.let { "\"$it\"" } ?: "null"},
            "amount": $amount,
            "maxCost": $maxCostCoins,
            "maxCostPotions": $maxCostPotions,
            "allowedTypes": [${allowedTypes.joinToString { "\"${it.name}\"" }}],
            "excludedTypes": [${excludedTypes.joinToString { "\"${it.name}\"" }}],
            "isRelativeCost": $isRelativeCost,
            "fromZone": "$fromZone",
            "destination": "$destination"
        }
    """.trimIndent()
}