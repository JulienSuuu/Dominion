package fr.umontpellier.iut.dominion.cards.factories.Empires

import fr.umontpellier.iut.dominion.Annotation.Dominion_Card
import fr.umontpellier.iut.dominion.Annotation.PileType
import fr.umontpellier.iut.dominion.Button
import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.game.Game
import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.PlayerComponent.addCardEffect
import fr.umontpellier.iut.dominion.Player.PlayerComponent.cardGainedCurrentTurn
import fr.umontpellier.iut.dominion.Player.PlayerComponent.removeCardEffect
import fr.umontpellier.iut.dominion.Player.PlayerComponent.underPossession
import fr.umontpellier.iut.dominion.Player.Skills.choose
import fr.umontpellier.iut.dominion.Player.Skills.discard
import fr.umontpellier.iut.dominion.Player.Skills.discardFromHand
import fr.umontpellier.iut.dominion.Player.Skills.discardList
import fr.umontpellier.iut.dominion.Player.Skills.discardUntilYouStopAndDo
import fr.umontpellier.iut.dominion.Player.Skills.draw
import fr.umontpellier.iut.dominion.Player.Skills.drawByAction
import fr.umontpellier.iut.dominion.Player.Skills.drawToHand
import fr.umontpellier.iut.dominion.Player.Skills.move
import fr.umontpellier.iut.dominion.Player.Skills.moveAll
import fr.umontpellier.iut.dominion.Player.Skills.moveTo
import fr.umontpellier.iut.dominion.Player.Skills.playCard
import fr.umontpellier.iut.dominion.Player.Skills.reveals
import fr.umontpellier.iut.dominion.Player.Skills.trash
import fr.umontpellier.iut.dominion.Player.Skills.trashAndEffect
import fr.umontpellier.iut.dominion.Player.Skills.trashUntilYouStopAndDo
import fr.umontpellier.iut.dominion.Supply.EmptySupply
import fr.umontpellier.iut.dominion.Supply.ReadableSupplyPile
import fr.umontpellier.iut.dominion.game.ShadowKey
import fr.umontpellier.iut.dominion.Supply.SupplyPile
import fr.umontpellier.iut.dominion.cards.Bonus.Bonus
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.Events.Event
import fr.umontpellier.iut.dominion.cards.Events.GainType
import fr.umontpellier.iut.dominion.cards.Events.OnGainEvent
import fr.umontpellier.iut.dominion.cards.builders.branchDecision
import fr.umontpellier.iut.dominion.cards.builders.filterListAndNotEmpty
import fr.umontpellier.iut.dominion.cards.builders.filterNotNull
import fr.umontpellier.iut.dominion.cards.builders.filterTupleNotNull
import fr.umontpellier.iut.dominion.cards.builders.forEachCard
import fr.umontpellier.iut.dominion.cards.builders.gainFromSupply
import fr.umontpellier.iut.dominion.cards.builders.listIsNotEmpty
import fr.umontpellier.iut.dominion.cards.builders.matchAll
import fr.umontpellier.iut.dominion.cards.builders.processHandDown
import fr.umontpellier.iut.dominion.cards.builders.thenIfFail
import fr.umontpellier.iut.dominion.cards.builders.thenWithPipe
import fr.umontpellier.iut.dominion.cards.component.BiEffect
import fr.umontpellier.iut.dominion.cards.component.InteractionRequest
import fr.umontpellier.iut.dominion.cards.component.Price
import fr.umontpellier.iut.dominion.cards.component.RaceCompetitors
import fr.umontpellier.iut.dominion.cards.component.attackOthers
import fr.umontpellier.iut.dominion.cards.component.chooseCardFromHand
import fr.umontpellier.iut.dominion.cards.component.chooseCardFromList
import fr.umontpellier.iut.dominion.cards.component.chooseCardFromSupply
import fr.umontpellier.iut.dominion.cards.component.chooseWhatToDo
import fr.umontpellier.iut.dominion.cards.component.gainFromSupply
import fr.umontpellier.iut.dominion.cards.component.gainFromSupplyAndWith
import fr.umontpellier.iut.dominion.cards.component.gainSpecificCardFromSupply
import fr.umontpellier.iut.dominion.cards.component.gainSpecificCardFromSupplyAndDo
import fr.umontpellier.iut.dominion.cards.component.increment
import fr.umontpellier.iut.dominion.cards.component.lookingAt
import fr.umontpellier.iut.dominion.cards.component.processHandDown
import fr.umontpellier.iut.dominion.cards.component.reveal
import fr.umontpellier.iut.dominion.cards.component.then
import fr.umontpellier.iut.dominion.cards.component.trashCardFromHand
import fr.umontpellier.iut.dominion.cards.count
import fr.umontpellier.iut.dominion.cards.displayCoin
import fr.umontpellier.iut.dominion.cards.factories.EFFECT
import fr.umontpellier.iut.dominion.cards.factories.createMixedSupplyPile
import fr.umontpellier.iut.dominion.cards.factories.follow
import fr.umontpellier.iut.dominion.cards.gainFromSupply
import fr.umontpellier.iut.dominion.cards.gainMultiplyCardFromSupply
import fr.umontpellier.iut.dominion.cards.getTopCards
import fr.umontpellier.iut.dominion.cards.hasNotType
import fr.umontpellier.iut.dominion.cards.plusAssign
import fr.umontpellier.iut.dominion.cards.testTypes
import fr.umontpellier.iut.dominion.cards.triggerEffect
import fr.umontpellier.iut.dominion.game.rules.registerEnchantress
import fr.umontpellier.iut.dominion.game.rules.unregisterEnchantress
import kotlinx.coroutines.flow.update
import org.aspectj.lang.annotation.After
import kotlin.collections.emptyList

object EmpiresFactoryKt {

