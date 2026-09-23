package fr.umontpellier.iut.dominion.cards.factories.Cornucopia_Guilds

import fr.umontpellier.iut.dominion.Annotation.Dominion_Card
import fr.umontpellier.iut.dominion.Annotation.PileType
import fr.umontpellier.iut.dominion.Button
import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.*
import fr.umontpellier.iut.dominion.Player.Skills.choose
import fr.umontpellier.iut.dominion.Player.Skills.discard
import fr.umontpellier.iut.dominion.Player.Skills.discardFromHand
import fr.umontpellier.iut.dominion.Player.Skills.discardList
import fr.umontpellier.iut.dominion.Player.Skills.discardUntil
import fr.umontpellier.iut.dominion.Player.Skills.discardUntilAndDo
import fr.umontpellier.iut.dominion.Player.Skills.draw
import fr.umontpellier.iut.dominion.Player.Skills.drawByAction
import fr.umontpellier.iut.dominion.Player.Skills.gain
import fr.umontpellier.iut.dominion.Player.Skills.moveAll
import fr.umontpellier.iut.dominion.Player.Skills.moveAllAndChooseTheOrder
import fr.umontpellier.iut.dominion.Player.Skills.moveTo
import fr.umontpellier.iut.dominion.Player.Skills.playCard
import fr.umontpellier.iut.dominion.Player.Skills.reveals
import fr.umontpellier.iut.dominion.Player.Skills.trash
import fr.umontpellier.iut.dominion.Properties
import fr.umontpellier.iut.dominion.cards.*
import fr.umontpellier.iut.dominion.cards.Bonus.Bonus
import fr.umontpellier.iut.dominion.cards.Events.Event
import fr.umontpellier.iut.dominion.cards.builders.*
import fr.umontpellier.iut.dominion.cards.component.BiEffect
import fr.umontpellier.iut.dominion.cards.component.InteractionRequest
import fr.umontpellier.iut.dominion.cards.component.Price
import fr.umontpellier.iut.dominion.cards.component.attack
import fr.umontpellier.iut.dominion.cards.component.chooseCardFromHand
import fr.umontpellier.iut.dominion.cards.component.lookingAt
import fr.umontpellier.iut.dominion.cards.component.processHandDown
import fr.umontpellier.iut.dominion.cards.component.reveal
import fr.umontpellier.iut.dominion.cards.component.then
import fr.umontpellier.iut.dominion.cards.factories.CG
import fr.umontpellier.iut.dominion.cards.factories.createSupplyPile
import fr.umontpellier.iut.dominion.cards.factories.follow
import kotlin.Pair
import kotlin.String
import kotlin.Unit
import kotlin.let

object Cornucopia_GuildsFactoryKt {
    @Dominion_Card(extension = CG)
    fun Advisor() = Card.action("Advisor", Price.cornucopia(4))
        .setup {
            onPlay(
                Bonus.Action.onPlay()
                .lookingAt { player, _ ->
                    val left = player.game.onTheLeft(player)
                    PipelineState(left, player.getTopCards(3), Unit)
                }.thenWith { player, p -> player.reveals(p.current) }
                .chooseCardFromList({ _, _ -> source }) { player, _ ->
                    InteractionRequest(
                        instruction = "$source discard one of those card from $player",
                        cards = current
                    )
                }
                .thenDo { player, _, pipe, chosen ->
                    player.discard(chosen)
                    pipe.current.remove(chosen)
                    player.moveAllAndChooseTheOrder(
                        pipe.current,
                        Destination.TempZone.Temp,
                        Destination.PlayerZone.Draw
                    )
                }
                .end()
            )
        }

    @Dominion_Card(extension = CG)
    fun Baker(): Card {
        val bonus = Bonus.action().with(Item.COFFER).draw()
        return Card.action("Baker", Price.cornucopia(5)).setup {
            simpleAction(bonus)

            onSetup(true) {
                players.forEach { it.increment(Item.COFFER) }
            }

        }
    }


    @Dominion_Card(extension = CG)
    fun Butcher(): Card {
        val coffers = Bonus.coffer(2)
        return Card.action("Butcher", Price.cornucopia(5))
            .setup {
                onPlay(
                    coffers.onPlay()
                    .chooseCardFromHand(
                        interactionRequest = InteractionRequest(
                            instruction = "Trash a card from your hand",
                            canPass = true
                        )
                    )
                    .filter(Player::trash).result
                    .mapPipe { _, _ -> current.costValue }
                    .thenWithPipe { player, _ -> runButcherDecision(player, current) }
                    .end()
                )
            }
    }

