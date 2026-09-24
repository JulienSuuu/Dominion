package fr.umontpellier.iut.dominion.cards.factories.Futaba

import fr.umontpellier.iut.dominion.Annotation.Dominion_Card
import fr.umontpellier.iut.dominion.Annotation.PileType
import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.Skills.discard
import fr.umontpellier.iut.dominion.Player.Skills.draw
import fr.umontpellier.iut.dominion.Player.Skills.reveals
import fr.umontpellier.iut.dominion.cards.Bonus.Bonus
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.Events.Event
import fr.umontpellier.iut.dominion.cards.component.BiEffect
import fr.umontpellier.iut.dominion.cards.component.OnPlayComponent
import fr.umontpellier.iut.dominion.cards.component.Price
import fr.umontpellier.iut.dominion.cards.component.ScoreComponent
import fr.umontpellier.iut.dominion.cards.component.attack
import fr.umontpellier.iut.dominion.cards.component.filter
import fr.umontpellier.iut.dominion.cards.component.then
import fr.umontpellier.iut.dominion.cards.gainFromSupply
import fr.umontpellier.iut.dominion.cards.plusAssign

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

    @Dominion_Card(extension = "Futaba", pileType = PileType.VICTORY)
    fun FutabaRecycler() = Card.duration("Futaba Recycler", Price.dominion(5)).addType(CardType.ACTION, CardType.VICTORY, CardType.FUTABA)
        .setup {
            onCardTrash {
                onEffect(BiEffect.empty<Event, Card>()
                    .then {_, card -> card.getComponent<ScoreComponent>()?.incrementValue() }
                    .then { event, card -> event.player.discard(card) }
                )
                onCondition { event, player -> event.cameFrom(Destination.PlayerZone.Hand) }
            }
            mutableScore()
            onDuration { infinite() }
        }

    @Dominion_Card(extension = "Futaba")
    fun Futacar() = Card.attack("Futacar", Price.dominion(5)).addType(CardType.FUTABA)
        .setup {
            onPlay(Bonus.money(2).onPlay()
                .attack { _, opponent, _ ->
                    if(opponent.getList(Destination.PlayerZone.Draw).any{it.hasType(CardType.FUTABA)}) return@attack
                    opponent.gainFromSupply("Curse")
                }
                .end()
            )
        }

    @Dominion_Card(extension = "Futaba")
    fun Hacker() = Card.action("Hacker", Price.dominion(5)).addType(CardType.FUTABA)
        .setup {
            onPlay(
                Bonus.Action.onPlay()
                    .filter { player, _ -> player.sizeOf(Destination.PlayerZone.InPlay) == 2 }
                    .thenDo { player, card, _ ->
                        val toCopy = player.getList(Destination.PlayerZone.InPlay).first().copy()
                        val onPlay = toCopy.getComponent<OnPlayComponent>() ?: return@thenDo

                        card.removeComponent<OnPlayComponent>()
                        card.setup { onPlay(Bonus.Action.onPlay().then { player, card -> onPlay(player, card) }) }

                    }
                    .end()
            )
        }

    @Dominion_Card(extension = "Futaba")
    fun LukaMegurine() = Card.duration("Luka Megurine", Price.dominion(8)).addType(CardType.TREASURE)
        .setup {
            onPlay(Bonus.FISH.onPlay().then { getFlag("EndTurn") += true })
            onDuration {
                onEffect(BiEffect.empty<Player, Card>() then { incrementByAction(Item.MONEY){getValueOf(Item.FISH)} })
            }
        }
    @Dominion_Card(extension = "Futaba")
    fun Maidtaba() = Card.action("Maidtaba", Price.dominion(5)).addType(CardType.REACTION)
        .setup {
            simpleAction(Bonus.action(3))
            onCardPlayed {
                onEffect{owner, event -> owner.draw(2)}
                onCondition { event, player -> event.cardHasType(CardType.DURATION) && !event.isSamePlayer(player) }
            }
        }
}