    @Dominion_Card(extension = "Empires")
    fun Archive() = Card.action("Archive", Price.empires(5)).addType(CardType.DURATION)
        .setup {
            onPlay(Bonus.Action.onPlay()
                .lookingAt { player, _ -> player.getTopCards(3) }.listIsNotEmpty()
                .chooseCardFromList { player, _ -> InteractionRequest(
                    instruction = "$player, move one to your hand, other will go to your aside zone",
                    cards = this
                ) }
                .thenDo { player, self, list, card ->
                    player.moveTo(card, Destination.PlayerZone.Hand)
                    list.remove(card)
                    player.moveAll(list, Destination.PlayerZone.Aside)
                    val playCounter = self.getValue("playCounter").toInt() + 1
                    self.set("playCounter", playCounter)
                    player.addListToShadowZone(ShadowKey.get(self.id, playCounter), list)
                }
                .end()
            )
            onDuration {
                onEffect(BiEffect.empty<Player, Card>()
                    .lookingAt { player, card ->
                        val activeKeys = player.getAllKeysFor(card)

                        if (activeKeys.isNotEmpty()) {
                            val callIndex = (card.getValue("durationCallIndex").toInt() % activeKeys.size) + 1
                            card.set("durationCallIndex", callIndex)

                            val targetKey = activeKeys.getOrNull(callIndex - 1)
                            if (targetKey != null) {
                                card.set("keyShadowZone", targetKey.activationIndex)
                                val list = player.game.shadowList(targetKey)
                                list
                            } else emptyList()
                        } else emptyList()
                    }
                    .listIsNotEmpty()
                    .chooseCardFromList { player, _ -> InteractionRequest(
                        instruction = "$player, move one to your hand",
                        cards = this
                    ) }
                    .thenDo { player, self, list, card ->
                        player.moveTo(card, Destination.PlayerZone.Hand)
                        val currentActivation = self.getValue("keyShadowZone").toInt()
                        val key = ShadowKey.get(self.id, currentActivation)
                        player.updateShadowZone(key, list - card)
                    }
                    .end()
                )
                withTime(2)
                shouldBeDiscardWhen { player, card -> player.getAllListFromShadowZoneOf(card).isEmpty() }
            }
        }

    @Dominion_Card(extension = "Empires")
    fun Capital() : Card {
        val bonusPlay = Bonus.buy().with(Item.MONEY, 6)
        val tax = Bonus.debt(6)
        
        return Card.treasure("Capital", Price.empires(5))
            .setup { 
                simpleAction(bonusPlay)
                checkItselfDiscard {
                    onEffect(tax.onDiscard())
                    onCondition { event, _ -> event.initialCameFrom(Destination.PlayerZone.InPlay)  }
                }
            }
    }

