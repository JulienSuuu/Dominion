package fr.umontpellier.iut.dominion.cards.factories.Dark_Ages

import fr.umontpellier.iut.dominion.Annotation.Dominion_Card
import fr.umontpellier.iut.dominion.Annotation.PileType
import fr.umontpellier.iut.dominion.Button
import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Flags
import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.PlayerComponent.cardGainedCurrentTurn
import fr.umontpellier.iut.dominion.Player.Skills.choose
import fr.umontpellier.iut.dominion.Player.Skills.discard
import fr.umontpellier.iut.dominion.Player.Skills.discardFromHand
import fr.umontpellier.iut.dominion.Player.Skills.discardList
import fr.umontpellier.iut.dominion.Player.Skills.discardUntil
import fr.umontpellier.iut.dominion.Player.Skills.discardUntilYouStop
import fr.umontpellier.iut.dominion.Player.Skills.discardUntilYouStopAndDo
import fr.umontpellier.iut.dominion.Player.Skills.draw
import fr.umontpellier.iut.dominion.Player.Skills.drawByAction
import fr.umontpellier.iut.dominion.Player.Skills.move
import fr.umontpellier.iut.dominion.Player.Skills.moveAll
import fr.umontpellier.iut.dominion.Player.Skills.moveAllAndChooseTheOrder
import fr.umontpellier.iut.dominion.Player.Skills.moveList
import fr.umontpellier.iut.dominion.Player.Skills.moveTo
import fr.umontpellier.iut.dominion.Player.Skills.playCard
import fr.umontpellier.iut.dominion.Player.Skills.reveals
import fr.umontpellier.iut.dominion.Player.Skills.revealsIf
import fr.umontpellier.iut.dominion.Player.Skills.trash
import fr.umontpellier.iut.dominion.Player.Skills.trashAll
import fr.umontpellier.iut.dominion.Player.Skills.trashAndDo
import fr.umontpellier.iut.dominion.Player.Skills.trashAndEffect
import fr.umontpellier.iut.dominion.cards.Bonus.Bonus
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.Events.Event
import fr.umontpellier.iut.dominion.cards.builders.PipelineState
import fr.umontpellier.iut.dominion.cards.builders.attack
import fr.umontpellier.iut.dominion.cards.builders.branchDecision
import fr.umontpellier.iut.dominion.cards.builders.filterNotNull
import fr.umontpellier.iut.dominion.cards.builders.filterNotNullWithContext
import fr.umontpellier.iut.dominion.cards.builders.listIsNotEmpty
import fr.umontpellier.iut.dominion.cards.builders.matchAll
import fr.umontpellier.iut.dominion.cards.builders.revealCard
import fr.umontpellier.iut.dominion.cards.builders.revealList
import fr.umontpellier.iut.dominion.cards.component.BiEffect
import fr.umontpellier.iut.dominion.cards.component.InteractionRequest
import fr.umontpellier.iut.dominion.cards.component.Price
import fr.umontpellier.iut.dominion.cards.component.attackOthers
import fr.umontpellier.iut.dominion.cards.component.chooseCardFromHand
import fr.umontpellier.iut.dominion.cards.component.chooseCardFromList
import fr.umontpellier.iut.dominion.cards.component.chooseCardFromSupply
import fr.umontpellier.iut.dominion.cards.component.chooseWhatToDo
import fr.umontpellier.iut.dominion.cards.component.filter
import fr.umontpellier.iut.dominion.cards.component.gainFromSupply
import fr.umontpellier.iut.dominion.cards.component.lookingAt
import fr.umontpellier.iut.dominion.cards.component.processHandDown
import fr.umontpellier.iut.dominion.cards.component.reveal
import fr.umontpellier.iut.dominion.cards.component.then
import fr.umontpellier.iut.dominion.cards.component.trashCardFromHand
import fr.umontpellier.iut.dominion.cards.count
import fr.umontpellier.iut.dominion.cards.factories.ACTION
import fr.umontpellier.iut.dominion.cards.factories.DA
import fr.umontpellier.iut.dominion.cards.factories.createMixedSupplyPile
import fr.umontpellier.iut.dominion.cards.factories.follow
import fr.umontpellier.iut.dominion.cards.gainFromSupply
import fr.umontpellier.iut.dominion.cards.gainFromTrash
import fr.umontpellier.iut.dominion.cards.gainMultiplyCardFromSupply
import fr.umontpellier.iut.dominion.cards.gainSpecificAsideCard
import fr.umontpellier.iut.dominion.cards.getTopCards
import fr.umontpellier.iut.dominion.cards.plusAssign
import fr.umontpellier.iut.dominion.cards.testTypes
import fr.umontpellier.iut.dominion.cards.triggerEffect

object Dark_AgesFactoryKt {
    const val NAME = "Dark Ages"

