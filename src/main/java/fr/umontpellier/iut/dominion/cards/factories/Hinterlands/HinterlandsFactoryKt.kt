package fr.umontpellier.iut.dominion.cards.factories.Hinterlands

import fr.umontpellier.iut.dominion.Annotation.Dominion_Card
import fr.umontpellier.iut.dominion.Annotation.PileType
import fr.umontpellier.iut.dominion.Button
import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Flags
import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.Skills.discard
import fr.umontpellier.iut.dominion.Player.Skills.discardAndDo
import fr.umontpellier.iut.dominion.Player.Skills.discardFromHand
import fr.umontpellier.iut.dominion.Player.Skills.discardList
import fr.umontpellier.iut.dominion.Player.Skills.discardListUntilYouStop
import fr.umontpellier.iut.dominion.Player.Skills.discardTo
import fr.umontpellier.iut.dominion.Player.Skills.draw
import fr.umontpellier.iut.dominion.Player.Skills.drawByAction
import fr.umontpellier.iut.dominion.Player.Skills.moveAllAndChooseTheOrder
import fr.umontpellier.iut.dominion.Player.Skills.moveList
import fr.umontpellier.iut.dominion.Player.Skills.moveTo
import fr.umontpellier.iut.dominion.Player.Skills.playCard
import fr.umontpellier.iut.dominion.Player.Skills.reveals
import fr.umontpellier.iut.dominion.Player.Skills.trash
import fr.umontpellier.iut.dominion.Properties
import fr.umontpellier.iut.dominion.cards.Bonus.Bonus
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.Events.Event
import fr.umontpellier.iut.dominion.cards.builders.branchDecision
import fr.umontpellier.iut.dominion.cards.builders.filterListAndNotEmpty
import fr.umontpellier.iut.dominion.cards.builders.filterNotNull
import fr.umontpellier.iut.dominion.cards.builders.filterNotNullWithContext
import fr.umontpellier.iut.dominion.cards.builders.gainFromSupply
import fr.umontpellier.iut.dominion.cards.builders.increment
import fr.umontpellier.iut.dominion.cards.builders.listIsNotEmpty
import fr.umontpellier.iut.dominion.cards.builders.revealList
import fr.umontpellier.iut.dominion.cards.component.BiEffect
import fr.umontpellier.iut.dominion.cards.component.InteractionRequest
import fr.umontpellier.iut.dominion.cards.component.Price
import fr.umontpellier.iut.dominion.cards.component.attack
import fr.umontpellier.iut.dominion.cards.component.benefit
import fr.umontpellier.iut.dominion.cards.component.chooseCardFromHand
import fr.umontpellier.iut.dominion.cards.component.chooseWhatToDo
import fr.umontpellier.iut.dominion.cards.component.draw
import fr.umontpellier.iut.dominion.cards.component.filter
import fr.umontpellier.iut.dominion.cards.component.gainFromSupply
import fr.umontpellier.iut.dominion.cards.component.globalEffect
import fr.umontpellier.iut.dominion.cards.component.lookingAt
import fr.umontpellier.iut.dominion.cards.component.processHandDown
import fr.umontpellier.iut.dominion.cards.component.then
import fr.umontpellier.iut.dominion.cards.component.trashCardFromHand
import fr.umontpellier.iut.dominion.cards.count
import fr.umontpellier.iut.dominion.cards.factories.ACTION
import fr.umontpellier.iut.dominion.cards.factories.EFFECT
import fr.umontpellier.iut.dominion.cards.factories.GAIN_ACTION
import fr.umontpellier.iut.dominion.cards.factories.TRASHED_ACTION
import fr.umontpellier.iut.dominion.cards.gainFromSupply
import fr.umontpellier.iut.dominion.cards.gainMultiplyCardFromSupply
import fr.umontpellier.iut.dominion.cards.getTopCards
import fr.umontpellier.iut.dominion.cards.hasNotType
import fr.umontpellier.iut.dominion.cards.minusAssign
import fr.umontpellier.iut.dominion.cards.plusAssign
import fr.umontpellier.iut.dominion.cards.triggerEffect

