package fr.umontpellier.iut.dominion.cards.factories.Alchimie

import fr.umontpellier.iut.dominion.Annotation.Dominion_Card
import fr.umontpellier.iut.dominion.Annotation.PileType
import fr.umontpellier.iut.dominion.Button
import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.PlayerComponent.preparePossession
import fr.umontpellier.iut.dominion.Player.Skills.discard
import fr.umontpellier.iut.dominion.Player.Skills.discardUntilAndDo
import fr.umontpellier.iut.dominion.Player.Skills.drawByAction
import fr.umontpellier.iut.dominion.Player.Skills.drawUntilAndDo
import fr.umontpellier.iut.dominion.Player.Skills.moveAllAndChooseTheOrder
import fr.umontpellier.iut.dominion.Player.Skills.moveList
import fr.umontpellier.iut.dominion.Player.Skills.moveTo
import fr.umontpellier.iut.dominion.Player.Skills.playCardFromList
import fr.umontpellier.iut.dominion.Player.Skills.reveals
import fr.umontpellier.iut.dominion.Player.Skills.trash
import fr.umontpellier.iut.dominion.Player.Skills.trashAndEffect
import fr.umontpellier.iut.dominion.cards.Bonus.Bonus
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.builders.filterNotNull
import fr.umontpellier.iut.dominion.cards.builders.matchAll
import fr.umontpellier.iut.dominion.cards.component.BiEffect
import fr.umontpellier.iut.dominion.cards.component.InteractionRequest
import fr.umontpellier.iut.dominion.cards.component.Price
import fr.umontpellier.iut.dominion.cards.component.attack
import fr.umontpellier.iut.dominion.cards.component.chooseCardFromHand
import fr.umontpellier.iut.dominion.cards.component.globalEffect
import fr.umontpellier.iut.dominion.cards.component.lookingAt
import fr.umontpellier.iut.dominion.cards.component.then
import fr.umontpellier.iut.dominion.cards.count
import fr.umontpellier.iut.dominion.cards.gainFromSupply
import fr.umontpellier.iut.dominion.cards.getTopCards