    @Dominion_Card(extension = CG)
    fun CandlestickMaker(): Card {
        val bonus = Bonus.action().with(Item.BUY).with(Item.COFFER)
        return Card.action("Candlestick Maker", Price.cornucopia(2)) setup { simpleAction(bonus) }
    }

    @Dominion_Card(extension = CG)
    fun Carnival() = Card.action("Carnival", Price.cornucopia(5))
        .setup {
            onPlay(
                BiEffect.empty<Player, Card>()
                .lookingAt { player, _ -> player.getTopCards(4) }
                .distinctListAndNotEmpty { it.name }
                .thenWithPipe { player, _ ->
                    player.moveAll(current, Destination.PlayerZone.Hand) { source.remove(it) }
                    player.discardList(source)
                }
                .end()
            )
        }

    @Dominion_Card(extension = CG, pileType = PileType.VICTORY)
    fun Fairgrounds() = Card.victory("Fairgrounds", Price.cornucopia(6))
        .setup { score { (it.allOwnedCards.distinctBy { c -> c.name }.size / 5) * 2 } }

    @Dominion_Card(extension = CG)
    fun FarmHands(): Card {
        val bonus = Bonus.action(2).draw()
        return Card.action("Farmhands", Price.cornucopia(4))
            .setup {
                simpleAction(bonus)
                checkGain {
                    onEffect(BiEffect.empty<Event, Card>()
                        .chooseCardFromHand(
                            interactionRequest = InteractionRequest(
                                "You may set aside a card from your hand ( you will play it at your next turn ) ",
                                canPass = true
                            )
                        )
                        .thenWith { event, card ->
                            event.player.moveTo(card, Destination.PlayerZone.Aside)
                            event.player.addNextTurnEffect { playCard(card) } }
                        .end())
                }
            }
    }
    @Dominion_Card(extension = CG)
    fun Farrier(): Card {
        val bonus = Bonus.action().with(Item.BUY).draw()
        return Card.action("Farrier", Price.cornucopia(2)).addType(CardType.OVERPAID)
            .setup {
                simpleAction(bonus)
                overpaid { onEffect{player, self -> player.updateDrawBonusValue(self.getValue("OverpaidNumber").toInt())} }
            }
    }
    @Dominion_Card(extension = CG)
    fun Ferryman(): Card {
        val bonus = Bonus.action().draw(2)
        return Card.action("Ferryman", Price.cornucopia(5))
            .setup {
                onPlay(bonus.onPlay() then { discardFromHand() })
                checkGain {
                    onEffect(BiEffect.empty<Event, Card>()
                        .lookingAt { event, _ -> event.player.game.getAvailableAsidePilesCard("Ferryman") }
                        .revealList()
                        .thenWith { event, cards -> event.player.gain(cards.first()) }
                        .end()
                    )
                }
            }
    }
    @Dominion_Card(extension = CG)
    fun Footpad(): Card {
        val coffers = Bonus.coffer(2)
        return Card.attack("Footpad", Price.cornucopia(5))
            .setup { onPlay(coffers.onPlay().processHandDown(toReach = 3, mayDiscard = true)) }
    }
    @Dominion_Card(extension = CG)
    fun Hamlet(): Card {
        val bonus = Bonus.action().draw()
        return Card.action("Hamlet", Price.cornucopia(2))
            .setup {
                onPlay(
                    bonus.onPlay()
                        .chooseCardFromHand(
                            interactionRequest = InteractionRequest(
                                "You may discard a card from your hand for 1 action",
                                canPass = true
                            )
                        )
                        .thenWith { player, card -> player.discard(card) { increment(Item.ACTION) } }.end()
                        .chooseCardFromHand(
                            interactionRequest = InteractionRequest(
                                "You may discard a card from your hand for 1 buy",
                                canPass = true
                            )
                        )
                        .thenWith { player, card -> player.discard(card) { increment(Item.BUY) } }.end()
                )
            }
    }
    @Dominion_Card(extension = CG)
    fun Herald(): Card {
        val bonus = Bonus.action().draw()
        return Card.action("Herald", Price.cornucopia(4)).addType(CardType.OVERPAID)
            .setup {
                onPlay(
                    bonus.onPlay()
                    .lookingAt { player, _ -> player.getCardFromDeck() }.filterNotNull()
                    .revealCard()
                    .branchDecision {
                        on { data.hasType(CardType.ACTION) } then { player, self, card ->
                            player.playCard(card)
                        }
                    }
                    .end()
                )
                overpaid {
                    onEffect(BiEffect.empty<Player, Card>()
                        .lookingAt { _, card -> card.getValue("OverpaidNumber").toInt() }
                        .thenWith { player, overpaid -> runHeraldChoose(player, overpaid) }
                        .end()
                    )
                }
            }
    }
    @Dominion_Card(extension = CG)
    fun HornOfPlenty() = Card.treasure("Horn of Plenty", Price.cornucopia(5))
        .setup {
            onPlay(
                BiEffect.empty<Player, Card>()
                .lookingAt { player, _ -> player.getDistinctCards(Destination.PlayerZone.InPlay).size }
                .map { player, i ->
                    player.gainFromSupply(
                        instruction = "$player, gain a card costing up to $i",
                        filter = { it isAtMost i },
                        dest = Destination.PlayerZone.Discard
                    )
                }
                .filter { it.hasType(CardType.VICTORY) }
                .so { player, self -> player.trash(self) }
                .end()
            )
        }
    @Dominion_Card(extension = CG)
    fun HuntingParty(): Card {
        val bonus = Bonus.action().draw()
        return Card.action("Hunting Party", Price.cornucopia(5))
            .setup {
                onPlay(
                    bonus.onPlay()
                    .reveal { getList(Destination.PlayerZone.Hand) }
                    .then {
                        val distinct = getDistinctCards(Destination.PlayerZone.Hand)
                        discardUntil(check = { distinct.none { c -> c.hasSameNameAs(it) } }) {
                            moveTo(
                                it,
                                Destination.PlayerZone.Hand
                            )
                        }
                    }
                )
            }
    }
    @Dominion_Card(extension = CG)
    fun Infirmary() = Card.action("Infirmary", Price.cornucopia(3)).addType(CardType.OVERPAID)
        .setup {
            onPlay(
                Bonus.draw.onPlay()
                    .chooseCardFromHand(
                        interactionRequest = InteractionRequest(
                            "You may trash a card from your hand",
                            canPass = true
                        )
                    )
                    .thenWith(Player::trash)
                    .end()
            )
            overpaid {
                onEffect(BiEffect.empty<Player, Card>()
                    .lookingAt { _, self -> self.getValue("OverpaidNumber").toInt() }
                    .thenDo { player, self, i -> player.playCard(self, i) }
                    .end()
                )
            }
        }
    @Dominion_Card(extension = CG)
    fun Jester(): Card {
        val money = Bonus.money(2)
        return Card.attack("Jester", Price.cornucopia(5))
            .setup {
                onPlay(
                    money.onPlay()
                    .attack { attacker, opponent, _ ->
                        val effect = BiEffect.empty<Player, Player>()
                            .lookingAt { opp, _ -> opp.getCardFromDeck() }.filterNotNull()
                            .thenWith { opp, card -> opp.discard(card) }
                            .branchDecision {
                                on { data.hasType(CardType.VICTORY) } then { opp, _, _ -> opp.gainFromSupply("Curse") }
                                otherwise { opp, att, card ->
                                    att.chooseWhatToDo(
                                        "$att, Choose who gain a copy of $card",
                                        list = listOf(card),
                                        buttons = listOf(Button("You", "y"), Button("$opp", "n"))
                                    )
                                        .let {
                                            val ref = if (it == "y") att else opp
                                            ref.gainFromSupply(card.name)
                                        }
                                }
                            }
                            .end()

                        effect(opponent, attacker)
                    }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = CG)
    fun Journeyman() = Card.action("Journeyman", Price.cornucopia(5))
        .setup {
            onPlay(
                BiEffect.empty<Player, Card>()
                .lookingAt { player, _ -> player.choose("$player, name a card in chat") }
                .thenWith { player, named ->
                    player.discardUntilAndDo({ it < 3 }, { it.hasName(named) }) { list ->
                        list.forEach { moveTo(it, Destination.PlayerZone.Hand) }
                    }
                }
                .end()
            )
        }
    @Dominion_Card(extension = CG)
    fun Joust(): Card {
        val bonus = Bonus.action().with(Item.MONEY).draw()
        return Card.action("Joust", Price.cornucopia(5))
            .setup {
                onPlay(
                    bonus.onPlay()
                    .chooseCardFromHand(
                        interactionRequest = InteractionRequest(
                            instruction = "You may set aside a Province from your hand",
                            filter = { it.hasName("Province") },
                            canPass = true
                        )
                    )
                    .thenWith { player, card ->
                        player.moveTo(card, Destination.PlayerZone.Aside)
                        player.addEndTurnEffect { discard(card) }
                        player.gainCardFromAsideSupply(
                            namePile = "Rewards",
                            instruction = "$player, gain a reward",
                            dest = Destination.PlayerZone.Hand
                        )
                    }
                    .end()
                )
                onSetup {
                    factory.getMixedCards(CardType.REWARDS)?.forEach { reward ->
                        asideSupplyPiles.getOrPut("Rewards") { mutableListOf() }.add(createSupplyPile(reward))
                    }
                }
            }
    }
    @Dominion_Card(extension = CG)
    fun Menagerie() = Card.action("Menagerie", Price.cornucopia(3))
        .setup {
            onPlay(
                Bonus.Action.onPlay()
                .lookingAt { player, _ -> player.getList(Destination.PlayerZone.Hand) }
                .revealList()
                .distinctList { it.name }
                .branchDecision {
                    on { data.current.isEmpty() || data.current sizeIsEqualTo data.source } then { player, _, _ ->
                        player.draw(
                            3
                        )
                    }
                    otherwise { player, _, _ -> player.draw() }
                }
                .end()
            )
        }
    @Dominion_Card(extension = CG)
    fun MerchantGuild(): Card {
        val bonus = Bonus.buy().with(Item.MONEY)
        return Card.action("Merchant Guild", Price.cornucopia(5))
            .setup {
                onPlay(bonus.onPlay() then { _, self -> self.set("mult", self.getValue("mult").toInt() + 1) })
                onEndBuy {
                    onEffect{ player, self ->
                        player.incrementByAction(Item.COFFER) {
                            self.getValue("mult").toInt() * getProperties(Properties.Cards_Bought).value
                        }
                    }
                }
            }
    }
    @Dominion_Card(extension = CG)
    fun Plaza(): Card {
        val bonus = Bonus.action(2).draw()
        return Card.action("Plaza", Price.cornucopia(4))
            .setup {
                onPlay(
                    bonus.onPlay()
                    .chooseCardFromHand(
                        interactionRequest = InteractionRequest(
                            instruction = "You may trash a treasure from your hand ( +1 coffer )",
                            filter = { it.hasType(CardType.TREASURE) },
                            canPass = true
                        )
                    ).filter(Player::trash)
                    .so { player -> player.increment(Item.COFFER) }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = CG)
    fun Remake() = Card.action("Remake", Price.cornucopia(4))
        .setup {
            onPlay(
                BiEffect.empty<Player, Card>()
                .chooseCardFromHand(interactionRequest = InteractionRequest("Trash a card from your hand"))
                .filter(Player::trash)
                .thenWith { player, trashed ->
                    player.gainFromSupply(
                        instruction = "$player, gain a card costing exactly ${trashed.costInstruction(1)}",
                        filter = { it.isEqualWithBonus(trashed, 1) },
                        dest = Destination.PlayerZone.Discard
                    )
                }
                .repeat(2)
                .end()
            )
        }
    @Dominion_Card(extension = CG)
    fun Shop(): Card {
        val bonus = Bonus.money().draw()
        return Card.action("Shop", Price.cornucopia(3))
            .setup {
                onPlay(
                    bonus.onPlay()
                    .chooseCardFromHand { player, _ ->
                        InteractionRequest(
                            instruction = "$player, you may play an action card you don't have in play",
                            filter = {
                                it.hasType(CardType.ACTION) && player.getDistinctCards(Destination.PlayerZone.InPlay)
                                    .none { c -> c.hasSameNameAs(it) }
                            },
                            canPass = true
                        )
                    }.thenWith { player, chosen ->
                        player.playCard(chosen)
                    }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = CG)
    fun Soothsayer() = Card.action("Soothsayer", Price.cornucopia(5))
        .setup {
            onPlay(
                BiEffect.empty<Player, Card>()
                .then { gainFromSupply("Gold") }
                .attack { _, opponent, _ -> opponent.gainFromSupply("Curse")?.let { opponent.draw() } }
                .end()
            )
        }
    @Dominion_Card(extension = CG)
    fun Stonemason() = Card.action("Stonemason", Price.cornucopia(2)).addType(CardType.OVERPAID)
        .setup {
            onPlay(
                BiEffect.empty<Player, Card>()
                .chooseCardFromHand { player, _ -> InteractionRequest(instruction = "$player, trash a card from your hand") }
                .filter(Player::trash)
                .thenWith { player, trashed ->
                    player.gainMultipleCardFromSupply(
                        instruction = "$player, gain two card costing up to ${trashed.costInstruction(-1)}",
                        filter = { it.isAtMostWithBonus(trashed, -1) },
                        dest = Destination.PlayerZone.Discard,
                        number = 2
                    )
                }
                .end()
            )
            overpaid {
                onEffect(BiEffect.empty<Player, Card>()
                    .lookingAt { _, self ->
                        Pair(
                            self.getValue("OverpaidNumber").toInt(),
                            self.getValue("Potion").toInt()
                        )
                    }.filter { pair -> pair.first > 0 }
                    .thenWith { player, pair ->
                        player.gainMultipleCardFromSupply(
                            instruction = "$player, gain two action cards costing exactly ${pair.first} $,  ${if (pair.second > 0) "${pair.second} potion(s)" else ""}",
                            filter = { it.hasType(CardType.ACTION) && it.isAtMost(pair.first, pair.second, 0) },
                            dest = Destination.PlayerZone.Discard,
                            number = 2
                        )
                    }
                    .end()
                )
            }
        }
    @Dominion_Card(extension = CG)
    fun YoungWitch(): Card {
        val draw = Bonus.draw(2)
        return Card.attack("Young Witch", Price.cornucopia(4))
            .setup {
                onPlay(
                    draw.onPlay()
                    .then { discardFromHand(2) }
                    .attack { attacker, opponent, _ ->
                        val effect = BiEffect.empty<Player, String>()
                            .chooseCardFromHand { player, string ->
                                InteractionRequest(
                                    instruction = "$player, reveal a bane card from your hand ($string) or gain a curse",
                                    filter = { it.hasName(string) },
                                    canPass = true
                                )
                            }
                            .branchDecision {
                                on { choice.hasName(left) } then { player, _, _ -> player.reveals(this) }
                                otherwise { player, _, _ -> player.gainFromSupply("Curse") }
                            }
                            .endParent()

                        effect(opponent, attacker.game.banes)
                    }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = CG, pileType = PileType.MIXED)
    fun Coronet() = Card.attack("Coronet", Price.cornucopia(0)).addType(CardType.REWARDS)
        .setup {
            onPlay(
                BiEffect.empty<Player, Card>()
                .chooseCardFromHand { player, _ ->
                    InteractionRequest(
                        instruction = "$player, you may play an action card 2 times",
                        filter = { it.hasType(CardType.ACTION) && !it.hasType(CardType.REWARDS) },
                        canPass = true
                    )
                }.thenWith { player, self, card -> player.playCard(card, 2); self.follow(player, card) }.end()
                .chooseCardFromHand { player, _ ->
                    InteractionRequest(
                        instruction = "$player, you may play a treasure card 2 times",
                        filter = { it.hasType(CardType.TREASURE) && !it.hasType(CardType.REWARDS) },
                        canPass = true
                    )
                }.thenWith { player, self, card -> player.playCard(card, 2); self.follow(player, card) }.end()
            )
            follower()
        }
    @Dominion_Card(extension = CG, pileType = PileType.MIXED)
    fun Courser() = Card.action("Courser", Price.cornucopia(0)).addType(CardType.REWARDS)
        .setup { onPlay { player, card -> runCourserChoices(player, card) } }

    @Dominion_Card(extension = CG, pileType = PileType.MIXED)
    fun Demesne(): Card {
        val bonus = Bonus.action(2).with(Item.BUY, 2)
        return Card("Demesne", Price.cornucopia(0), CardType.ACTION, CardType.VICTORY, CardType.REWARDS)
            .setup {
                onPlay(bonus.onPlay() then { gainFromSupply("Gold") })
                score { it.getList(Destination.PlayerZone.Hand).count { hasName("Gold") } }
            }
    }
    @Dominion_Card(extension = CG, pileType = PileType.MIXED)
    fun Housecarl() = Card.action("Housecarl", Price.cornucopia(0)).addType(CardType.REWARDS)
        .setup {
            onPlay { player, _ ->
                player.drawByAction {
                    getDistinctCards(Destination.PlayerZone.InPlay).count { hasType(CardType.ACTION) }
                }
            }
        }
    @Dominion_Card(extension = CG, pileType = PileType.MIXED)
    fun HugeTurnip(): Card {
        val coffers = Bonus.coffer(2)
        return Card.treasure("Huge Turnip", Price.cornucopia(0)).addType(CardType.REWARDS)
            .setup { onPlay(coffers.onPlay() then { increment(Item.MONEY, coffer) }) }
    }
    @Dominion_Card(extension = CG, pileType = PileType.MIXED)
    fun Renown() = Card.action("Renown", Price.cornucopia(0)).addType(CardType.REWARDS)
        .setup { onPlay(Bonus.Buy.onPlay() then { game.stat.reduction += 2 }) }

}