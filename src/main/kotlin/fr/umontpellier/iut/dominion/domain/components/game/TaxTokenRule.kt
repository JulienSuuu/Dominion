package fr.umontpellier.iut.dominion.domain.components.game

import fr.umontpellier.iut.dominion.domain.model.core.Item
import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.components.player.isAffectedBy
import fr.umontpellier.iut.dominion.domain.components.player.updateTokenFlag
import fr.umontpellier.iut.dominion.domain.model.game.Game
import fr.umontpellier.iut.dominion.domain.model.token.Token

class TaxTokenRule(val game: Game) : ResourceInterceptor {

    override fun modify(player: Player, item: Item, amount: Int): Int {
        if (item == Item.MONEY && amount > 0 && player.isAffectedBy(Token.OnPlayer.TaxToken)) {
            player.updateTokenFlag(Token.OnPlayer.TaxToken, false)
            return amount - 1
        }
        return amount
    }
}