package fr.umontpellier.iut.dominion.cards.component

import kotlinx.coroutines.flow.MutableStateFlow

data class Price(
    override val coinsProperty: MutableStateFlow<Int>,
    override val potions: Int,
    override val debtProperty: MutableStateFlow<Int>
) : ReadablePrice {

    fun toJson() = """{"cost": $coins, "potion": $potions, "debt": $debt}"""

    companion object {

        fun night(coins: Int) : Price = Price(
            coinsProperty = MutableStateFlow(coins),
            potions = 0,
            debtProperty = MutableStateFlow(0)
        )

        fun classic(coins: Int): Price = Price(
            coinsProperty = MutableStateFlow(coins),
            potions = 0,
            debtProperty = MutableStateFlow(0)
        )

        fun alchemy(coins: Int = 0, potions: Int): Price = Price(
            coinsProperty = MutableStateFlow(coins),
            potions = potions,
            debtProperty = MutableStateFlow(0)
        )

        fun empires(coins: Int = 0, debt: Int = 0): Price = Price(
            coinsProperty = MutableStateFlow(coins),
            potions = 0,
            debtProperty = MutableStateFlow(debt)
        )

        fun seaside(coins: Int) = classic(coins)
        fun dominion(coins: Int) = classic(coins)
        fun intrigue(coins: Int) = classic(coins)
        fun prosperity(coins: Int) = classic(coins)
        fun cornucopia(coins: Int) = classic(coins)
        fun hinterlands(coins: Int) = classic(coins)
        fun darkAges(coins: Int) = classic(coins)
        fun adventure(coins: Int) = classic(coins)
    }
}