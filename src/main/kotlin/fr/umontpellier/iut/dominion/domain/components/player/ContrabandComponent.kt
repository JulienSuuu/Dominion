package fr.umontpellier.iut.dominion.domain.components.player

import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.card.Card

class ContrabandComponent(val self : Player) : BuyValidator {
    override fun canBuy(card: Card): Boolean {
        return card.name !in self.game.getNamedCardsThisTurn("contraband")
    }
}