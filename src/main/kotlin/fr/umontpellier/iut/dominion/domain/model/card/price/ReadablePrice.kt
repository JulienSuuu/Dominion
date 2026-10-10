package fr.umontpellier.iut.dominion.domain.model.card.price

import kotlinx.coroutines.flow.StateFlow

interface ReadablePrice {
    val coinsProperty : StateFlow<Int>
    val debtProperty : StateFlow<Int>
    val potions : Int


    val coins : Int get() = coinsProperty.value
    val debt : Int get() = debtProperty.value
}