package fr.umontpellier.iut.dominion.cards.factories.Adventures

import fr.umontpellier.iut.dominion.*
import fr.umontpellier.iut.dominion.Annotation.Dominion_Card
import fr.umontpellier.iut.dominion.Annotation.PileType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Player.*
import fr.umontpellier.iut.dominion.Player.PlayerComponent.addCardEffect
import fr.umontpellier.iut.dominion.Player.PlayerComponent.cardGainedLastTurn
import fr.umontpellier.iut.dominion.Player.PlayerComponent.flipJourneyToken
import fr.umontpellier.iut.dominion.Player.PlayerComponent.getToken
import fr.umontpellier.iut.dominion.Player.PlayerComponent.getTokenFlag
import fr.umontpellier.iut.dominion.Player.PlayerComponent.setToken
import fr.umontpellier.iut.dominion.Player.PlayerComponent.updateTokenFlag
import fr.umontpellier.iut.dominion.Player.Skills.chooseToken
import fr.umontpellier.iut.dominion.Player.Skills.discard
import fr.umontpellier.iut.dominion.Player.Skills.discardAList
import fr.umontpellier.iut.dominion.Player.Skills.discardAll
import fr.umontpellier.iut.dominion.Player.Skills.discardFromHand
import fr.umontpellier.iut.dominion.Player.Skills.discardUntilYouStop
import fr.umontpellier.iut.dominion.Player.Skills.draw
import fr.umontpellier.iut.dominion.Player.Skills.drawByAction
import fr.umontpellier.iut.dominion.Player.Skills.move
import fr.umontpellier.iut.dominion.Player.Skills.moveAll
import fr.umontpellier.iut.dominion.Player.Skills.moveAllAndChooseTheOrder
import fr.umontpellier.iut.dominion.Player.Skills.moveTo
import fr.umontpellier.iut.dominion.Player.Skills.playCard
import fr.umontpellier.iut.dominion.Player.Skills.trash
import fr.umontpellier.iut.dominion.Player.Skills.trashAndEffect
import fr.umontpellier.iut.dominion.Player.Skills.trashWithCondition
import fr.umontpellier.iut.dominion.Player.Tokens.JourneyFace
import fr.umontpellier.iut.dominion.Player.Tokens.Token
import fr.umontpellier.iut.dominion.cards.*
import fr.umontpellier.iut.dominion.cards.Bonus.Bonus
import fr.umontpellier.iut.dominion.cards.Events.Event
import fr.umontpellier.iut.dominion.cards.Events.GainType
import fr.umontpellier.iut.dominion.cards.Events.TriggerEvent
import fr.umontpellier.iut.dominion.cards.builders.*
import fr.umontpellier.iut.dominion.cards.component.*
import fr.umontpellier.iut.dominion.cards.factories.*

