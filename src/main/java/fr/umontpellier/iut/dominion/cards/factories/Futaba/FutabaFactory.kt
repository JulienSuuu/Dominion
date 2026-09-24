package fr.umontpellier.iut.dominion.cards.factories.Futaba

import fr.umontpellier.iut.dominion.Annotation.Dominion_Card
import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.Skills.discard
import fr.umontpellier.iut.dominion.Player.Skills.reveals
import fr.umontpellier.iut.dominion.cards.Bonus.Bonus
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.component.BiEffect
import fr.umontpellier.iut.dominion.cards.component.Price
import fr.umontpellier.iut.dominion.cards.component.filter
import fr.umontpellier.iut.dominion.cards.component.then

object FutabaFactory {

    @Dominion_Card(extension = "Futaba")
    fun DealerLuka() = Card.action("Dealer Luka", Price.dominion(5)).addType(CardType.REACTION, CardType.FUTABA, CardType.POKER)
        .setup {
            simpleAction(Bonus.ActionAndDraw)
            onPokerHand {
                onEffect(BiEffect.empty<Player, EvaluatedPokerHand>()
                    .then {player, hand ->
                        player.reveals(hand.fiveCardHand)
                        player.incrementByAction(Item.MONEY){hand.scoringCards.size * 2}
                        player.discard(scope)
                    }
                )
                onCondition { event, _ ->  !event.isPokerHand(PokerHand.HIGH_CARD)}
            }
        }
}