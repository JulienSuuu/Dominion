package fr.umontpellier.iut.dominion.Player.PlayerComponent

import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Supply.EmptySupply.game
import fr.umontpellier.iut.dominion.cards.Card

class ContrabandComponent(val self : Player) : PlayerComponent {
    override fun canBuy(card: Card): Boolean {
        return card.name !in self.game.getNamedCardsThisTurn("contraband")
    }
}