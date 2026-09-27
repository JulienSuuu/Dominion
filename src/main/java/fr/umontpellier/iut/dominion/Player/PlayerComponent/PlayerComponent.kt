package fr.umontpellier.iut.dominion.Player.PlayerComponent

import fr.umontpellier.iut.dominion.cards.Card

interface PlayerComponent {
    fun canBuy(card: Card): Boolean = true
    fun onTurnStart(){}
    fun onCleanUp(){}
}