package fr.umontpellier.iut.dominion.cards.component

import kotlinx.coroutines.flow.StateFlow

interface ReadablePrice {
    val coinsProperty : StateFlow<Int>
    val debtProperty : StateFlow<Int>
    val potions : Int


    val coins : Int get() = coinsProperty.value
    val debt : Int get() = debtProperty.value
}