    @Dominion_Card(extension = DA)
    fun Altar() = Card.action("Altar", Price.darkAges(5))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .trashCardFromHand().endParent()
                .gainFromSupply(instruction = {"$player, gain a card costing up to 5$"}, filter = {it isAtMost 5})
            )
        }
    @Dominion_Card(extension = DA)
    fun Armory() = Card.action("Armory", Price.darkAges(4))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .gainFromSupply(
                    instruction = {"$player, gain a card costing up to 4$"},
                    filter = {it isAtMost 4},
                    destination = Destination.PlayerZone.Draw
                )
            )
        }
    @Dominion_Card(extension = DA)
    fun BandOfMisfits() = Card.action("Band Of Misfits", Price.darkAges(5)).addType(CardType.COMMAND)
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .chooseCardFromSupply{player, self -> InteractionRequest(
                    instruction = "$player, play a non Duration, non Command action card from the supply costing less than ${self.costInstruction()}",
                    filter = { it.isLessThanWithBonus(self)
                            && it.testTypes {
                                any(CardType.ACTION)
                                none(CardType.COMMAND, CardType.DURATION)
                            }
                    }
                ) }
                .thenWith { player, self,  card ->
                    val flowMisfit = player.getFlag(Flags.resolveBandOfMisfit)
                    flowMisfit += true
                    card.set("cant", true)

                    card.play(player)
                    self.follow(player, card)

                    flowMisfit += false
                    card.set("cant", false)
                }
                .end()
            )
            follower()
        }
    @Dominion_Card(extension = DA)
    fun BanditCamp() : Card{
        val bonus = Bonus.action(2).draw()
        return Card.action("Bandit Camp", Price.darkAges(5))
            .setup { onPlay(bonus.onPlay() then { gainSpecificAsideCard("Spoils", this@Dark_AgesFactoryKt.NAME) }) }
    }
    @Dominion_Card(extension = DA)
    fun Beggar() = Card.action("Beggar", Price.darkAges(2)).addType(CardType.REACTION)
        .setup {
            onPlay(BiEffect.empty<Player, Card>() then { gainMultiplyCardFromSupply("Copper", Destination.PlayerZone.Hand, 3) })
            beforeCardPlayed {
                onEffect(BiEffect.empty<Player, Event>()
                    .chooseWhatToDo { player, _ -> InteractionRequest(
                        instruction = "$player, Do you want to discard $scope ?",
                        cards = listOf(scope),
                        buttons = Button.yesOrNo
                    ) }
                    .branch(
                        "y" to {player, _, _ ->
                            if(player.discard(scope)){
                                player.gainFromSupply("Silver", Destination.PlayerZone.Draw)
                                player.gainFromSupply("Silver")
                            }
                        }
                    )
                    .end()
                )
                onCondition { event, player -> !event.isSamePlayer(player) && event.cardHasType(CardType.ATTACK) }
            }
        }
    @Dominion_Card(extension = DA)
    fun Catacombs() = Card.action("Catacombs", Price.darkAges(5))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .lookingAt { player, _ -> player.getTopCards(3)  }.listIsNotEmpty()
                .chooseWhatToDo { player, _ -> InteractionRequest(
                    instruction = "$player, Choose : the ${this.size} card(s) in your hand or discard them for 3 cards",
                    cards = this,
                    buttons = listOf(Button.Hand, Button.Discard)
                ) }
                .branch(
                    "h" to {player, _, cards -> player.moveAll(cards, Destination.PlayerZone.Hand)},
                    "d" to {player, _, cards -> player.discardList(cards); player.draw(3)}
                )
                .end()
            )
            checkItselfTrash {
                onEffect(BiEffect.empty<Event, Card>()
                    .gainFromSupply(
                        instruction = {"$player, gain a card costing less than $pipeLineCard"},
                        filter = {it.isLessThanWithBonus(pipeLineCard)}
                    )
                )
            }
        }
    @Dominion_Card(extension = DA)
    fun Count() = Card.action("Count", Price.darkAges(5))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .chooseWhatToDo { player, card -> InteractionRequest(
                    instruction = "$player, Choose: 1 option",
                    cards = listOf(card),
                    buttons = mutableListOf<Button>().apply {
                        add(Button("Discard ${if(player.getList(Destination.PlayerZone.Hand).size >= 2) "2" else "${player.getList(Destination.PlayerZone.Hand).size}"} cards", "d"))
                        add(Button("put a card onto draw", "ctd"))
                        add(Button("gain a copper", "c"))
                    }
                )}
                .branch(
                    "d" to {player, _, _ -> player.discardFromHand(2)},
                    "ctd" to {player, _, _ -> player.move(Destination.PlayerZone.Hand, Destination.PlayerZone.Draw)},
                    "c" to {player, _, _ -> player.gainFromSupply("Copper")},
                ).end()
                .chooseWhatToDo { player, card ->  InteractionRequest(
                    instruction = "$player, Choose: 1 option",
                    cards = listOf(card),
                    buttons = mutableListOf<Button>().apply {
                        add(Button("3$", "m"))
                        add(Button("trash your hand", "t"))
                        add(Button("gain a duchy", "d"))
                    }
                ) }
                .branch(
                    "m" to {player, _, _ -> player.increment(Item.MONEY, 3)},
                    "t" to {player, _, _ -> player.trashAll(Destination.PlayerZone.Hand)},
                    "d" to {player, _, _ -> player.gainFromSupply("Duchy")},
                )
                .end()
            )
        }

    @Dominion_Card(extension = DA)
    fun Counterfeit() : Card{
        val bonus = Bonus.buy().with(Item.MONEY)
        return Card.treasure("Counterfeit", Price.darkAges(5))
            .setup {
                onPlay(bonus.onPlay()
                    .chooseCardFromHand{player, _ -> InteractionRequest(
                        instruction = "$player, you may play twice a non duration treasure from your hand and trash it afterward",
                        filter = {it.testTypes {
                            any(CardType.TREASURE)
                            none(CardType.DURATION)
                        }},
                        canPass = true
                    )}
                    .thenWith { player, card ->
                        player.playCard(card, 2)
                        player.trash(card)
                    }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = DA)
    fun Cultist(): Card{
        val draw = Bonus.draw(2)
        return Card.attack("Cultist", Price.darkAges(5)).addType(CardType.LOOTER)
            .setup {
                onPlay(draw.onPlay()
                    .attackOthers { _, opponent, _ -> opponent.gainFromSupply("Ruins")  }
                    .chooseCardFromHand { player, card -> InteractionRequest(
                        instruction = "$player, you may play a cultist from your hand",
                        filter = {it.hasSameNameAs(card)},
                        canPass = true
                    ) }
                    .thenWith { player, card ->  player.playCard(card) }
                    .end()
                )
                checkItselfTrash { onEffect{event, _ -> event.player.draw(3)} }
            }
    }
    @Dominion_Card(extension = DA)
    fun DeathCart() = Card.action("Death Cart", Price.darkAges(4)).addType(CardType.LOOTER)
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .chooseWhatToDo { player, card -> InteractionRequest(
                    instruction = "$player, you may trash $card or a card from your hand",
                    cards = listOf(card),
                    buttons = listOf(Button("$card", "self" ), Button("trash from hand", "hand")),
                    canPass = true
                ) }
                .branch(
                    "self" to {player, card, _ -> if(player.trash(card)) player.increment(Item.MONEY, 5) },
                    "hand" to {player, _, _ -> player.trashAndEffect(
                        from = Destination.PlayerZone.Hand,
                        canPass = false,
                        action = { player.increment(Item.MONEY, 5) }
                    )}
                )
                .end()
            )
            checkGain { onEffect{ event, _ -> event.player.gainMultiplyCardFromSupply("Ruins", Destination.PlayerZone.Discard, 2)} }
        }

    @Dominion_Card(extension = DA, pileType = PileType.VICTORY)
    fun Feodum() = Card.victory("Feodum", Price.darkAges(4))
        .setup {
            score { it.allOwnedCards.count { c -> c.hasName("Silver") } / 3 }
            checkItselfTrash { onEffect{ event, _ -> event.player.gainMultiplyCardFromSupply("Silver", Destination.PlayerZone.Discard, 3)} }
        }
    @Dominion_Card(extension = DA)
    fun Forager() : Card {
        val bonus = Bonus.action().with(Item.BUY)
        return Card.action("Forager", Price.darkAges(5))
            .setup {
                onPlay(bonus.onPlay()
                    .trashCardFromHand().endParent()
                    .then { incrementByAction(Item.MONEY){getDistinctTrashCards().count { hasType(CardType.TREASURE) }} }
                )
            }
    }
    @Dominion_Card(extension = DA)
    fun Fortress() : Card {
        val bonus = Bonus.action(2).draw()
        return Card.action("Fortress", Price.darkAges(4))
            .setup {
                simpleAction(bonus)
                checkItselfTrash { onEffect{event, _ -> event.destination = Destination.PlayerZone.Hand} }
            }
    }
    @Dominion_Card(extension = DA)
    fun Graverobber() = Card.action("Graverobber", Price.darkAges(5))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .chooseWhatToDo { player, card -> InteractionRequest(
                    instruction = "$player, Choose : Gain a card from trash or trash a card from your hand",
                    cards = listOf(card),
                    buttons = listOf(Button("Gain a card from trash", "gain"), Button.Trash)
                ) }
                .branch(
                    "gain" to {player, _, _ -> player.gainFromTrash(
                        instruction =  "$player, gain a card from trash costing between 3 and 6",
                        filter = { it.isBetween(3, 6)},
                        dest = Destination.PlayerZone.Draw
                    )?.let { player.reveals(it) } },

                    "t" to {player, _, _ ->
                        player.trashAndEffect(
                            from = Destination.PlayerZone.Hand,
                            canPass = false)
                        { gainFromSupply(
                            instruction = "$this, gain a card costing up to ${it.costInstruction(3)}",
                            filter = { card ->  card.isAtMostWithBonus(it, 3)},
                            dest = Destination.PlayerZone.Discard
                        ) }
                    }
                )
                .end()
            )
        }
    @Dominion_Card(extension = DA)
    fun Hermit() = Card.action("Hermit", Price.darkAges(3))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .chooseCardFromList { player, _ -> InteractionRequest(
                    instruction = "$player, you may trash a non treasure card from your discard or from your hand",
                    cards = player.getList(Destination.PlayerZone.Discard) + player.getList(Destination.PlayerZone.Hand),
                    filter = {!it.hasType(CardType.TREASURE)},
                    canPass = true
                ) }
                .thenWith { player, card -> player.trash(card) }.end()
                .gainFromSupply(
                    instruction = {"$player, you may gain a card costing up to 3$"},
                    filter = { it isAtMost 3},
                    canPass = true
                )
            )
            onEndBuy {
                onEffect(BiEffect.empty<Player, Card>()
                    .filter { player, _ -> player.cardGainedCurrentTurn.isEmpty()  }
                    .map { player, _, _ -> player.game.getAvailableAsideCard("Madman", this@Dark_AgesFactoryKt.NAME) }
                    .filterNotNullWithContext { _, self, _ -> self.replaceInSupply()  }
                    .thenWith { player, card -> player.moveTo(card, Destination.PlayerZone.Discard) }
                    .end()
                )
            }
        }
    @Dominion_Card(extension = DA)
    fun HuntingGrounds() : Card {
        val draw = Bonus.draw(4)
        return Card.action("Hunting Grounds", Price.darkAges(6))
            .setup {
                simpleAction(draw)
                checkItselfTrash {
                    onEffect(BiEffect.empty<Event, Card>()
                        .chooseWhatToDo { event, card -> InteractionRequest(
                            instruction = "${event.player}, Choose: gain a duchy or 3 estate",
                            cards = listOf(card),
                            buttons = listOf(Button("duchy", "duchy"), Button("3 estate", "estate"))
                        ) }
                        .branch(
                            "duchy" to {event, _, _ -> event.player.gainMultiplyCardFromSupply("Duchy", Destination.PlayerZone.Discard, 1) },
                            "estate" to {event, _, _ -> event.player.gainMultiplyCardFromSupply("Estate", Destination.PlayerZone.Discard, 3) }
                        )
                        .end()
                    )
                }
            }
    }
    @Dominion_Card(extension = DA)
    fun Ironmonger() : Card {
        val bonus = Bonus.action().draw()
        return Card.action("Ironmonger", Price.darkAges(4))
            .setup {
                onPlay(bonus.onPlay()
                    .lookingAt { player, _ -> player.getCardFromDeck() }.filterNotNull()
                    .revealCard()
                    .chooseWhatToDo { player, _ -> InteractionRequest(
                        instruction = "$player, Do you want to discard $this ?",
                        cards = listOf(this),
                        buttons = Button.yesOrNo
                    ) }
                    .matchAll {
                        on {"y" == choice} then { player, _, card -> player.discard(card)}
                        on {source.hasType(CardType.ACTION)} then { player, _, _ -> player.increment(Item.ACTION)}
                        on {source.hasType(CardType.TREASURE)} then { player, _, _ -> player.increment(Item.MONEY)}
                        on {source.hasType(CardType.VICTORY)} then { player, _, _ -> player.draw()}
                    }
                    .endParent()
                )
            }
    }
    @Dominion_Card(extension = DA)
    fun JunkDealer() : Card{
        val bonus = Bonus.action().with(Item.MONEY).draw()
        return Card.action("Junk Dealer", Price.darkAges(5))
            .setup { onPlay(bonus.onPlay().trashCardFromHand().endParent()) }
    }
    @Dominion_Card(extension = DA)
    fun Marauder() = Card.attack("Marauder", Price.darkAges(4)).addType(CardType.LOOTER)
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .then { gainSpecificAsideCard("Spoils", this@Dark_AgesFactoryKt.NAME) }
                .attackOthers { _, opponent, _ -> opponent.gainFromSupply("Ruins") }

            )
        }
    @Dominion_Card(extension = DA)
    fun MarketSquare() : Card {
        val bonus = Bonus.action().with(Item.BUY).draw()
        return Card.action("Market Square", Price.darkAges(3)).addType(CardType.REACTION)
            .setup {
                simpleAction(bonus)
                onCardTrash {
                    onEffect(BiEffect.empty<Event, Card>()
                        .chooseWhatToDo { event, card -> InteractionRequest(
                            instruction = "${event.player}, Do you want to discard $this to gain a gold ?",
                            cards = listOf(card),
                            buttons = Button.yesOrNo
                        )}
                        .branch("y" to {event, card, _ -> event.player.discard(card){ gainFromSupply("Gold") } })
                        .end()
                    )
                    onCondition { event, _ -> !event.cameFrom(Destination.Supply)  }
                }
            }
    }
    @Dominion_Card(extension = DA)
    fun Mystic() : Card {
        val bonus = Bonus.action().with(Item.MONEY, 2)
        return Card.action("Mystic", Price.darkAges(5))
            .setup {
                onPlay(bonus.onPlay()
                    .lookingAt { player, _ -> player.getCardFromDeck() }.filterNotNull()
                    .map { player, _, top -> PipelineState(top, player.choose("$player, name a card", false), Unit)}
                    .thenWith { player, state -> player.reveals(state.source) }
                    .branchDecision {
                        on { data.source.hasName(data.current) } then {player, _, state -> player.moveTo(state.source, Destination.PlayerZone.Hand ) }
                    }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = DA)
    fun Pillage() = Card.attack("Pillage", Price.darkAges(5))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .chooseWhatToDo { player, card -> InteractionRequest(
                    instruction = "$player, do you want to trash $card ?",
                    cards = listOf(card),
                    buttons = Button.yesOrNo
                )  }
                .filter { it == "y" }
                .filterDo { player, card, _, _ -> player.trash(card) }
                .so { player -> player.gainSpecificAsideCard("Spoils", this@Dark_AgesFactoryKt.NAME, number = 2) }
                .attack { attacker, opponent, _ ->
                    val effect = BiEffect.empty<Player, Player>()
                        .lookingAt { opp, _ -> opp.getList(Destination.PlayerZone.Hand) }
                        .filter { it.size >= 5 }
                        .revealList()
                        .chooseCardFromList({_, att -> att}) { opp, att -> InteractionRequest(
                            instruction = "$att, discard a card from $opp hand",
                            cards = this
                        ) }
                        .thenWith { opp, chosen -> opp.discard(chosen) }
                        .end()

                    effect(opponent, attacker)
                }
                .end()
            )
        }
    @Dominion_Card(extension = DA)
    fun PoorHouse() : Card {
        val money = Bonus.money(4)
        return Card.action("Poor House", Price.darkAges(1))
            .setup {
                onPlay(money.onPlay()
                    .reveal { getList(Destination.PlayerZone.Hand) }
                    .then { decrementByAction(Item.MONEY){
                        minOf(this.money, getList(Destination.PlayerZone.Hand).count { hasType(CardType.TREASURE) })
                    } }

                )
            }
    }
    @Dominion_Card(extension = DA)
    fun Procession() = Card.action("Procession", Price.darkAges(4))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .chooseCardFromHand { player, _ -> InteractionRequest(
                    instruction = "$player, you may play a non duration action card from your hand twice and trash it",
                    filter = {it.testTypes {
                        any(CardType.ACTION)
                        none(CardType.DURATION)
                    }},
                    canPass = true
                )  }
                .thenWith { player, card ->
                    player.playCard(card, 2)
                    player.trash(card)
                    player.gainFromSupply(
                        instruction = "$player, gain an action card costing up exactly ${card.costInstruction(1)}",
                        filter = {it.hasType(CardType.ACTION) && it.isEqualWithBonus(card, 1)},
                        dest = Destination.PlayerZone.Discard
                    )
                }
                .end()

            )
        }
    @Dominion_Card(extension = DA, pileType = PileType.RATS)
    fun Rats() : Card{
        val bonus = Bonus.action().draw()
        return Card.action("Rats", Price.darkAges(4))
            .setup {
                onPlay(bonus.onPlay()
                    .then { gainFromSupply("Rats") }
                    .trashCardFromHand(extraInstruction = "non rats", filter = {!it.hasName("Rats")}).result
                    .otherwise { player, _ ->
                        player.revealsIf(player.getList(Destination.PlayerZone.Hand)){ it.all {c -> c.hasName("Rats") } }
                    }
                    .end()
                )
                checkItselfTrash { onEffect{event, _ -> event.player.draw()} }
            }
    }
    @Dominion_Card(extension = DA)
    fun Rebuild() = Card.action("Rebuild", Price.darkAges(5))
        .setup {
            onPlay(Bonus.Action.onPlay()
                .lookingAt { player, _ -> player.choose("$player, name a card in chatBox") }
                .thenWith { player, string ->
                    player.discardUntil(check = {!it.hasName(string) && it.hasType(CardType.VICTORY)} ) {card ->
                        trash(card)
                        gainFromSupply(
                            "$this, gain a victory card costing up to ${card.costInstruction(3)} ",
                            filter ={it.hasType(CardType.VICTORY) && it.isAtMostWithBonus(card, 3)},
                            dest = Destination.PlayerZone.Discard
                        )
                    }
                }
                .end()
            )
        }
    @Dominion_Card(extension = DA)
    fun Rogue() : Card {
        val money = Bonus.money(2)
        return Card.attack("Rogue", Price.darkAges(5) )
            .setup {
                onPlay(money.onPlay()
                    .lookingAt { player, _ -> player.game.trashedCards  }.listIsNotEmpty()
                    .so {player -> player.gainFromTrash(
                        instruction = "$this, gain a card form trash costing between 3$ and 6$",
                        filter = {it.isBetween(3, 6)},
                        dest = Destination.PlayerZone.Discard
                    ){reveals(it)} }
                    .otherwise { player, card ->
                        player.game.processAttackWithReveals(
                            attacker = player,
                            attackCard = card,
                            count = 2,
                            filter = {it.isBetween(3, 6)})
                        {att, v, options -> att.game.chooseCard(v, options) }
                    }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = DA)
    fun Sage() = Card.action("Sage", Price.darkAges(3))
        .setup {
            onPlay(Bonus.Action.onPlay()
                .then { discardUntil(
                    check = {it.isAtLeast(3)},
                    action = {moveTo(it, Destination.PlayerZone.Hand)}
                ) }
            )
        }
    @Dominion_Card(extension = DA)
    fun Scavager() : Card {
        val bonus = Bonus.money(2)
        return Card.action("Scavager", Price.darkAges(4) )
            .setup {
                onPlay(bonus.onPlay()
                    .chooseWhatToDo { player, card -> InteractionRequest(
                        instruction = "$player, Do you want to put your draw into your discard ?",
                        cards = listOf(card),
                        buttons = Button.yesOrNo
                    )}
                    .branch(
                        "y" to { player, _, _ ->
                            player.moveAll(Destination.PlayerZone.Draw, Destination.PlayerZone.Discard)
                            player.shuffling(Destination.PlayerZone.Discard)
                        }
                    )
                    .end()
                    .chooseCardFromList { player, _ ->  InteractionRequest(
                        instruction = "$player, take a card in you discard and put the chosen one in you deck",
                        cards = player.getList(Destination.PlayerZone.Discard),
                    )}
                    .thenWith { player, card -> player.moveTo(card, Destination.PlayerZone.Draw) }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = DA)
    fun Squire() = Card.action("Squire", Price.darkAges(2))
        .setup {
            onPlay(Bonus.Money.onPlay()
                .chooseWhatToDo { player, card -> InteractionRequest(
                    instruction = "$player, Choose: +2 Actions or +2 Buys or gain a Silver",
                    cards = listOf(card),
                    buttons = listOf(Button("+2 Actions", "a"), Button("+2 Buys", "b"), Button("gain a Silver", "s"))
                ) }
                .branch(
                    "a" to { player, _, _ -> player.increment(Item.ACTION, 2)},
                    "b" to { player, _, _ -> player.increment(Item.BUY, 2) },
                    "s" to { player, _, _ -> player.gainFromSupply("Silver") }
                )
                .end()
            )
            checkItselfTrash {
                onEffect(BiEffect.empty<Event, Card>()
                    .gainFromSupply(instruction = {"$player, gain an attack card"}, filter = {it.hasType(CardType.ATTACK)})
                )
            }
        }
    @Dominion_Card(extension = DA)
    fun Storeroom() = Card.action("Storeroom", Price.darkAges(3))
        .setup {
            onPlay(Bonus.Buy.onPlay()
                .then {
                    discardUntilYouStop(Destination.PlayerZone.Hand){ draw(it) }
                    discardUntilYouStopAndDo(Destination.PlayerZone.Hand, instruction = "to gain 1$ per card discarded"){ increment(Item.MONEY) }
                }
            )
        }
    @Dominion_Card(extension = DA)
    fun Urchin() : Card {
        val bonus = Bonus.action().draw()
        return Card.attack("Urchin", Price.darkAges(3))
            .setup {
                onPlay(bonus.onPlay().processHandDown(toReach = 4, mayDiscard = true))
                beforeCardPlayed {
                    onEffect(BiEffect.empty<Player, Event>()
                        .chooseWhatToDo { owner, _ -> InteractionRequest(
                            instruction = "$owner, Do you want to trash $scope ?",
                            cards = listOf(scope),
                            buttons = Button.yesOrNo
                        ) }
                        .branch(
                            "y" to {player, _, _ ->
                                if(player.trash(scope)){
                                    player.gainSpecificAsideCard("Mercenary", this@Dark_AgesFactoryKt.NAME)
                                }
                            }
                        )
                        .end()
                    )
                    onCondition { event, player -> event.isSamePlayer(player) && event.cardHasType(CardType.ATTACK) && event.card != scope }
                }
            }
    }
    @Dominion_Card(extension = DA)
    fun Vagrant(): Card {
        val bonus = Bonus.action().draw()
        return Card.action("Vagrant", Price.darkAges(2))
            .setup {
                onPlay(bonus.onPlay()
                    .lookingAt { player, _ -> player.getCardFromDeck() }.filterNotNull()
                    .revealCard()
                    .filter { it.testTypes {
                        any(CardType.CURSE, CardType.SHELTER, CardType.RUINS, CardType.VICTORY)
                    } }
                    .thenWith { player, card -> player.moveTo(card, Destination.PlayerZone.Hand) }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = DA)
    fun WanderingMinstrel() : Card {
        val bonus = Bonus.action(2).draw()
        return Card.action("Wandering Minstrel", Price.darkAges(4))
            .setup {
                onPlay(bonus.onPlay()
                    .lookingAt { player, _ -> player.getTopCards(3)  }
                    .listIsNotEmpty()
                    .revealList()
                    .thenWith { player, cards ->
                        player.moveList(
                            instruction = "$player, put the actions card back into your deck in any order",
                            cards = cards,
                            filter = {it.hasType(CardType.ACTION)},
                            to = Destination.PlayerZone.Draw,
                        )

                        player.discardList(cards)
                    }
                    .end()

                )
            }
    }

    @Dominion_Card(extension = DA, pileType = PileType.UNIQUE)
    fun Knights() = Knight("Knights").addType(CardType.TEMPLATE).setup {
        onSetup {
            val knights = createMixedSupplyPile(factory.getMixedCards(CardType.KNIGHT), scope).apply { shuffle() }
            allPilesForSupply.add(knights)
        }
    }

    fun Knight(name : String) = Card.attack(name, Price.darkAges(5)).addType(CardType.KNIGHT)

    @Dominion_Card(extension = DA, pileType = PileType.UNIQUE)
    fun DameAnna() = Knight("Dame Anna")
        .setup { onPlay(BiEffect.empty<Player, Card>().processKnightAttack { trash(2) }) }

    @Dominion_Card(extension = DA, pileType = PileType.UNIQUE)
    fun DameJosephine() = Knight("Dame Josephine").addType(CardType.VICTORY)
        .setup {
            onPlay(BiEffect.empty<Player, Card>().processKnightAttack())
            score { 2 }
        }

    @Dominion_Card(extension = DA, pileType = PileType.UNIQUE)
    fun DameMolly() : Card {
        val actions = Bonus.action(2)
        return Knight("Dame Molly")
            .setup { onPlay(actions.onPlay().processKnightAttack()) }
    }
    @Dominion_Card(extension = DA, pileType = PileType.UNIQUE)
    fun DameNathalie() = Knight("Dame Nathalie")
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .gainFromSupply(instruction = {"$player, gain a card costing up to 3$" }, filter = {it isAtMost 3})
                .processKnightAttack()
            )
        }
    @Dominion_Card(extension = DA, pileType = PileType.UNIQUE)
    fun DameSylvie() : Card {
        val moneys = Bonus.money(2)
        return Knight("Dame Sylvie")
            .setup { onPlay(moneys.onPlay().processKnightAttack()) }
    }
    @Dominion_Card(extension = DA, pileType = PileType.UNIQUE)
    fun SirBailey() : Card{
        val bonus = Bonus.action().draw()
        return Knight("Sir Bailey")
            .setup { onPlay(bonus.onPlay().processKnightAttack()) }
    }
    @Dominion_Card(extension = DA, pileType = PileType.UNIQUE)
    fun SirDestry() : Card {
        val draws = Bonus.draw(2)
        return Knight("Sir Destry")
            .setup { onPlay(draws.onPlay().processKnightAttack()) }
    }
    @Dominion_Card(extension = DA, pileType = PileType.UNIQUE)
    fun SirMartin() : Card {
        val buys = Bonus.buy(2)
        return Knight("Sir Martin").setPrice(4)
            .setup { onPlay(buys.onPlay().processKnightAttack ()) }
    }
    @Dominion_Card(extension = DA, pileType = PileType.UNIQUE)
    fun SirMichael() = Knight("Sir Michael")
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .processHandDown(toReach = 3, mayDiscard = true)
                .processKnightAttack()
            )
        }
    @Dominion_Card(extension = DA, pileType = PileType.UNIQUE)
    fun SirVander() = Knight("Sir Vander")
        .setup {
            onPlay(BiEffect.empty<Player, Card>().processKnightAttack())
            checkItselfTrash { onEffect(BiEffect.empty<Event, Card>() then { player.gainFromSupply("Gold") }) }
        }

    private fun BiEffect<Player, Card>.processKnightAttack(action : suspend Player.(Card) -> Unit = {}) : BiEffect<Player, Card> {
        return this.then {player, self -> player.action(self)}
            .lookingAt { player, self -> player.game.processAttackWithReveals(
                attacker = player,
                attackCard = self,
                count = 2,
                filter = {it.isBetween(3, 6) }
            ){att, opp, options -> att.game.chooseCard(opp, options)} }
            .branchDecision {
                on {data.any{it.hasType(CardType.KNIGHT)}} then {player, self, _ -> player.trash(self)}
            }
            .end()
    }

    fun Ruins(cardName : String) = Card.action(cardName, Price.darkAges(0)).addType(CardType.RUINS)

    @Dominion_Card(extension = DA, pileType = PileType.RUINS) fun AbandonedMine() = Ruins("Abandoned Mine").setup { simpleAction(Bonus.Money) }
    @Dominion_Card(extension = DA, pileType = PileType.RUINS) fun RuinedLibrary() = Ruins("Ruined Library").setup { simpleAction(Bonus.draw) }
    @Dominion_Card(extension = DA, pileType = PileType.RUINS) fun RuinedMarket() = Ruins("Ruined Market").setup { simpleAction(Bonus.Buy) }
    @Dominion_Card(extension = DA, pileType = PileType.RUINS) fun RuinedVillage() = Ruins("Ruined Village").setup { simpleAction(Bonus.Action) }

    @Dominion_Card(extension = DA, pileType = PileType.RUINS)
    fun Survivors() = Ruins("Survivors")
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .lookingAt { player, _ -> player.getTopCards(2) }.listIsNotEmpty()
                .chooseWhatToDo { player, _ -> InteractionRequest(
                    instruction = "$player, discard $this or put them back in your deck",
                    cards = this,
                    buttons = Button.DeckOrDiscard
                ) }
                .branch(
                    "discard" to {player, _, cards -> player.discardList(cards)},
                    "deck" to {player, _, cards -> player.moveAllAndChooseTheOrder(cards, Destination.TempZone.Temp, Destination.PlayerZone.Draw)}
                )
                .end()
        )
    }

    @Dominion_Card(extension = DA, pileType = PileType.UNIQUE)
    fun Hovel() = Card("Hovel", Price.darkAges(1), CardType.REACTION, CardType.SHELTER)
        .setup {
            onGain {
                onEffect(BiEffect.empty<Player, Event>()
                    .chooseWhatToDo { player, _ ->  InteractionRequest(
                        instruction = "$player, do you want to trash $scope ?",
                        cards = listOf(scope),
                        buttons = Button.yesOrNo
                    ) }
                    .branch("y" to {player, _, _ -> player.trash(scope)})
                    .end()
                )
                onCondition { event, player -> event.isSamePlayer(player) && event.cardHasType(CardType.VICTORY) }
            }
        }
    @Dominion_Card(extension = DA, pileType = PileType.UNIQUE)
    fun Necropolis() : Card {
        val action = Bonus.action(2)
        return Card.action("Necropolis", Price.darkAges(1)).addType(CardType.SHELTER)
            .setup {simpleAction(action)}
    }
    @Dominion_Card(extension = DA, pileType = PileType.UNIQUE)
    fun OvergrownEstate() = Card.victory("Overgrown Estate", Price.darkAges(1)).addType(CardType.SHELTER)
        .setup {
            score { 0 }
            checkItselfTrash { onEffect{event, _ -> event.player.draw()} }
        }
    @Dominion_Card(extension = DA)
    fun Madman() : Card {
        val action = Bonus.action(2)
        return Card.action("Madman", Price.darkAges(0)).addType(CardType.ASIDE)
            .setup {
                onPlay(action.onPlay()
                    .filter {_, card -> card.replaceInSupply() }
                    .so { it.drawByAction { getList(Destination.PlayerZone.Hand).size } }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = DA)
    fun Mercenary() : Card {
        val bonus = Bonus.action(2).draw(2)
        return Card.attack("Mercenary", Price.darkAges(0)).addType(CardType.ASIDE)
            .setup {
                onPlay(BiEffect.empty<Player, Card>()
                    .then { player, self ->
                        player.trashAndDo(
                            instruction = "to apply some effect",
                            number = 2,
                            from = Destination.PlayerZone.Hand,
                            canPass = false
                        ){
                            if(it == 2 ){
                                triggerEffect(ACTION, self, bonus)
                                game.processHandDown(this, self, toReach = 3, mayDiscard = true )
                            }
                        }
                    }
                )
            }
    }

    @Dominion_Card(extension = DA)
    fun Spoils() : Card {
        val money = Bonus.money(3)
        return Card.treasure("Spoils", Price.darkAges(0)).addType(CardType.ASIDE)
            .setup {
                onPlay(money.onPlay()
                    .then { _, self -> self.replaceInSupply() }
                )
            }
    }
}