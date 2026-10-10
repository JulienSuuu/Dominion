package fr.umontpellier.iut.dominion.domain.components.player

import fr.umontpellier.iut.dominion.domain.model.card.Card



interface PlayerComponent

interface PlayerLifecycleHook : PlayerComponent {
    fun onCleanUp() {}
}

interface BuyValidator : PlayerComponent {
    fun canBuy(card: Card): Boolean
}