object AdventuresFactoryKt {
    @Dominion_Card(extension = "Adventures")
    fun Amulet() = Card.duration("Amulet", Price.adventure(3)).addType(CardType.ACTION)
        .setup {
            onPlay(runAmulet)
            onDuration{ onEffect(runAmulet) }
        }
    @Dominion_Card(extension = "Adventures")
    fun Artificer() : Card {
        val bonus = Bonus.action().with(Item.MONEY).draw()
        return Card.action("Artificer", Price.adventure(5))
            .setup {
                onPlay(bonus.onPlay()
                    .then { discardUntilYouStop(
                        from = Destination.PlayerZone.Hand
                    ) {
                        gainFromSupply(
                            instruction = "gain a card costing exactly $it$",
                            filter = {c -> c.isEqual(it)},
                            dest = Destination.PlayerZone.Draw
                        )
                    } }
                )
            }
    }
    @Dominion_Card(extension = "Adventures")
    fun BridgeTroll() = Card.attack("Bridge Troll", Price.adventure(5)).addType(CardType.DURATION)
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .attackOthers { _, opponent, _ -> opponent.updateTokenFlag(Token.OnPlayer.TaxToken, true) }
                .runBridgeTroll()
            )
            onDuration { onEffect(BiEffect.empty<Player, Card>().runBridgeTroll()) }
        }
    @Dominion_Card(extension = "Adventures")
    fun CaravanGuard() : Card {
        val bonus = Bonus.action().draw()
        return Card("Caravan Guard", Price.adventure(3), CardType.ACTION, CardType.DURATION, CardType.REACTION)
            .setup {
                simpleAction(bonus)
                simpleDuration(Bonus.Money)
                beforeCardPlayed {
                    onEffect(
                        BiEffect.empty<Player, TriggerEvent>()
                        .chooseWhatToDo { _, event ->
                            InteractionRequest(
                                instruction = "do you want to play your ${event.scope}",
                                cards = listOf(scope),
                                buttons = Button.yesOrNo
                            )
                        }
                        .branch("y" to { player, event, _ -> player.playCard(event.scope) })
                        .end()
                    )
                    onCondition { event, player -> !event.isSamePlayer(player) && event.cardHasType(CardType.ATTACK) }
                }
            }
    }
    @Dominion_Card(extension = "Adventures")
    fun CoinOfTheRealm() = Card.treasure("Coin of the Realm", Price.adventure(2)).addType(CardType.RESERVE)
        .setup {
            simpleAction(Bonus.Money)
            afterCardPlayed {
                onEffect{owner, _ -> owner.increment(Item.ACTION, 2)}
                reserveCondition { event, _ -> event.cardHasType(CardType.RESERVE) }
            }
        }
    @Dominion_Card(extension = "Adventures", pileType = PileType.VICTORY)
    fun DistantLands() = Card("Distant Lands", Price.adventure(3), CardType.VICTORY, CardType.ACTION, CardType.RESERVE)
        .setup {  score { if(scope.hasForLocation(Destination.OtherZone.Tavern)) 4 else 0 }  }

    @Dominion_Card(extension = "Adventures")
    fun Dungeon() = Card.duration("Dungeon", Price.adventure(3)).addType(CardType.ACTION)
        .setup {
            onPlay(Bonus.Action.onPlay().runDungeonDrawAndDiscardPhase())
            onDuration { onEffect(BiEffect.empty<Player, Card>().runDungeonDrawAndDiscardPhase()) }
        }
    @Dominion_Card(extension = "Adventures")
    fun Duplicate() = Card.action("Duplicate", Price.adventure(3)).addType(CardType.RESERVE)
        .setup {
            afterGain {
                onEffect(BiEffect.empty<Player, Event>()
                    .lookingAt { _, event -> event.card }.filterNotNull()
                    .thenWith { player, card ->  player.gainFromSupply(card.name)}
                    .end()
                )
                reserveCondition { event, _ -> event.testCard { it isAtMost 6 } }
            }
        }
    @Dominion_Card(extension = "Adventures")
    fun Gear() : Card {
        val draw = Bonus.draw(2)
        return Card.duration("Gear", Price.adventure(3)).addType(CardType.ACTION)
            .setup {
                onPlay(draw.onPlay()
                    .then {player, self ->
                        player.move(
                            from = Destination.PlayerZone.Hand,
                            to = Destination.PlayerZone.Aside,
                            number = 2,
                            canPass = true,
                        ){addCardToShadowZone(self, it)}
                    }
                )
                onDuration {
                    onEffect(BiEffect.empty<Player, Card>()
                        .lookingAt { player, self ->  player.getShadowList(self)}
                        .thenWith { player, cards ->
                            player.moveAll(cards.toList(), Destination.PlayerZone.Hand)
                        }
                        .sendMessage(Player.clearShadowZone, {_, self, _ -> self})
                        .end()
                    )
                    shouldBeDiscardWhen { player, card -> player.getShadowList(card).isEmpty() }
                }
            }
    }
    @Dominion_Card(extension = "Adventures")
    fun Giant() : Card {
        val face_down = Bonus.money(1)
        val face_up = Bonus.money(5)
        return Card.attack("Giant", Price.adventure(5))
            .setup {
                onPlay(BiEffect.empty<Player, Card>()
                    .lookingAt { player, _ -> player.flipJourneyToken() }
                    .branchDecision {
                        on {data == JourneyFace.FACE_DOWN} then {player, self, _ -> player.triggerEffect(ACTION, self, face_down ) }
                        on {data == JourneyFace.FACE_UP} then {player, self, _ -> runGiantAttack(player, self, face_up )
                        }
                    }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = "Adventures")
    fun Guide() : Card {
        val bonus = Bonus.action().draw()
        return Card.action("Guide", Price.adventure(3)).addType(CardType.RESERVE)
            .setup {
                simpleAction(bonus)
                onStartTurn {
                    onEffect{player, _->
                        player.discardAll(Destination.PlayerZone.Hand)
                        player.draw(5)
                    }
                    reserveCondition()
                }
            }
    }
    @Dominion_Card(extension = "Adventures")
    fun HauntedWood() : Card {
        val draw = Bonus.draw(3)
        return Card.attack("Haunted Woods", Price.adventure(5)).addType(CardType.DURATION)
            .setup {
                simpleDuration(draw)
                onGain {
                    onEffect{_, event -> event.player.moveAllAndChooseTheOrder(Destination.PlayerZone.Hand, Destination.PlayerZone.Draw)}
                    onCondition { event, player ->  !event.isSamePlayer(player)
                            && event.player.isInBuyPhase
                            && activate(player, scope)
                            && event.isBuy }
                }
            }
    }
    @Dominion_Card(extension = "Adventures")
    fun Hireling() = Card.action("Hireling", Price.adventure(6)).addType(CardType.DURATION)
        .setup {
            simpleAction(Bonus.draw)
            onDuration {
                onEffect(Bonus.draw.onDuration())
                infinite()
            }
        }
    @Dominion_Card(extension = "Adventures")
    fun LostCity() : Card {
        val bonus = Bonus.action(2).draw(2)
        return Card.action("LostCity", Price.adventure(5))
            .setup {
                simpleAction(bonus)
                checkGain(BiEffect.empty<Event, Card>().benefit { it.draw() })
            }
    }
    @Dominion_Card(extension = "Adventures")
    fun Magpie() = Card.action("Magpie", Price.adventure(4))
        .setup {
            onPlay(
                Bonus.ActionAndDraw.onPlay()
                .lookingAt { player, _ -> player.getCardFromDeck() }.filterNotNull()
                .branchDecision {
                    on {data.hasType(CardType.TREASURE)} then {player, _, drawn -> player.moveTo(drawn, Destination.PlayerZone.Hand) }
                    on {data.testTypes { any(CardType.ACTION, CardType.VICTORY) }} then {player, _, _ -> player.gainFromSupply("Magpie")}
                }
                .end()
            )
        }
    @Dominion_Card(extension = "Adventures")
    fun Messenger() : Card {
        val bonus = Bonus.buy(2).with(Item.MONEY)
        return Card.action("Messenger", Price.adventure(4))
            .setup {
                onPlay(bonus.onPlay()
                    .chooseWhatToDo { _, card -> InteractionRequest(
                        instruction = "do you want to put your deck into your discard ?",
                        cards = listOf(card),
                        buttons = Button.yesOrNo
                    ) }
                    .branch("y" to {player, _, _ -> player.moveAll(Destination.PlayerZone.Draw, Destination.PlayerZone.Discard)})
                    .end()
                )
                checkGain {
                    onEffect(BiEffect.empty<Event, Card>()
                        .gainFromSupplyAndWith(
                            instruction = {"gain a card costing up to ${displayCoin(4)}"},
                            filter = {it isAtMost 4}
                        )
                        .filterNotNull()
                        .benefitOthers { _, opponent ->
                            opponent.gainFromSupply(this.name)
                        }
                        .end()
                    )
                    onCondition { event, player -> event.isBuy && player.getProperties(Properties.Cards_Bought).value == 0 }
                }
            }
    }
    @Dominion_Card(extension = "Adventures")
    fun Miser() = Card.action("Miser", Price.adventure(4))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .lookingAt { player, _ -> player.getList(Destination.OtherZone.Tavern).count { hasName("Copper") } }
                .chooseWhatToDo { player, card -> InteractionRequest(
                    instruction = "put a copper in your tavern or take ${displayCoin(1)} per copper in your tavern ( ${displayCoin(this)} )",
                    cards = listOf(card),
                    buttons = listOf(Button("Add a copper", "add" ), Button("Take ${displayCoin(this)}", "take"))
                ) }
                .branch(
                    "add" to {player, _, _ -> player.move(Destination.PlayerZone.Hand, Destination.OtherZone.Tavern, filter = {it.hasName("Copper")})},
                    "take" to {player, _, number -> player.increment(Item.MONEY, number)}
                )
                .end()
            )
        }
    @Dominion_Card(extension = "Adventures")
    fun Port() : Card {
        val bonus = Bonus.action(2).draw()
        return Card.action("Port", Price.adventure(4))
            .setup {
                simpleAction(bonus)
                checkGain {
                    onEffect(BiEffect.empty<Event, Card>()
                        .lookingAt { event, card ->
                            val f = event.player.getFlag(card.name)
                            f += true
                            f
                        }
                        .thenDo { event, card, flow ->
                            event.player.gainFromSupply(card.name)
                            flow += false
                        }
                        .end()
                    )
                    onCondition { _, player ->  !player.isFlagSet(scope.name) }
                }
            }
    }

    @Dominion_Card(extension = "Adventures")
    fun Ranger() = Card.action("Ranger", Price.adventure(4))
        .setup {
            onPlay(
                Bonus.Buy.onPlay()
                .filter { player, _ -> player.flipJourneyToken() == JourneyFace.FACE_UP }
                .draw(5)
                .end()
            )
        }

    @Dominion_Card(extension = "Adventures")
    fun Ratcatcher() = Card.action("Ratcatcher", Price.adventure(2)).addType(CardType.RESERVE)
        .setup {
            simpleAction(Bonus.ActionAndDraw)
            onStartTurn {
                onEffect{p, _ -> p.trash()}
                reserveCondition()
            }
        }

    @Dominion_Card(extension = "Adventures")
    fun Raze() = Card.action("Raze", Price.adventure(2))
        .setup {
            onPlay(
                Bonus.Action.onPlay()
                .chooseWhatToDo { player, card -> InteractionRequest(
                    instruction = "trash $card or a card from your hand",
                    cards = listOf(card),
                    buttons = listOf(Button(card.name, "self"), Button("a card from hand", "hand"))
                ) }
                .branch(
                    "self" to {player, card, _ -> runRazeReveals(player, card )},
                    "hand" to {player, _, _ -> player.chooseCardFromHand("$player, trash a card from your hand")
                        ?.let { runRazeReveals(player, it) }
                    }
                )
                .end()
            )
        }
    @Dominion_Card(extension = "Adventures")
    fun Relic() : Card {
        val money = Bonus.money(2)
        return Card.treasure("Relic", Price.adventure(5)).addType(CardType.ATTACK)
            .setup {
                onPlay(money.onPlay()
                    .attackOthers { _, opponent, _ ->
                        opponent.updateTokenFlag(Token.OnPlayer.MinusOneCardToken, true)
                    }
                )
            }
    }
    @Dominion_Card(extension = "Adventures")
    fun RoyalCarriage() = Card.action("Royal Carriage", Price.adventure(5)).addType(CardType.ACTION)
        .setup {
            simpleAction(Bonus.Action)
            afterCardPlayed {
                onEffect(BiEffect.empty<Player, Event>()
                    .lookingAt { _, event -> event.card }.filterNotNull()
                    .thenWith { player, card ->
                        card.play(player)
                        scope.follow(player, card)
                    }
                    .end()
                )
                reserveCondition { event, _ -> event.cardHasType(CardType.ACTION)
                        && event.isCardIn(Destination.PlayerZone.InPlay) }
            }
            follower()
        }

    @Dominion_Card(extension = "Adventures")
    fun Storyteller() = Card.action("Storyteller", Price.adventure(5))
        .setup {
            onPlay(
                Bonus.Action.onPlay()
                .loop(3){ counter ->
                    chooseCardFromHand { player, _ -> InteractionRequest(
                        instruction = "you may play again ${ 3- counter.current} treasure card from your hand",
                        filter = {it.hasType(CardType.TREASURE)},
                        canPass = true
                    ) }.thenWith { player, chosen ->  player.playCard(chosen)}
                }
                .then {
                    drawByAction { 1 + getValueOf(Item.MONEY) }
                    decrementByAction(Item.MONEY){getValueOf(Item.MONEY)}
                }

            )
        }
    @Dominion_Card(extension = "Adventures")
    fun SwampHag() : Card{
        val money = Bonus.money(3)
        return Card.attack("Swamp Hag", Price.adventure(5)).addType(CardType.DURATION)
            .setup {
                simpleDuration(money)
                onGain {
                    onEffect{_, event -> event.player.gainFromSupply("Curse")}
                    onCondition { event, player -> !event.isSamePlayer(player) && event.isBuy && activate(player, scope)  }
                }
            }
    }
    @Dominion_Card(extension = "Adventures")
    fun Transmogrify() = Card.action("Transmogrify", Price.adventure(4)).addType(CardType.RESERVE)
        .setup {
            simpleAction(Bonus.Action)
            onStartTurn(BiEffect.empty<Player, Unit>()
                .trashCardFromHand(canPass = true)
                .gainFromSupply(
                    instruction = {"gain a card costing up to ${pipeLineCard.costInstruction(1)}"},
                    filter = {it.isAtMostWithBonus(pipeLineCard, 1)},
                    destination = Destination.PlayerZone.Hand
                )
                .endParent()
            )
        }
    @Dominion_Card(extension = "Adventures")
    fun TreasureTrove() : Card{
        val money = Bonus.money(2)
        return Card.treasure("Treasure Trove", Price.adventure(5))
            .setup {
                onPlay(money.onPlay()
                    .gainSpecificCardFromSupply(cardName = "Gold")
                    .gainSpecificCardFromSupply(cardName = "Copper")
                )
            }
    }
    @Dominion_Card(extension = "Adventures")
    fun WineMerchant() : Card{
        val bonus = Bonus.money(4).with(Item.BUY)
        return Card.action("Wine Merchant", Price.adventure(5)).addType(CardType.RESERVE)
            .setup {
                simpleAction(bonus)
                onEndBuy {
                    onEffect{player, self -> player.discard(self)}
                    onCondition { _, player -> player.getValueOf(Item.MONEY) >= 2 }
                }
            }
    }

    private fun Traveller(cardName: String, price: Price, nextCard: String) =
        Card.action(cardName, price).addType(CardType.TRAVELLER)
            .setup {
                checkItselfDiscard {
                    onEffect(travellerUpgrade(nextCard))
                    onCondition { event, _ -> event.initialCameFrom(Destination.PlayerZone.InPlay)  }
                }
            }

    @Dominion_Card(extension = "Adventures")
    fun Page() = Traveller("Page", Price.adventure(4), "Treasure Hunter")
        .setup {
            simpleAction(Bonus.ActionAndDraw)
            onSetup {
                getTraveller("Page").forEach {
                    asideSupplyPiles.getOrPut("Page upgrade") { mutableListOf() }
                        .add(createSupplyPile(it))
                }
            }
        }
    @Dominion_Card(extension = "Adventures")
    fun TreasureHunter() : Card{
        val bonus = Bonus.action().with(Item.MONEY)
        return Traveller("Treasure Hunter", Price.adventure(3), "Warrior").addType(CardType.ASIDE)
            .setup {
                onPlay(bonus.onPlay()
                    .lookingAt { player, _ ->  player.game.onTheRight(player).cardGainedLastTurn.size }
                    .thenWith { player, i -> player.gainMultiplyCardFromSupply("Silver", Destination.PlayerZone.Discard, i) }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = "Adventures")
    fun Warrior() : Card {
        val draw = Bonus.draw(2)
        return Traveller("Warrior", Price.adventure(4), "Hero").addType(CardType.ASIDE)
            .setup {
                onPlay(draw.onPlay()
                    .lookingAt { player, _ -> player.getList(Destination.PlayerZone.InPlay) }
                    .filterListAndNotEmpty { it.hasType(CardType.TRAVELLER) }
                    .forEachCard(elements = {_, list -> list}){_, _ ->
                        attack { _, opponent, _ ->
                            val effect = BiEffect.empty<Player, Unit>()
                                .lookingAt { player, _ -> player.getCardFromDeck() }.filterNotNull()
                                .branchDecision {
                                    on {data.isBetween(3, 4)} then {player, _, top -> player.trash(top)}
                                    otherwise { player, _, card ->  player.discard(card) }
                                }
                                .end()

                            effect(opponent, Unit)
                        }
                    }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = "Adventures")
    fun Hero() : Card{
        val money = Bonus.money(2)
        return Traveller("Hero", Price.adventure(5), "Champion").addType(CardType.ASIDE)
            .setup {
                onPlay(money.onPlay()
                    .gainFromSupply(
                        instruction = {"gain a treasure from supply"},
                        filter = {it.hasType(CardType.TREASURE)}
                    )
                )
            }
    }
    @Dominion_Card(extension = "Adventures", pileType = PileType.UNIQUE)
    fun Champion() = Card("Champion", Price.adventure(6), CardType.ACTION, CardType.DURATION, CardType.TRAVELLER, CardType.ASIDE)
        .setup {
            onPlay(
                Bonus.Action.onPlay()
                .benefit { opp ->
                    val previous = opp.getList(Destination.PlayerZone.InPlay).filter { it.hasType(CardType.ATTACK) }
                    scope.getMutableCollection<Card>("notImmune").addAll(previous)
                })
            checkGain{_, self -> self.removeType(CardType.TRAVELLER)}
            beforeCardPlayed {
                onEffect{owner, _ -> owner.triggerEffect(EFFECT, scope, Bonus.Action)}
                onCondition { event, player ->  event.isSamePlayer(player) && event.cardHasType(CardType.ACTION) }
            }
            onDuration {
                onEffect{_, self -> self.clear()}
                infinite()
            }
            immunity{
                onEffect(object : TriggerComponent.Immunity {
                    override suspend fun isImmuneAgainst(self: Card, attack: Card?): Boolean {
                        return !self.getCollection<Card>("notImmune").contains(attack)
                    }
                })
            }

        }
    @Dominion_Card(extension = "Adventures")
    fun Peasant() : Card{
        val bonus = Bonus.buy().with(Item.MONEY)
        return Traveller("Peasant", Price.adventure(2), "Soldier")
            .setup {
                simpleAction(bonus)
                onSetup {
                    getTraveller("Peasant").forEach {
                        asideSupplyPiles.getOrPut("Peasant upgrade") { mutableListOf() }
                            .add(createSupplyPile(it))
                    }
                }
            }
    }
    @Dominion_Card(extension = "Adventures")
    fun Soldier() : Card{
        val money = Bonus.money(2)
        return Traveller("Soldier", Price.adventure(3), "Fugitive").addType(CardType.ASIDE, CardType.ATTACK)
            .setup {
                onPlay(money.onPlay()
                    .then { incrementByAction(Item.MONEY){
                        getList(Destination.PlayerZone.InPlay).count { hasType(CardType.ATTACK) } }
                    }
                    .attackOthers { _, opponent, _ ->
                        if(opponent.sizeOf(Destination.PlayerZone.Hand) >= 4) opponent.discardFromHand()
                    }
                )
            }
    }
    @Dominion_Card(extension = "Adventures")
    fun Fugitive() : Card{
        val bonus = Bonus.action().draw(2)
        return Traveller("Fugitive", Price.adventure(4), "Disciple").addType(CardType.ASIDE)
            .setup { onPlay(bonus.onPlay() then {discardFromHand()}) }
    }
    @Dominion_Card(extension = "Adventures")
    fun Disciple() = Traveller("Disciple", Price.adventure(4), "Teacher").addType(CardType.ASIDE)
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .chooseCardFromHand { player, _ -> InteractionRequest(
                    instruction = "$player, you may play an action card twice",
                    filter = { it.hasType(CardType.ACTION) }
                ) }
                .thenWith{ player, self,  card -> player.playCard(card, 2); self.follow(player, card) }
                .end()
            )
            follower()
        }

    private val TeacherTokens: Set<Token.OnPile> = setOf(
        Token.OnPile.OneMoneyToken,
        Token.OnPile.OneActionToken,
        Token.OnPile.OneCardToken,
        Token.OnPile.OneBuyToken
    )
    @Dominion_Card(extension = "Adventures", pileType = PileType.UNIQUE)
    fun Teacher() = Card("Teacher", Price.adventure(6), CardType.ACTION, CardType.RESERVE, CardType.TRAVELLER, CardType.ASIDE)
        .setup {
            checkGain{_, self -> self.removeType(CardType.TRAVELLER)}
            onStartTurn{player, _ -> player.chooseToken(
                instruction = "move one of your +Token bonus on an action supply",
                filter = {t : Token.OnPile -> t in TeacherTokens && player.getToken(t).isEmpty()},
            )}
        }

    private fun travellerUpgrade(nextCard : String,  pile : String = if(page.contains(nextCard)) "Page upgrade" else "Peasant upgrade") : BiEffect<Event, Card>{
        return BiEffect.empty<Event, Card>()
            .lookingAt { event, _ -> event.player.game.getAvailableAsideCard(nextCard, pile)  }
            .filterNotNull()
            .chooseWhatToDo { event, card -> InteractionRequest(
                instruction = "do you want upgrade $card to $this ?",
                cards = listOf(card, this),
                buttons = Button.yesOrNo
            ) }
            .branch(
                "y" to {event, card, next ->
                    card.replaceInSupply()
                    if(card.hasForLocation(Destination.Supply)) event.player.moveTo(next, Destination.PlayerZone.Discard)
                }
            )
            .end()
    }

    private val page: Set<String> = setOf("Treasure Hunter", "Warrior", "Hero", "Champion")
    private val peasant: Set<String> = setOf("Soldier", "Fugitive", "Disciple", "Teacher")

    fun getTraveller(namePile : String) : Set<String>{
        return when(namePile){
            "Page" -> page
            "Peasant" -> peasant
            else -> emptySet()
        }
    }


//region EVENT SECTION
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    fun Alms() = Card.event("Alms", Price.adventure(0))
        .setup {
            checkItselfBuy {
                onEffect(BiEffect.empty<Event, Card>()
                    .gainFromSupply(
                        instruction = {"gain a card costing up to ${displayCoin(4)}"},
                        filter = { it isAtMost 4}
                    )
                    .then {event, self -> event.player.getFlag(self.name) += true }
                )
                onCondition { _, player -> player.getList(Destination.PlayerZone.InPlay).none{it.hasType(CardType.TREASURE)} }
            }
            available { !it.isUsed(scope) }
        }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    fun Ball() = Card.event("Ball", Price.adventure(5))
        .setup {
            checkItselfBuy(BiEffect.empty<Event, Card>()
                .then {
                    player.updateTokenFlag(Token.OnPlayer.TaxToken, true)
                    player.gainMultipleCardFromSupply("gain 2 cards costing up to ${displayCoin(4)}", {it isAtMost 4}, Destination.PlayerZone.Discard, 2)
                }
            )
        }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    fun Bonfire() = Card.event("Bonfire", Price.adventure(3))
        .setup {
            checkItselfBuy(BiEffect.empty<Event, Card>()
                .then { player.trashWithCondition(
                    number = 2,
                    from = Destination.PlayerZone.InPlay,
                    filter = {it.hasName("Copper")}
                ) }
            )
        }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    fun Borrow() = Card.event("Borrow", Price.adventure(0))
        .setup {
            checkItselfBuy(
                Bonus.Buy.onBuy()
                .setFlag(scope.name)
                .lookingAt { event, _ -> event.player.getTokenFlag(Token.OnPlayer.MinusOneCardToken) }
                .filterNotNull {flow -> ! flow.value }
                .draw()
                .thenDo {flow ->  flow += true }
                .end()
            )
            available { !it.isUsed(scope) }
        }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    fun Expedition() = Card.event("Expedition", Price.adventure(3))
        .setup { checkItselfBuy{event, _ -> event.player.updateDrawBonusValue(2)} }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    fun Ferry() = Card.event("Ferry", Price.adventure(3))
        .setup {
            checkItselfBuy{event, _ ->
                event.player.chooseToken(
                    instruction = "put your ${displayCoin(-2)} reduction on an action pile",
                    filter = {it == Token.OnPile.CardReductionToken}
                )
            }
        }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    fun Inheritance() = Card.event("Inheritance", Price.adventure(7))
        .setup {
            checkItselfBuy(BiEffect.empty<Event, Card>()
                .chooseCardFromSupply { event, _ -> InteractionRequest(
                    instruction = "set aside a non Duration, non Command action card from supply costing up to ${displayCoin(4)} ",
                    filter = {
                        it isAtMost 4 && it.testTypes {
                            any(CardType.ACTION)
                            none(CardType.DURATION, CardType.COMMAND) }
                    }
                ) }
                .thenWith { event, self,  card ->
                    event.player.moveTo(card, Destination.PlayerZone.Aside)
                    card.set("unable", true)
                    event.player.getPersistentFlag(self.name) += true
                    event.player.setToken(Token.OnPile.EstateToken, card.name)
                }
                .end()
            )
            available { !it.getPersistentFlag(scope.name).value }
        }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    fun LostArts() = Card.event("Lost Arts", Price.adventure(6))
        .setup {
            checkItselfBuy{event, _ ->
                event.player.chooseToken(
                    instruction = "put your +1 action to an action pile",
                    filter = {it == Token.OnPile.OneActionToken}
                )
            }
        }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    fun Mission() = Card.event("Mission", Price.adventure(4))
        .setup {
            checkItselfBuy{event, _ ->
                event.player.getPersistentFlag(Flags.expedition) += true
                event.player.addNextTurnEffect { getFlag(Flags.expedition) += true }}
        }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    fun Pathfinding() = Card.event("Pathfinding", Price.adventure(8))
        .setup {
            checkItselfBuy{event, _ ->
                event.player.chooseToken(
                    instruction = "put your +1 card to an action pile",
                    filter = {it == Token.OnPile.OneCardToken}
                )
            }
        }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    fun Pilgrimage() = Card.event("Pilgrimage", Price.adventure(4))
        .setup {
            checkItselfBuy(BiEffect.empty<Event, Card>()
                .lookingAt { event, _ -> event.player.flipJourneyToken() }
                .filter { it == JourneyFace.FACE_UP }
                .loop(3){ _, counter ->
                    lookingAt { event, _ -> event.player.getDistinctCards(Destination.PlayerZone.InPlay) }
                        .filterListAndNotEmpty()
                        .chooseCardFromList { event, _ -> InteractionRequest(
                            instruction = "you may copy again ${if(this.size > 3) (3-counter.current) else this.size } card(s) from your play zone",
                            cards = this,
                            canPass = true
                        ) }
                        .thenDo { event, self, playZone, card ->  event.player.addCardToShadowZone(self, card){playZone.remove(it)}  }
                }
                .so { event, self -> event.player.getShadowList(self).forEach { event.player.gainFromSupply(it.name) } }
                .sendMessage(Player.clearShadowZone){_, self, _ -> self}
                .end()
            )
        }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    fun Plan() = Card.event("Plan", Price.adventure(3))
        .setup {
            checkItselfBuy{event, _ ->
                event.player.chooseToken(
                    instruction = "put your trash token to an action pile",
                    filter = {it == Token.OnPile.TrashingToken}
                )
            }
        }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    fun Quest() = Card.event("Quest", Price.adventure(0))
        .setup {
            checkItselfBuy(BiEffect.empty<Event, Card>()
                .chooseWhatToDo { event, card -> InteractionRequest(
                    instruction = "choose: you may discard an attack card, 2 curses or 6 cards from your hand to gain a gold",
                    cards = event.player.getList(Destination.PlayerZone.Hand),
                    buttons = listOf(Button("attack card", "attack"), Button("2 curses", "curses"), Button("6 cards", "cards")),
                    canPass = true
                ) }
                .branch(
                    "attack" to {event, _, _ ->
                        event.player.discardFromHand(
                            instruction = "attack",
                            filter = {it.hasType(CardType.ATTACK)},
                            nextAction = {if(it == 1) gainFromSupply("Gold")}
                        )
                    },
                    "curses" to {event, _, _ ->
                        event.player.discardFromHand(
                            number = 2,
                            instruction = "curse",
                            filter = {it.hasType(CardType.CURSE)},
                            nextAction = {if(it == 2) gainFromSupply("Gold")}
                        )
                    },
                    "cards" to {event, _, _ ->
                        event.player.discardFromHand(
                            number = 6,
                            nextAction = {if(it == 6) gainFromSupply("Gold")}
                        )
                    }
                )
                .end()
            )
        }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    fun Raid() = Card.event("Raid", Price.adventure(5))
        .setup {
            checkItselfBuy(BiEffect.empty<Event, Card>()
                .then { player.gainMultiplyCardFromSupply("Silver", Destination.PlayerZone.Discard , player.getList(Destination.PlayerZone.InPlay).count { hasName("Silver") }) }
                .attackOthers { _, opponent, _ -> opponent.updateTokenFlag(Token.OnPlayer.MinusOneCardToken, true) }
            )
        }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    fun Save() = Card.event("Save", Price.adventure(1))
        .setup {
            checkItselfBuy {
                onEffect(Bonus.Buy.onBuy()
                    .setFlag(scope.name)
                    .chooseCardFromHand { _, _ -> InteractionRequest(
                        instruction = "put aside a card from your hand"
                    ) }
                    .thenWith { event, card ->
                        val player = event.player
                        player.moveTo(card, Destination.PlayerZone.Aside)
                        player.addEndTurnEffect { if(card.hasForLocation(Destination.PlayerZone.Aside)) moveTo(card, Destination.PlayerZone.Hand) }
                    }
                    .end()
                )
                onCondition { _, player -> !player.isUsed(scope)  }
            }
        }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    fun ScoutingParty() = Card.event("Scouting Party", Price.adventure(2))
        .setup {
            checkItselfBuy(
                Bonus.Buy.onBuy()
                .lookingAt { event, _ -> event.player.getTopCards(5)  }
                .listIsNotEmpty()
                .thenWith { event, cards ->
                    event.player.discardAList(cards, 3)
                    event.player.moveAllAndChooseTheOrder(cards, Destination.TempZone.Temp, Destination.PlayerZone.Draw)
                }
                .end()
            )
        }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    fun Seaway() = Card.event("Seaway", Price.adventure(5))
        .setup {
            checkItselfBuy(BiEffect.empty<Event, Card>()
                .gainFromSupplyAndWith(
                    instruction = {"gain an action card costing up to ${displayCoin(4)}" },
                    filter = {it isAtMost 4 && it.hasType(CardType.ACTION)}
                ).filterNotNull()
                .thenWith { event, card -> event.player.setToken(Token.OnPile.OneBuyToken, card.name) }
                .end()
            )
        }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    fun Trade() = Card.event("Trade", Price.adventure(5))
        .setup {
            checkItselfBuy(BiEffect.empty<Event, Card>()
                .then {
                    player.trashAndEffect(
                        extraInstruction = "to gain silver per card trashed",
                        number = 2,
                        from = Destination.PlayerZone.Hand,
                    ){gainFromSupply("Silver")}
                }
            )
        }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    fun Training() = Card.event("Training", Price.adventure(6))
        .setup {
            checkItselfBuy{event, _ -> event.player.chooseToken(
                instruction = "move your +1 action token to an action pile",
                filter = {it == Token.OnPile.OneActionToken}
            ) }
        }
    @Dominion_Card(extension = "Adventures", pileType = PileType.EVENT)
    fun TravellingFair() = Card.event("Travelling Fair", Price.adventure(2))
        .setup {
            checkItselfBuy{event, self -> event.player.addCardEffect(self) }
            onSideEffectGain(GainType.DURING){
                onEffect(BiEffect.empty<Player, Event>()
                    .lookingAt { _, event -> event.card  }.filterNotNull()
                    .chooseWhatToDo { _, event -> InteractionRequest(
                        instruction = "${event.player}, do you want to put $this on top of your deck ?",
                        cards = listOf(this),
                        buttons = Button.yesOrNo
                    ) }
                    .branch("y" to {_, event, _ -> event.updateDest(Destination.PlayerZone.Draw)})
                    .end()
                )
                onCondition { event, player -> event.isSamePlayer(player) && event.notMoved && event.isSameCard }
            }
        }
//endregion



}