    @Dominion_Card(extension = "Empires")
    fun ChariotRace() = Card.action("ChariotRace", Price.empires(3))
        .setup {
            onPlay(Bonus.Action.onPlay()
                .lookingAt { player, _ ->
                    val left = player.onTheLeft()
                    RaceCompetitors(player.drawToHand(), left, left.getCardFromDeck())
                }
                .thenWith { player, competitors ->  player.reveals(competitors.myCard) }
                .filterTupleNotNull()
                .branchDecision {
                    on {data.opponentCard.isLessThanWithBonus(data.myCard)} then {player, _, _ ->
                        player.increment(Item.MONEY)
                        player.increment(Item.VICTORY_TOKEN)
                    }
                }
                .end()
            )
        }
    @Dominion_Card(extension = "Empires")
    fun Charm() : Card {
        val bonus = Bonus.buy().with(Item.MONEY, 2)
        return Card.treasure("Charm", Price.empires(5))
            .setup {
                onPlay(bonus.onPlay()
                    .chooseWhatToDo { player, card -> InteractionRequest(
                        instruction = "$player, Choose : +1 buy and +${displayCoin(2)} or a side effect on gain",
                        cards = listOf(card),
                        buttons = listOf(Button("+1 buy & ${displayCoin(2)}", "bonus"), Button("an effect on gain", "effect"))
                    )}
                    .branch(
                        "bonus" to {player, self, _ -> player.triggerEffect(EFFECT, self, bonus)},
                        "effect" to {player, self, _ -> player.addCardEffect(self)}
                    )
                    .end()
                )
                onSideEffectGain(GainType.AFTER) {
                    onEffect(BiEffect.empty<Player, Event>()
                        .lookingAt { _, event -> event.card }.filterNotNull()
                        .thenWith { player, card ->
                            player.gainFromSupply(
                                instruction = "$player, gain a different card costing exactly ${card.costInstruction()}",
                                filter = {!it.hasSameNameAs(card) && it.isEqualWithBonus(card)},
                                dest = Destination.PlayerZone.Discard
                            )?.let { player.removeCardEffect(scope) }
                        }
                        .end()
                    )
                    onCondition { event, player -> event.isSamePlayer(player)  }
                }
            }
    }
    @Dominion_Card(extension = "Empires")
    fun CityQuarter() : Card {
        val actions = Bonus.action(2)
        return Card.action("City Quarter", Price.empires(debt = 8))
            .setup {
                onPlay(actions.onPlay()
                    .reveal { getList(Destination.PlayerZone.Hand) }
                    .then { drawByAction { getList(Destination.PlayerZone.Hand).count { hasType(CardType.ACTION) } } }
                )
            }
    }
    @Dominion_Card(extension = "Empires")
    fun Crown() = Card.action("Crown", Price.empires(coins = 5)).addType(CardType.TREASURE)
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .chooseCardFromHand { player, _ ->
                    val isActionPhase = player.isFlagSet("Action")
                    InteractionRequest(
                        instruction = "$player, you may play twice ${if (isActionPhase) "an action card" else "a treasure"} from your hand",
                        filter = {if(isActionPhase) it.hasType(CardType.ACTION) else it.hasType(CardType.TREASURE)},
                        canPass = true
                    )
                }
                .thenWith { player, self, card ->
                    player.playCard(card, 2)
                    self.follow(player, card)
                }
                .end()
            )
            follower()
        }
    @Dominion_Card(extension = "Empires")
    fun Enchantress() = Card.attack("Enchantress", Price.empires(coins = 3)).addType(CardType.DURATION)
        .setup {
            val draw = Bonus.draw(2)

            onPlay(BiEffect.empty<Player, Card>()
                .then { player, card ->
                     card.set("Players", player.game.scanImmunity(player, card))
                    player.game.getRule<EmpiresRules>()?.registerEnchantress(card, player)
                }
            )
            onDuration {
                onEffect(draw.onDuration() then { player, card -> player.game.getRule<EmpiresRules>()?.unregisterEnchantress(card) })
            }
        }
    @Dominion_Card(extension = "Empires")
    fun Engineer() = Card.action("Engineer", Price.empires(debt = 4))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .gainFromSupplyAndWith(
                    instruction = {"$player, gain a card costing up to ${displayCoin(4)}"},
                    filter = {it isAtMost 4}
                )
                .filterNotNull()
                .chooseWhatToDo { player, card -> InteractionRequest(
                    instruction = "$player, you may trash $card to gain another card costing up to ${displayCoin(4)}",
                    cards = listOf(card),
                    buttons = Button.yesOrNo
                ) }
                .branch("y" to {player, self, _ ->
                    if(player.trash(self)) player.gainFromSupply(
                        instruction = "$player, gain a card costing up to ${displayCoin(4)}",
                        filter = {it isAtMost 4},
                        dest = Destination.PlayerZone.Discard
                    )
                })
                .end()
            )
        }
    @Dominion_Card(extension = "Empires")
    fun FarmersMarket() = Card.action("Farmers' Market", Price.empires(3)).addType(CardType.GATHERING)
        .setup {
            onPlay(Bonus.Buy.onPlay()
                .lookingAt { _, card -> card.flowOfSupply(SupplyPile.VICTORYTOKEN) }.filterNotNull()
                .branchDecision {
                    on {data.value >= 4 } then {player, self, flow ->
                        player.trash(self)
                        player.increment(Item.VICTORY_TOKEN, flow.value)
                        self.clearResource(SupplyPile.VICTORYTOKEN)
                    }
                    otherwise { player, self, flow ->
                        self.updateResource(SupplyPile.VICTORYTOKEN, 1)
                        player.increment(Item.MONEY, flow.value) }
                }
                .end()
            )
        }
    @Dominion_Card(extension = "Empires")
    fun Forum() : Card {
        val bonus = Bonus.action().draw(3)

        return Card.action("Forum", Price.empires(5))
            .setup {
                onPlay(bonus.onPlay() then { discardFromHand(2)})
                checkGain { onEffect{event, _ -> event.player.increment(Item.BUY)} }
            }
    }
    @Dominion_Card(extension = "Empires")
    fun Groundskeeper() = Card.action("Groundskeeper", Price.empires(5))
        .setup {
            onPlay(Bonus.ActionAndDraw.onPlay()
                .then { player, self ->
                    self.set("victoryPoint", self.getValue("victoryPoint").toInt() + 1)
                    player.addCardEffect(self)
                }
            )
            onSideEffectGain(GainType.AFTER){
                onEffect{player, _ -> player.increment(Item.VICTORY_TOKEN, scope.getValue("victoryPoint").toInt())}
                onCondition { event, player -> event.isSamePlayer(player) && event.cardHasType(CardType.VICTORY) }
            }
        }
    @Dominion_Card(extension = "Empires")
    fun Legionary() : Card {
        val money = Bonus.money(3)
        return Card.attack("Legionary", Price.empires(5))
            .setup {
                onPlay(money.onPlay()
                    .then { player, card -> card.set("Players", player.game.scanImmunity(player, card)) }
                    .chooseCardFromHand { player, _ -> InteractionRequest(
                        instruction = "$player, you may reveal a gold from your hand to attack others",
                        filter = {it.hasName("Gold")},
                        canPass = true
                    ) }
                    .thenWith { player, card -> player.reveals(card) }
                    .processHandDown(toReach = 2, mayDiscard = true){vi -> vi.draw()}
                    .end()
                )
            }
    }

    @Dominion_Card(extension = "Empires")
    fun Overlord() = Card.action("Overlord", Price.empires(debt = 8)).addType(CardType.COMMAND)
        .setup { 
            onPlay(BiEffect.empty<Player, Card>()
                .chooseCardFromSupply { player, _ -> InteractionRequest(
                    instruction = "$player, play a non Command non duration card from the supply costing up to ${displayCoin(5)}",
                    filter = {it isAtMost 5 && it.testTypes { none(CardType.COMMAND, CardType.DURATION) }}
                ) }
                .thenWith { player,self, card ->
                    card.copy().play(player)
                    self.follow(player, card)
                }
                .end()
            )
            follower()
        }
    @Dominion_Card(extension = "Empires")
    fun RoyalBlacksmith() = Card.action("Royal Blacksmith", Price.empires(debt = 8))
        .setup {
            onPlay(Bonus.draw(5).onPlay()
                .lookingAt { player, _ -> player.getList(Destination.PlayerZone.Hand)  }
                .filterListAndNotEmpty { it.hasName("Copper") }
                .thenWith { player, cards -> player.discardList(cards) }
                .end()
            )
        }
    @Dominion_Card(extension = "Empires")
    fun Sacrifice() = Card.action("Sacrifice", Price.empires(4))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .trashCardFromHand()
                .matchAll {
                    on {choice.hasType(CardType.ACTION)} then {player, _, _ -> player.draw(2); player.increment(Item.ACTION, 2)}
                    on {choice.hasType(CardType.TREASURE)} then {player, _, _ -> player.increment(Item.MONEY, 2)}
                    on {choice.hasType(CardType.VICTORY)} then {player, _, _ -> player.increment(Item.VICTORY_TOKEN, 2)}
                }
                .endParent()
            )
        }
    @Dominion_Card(extension = "Empires")
    fun Temple() = Card.action("Temple", Price.empires(4)).addType(CardType.GATHERING)
        .setup {
            onPlay(Bonus.VictoryToken.onPlay()
                .then {
                    val differentNamed = mutableSetOf<String>()
                    trashAndEffect(
                        extraInstruction = " to add 1 token to Temple pile",
                        number = 3,
                        from = Destination.PlayerZone.Hand,
                        filter = {!differentNamed.contains(it.name)}
                    ){
                        scope.updateResource(SupplyPile.VICTORYTOKEN, 1)
                        differentNamed.add(it.name)
                    }
                }
            )
            checkGain {
                onEffect{event, card ->
                    event.player.incrementByAction(Item.VICTORY_TOKEN){card.takePointFrom(SupplyPile.VICTORYTOKEN)}
                }
                onCondition { _, _ -> scope.pickResource(SupplyPile.VICTORYTOKEN) > 0 }
            }

        }
    @Dominion_Card(extension = "Empires")
    fun Villa() : Card {
        val bonus = Bonus.action(2).with(Item.BUY).with(Item.MONEY)
        return Card.action("Villa", Price.empires(4))
            .setup {
                simpleAction(bonus)
                checkGain {
                    onEffect{event, card ->
                        val player = event.player
                        player.increment(Item.ACTION)
                        if(player.isInBuyPhase){ player.returnToActionPhase() }
                    }
                }
            }
    }
    @Dominion_Card(extension = "Empires")
    fun WildHunt() = Card.action("Wild Hunt", Price.empires(5)).addType(CardType.GATHERING)
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .chooseWhatToDo { player, card -> InteractionRequest(
                    instruction = "$player, Choose: draw 3 cards and add +1 token to Wild Hunt pile or gain an estate and the tokens",
                    cards = listOf(card),
                    buttons = listOf(Button("+3 cards & add 1 token", "effect1"), Button("an estate and the token", "effect2"))
                ) }
                .branch(
                    "effect1" to {player, self, _ ->
                        player.draw(3)
                        self.updateResource(SupplyPile.VICTORYTOKEN, 1)
                    },
                    "effect2" to {player, self, _ ->
                        player.gainFromSupply("Estate")?.let {
                            player.increment(Item.VICTORY_TOKEN, self.takePointFrom(SupplyPile.VICTORYTOKEN))
                        }
                    }
                )
                .end()
            )
        }

    val castleComparator = compareByDescending<Card> { it.costValue }
    @Dominion_Card(extension = "Empires", pileType = PileType.UNIQUE)
    fun Castles() = TCastles("Castles").addType(CardType.TEMPLATE)
        .setup { onSetup { allPilesForSupply.add(createMixedSupplyPile(factory.getMixedCards(CardType.CASTLE), scope).sortWith(castleComparator)) } }

    @Dominion_Card(extension = "Empires", pileType = PileType.MIXED)
    fun HumbleCastle() = TCastles("Humble Castle").addType(CardType.TREASURE)
        .setup {
            score { it.allOwnedCards.count { hasType(CardType.CASTLE) } }
            simpleAction(Bonus.Money)
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.UNIQUE)
    fun CrumblingCastle() = TCastles("Crumbling Castle", 4 )
        .setup {
            score { 1 }
            checkGain { onEffect(runCrumblingCastle) }
            checkItselfTrash{onEffect(runCrumblingCastle)}
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.MIXED)
    fun SmallCastle() = TCastles("Small Castle", 5).addType(CardType.ACTION)
        .setup {
            score { 2 }
            onPlay(BiEffect.empty<Player, Card>()
                .chooseWhatToDo { player, card -> InteractionRequest(
                    instruction = "$player, trash $card, or a castle from you hand to gain a castle",
                    cards = player.getList(Destination.PlayerZone.Hand),
                    buttons = listOf(Button(card.name, card.name), Button("from hand", "hand"))
                ) }
                .branch(
                    "self" to {player, self, _ -> if(player.trash(self)) player.gainFromSupply("Castles")},
                    "hand" to {player, _ , _ -> player.trashAndEffect(
                        extraInstruction = "to gain a castle",
                        from = Destination.PlayerZone.Hand,
                        canPass = false,
                        filter = {it.hasType(CardType.CASTLE)},
                        action = {gainFromSupply("Castles")}
                    )}
                )
                .end()
            )
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.UNIQUE)
    fun HauntedCastle() = TCastles("Haunted Castle", 6)
        .setup {
            score { 2 }
            checkGain {
                onEffect(BiEffect.empty<Event, Card>()
                    .gainSpecificCardFromSupply(cardName = "Gold")
                    .attackOthers { _, opponent, _ ->
                        if(opponent.sizeOf(Destination.PlayerZone.Hand) >= 5){
                            opponent.move(Destination.PlayerZone.Hand, Destination.PlayerZone.Draw, 2)
                        }
                    }
                )
                onCondition { _, player ->  player.isActive }
            }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.MIXED)
    fun OpulentCastle() = TCastles("Opulent Castle", 7).addType(CardType.ACTION)
        .setup {
            score { 3 }
            onPlay(BiEffect.empty<Player, Card>()
                .then {
                    discardUntilYouStopAndDo(
                        from = Destination.PlayerZone.Hand,
                        instruction = "for +${displayCoin(2)}",
                        extraCardInformation = "victory",
                        filter = { it.hasType(CardType.VICTORY) },
                    ){
                        reveals(it)
                        increment(Item.MONEY, 2)
                    }
                }
            )
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.UNIQUE)
    fun Sprawling_Castle() = TCastles("Sprawling Castle", 8)
        .setup {
            score { 4 }
            checkGain {
                onEffect(BiEffect.empty<Event, Card>()
                    .chooseWhatToDo { event, card -> InteractionRequest(
                        instruction = "${event.player}, Choose: gain a duchy or 3 estates",
                        cards = listOf(card),
                        buttons = listOf(Button("a duchy", "duchy"), Button("3 estates", "estate"))
                    )}
                    .branch(
                        "duchy" to {event, _, _ -> event.player.gainFromSupply("Duchy") },
                        "estate" to {event, _, _ -> event.player.gainMultiplyCardFromSupply("Estate", Destination.PlayerZone.Discard, 3)}
                    )
                    .end()
                )
            }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.UNIQUE)
    fun GrandCastle() = TCastles("Grand Castle", 9)
        .setup {
            score { 5 }
            checkGain {
                onEffect{event, _ ->
                    event.player.reveals(event.player.getList(Destination.PlayerZone.Hand))
                    event.player.incrementByAction(Item.VICTORY_TOKEN){
                        val list = getList(Destination.PlayerZone.Hand) + getList(Destination.PlayerZone.InPlay)
                        list.count { hasType(CardType.VICTORY) }
                    }
                }
            }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.MIXED)
    fun KingsCastle() = TCastles("King's Castle", 10)
        .setup { score { it.allOwnedCards.count { hasType(CardType.CASTLE) } * 2 } }



    fun TCastles(cardName : String, price : Int = 3) = Card.victory(cardName, Price.empires(price)).addType(CardType.CASTLE)

    @Dominion_Card(extension = "Empires", pileType = PileType.UNIQUE)
    fun CatapultRocks() = Card.attack("Catapult Rocks", Price.empires(3)).addType(CardType.TEMPLATE, CardType.Split)
        .setup {
            onSetup { allPilesForSupply.add(createMixedSupplyPile(factory.getSpecificMixedSupply(CardType.Split, "Catapult", "Rocks").reversed(), scope)) }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 5)
    fun Catapult() = Card.attack("Catapult", Price.empires(3)).addType(CardType.Split)
        .setup {
            onPlay(Bonus.Money.onPlay()
                .trashCardFromHand()
                .matchAll {
                    var effect = BiEffect.empty<Player, Card>()
                    on {choice.isAtLeast(3)} then {_, _, _ -> effect = effect.attackOthers { _, opponent, _ -> opponent.gainFromSupply("Curse") }}
                    on {choice.hasType(CardType.TREASURE)} then {_, _, _ -> effect = effect.processHandDown(toReach = 3, mayDiscard = true)}
                    on {true} then {player, self, _ -> effect(player, self)}
                }
                .endParent()
            )
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 5)
    fun Rocks() = Card.treasure("Rocks", Price.empires(4)).addType(CardType.Split)
        .setup {
            simpleAction(Bonus.Money)
            checkGain{onEffect(runRocks)}
            checkItselfTrash{onEffect(runRocks)}
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.UNIQUE)
    fun EncampmentPlunder() = Card.action("Encampment Plunder", Price.empires(2)).addType(CardType.TEMPLATE)
        .setup {
            onSetup {
                allPilesForSupply.add(createMixedSupplyPile(factory.getSpecificMixedSupply(CardType.Split, "Encampment", "Plunder").reversed(), scope))
            }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 5)
    fun Encampment() : Card {
        val bonus = Bonus.action(2).draw(2)
        return Card.action("Encampment", Price.empires(2)).addType(CardType.Split)
            .setup {
                onPlay(bonus.onPlay()
                    .chooseCardFromHand { player, _ -> InteractionRequest(
                        "$player, you may reveal a Gold or a Plunder from your hand",
                        filter = {it.hasName("Gold") || it.hasName("Plunder")},
                        canPass = true
                    ) }
                    .thenWith { player, card ->  player.reveals(card)}
                    .otherwise { player, card ->
                        player.moveTo(card, Destination.PlayerZone.Aside)
                        player.addCleanUpEffect { card.replaceInSupply() }
                    }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 5)
    fun Plunder() : Card {
        val bonus = Bonus.money(2).with(Item.VICTORY_TOKEN)
        return Card.treasure("Plunder", Price.empires(2)).addType(CardType.Split)
            .setup { simpleAction(bonus) }
    }

    @Dominion_Card(extension = "Empires", pileType = PileType.UNIQUE)
    fun GladiatorFortune() = Card.action("Gladiator Fortune", Price.empires(3)).addType(CardType.TEMPLATE)
        .setup {
            onSetup {
                allPilesForSupply.add(createMixedSupplyPile(factory.getSpecificMixedSupply(CardType.Split, "Gladiator", "Fortune").reversed(), scope))
            }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 5)
    fun Gladiator() : Card {
        val money = Bonus.money(2)
        return Card.action("Gladiator", Price.empires(2)).addType(CardType.Split)
            .setup {
                onPlay(money.onPlay()
                    .lookingAt { player, _ -> player.onTheLeft() }
                    .chooseCardFromHand { player, _ ->  InteractionRequest(
                        instruction = "$player, reveal a card from your hand",
                    )}
                    .also { player, _, revealed -> player.reveals(revealed) }
                    .thenChooseCardFromList({_, _, opponent -> opponent}) { player, _, opponent, revealed -> InteractionRequest(
                        instruction = "$opponent, you may reveal a $revealed from your hand that $player revealed ",
                        cards = opponent.getList(Destination.PlayerZone.Hand),
                        filter = { it.hasSameNameAs(revealed) },
                        canPass = true
                    ) }.result
                    .thenWithPipe { _, _ -> source.reveals(extraData) }
                    .thenIfFail { player, _ ->
                        player.increment(Item.MONEY)
                        player.chooseCardFromSupply("$player, trash a gladiator from the supply", {it.hasName("Gladiator")})
                            ?.let { player.trash(it) }
                    }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 5)
    fun Fortune() = Card.treasure("Fortune", Price.empires(8, 8)).addType(CardType.Split)
        .setup {
            onPlay(Bonus.Buy.onPlay()
                .lookingAt { player, card -> player.isFlagSet(card.name) }
                .branch(
                    false to {player, self ->
                        player.incrementByAction(Item.MONEY){ getValueOf(Item.MONEY)}
                        player.getFlag(self.name) += true
                    }
                )
                .end()
            )
            checkGain {
                onEffect(BiEffect.empty<Event, Card>()
                    .lookingAt { event, _ -> event.player.getList(Destination.PlayerZone.InPlay).count { hasName("Gladiator") } }
                    .thenWith { event, i -> event.player.gainMultiplyCardFromSupply("Gold", Destination.PlayerZone.Discard, i) }
                    .end()
                )
                onCondition {_ , player -> player.getList(Destination.PlayerZone.InPlay).any{it.hasName("Gladiator") }  }
            }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.UNIQUE)
    fun PatricianEmporium() = Card.action("Patrician Emporium", Price.empires(2)).addType(CardType.TEMPLATE)
        .setup {
            onSetup {
                allPilesForSupply.add(createMixedSupplyPile(factory.getSpecificMixedSupply(CardType.Split, "Patrician", "Emporium").reversed(), scope))
            }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 5)
    fun Patrician() = Card.action("Patrician", Price.empires(2)).addType(CardType.Split)
        .setup {
            onPlay(Bonus.ActionAndDraw.onPlay()
                .lookingAt { player, _ -> player.getCardFromDeck() }.filterNotNull{it.isAtLeast(5)}
                .thenWith { player, card -> player.moveTo(card, Destination.PlayerZone.Hand) }
                .end()
            )
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 5)
    fun Emporium() = Card.action("Emporium", Price.empires(5)).addType(CardType.Split)
        .setup {
            val bonus = Bonus.action().with(Item.MONEY).draw()
            simpleAction(bonus)
            checkGain {
                onEffect{event, _ -> event.player.increment(Item.VICTORY_TOKEN, 2)}
                onCondition { _, player -> player.getList(Destination.PlayerZone.InPlay).count { hasType(CardType.ACTION) } >= 5 }
            }
        }

    @Dominion_Card(extension = "Empires", pileType = PileType.UNIQUE)
    fun SettlerBustlingVillage() = Card.action("Settler Bustling Village", Price.empires(2)).addType(CardType.TEMPLATE)
        .setup {
            onSetup {
                allPilesForSupply.add(createMixedSupplyPile(factory.getSpecificMixedSupply(CardType.Split, "Settler", "Bustling Village").reversed(), scope))
            }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 5)
    fun Settlers() = Card.action("Settlers", Price.empires(2)).addType(CardType.Split)
        .setup {
            onPlay(Bonus.ActionAndDraw.onPlay()
                .chooseCardFromList { player, _ -> InteractionRequest(
                    instruction = "$player, you may reveal a copper from your discard pile",
                    cards = player.getList(Destination.PlayerZone.Discard),
                    filter = {it.hasName("Copper")},
                    canPass = true
                ) }
                .thenWith { player, card -> player.reveals(card); player.moveTo(card, Destination.PlayerZone.Hand)}
                .end()
            )
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 5)
    fun BustlingVillage() = Card.action("Bustling Village", Price.empires(5)).addType(CardType.Split)
        .setup {
            val bonus = Bonus.action(3).draw()
            onPlay(bonus.onPlay()
                .chooseCardFromList { player, _ -> InteractionRequest(
                    instruction = "$player, you may reveal a settler from your discard pile",
                    cards = player.getList(Destination.PlayerZone.Discard),
                    filter = {it.hasName("Settler")},
                    canPass = true
                ) }
                .thenWith { player, card -> player.reveals(card); player.moveTo(card, Destination.PlayerZone.Hand)}
                .end()

            )
        }


    //region EVENT
    @Dominion_Card(extension = "Empires", pileType = PileType.EVENT)
    fun Advance() = Card.event("Advance", Price.empires(0))
        .setup {
            checkItselfBuy {
                onEffect(BiEffect.empty<Event, Card>()
                    .trashCardFromHand(canPass = true, filter = {it.hasType(CardType.ACTION)}, extraInstruction = "action")
                    .gainFromSupply(
                        instruction = {"$player, gain an action card costing up to ${displayCoin(6)}"},
                        filter = {it isAtMost 6 && it.hasType(CardType.ACTION)}
                    )
                    .endParent())
            }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.EVENT)
    fun Annex() = Card.event("Annex", Price.empires(debt = 8))
        .setup {
            checkItselfBuy(BiEffect.empty<Event, Card>()
                .lookingAt { event, _ -> event.player.getList(Destination.PlayerZone.Discard)}
                .filterListAndNotEmpty()
                .loop(5){discards, counter ->
                    chooseCardFromList { event, _ -> InteractionRequest(
                        instruction = "${event.player}, you may choose to put ${if(counter.current > 1) "again" else "" } ${5 - counter.current} card(s) on the top of your deck",
                        cards = discards,
                        canPass = true
                    ) }
                        .thenWith { event, self,  card ->
                            discards -= card
                            event.player.addCardToShadowZone(self, card)
                        }
                }
                .so { event -> event.player.shuffle() }
                .map { event, card, _ -> event.player.getShadowList(card) }.listIsNotEmpty()
                .forEachCard({_, list -> list}){_, card -> so { it.player.moveTo(card, Destination.PlayerZone.Draw) } }
                .sendMessage(Player.clearShadowZone) { _, self, _ -> self }
                .end()
                .gainSpecificCardFromSupply(cardName = "Duchy")
            )
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.EVENT)
    fun Banquet() = Card.event("Banquet", Price.empires(3))
        .setup {
            checkItselfBuy(BiEffect.empty<Event, Card>()
                .then { player.gainMultiplyCardFromSupply("Copper", Destination.PlayerZone.Discard, numberOfCards = 2) }
                .gainFromSupply(
                    instruction = {"$player, gain a non victory card costing up to ${displayCoin(5)}"},
                    filter = {it isAtMost 5 && it.hasNotType(CardType.VICTORY)}
                )
            )
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.EVENT)
    fun Conquest() = Card.event("Conquest", Price.empires(6))
        .setup {
            checkItselfBuy(BiEffect.empty<Event, Card>()
                .then {
                    player.gainMultiplyCardFromSupply("Silver", Destination.PlayerZone.Discard, numberOfCards = 2)
                    player.incrementByAction(Item.VICTORY_TOKEN){cardGainedCurrentTurn.count { hasName("Silver") }}
                }
            )
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.EVENT)
    fun Delve() = Card.event("Delve", Price.empires(2))
        .setup {
            checkItselfBuy(Bonus.Buy.onBuy().gainSpecificCardFromSupply(cardName = "Silver"))
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.EVENT)
    fun Dominate() = Card.event("Dominate", Price.empires(14))
        .setup {
            checkItselfBuy(BiEffect.empty<Event, Card>()
                .then { if(player.gainFromSupply("Province") != null && !player.underPossession) player.increment(Item.VICTORY_TOKEN, 9) }
            )
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.EVENT)
    fun Donate() = Card.event("Donate", Price.empires(debt = 8))
        .setup {
            checkItselfBuy{event, _ ->
                event.player.addNextTurnEffect {
                    val effect = BiEffect.empty<Player, Unit>()
                        .lookingAt { player, _ ->
                            player.getList(Destination.PlayerZone.Draw) + player.getList(Destination.PlayerZone.Discard)
                        }.listIsNotEmpty()
                        .thenWith { player, cards ->
                            player.moveAll(cards, Destination.PlayerZone.Hand)
                            player.trashUntilYouStopAndDo(Destination.PlayerZone.Hand){}
                            player.moveAll(Destination.PlayerZone.Hand, Destination.PlayerZone.Draw)
                            player.shuffling(Destination.PlayerZone.Draw)
                            player.draw(5)
                        }.end()

                    effect(self, Unit)
                }
            }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.EVENT)
    fun Ritual() = Card.event("Ritual", Price.empires(4))
        .setup {
            checkItselfBuy(BiEffect.empty<Event, Card>()
                .gainSpecificCardFromSupplyAndDo(cardName = "Curse")
                .chooseCardFromHand { event, _ -> InteractionRequest(instruction = "${event.player}, trash a card from your hand") }
                .filterWith { event, _, chosen -> event.player.trash(chosen) }
                .thenWith { event, card -> event.player.incrementByAction(Item.VICTORY_TOKEN){card.costValue} }
                .end()
            )
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.EVENT)
    fun SaltTheEarth() = Card.event("Salt The Earth", Price.empires(4))
        .setup {
            checkItselfBuy(Bonus.VictoryToken.onBuy()
                .chooseCardFromSupply { event, _ -> InteractionRequest(
                    instruction = "${event.player}, trash a victory card from the supply",
                    filter = { it.hasType(CardType.VICTORY)}
                ) }
                .thenWith { event, card -> event.player.trash(card) }
                .end()
            )
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.EVENT)
    fun Tax() = Card.event("Tax", Price.empires(2))
        .setup {
            onEndSetup {
                supplyPiles.values.forEach { it.updateResource(SupplyPile.DEBTTAX, 1) }
                addListener<OnGainEvent>{ eve -> getRule<EmpiresRules>()?.taxPassive(eve) }
            }
            checkItselfBuy(BiEffect.empty<Event, Card>()
                .chooseCardFromSupply { event, _ -> InteractionRequest(
                    instruction = "${event.player}, add 2 debt on a supply pile of your choice"
                ) }
                .thenWith { event, card -> card.updateResource(SupplyPile.DEBTTAX, 2) }
                .end()
            )
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.EVENT)
    fun Triumph() = Card.event("Triumph", Price.empires(debt = 5))
        .setup {
            checkItselfBuy(BiEffect.empty<Event, Card>()
                .gainSpecificCardFromSupplyAndDo(cardName = "Estate")
                .so { event ->
                    event.player.incrementByAction(Item.VICTORY_TOKEN){ cardGainedCurrentTurn.size }
                }
                .end()
            )
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.EVENT)
    fun Wedding() = Card.event("Wedding", Price.empires(coins = 4, debt = 3))
        .setup {
            checkItselfBuy(Bonus.VictoryToken.onBuy()
                .gainSpecificCardFromSupply(cardName = "Gold")
            )
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.EVENT)
    fun Windfall() = Card.event("Windfall", Price.empires(5))
        .setup {
            checkItselfBuy {
                onEffect(BiEffect.empty<Event, Card>() then { player.gainMultiplyCardFromSupply("Gold", Destination.PlayerZone.Discard, 3) })
                onCondition { _, player -> player.getList(Destination.PlayerZone.Draw).isEmpty() && player.getList(Destination.PlayerZone.Discard).isEmpty() }
            }
        }
    //endregion

    //region LANDMARKS
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 1)
    fun Aqueduct() = Card.landmark("Aqueduct")
        .setup {
            onEndSetup {
                getSpecificSupply("Gold")?.updateResource(SupplyPile.VICTORYTOKEN, 8)
                getSpecificSupply("Silver")?.updateResource(SupplyPile.VICTORYTOKEN, 8)
                addListener<OnGainEvent>{ gain -> runAqueduc(scope)(gain.player, gain) }
            }
        }

    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 1)
    fun Arena() = Card.landmark("Arena").setup {
        onEndSetup { scope.updateResource(SupplyPile.VICTORYTOKEN, 6 * nbPlayers) }
        onStartBuyPhase {
            onEffect(BiEffect.empty<Player, Card>()
                .lookingAt { _, card -> card.flowOfSupply(SupplyPile.VICTORYTOKEN) }
                .filterNotNull()
                .chooseCardFromHand { player, _ -> InteractionRequest(
                    instruction = "you may discard an action card from your hand",
                    filter = {it.hasType(CardType.ACTION)},
                    canPass = true
                ) }
                .filterDo { player, _ , flow,  card -> player.discard(card) && flow.value > 0 }
                .thenDo { player, self->
                    val toUsed = self.takePointFrom(SupplyPile.VICTORYTOKEN, 2)
                    player.increment(Item.VICTORY_TOKEN, toUsed)
                }
                .end()
            )
            onCondition { _, player -> player.getList(Destination.PlayerZone.Hand).any{it.hasType(CardType.ACTION)} }
        }
    }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 1)
    fun BanditFort() = Card.landmark("Bandit Fort")
        .setup { score { it.allOwnedCards.count { hasName("Silver") || hasName("Gold") } * -2 } }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 1)
    fun Basilica() = Card.landmark("Basilica")
        .setup {
            onEndSetup {
                scope.updateResource(SupplyPile.VICTORYTOKEN, 6 * nbPlayers)
                addListener<OnGainEvent>{event ->

                    val (player, card) = event
                    if(card == null) return@addListener

                    if(player.money >= 2 && player.isInBuyPhase) {
                        player.incrementByAction(Item.VICTORY_TOKEN){
                            scope.takePointFrom(SupplyPile.VICTORYTOKEN, 2)
                        }
                    }
                }

            }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 1)
    fun Baths() = Card.landmark("Baths")
        .setup {
            onEndSetup {
                scope.updateResource(SupplyPile.VICTORYTOKEN, 6 * nbPlayers)
            }
            onEndTurn {
                onEffect{player, self -> player.giveVictoryPointBy(self)}
                onCondition { _, player -> player.cardGainedCurrentTurn.isEmpty()  }
            }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 1)
    fun BattleField() = Card.landmark("Battle Field")
        .setup {
            onEndSetup {
                scope.updateResource(SupplyPile.VICTORYTOKEN, 6 * nbPlayers)
                addListener<OnGainEvent> { event ->
                    if(event.cardHasType(CardType.VICTORY)){
                        event.player.giveVictoryPointBy(scope)
                    }
                }
            }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 1)
    fun Colonnade() = Card.landmark("Colonnade")
        .setup {
            onEndSetup {
                scope.updateResource(SupplyPile.VICTORYTOKEN, 6 * nbPlayers)
                addListener<OnGainEvent>{ event ->
                    if(event.cardHasType(CardType.ACTION)
                        && event.player.isInBuyPhase
                        && event.player.hasCopyIn(Destination.PlayerZone.InPlay, event.card))
                    { event.player.giveVictoryPointBy(scope) }
                }
            }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 1)
    fun DefiledShrine() = Card.landmark("Defiled Shrine")
        .setup {
            onEndSetup {
                allSupply.forEach { it.updateResource(SupplyPile.VICTORYTOKEN, 2) }
                addListener<OnGainEvent>{ runDefiledShrine(scope)(it.player, it) }
            }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 1)
    fun Fountain() = Card.landmark("Fountain")
        .setup { score { if( it.allOwnedCards.count { hasName("Copper") } >= 10 ) 15 else 0  } }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 1)
    fun Keep() = Card.landmark("Keep")
        .setup {
            score {player ->
                val treasureCounts = player.allOwnedCards
                    .filter { it.hasType(CardType.TREASURE) }
                    .groupBy { it.name }
                    .mapValues { it.value.size }

                treasureCounts.entries.sumOf { (name, number) -> if (player.game.compareCardToOther(player, name, number)) 5 else 0 }
            }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 1)
    fun Labyrinth() = Card.landmark("Labyrinth")
        .setup {
            onEndSetup {
                scope.updateResource(SupplyPile.VICTORYTOKEN, 6 * nbPlayers)
                addListener<OnGainEvent>{ event ->
                    if(event.player.cardGainedCurrentTurn.size == 2){
                        event.player.giveVictoryPointBy(scope)
                    }
                }
            }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 1)
    fun MountainPass() = Card.landmark("Mountain Pass")
        .setup {
            onEndSetup {
                addListener<OnGainEvent> { event ->
                    if (stat.firstProvinceGain.value) return@addListener
                    if (!event.cardHasName("Province")) return@addListener

                    val map = mutableMapOf<Player, Int>()
                    var minBidRequired = 1

                    processGlobalEffect(event.player.onTheLeft()) { p ->

                        val choice = p.choose(
                            "$p, bet between $minBidRequired and 40 Debt to overbid. Pass to bet 0.",
                            canPass = true
                        )

                        if (choice.isEmpty()) {
                            p.log("$p passed.")
                            map[p] = 0
                        } else {
                            val number = choice.toInt()
                            p.log("$p bet $number Debt.")
                            map[p] = number
                            minBidRequired = number + 1
                        }
                    }

                    val highestBidEntry = map.maxByOrNull { it.value }

                    if (highestBidEntry != null && highestBidEntry.value > 0) {
                        val winner = highestBidEntry.key
                        val winningBid = highestBidEntry.value

                        winner.increment(Item.VICTORY_TOKEN, 8)
                        winner.increment(Item.DEBT, winningBid)
                        winner.log("won the Mountain Pass auction with a bid of $winningBid Debt!")
                    } else {
                        log("Everyone passed. Nobody claims the Mountain Pass tokens.")
                    }
                    stat.firstProvinceGain.update { true }
                }
            }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 1)
    fun Museum() = Card.landmark("Museum")
        .setup { score {it.allOwnedCards.distinctBy {c -> c.name }.count() * 2} }

    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 1)
    fun Obelisk() = Card.landmark("Obelisk")
        .setup {
            onEndSetup { obeliskTarget = availableSupplyCard.filter { it.hasType(CardType.ACTION) }.random().name }
            score { it.allOwnedCards.count { supply?.verifyName(it.game.obeliskTarget) == true } * 2 }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 1)
    fun Orchard() = Card.landmark("Orchard")
        .setup {
            score { p ->
                p.allOwnedCards
                    .filter { it.hasType(CardType.ACTION) }
                    .groupBy { it.name }
                    .count { entry -> entry.value.size >= 3 } * 4
            }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 1)
    fun Palace() = Card.landmark("Palace")
        .setup {
            score { p ->
                val coppers = p.allOwnedCards.count { name == "Copper" }
                val silvers = p.allOwnedCards.count { name == "Silver" }
                val golds = p.allOwnedCards.count { name == "Gold" }

                val fullSets = minOf(coppers, silvers, golds)

                fullSets * 3
            }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 1)
    fun Tomb() = Card.landmark("Tomb")
        .setup {
            onCardTrash { onEffect(BiEffect.empty<Event, Card>().increment(Item.VICTORY_TOKEN)) }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 1)
    fun Tower() = Card.landmark("Tower")
        .setup {
            score {
                val emptyNonVictoryNames = it.game.supplyPiles.values
                    .filter { s -> s.isEmpty }
                    .filter { s -> !s.hasType(CardType.VICTORY) }
                    .map { s -> s.supplyName }
                    .toSet()

                it.allOwnedCards
                    .filter { c -> !c.hasType(CardType.VICTORY) }
                    .count { c -> c.supply?.supplyName in emptyNonVictoryNames }
            }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 1)
    fun TriumphalArch() = Card.landmark("Triumphal Arch")
        .setup {
            score { p ->
                val actionCounts = p.allOwnedCards
                    .filter { it.hasType(CardType.ACTION) }
                    .groupBy { it.name }
                    .map { it.value.size }
                    .sortedDescending()

                if (actionCounts.size >= 2) {
                    actionCounts[1] * 3
                } else {
                    0
                }
            }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 1)
    fun Wall() = Card.landmark("Wall")
        .setup {
            score {
                val excessCards = (it.allOwnedCards.size - 15).coerceAtLeast(0)
                -excessCards
            }
        }
    @Dominion_Card(extension = "Empires", pileType = PileType.CUSTOM, cardsNumber = 1)
    fun WolfDen() = Card.landmark("Wolf Den")
        .setup {
            score { p ->
                p.allOwnedCards
                    .filter { it.hasType(CardType.ACTION) }
                    .groupBy { it.name }
                    .count { entry -> entry.value.size == 1 } * -3
            }
        }
    


    internal fun Player.giveVictoryPointBy(card : Card, toTake : Int = 2) {
        incrementByAction(Item.VICTORY_TOKEN){
            card.takePointFrom(SupplyPile.VICTORYTOKEN, toTake)
        }
    }

    internal fun Player.takeAllVictoryPoint(card : Card){
        incrementByAction(Item.VICTORY_TOKEN){ card.takePointFrom(SupplyPile.VICTORYTOKEN) }
    }

    private fun Player.hasCopyIn(from : Destination.PlayerZone, card : Card?) : Boolean{
        if(card == null) return false
        return getList(from).any{it.hasSameNameAs(card)}
    }


    private fun Game.getSpecificSupply(nameSupply : String) =  supplyPiles[nameSupply]

    //endregion


}