object HinterlandsFactoryKt {

    @Dominion_Card(extension = "Hinterlands")
    fun Berserker() = Card.attack("Berserker", Price.hinterlands(5))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .gainFromSupply(
                    instruction = {"$player, gain a card costing less than ${pipeLineCard.costInstruction()}"},
                    filter = {it.isLessThanWithBonus(pipeLineCard)}
                )
                .processHandDown(toReach = 3, mayDiscard = true)
            )
            checkGain {
                onEffect{event, self ->
                    event.player.playCard(self)
                    event.destination = Destination.PlayerZone.InPlay
                }
                onCondition { event, player -> player.getList(Destination.PlayerZone.InPlay).any{it.hasType(CardType.ACTION)}
                        && event.goTo(Destination.PlayerZone.Discard) }
            }
        }
    @Dominion_Card(extension = "Hinterlands")
    fun BorderVillage() : Card {
        val bonus = Bonus.action(2).draw()
        return Card.action("Border Village", Price.hinterlands(6))
            .setup {
                simpleAction(bonus)
                checkGain {event, self -> event.player.gainFromSupply(
                    instruction = "${event.player}, gain a card costing less than ${self.costInstruction()}",
                    filter = {it.isLessThanWithBonus(self)},
                    dest = Destination.PlayerZone.Discard
                ) }
            }
    }
    @Dominion_Card(extension = "Hinterlands")
    fun Cartographer() : Card {
        val bonus = Bonus.action().draw()
        return Card.action("Cartographer", Price.hinterlands(5))
            .setup {
                onPlay(bonus.onPlay()
                    .lookingAt { player, _ -> player.getTopCards(4) }.listIsNotEmpty()
                    .thenWith { player, cards -> player.discardListUntilYouStop(instructionList = "$player, discard this list until you want to stop", cards) }
                    .thenWith { player, cards -> player.moveAllAndChooseTheOrder(cards, Destination.TempZone.Temp, Destination.PlayerZone.Draw) }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = "Hinterlands")
    fun Cauldron() : Card {
        val bonus = Bonus.buy().with(Item.MONEY, 2)
        return Card.treasure("Cauldron", Price.hinterlands(5)).addType(CardType.ATTACK)
            .setup {
                onPlay(bonus.onPlay() then { _, self -> self.set("NumberAction", 0) })
                afterGain {
                    onEffect{owner, _ -> owner.game.processAttack(owner, scope){it.gainFromSupply("Curse")}}
                    onCondition { event, player ->
                        if(!event.isSamePlayer(player) || !event.cardHasType(CardType.ACTION)) return@onCondition false

                        val number = scope.getValue("NumberAction").toInt() + 1
                        scope.set("NumberAction", number)
                        number == 3 }
                }
            }
    }
    @Dominion_Card(extension = "Hinterlands")
    fun Crossroads() = Card.action("Crossroads", Price.hinterlands(2))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .lookingAt { player, _ -> player.getList(Destination.PlayerZone.Hand) }
                .revealList()
                .thenWith { player, cards -> player.drawByAction { cards.count { hasType(CardType.VICTORY) } } }
                .filter { player, _ -> !player.isFlagSet(Flags.playedCrossroads) }
                .increment(Item.ACTION, 3)
                .end()
            )
        }
    @Dominion_Card(extension = "Hinterlands")
    fun Develop() = Card.action("Develop", Price.hinterlands(3))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .trashCardFromHand(canPass = true)
                .gainFromSupply(
                    instruction = {"$player, gain a card costing exactly ${pipeLineCard.costInstruction(1)}"},
                    filter = {it.isEqualWithBonus(pipeLineCard, 1)} ,
                    destination = Destination.PlayerZone.Draw)
                .gainFromSupply(
                    instruction = {"$player, gain a card costing exactly ${pipeLineCard.costInstruction(-1)}"},
                    filter = {it.isEqualWithBonus(pipeLineCard, -1)},
                    destination = Destination.PlayerZone.Draw
                )
                .endParent()
            )
        }
    @Dominion_Card(extension = "Hinterlands", pileType = PileType.VICTORY)
    fun Farmland() = Card.victory("Farmland", Price.hinterlands(6))
        .setup {
            score { 2 }
            checkGain(BiEffect.empty<Event, Card>()
                .trashCardFromHand(canPass = true)
                .gainFromSupply(
                    instruction = {"$player, gain a card costing exactly ${pipeLineCard.costInstruction(2)} and not a Farmland"},
                    filter = {it.isEqualWithBonus(pipeLineCard, 2) && !it.hasName("Farmland")},
                )
                .endParent()
            )
        }
    @Dominion_Card(extension = "Hinterlands")
    fun FoolsGold(): Card {
        val fullMoney = Bonus.money(4)
        return Card.treasure("Fool's Gold", Price.hinterlands(2)).addType(CardType.REACTION)
            .setup {
                onPlay{player, self ->
                    player.triggerEffect(EFFECT, self, if(player.isFlagSet(Flags.playedFoolsGold)) fullMoney else Bonus.Money)
                }
                afterGain {
                    onEffect(BiEffect.empty<Player, Event>()
                        .chooseWhatToDo { owner, _ -> InteractionRequest(
                            instruction = "$owner, do you want to trash $scope for a gold ?",
                            cards = listOf(scope),
                            buttons = Button.yesOrNo
                        )  }
                        .branch("y" to {player, _, _ ->
                            if(player.trash(scope)) player.gainFromSupply("Gold", Destination.PlayerZone.Draw)
                        })
                        .end()
                    )
                    onCondition { event, player -> !event.isSamePlayer(player) && event cardHasName "Province" }
                }
            }
    }
    @Dominion_Card(extension = "Hinterlands")
    fun GuardDog() : Card {
        val draw = Bonus.draw(2)
        return Card.action("Guard Dog", Price.hinterlands(3)).addType(CardType.REACTION)
            .setup {
                onPlay(draw.onPlay()
                    .filter { player, _ ->  player.getList(Destination.PlayerZone.Hand).size <= 5 }
                    .so { player, self -> player.triggerEffect(ACTION, self, draw)  }
                    .end()
                )
                onCardPlayed {
                    onEffect(BiEffect.empty<Player, Event>()
                        .chooseWhatToDo { owner, _ -> InteractionRequest(
                            instruction = "$owner, do you want to play $scope ?",
                            cards = listOf(scope),
                            buttons = Button.yesOrNo
                        ) }
                        .branch("y" to {owner, _, _ -> owner.playCard(scope)})
                        .end()
                    )
                    onCondition { event, player -> !event.isSamePlayer(player) && event cardHasType CardType.ATTACK }
                }
            }
    }
    @Dominion_Card(extension = "Hinterlands")
    fun Haggler() : Card {
        val money = Bonus.money(2)
        return Card.action("Haggler", Price.hinterlands(5))
            .setup {
                simpleAction(money)
                onBuy(BiEffect.empty<Player, Card>()
                    .gainFromSupply(
                        instruction = {"$player, gain a card costing less than ${pipeLineCard.costInstruction()} and not a Victory card"},
                        filter = {it.isLessThanWithBonus(pipeLineCard) && it hasNotType CardType.VICTORY }
                    )
                )
            }
    }
    @Dominion_Card(extension = "Hinterlands")
    fun Highway() : Card {
        val bonus = Bonus.action().draw()
        return Card.action("Highway", Price.hinterlands(5))
            .setup { onPlay(bonus.onPlay() then { game.stat.reduction+=1 }) }
    }
    @Dominion_Card(extension = "Hinterlands")
    fun Inn(): Card{
        val bonus = Bonus.action(2).draw(2)
        return Card.action("Inn", Price.hinterlands(5))
            .setup {
                onPlay(bonus.onPlay() then { discardFromHand(2) })
                checkGain(BiEffect.empty<Event, Card>()
                    .lookingAt { event, _ -> event.player.getList(Destination.PlayerZone.Discard)  }
                    .filterListAndNotEmpty { it.hasType(CardType.ACTION) }
                    .thenWith { event, cards ->
                        event.player.moveList(
                            instruction = "${event.player}, you may put any Action card from your discard in your draw",
                            cards = cards,
                            to = Destination.PlayerZone.Draw,
                            canPass = true,
                            action = {chosen -> if(event.card == chosen) event.destination = Destination.PlayerZone.Draw else moveTo(chosen, Destination.PlayerZone.Draw)}
                        ){moved -> reveals(moved); shuffling(Destination.PlayerZone.Draw)}
                    }
                    .end()

                )
            }
    }
    @Dominion_Card(extension = "Hinterlands")
    fun JackOfAllTrades() = Card.action("Jack of All Trades", Price.hinterlands(4))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .then { gainFromSupply("Silver") }
                .lookingAt { player, _ -> player.getCardFromDeck()  }.filterNotNull()
                .chooseWhatToDo { player, _ -> InteractionRequest(
                    instruction = "$player, Do you want to discard $this",
                    cards = listOf(this),
                    buttons = Button.yesOrNo
                )}
                .branch("y" to {player, _, card -> player.discard(card) }).end()
                .then{ drawByAction {
                    val size = getList(Destination.PlayerZone.Hand).size
                    if(size < 5) 5-size else 0
                }}
                .trashCardFromHand(canPass = true, filter = {it hasNotType CardType.TREASURE}, extraInstruction = "non treasure")
                .endParent()
            )
        }
    @Dominion_Card(extension = "Hinterlands")
    fun Margrave() : Card {
        val bonus = Bonus.buy().draw(3)
        return Card.attack("Margrave", Price.hinterlands(5))
            .setup {
                onPlay(bonus.onPlay()
                    .attack { _, opponent, _ ->
                        opponent.draw()
                        opponent.discardTo(3)
                    }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = "Hinterlands")
    fun Nomads() : Card {
        val buyMoney = Bonus.buy().with(Item.MONEY, 2)
        val gainOrTrash = Bonus.money(2)
        return Card.action("Nomads", Price.hinterlands(4))
            .setup {
                simpleAction(buyMoney)
                checkGain { onEffect{event, self -> event.player.triggerEffect(GAIN_ACTION, self, gainOrTrash)} }
                checkItselfTrash { onEffect{event, self -> event.player.triggerEffect(TRASHED_ACTION, self, gainOrTrash)} }
            }
    }
    @Dominion_Card(extension = "Hinterlands")
    fun Oasis() : Card {
        val bonus = Bonus.action().with(Item.BUY).draw()
        return Card.action("Oasis", Price.hinterlands(3))
            .setup { onPlay(bonus.onPlay().then { discardFromHand() }) }
    }

    @Dominion_Card(extension = "Hinterlands")
    fun Scheme() : Card {
        val bonus = Bonus.action().draw()
        return Card.action("Scheme", Price.hinterlands(3))
            .setup {
                onPlay(bonus.onPlay()
                    .lookingAt { player, _ -> player.getProperties(Properties.Scheme_Action)  }
                    .thenWith { _, flow -> flow += 1 }
                    .filter { it.value == 1 }
                    .thenDo { player, _, flow ->
                        player.addDiscardHook(BiEffect.empty<Event, Unit>()
                            .lookingAt { event, _ -> event.card  }
                            .filterNotNullWithContext { event, _, card ->
                                event.initialCameFrom(Destination.PlayerZone.InPlay)
                                        && flow.value > 0
                                        && card.hasType(CardType.ACTION)
                            }
                            .chooseWhatToDo { event, _ -> InteractionRequest(
                                instruction = "${event.player}, Do you want to put $this in your draw ?",
                                cards = listOf(this),
                                buttons = Button.yesOrNo
                            )  }
                            .branch("y" to {event, _, _ ->
                                event.destination = Destination.PlayerZone.Draw
                                flow -= 1
                            })
                            .end())
                    }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = "Hinterlands")
    fun Souk() = Card.action("Souk", Price.hinterlands(5))
        .setup {
            onPlay{player, self ->
                val money = maxOf(0, 7 - player.getList(Destination.PlayerZone.Hand).size)
                player.triggerEffect(EFFECT, self, Bonus.buy().with(Item.MONEY, money))
            }
            checkGain{event, _ -> event.player.trash(2)}
        }
    @Dominion_Card(extension = "Hinterlands")
    fun SpiceMerchant() = Card.action("Spice Merchant", Price.hinterlands(3))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .trashCardFromHand(canPass = true)
                .thenChooseWhatToDo { player, self, _, _ -> InteractionRequest(
                    instruction = "$player, Choose : 2 cards & 1 action or 2$ & 1 buy",
                    cards = listOf(self),
                    buttons = listOf(Button("+2 cards & +1 action", "c1"), Button("+2$ & +1 buy","c2"))
                ) }
                .branchDecision {
                    on {"c1" == extraData} then {player, _, _ -> player.draw(2); player.increment(Item.ACTION)}
                    on {"c2" == extraData} then {player, _, _ -> player.increment(Item.MONEY, 2); player.increment(Item.BUY)}
                }
                .endParent()
            )
        }
    @Dominion_Card(extension = "Hinterlands")
    fun Stables() : Card {
        val bonus = Bonus.action().draw(3)
        return Card.action("Stables", Price.hinterlands(5))
            .setup {
                onPlay(BiEffect.empty<Player, Card>()
                    .chooseCardFromHand { player, _ -> InteractionRequest(
                        instruction = "$player, you may discard a treasure from your hand",
                        filter = {it.hasType(CardType.TREASURE)},
                        canPass = true
                    ) }.filter(Player::discard)
                    .thenDo{player, card -> player.triggerEffect(EFFECT, card, bonus) }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = "Hinterlands")
    fun Trader() = Card.action("Trader", Price.hinterlands(5)).addType(CardType.REACTION)
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .trashCardFromHand { player, _ -> InteractionRequest(
                    instruction = "$player, you may trash a card from you hand to gain Silver(s)",
                    canPass = true
                )  }
                .thenWith { player, card -> player.gainMultiplyCardFromSupply("Silver", Destination.PlayerZone.Discard, numberOfCards = card.costValue) }
                .end()
            )
            onGain {
                onEffect(BiEffect.empty<Player, Event>()
                    .lookingAt { _, event -> event.card }.filterNotNull()
                    .chooseWhatToDo { player, _ -> InteractionRequest(
                        instruction = "$player, do you want to transform $this into silver ?",
                        cards = listOf(this),
                        buttons = Button.yesOrNo
                    ) }
                    .branch(
                        "y" to {player, event, _ ->
                            val c = player.getCardFromSupply("Silver") ?: return@to
                            event.updateCard(c)
                            event.destination = Destination.PlayerZone.Discard
                        }
                    )
                    .end()
                )
                onCondition { event, player -> event.isSamePlayer(player)
                        && (event.initialCameFrom(Destination.Supply) || event.initialCameFrom(Destination.Trash))
                        && event.notMoved }
            }
        }
    @Dominion_Card(extension = "Hinterlands")
    fun Trail() : Card {
        val bonus = Bonus.action().draw()
        return Card.action("Trail", Price.hinterlands(4)).addType(CardType.REACTION)
            .setup {
                simpleAction(bonus)
                checkGain{event, self -> runTrail(event, self )}
                checkItselfTrash{event, self -> runTrail(event, self )}
                checkItselfDiscard{
                    onEffect{event, self -> runTrail(event, self )}
                    onCondition { event, _ ->  event.isActionDiscard }
                }
            }
    }
    @Dominion_Card(extension = "Hinterlands", pileType = PileType.VICTORY)
    fun Tunnel() = Card.victory("Tunnel", Price.hinterlands(3)).addType(CardType.REACTION)
        .setup {
            score { 2 }
            checkItselfDiscard {
                onEffect(BiEffect.empty<Event, Card>()
                    .chooseWhatToDo { event, card -> InteractionRequest(
                        instruction = "${event.player}, do you want to reveal $card for a gold ?",
                        cards = listOf(card),
                        buttons = Button.yesOrNo
                    ) }
                    .branch(
                        "y" to {event, self , _ ->
                            val p = event.player
                            p.reveals(self)
                            p.gainFromSupply("Gold")
                        }
                    )
                    .end()
                )
                onCondition { event, _ -> event.isActionDiscard}
            }
        }
    @Dominion_Card(extension = "Hinterlands")
    fun Weaver() = Card.action("Weaver", Price.hinterlands(4)).addType(CardType.REACTION)
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .chooseWhatToDo { player, card -> InteractionRequest(
                    instruction = "$player, Choose : 2 silvers or a card costing up to 4",
                    cards = listOf(card),
                    buttons = listOf(Button("2 silvers", "s"), Button("gain a card", "c"))
                ) }
                .branch(
                    "s" to {player, _ , _ -> player.gainMultiplyCardFromSupply("Silver", Destination.PlayerZone.Discard, 2)},
                    "c" to {player, _, _ -> player.gainFromSupply(
                        instruction = "$player, gain a card costing up to 4$",
                        filter = {it isAtMost 4},
                        dest = Destination.PlayerZone.Discard
                    )}
                )
                .end()
            )
            checkItselfDiscard {
                onEffect(BiEffect.empty<Event, Card>()
                    .chooseWhatToDo { event, card -> InteractionRequest(
                        instruction = "${event.player}, do you want to play $card ?",
                        cards = listOf(card),
                        buttons = Button.yesOrNo
                    ) }
                    .branch("y" to {event, self , _ -> event.player.playCard(self); event.destination = Destination.PlayerZone.InPlay })
                    .end()
                )
                onCondition { event, _ -> event.isActionDiscard }
            }
        }
    @Dominion_Card(extension = "Hinterlands")
    fun Wheelwright() : Card {
        val bonus = Bonus.action().draw()
        return Card.action("Wheelwright", Price.hinterlands(5))
            .setup {
                onPlay(bonus.onPlay()
                    .chooseCardFromHand { player, _ -> InteractionRequest(
                        instruction = "$player, you may discard a card from you hand",
                        canPass = true
                    ) }
                    .filter(Player::discard)
                    .gainFromSupply(
                        instruction = {"$player, gain an action card costing up to ${pipeLineCard.costInstruction()}"},
                        filter = {it.hasType(CardType.ACTION) && it.isAtMostWithBonus(pipeLineCard)},
                        destination = Destination.PlayerZone.Discard
                    )
                    .endParent()
                )
            }
    }
    @Dominion_Card(extension = "Hinterlands")
    fun WitchHut() : Card {
        val draw = Bonus.draw(4)
        return Card.attack("Witch's Hut", Price.hinterlands(5))
            .setup {
                onPlay(draw.onPlay()
                    .then {player, self ->  player.discardAndDo(
                        from = Destination.PlayerZone.Hand,
                        number = 2,
                        action = { reveals(it); discard(it) && it.hasType(CardType.ACTION) },
                        nextAction = {n -> if(n == 2) game.processAttack(this, self) { it.gainFromSupply("Curse") } },
                    ) }
                )
            }
    }
    @Dominion_Card(extension = "Hinterlands")
    fun Cache() : Card {
        val money = Bonus.money(3)
        return Card.treasure("Cache", Price.hinterlands(5))
            .setup {
                simpleAction(money)
                checkGain{event, _ -> event.player.gainMultiplyCardFromSupply("Copper", Destination.PlayerZone.Discard, 2) }
            }
    }
    @Dominion_Card(extension = "Hinterlands")
    fun Duchess() : Card {
        val money = Bonus.money(2)
        return Card.action("Duchess", Price.hinterlands(2))
            .setup {
                onPlay(money.onPlay()
                    .globalEffect { _, _, player ->
                        val effect = BiEffect.empty<Player, Unit>()
                            .lookingAt { player, _ -> player.getCardFromDeck() }.filterNotNull()
                            .chooseWhatToDo { player, _ -> InteractionRequest(
                                instruction = "$player, do you want to discard $this ?",
                                cards = listOf(this),
                                buttons = Button.yesOrNo
                            )  }
                            .branch("y" to {player, _, card -> player.discard(card) })
                            .end()

                        effect(player, Unit)
                    }
                )
            }
    }
    @Dominion_Card(extension = "Hinterlands")
    fun Embassy() : Card {
        val draw = Bonus.draw(5)
        return Card.action("Embassy", Price.hinterlands(5))
            .setup {
                onPlay(draw.onPlay() then { discardFromHand(3) })
                checkGain(BiEffect.empty<Event, Card>().benefit { it.gainFromSupply("Silver") })
            }
    }
    @Dominion_Card(extension = "Hinterlands")
    fun IllGottenGains() = Card.treasure("Ill Gotten Gains", Price.hinterlands(5))
        .setup {
            onPlay(Bonus.Money.onPlay()
                .chooseWhatToDo { player, card -> InteractionRequest(
                    instruction = "$player, do you want to gain a copper to your hand ?",
                    cards = listOf(card),
                    buttons = Button.yesOrNo
                ) }
                .branch("y" to {player, _, _ -> player.gainFromSupply("Copper", Destination.PlayerZone.Hand)})
                .end()
            )
            checkGain(BiEffect.empty<Event, Card>().benefit { it.gainFromSupply("Curse") })
        }
    @Dominion_Card(extension = "Hinterlands")
    fun Mandarin() : Card {
        val money = Bonus.money(3)
        return Card.action("Mandarin", Price.hinterlands(5))
            .setup {
                onPlay(money.onPlay()
                    .chooseCardFromHand { player, _ -> InteractionRequest(
                        instruction = "$player, put a card from your hand into your draw"
                    ) }
                    .thenWith { player, card -> player.moveTo(card, Destination.PlayerZone.Draw) }
                    .end()
                )
                checkGain(BiEffect.empty<Event, Card>()
                    .lookingAt { event, _ -> event.player.getList(Destination.PlayerZone.InPlay) }
                    .filterListAndNotEmpty { it.hasType(CardType.TREASURE) }
                    .thenWith { event, cards -> event.player.moveAllAndChooseTheOrder(cards, Destination.PlayerZone.InPlay, Destination.PlayerZone.Draw) }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = "Hinterlands")
    fun NomadCamp() : Card {
        val bonus = Bonus.buy().with(Item.MONEY, 2)
        return Card.action("Nomad Camp", Price.hinterlands(4))
            .setup {
                simpleAction(bonus)
                checkGain{event, _ -> event.destination = Destination.PlayerZone.Draw}
            }
    }
    @Dominion_Card(extension = "Hinterlands")
    fun Oracle() = Card.action("Oracle", Price.hinterlands(3))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .draw(2)
                .globalEffect { _, _, player ->
                    val effect = BiEffect.empty<Player, Unit>()
                        .lookingAt { player, _ -> player.getTopCards(2) }.listIsNotEmpty()
                        .revealList()
                        .chooseWhatToDo { player, _ -> InteractionRequest(
                            instruction = "$player, discard $this or let them onto your draw",
                            cards = this,
                            buttons = Button.DeckOrDiscard
                        ) }
                        .branch("discard" to {player, _, cards -> player.discardList(cards)})
                        .end()

                    effect(player, Unit)
                }
            )
        }
    @Dominion_Card(extension = "Hinterlands", pileType = PileType.VICTORY)
    fun SilkRoad() = Card.victory("Silk Road", Price.hinterlands(4))
        .setup { score { it.allOwnedCards.count { hasType(CardType.VICTORY) } / 4 } }
}