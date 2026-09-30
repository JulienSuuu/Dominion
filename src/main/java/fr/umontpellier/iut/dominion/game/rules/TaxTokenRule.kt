package fr.umontpellier.iut.dominion.game.rules

import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.PlayerComponent.isAffectedBy
import fr.umontpellier.iut.dominion.Player.PlayerComponent.updateTokenFlag
import fr.umontpellier.iut.dominion.Player.Tokens.Token
import fr.umontpellier.iut.dominion.game.Game

class TaxTokenRule(val game: Game) : ResourceInterceptor {

    override fun modify(player: Player, item: Item, amount: Int): Int {
        if (item == Item.MONEY && amount > 0 && player.isAffectedBy(Token.OnPlayer.TaxToken)) {
            player.updateTokenFlag(Token.OnPlayer.TaxToken, false)
            return amount - 1
        }
        return amount
    }
}