object AlchemyKt {
    @Dominion_Card(extension = "Alchemy")
    fun Alchemist() : Card {
        val actionDraw = Bonus.action().draw(2)
        return Card.action("Alchemist", Price.alchemy(3, 1))
            .setup {
                simpleAction(actionDraw)
                onEndBuy {
                    onEffect(BiEffect.empty<Player, Card>()
                        .lookingAt { player, _ -> player.getList(Destination.PlayerZone.Hand).any { it.hasName("Potion") }  }.filter { it }
                        .chooseWhatToDo { player, self -> InteractionRequest(
                            instruction = "$player, do you want to move your $self into your draw ?",
                            cards = listOf(self),
                            buttons = Button.yesOrNo
                        ) }
                        .branch("y" to {player, self, _ -> player.moveTo(self, Destination.PlayerZone.Draw)})
                        .end()
                    )
                }
            }
    }
    @Dominion_Card(extension = "Alchemy")
    fun Apothecarty() : Card {
        val actionDraw = Bonus.action().draw()
        return Card.action("Apothecary", Price.alchemy(2, 1))
            .setup {
                onPlay(actionDraw.onPlay()
                    .lookingAt { player, _ -> player.getTopCards(4) }
                    .thenWith { player, cards ->
                        player.moveList(
                            instruction = "$player, you may put copper and potion in your hand",
                            cards = cards,
                            filter = { card -> card.hasName("Potion") || card.hasName("Copper") },
                            to = Destination.PlayerZone.Hand,
                            canPass = true
                        )
                        player.moveAllAndChooseTheOrder(cards, Destination.TempZone.Temp, Destination.PlayerZone.Draw)
                    }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = "Alchemy")
    fun Apprentice() = Card.action("Apprentice", Price.classic(5))
        .setup {
            onPlay(Bonus.Action.onPlay()
                .then {
                    trashAndEffect(from = Destination.PlayerZone.Hand, canPass = false){
                        drawByAction { it.costValue + if(it.potion > 0 ) 2 else 0 }
                    }
                }
            )
        }
    @Dominion_Card(extension = "Alchemy")
    fun Familiar() : Card {
        val actionDraw = Bonus.action().draw()
        return Card.attack("Familiar", Price.alchemy(3, 1))
            .setup { onPlay(actionDraw.onPlay().attack { _, opponent, _ -> opponent.gainFromSupply("Curse") }.end()) }
    }


    @Dominion_Card(extension = "Alchemy")
    fun Golem() = Card.action("Golem", Price.alchemy(4, 1))
        .setup {
            onPlay{player, self ->
                player.discardUntilAndDo(
                    stopCondition = {it < 2},
                    checkCard = {it.hasType(CardType.ACTION) && !it.hasName("Golem")}
                ){ playCardFromList(it)}
            }
        }
    @Dominion_Card(extension = "Alchemy")
    fun Herbalist() : Card {
        val buyMoney = Bonus.buy().with(Item.MONEY)
        return Card.action("Herbalist", Price.classic(2))
            .setup {
                simpleAction(buyMoney)
                onEndBuy {
                    onEffect(BiEffect.empty<Player, Card>()
                        .lookingAt { player, _ -> player.getList(Destination.PlayerZone.InPlay)  }
                        .chooseCardFromList { player, _-> InteractionRequest(
                            instruction = "$player, put one treasure from play on the top of your draw",
                            cards = this,
                            canPass = true,
                            filter = {it.hasType(CardType.TREASURE)}
                        ) }
                        .thenWith { player, card -> player.moveTo(card, Destination.PlayerZone.Draw) }
                        .end()
                    )
                }
            }
    }
    @Dominion_Card(extension = "Alchemy")
    fun PhilosopherStone() = Card.treasure("Philosopher's Stone", Price.alchemy(3, 1))
        .setup {
            onPlay{ player, _ ->
                player.incrementByAction(Item.MONEY){
                    val total = getList(Destination.PlayerZone.Draw).size + getList(Destination.PlayerZone.Discard).size
                    total / 5
                }
            }
        }
    @Dominion_Card(extension = "Alchemy")
    fun Possession() = Card.action("Possession", Price.alchemy(6, 1))
        .setup {
            onPlay{ player, _ ->
                val victim = player.game.onTheLeft(player)
                victim.preparePossession(player)
            }
        }

    @Dominion_Card(extension = "Alchemy")
    fun ScryingPool() = Card.attack("Scrying Pool", Price.alchemy(2, 1))
        .setup {
            onPlay(Bonus.Action.onPlay()
                .globalEffect { you, _, opp ->
                    val effect = BiEffect.empty<Player, Player>()
                        .lookingAt { _, opp -> opp.getCardFromDeck() }.filterNotNull()
                        .thenWith { player, card -> player.reveals(card)  }
                        .chooseWhatToDo { you, opp-> InteractionRequest(
                            instruction = "you may discard this card ${if(you != opp) "from $opp" else ""}",
                            cards = listOf(this),
                            buttons = Button.yesOrNo
                        ) }
                        .branch("y" to {_, opp, drawn -> opp.discard(drawn)})
                        .end()

                    effect(you, opp)
                }.then {
                    drawUntilAndDo(filter = {it.hasType(CardType.ACTION)}){cards ->
                        cards.forEach { moveTo(it, Destination.PlayerZone.Hand) }
                    }
                }

            )
        }

    @Dominion_Card(extension = "Alchemy")
    fun Transmute() = Card.action("Transmute", Price.alchemy(potions =  1))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .chooseCardFromHand(interactionRequest = InteractionRequest("Trash a card from your hand"))
                .filter(Player::trash)
                .matchAll {
                    on {choice.hasType(CardType.ACTION)} then { player, _, _ -> player.gainFromSupply("Duchy")}
                    on {choice.hasType(CardType.VICTORY)} then { player, _, _ -> player.gainFromSupply("Gold")}
                    on {choice.hasType(CardType.TREASURE)} then { player, _, _ -> player.gainFromSupply("Transmute")}
                }
                .endParent()
            )
        }
    @Dominion_Card(extension = "Alchemy")
    fun University() : Card {
        val actions = Bonus.action(2)
        return Card.action("University", Price.alchemy(2, 1))
            .setup { onPlay(actions.onPlay()
                .then { gainFromSupply(
                    instruction = "gain an Action card costing up to 5$",
                    filter = {it isAtMost 5 && it.hasType(CardType.ACTION)},
                    dest = Destination.PlayerZone.Discard
                ) }
            ) }
    }
    @Dominion_Card(extension = "Alchemy", pileType = PileType.VICTORY)
    fun Vineyard() = Card.victory("Vineyard", Price.alchemy(potions =  1))
        .setup { score { player -> player.getList(Destination.PlayerZone.Hand).count { hasType(CardType.ACTION) } / 3  } }
}
