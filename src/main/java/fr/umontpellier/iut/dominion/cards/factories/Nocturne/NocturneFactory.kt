package fr.umontpellier.iut.dominion.cards.factories.Nocturne

import fr.umontpellier.iut.dominion.Button
import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.PlayerComponent.addCardEffect
import fr.umontpellier.iut.dominion.Player.PlayerComponent.cardGainedCurrentTurn
import fr.umontpellier.iut.dominion.Player.Skills.discard
import fr.umontpellier.iut.dominion.Player.Skills.discardAndDo
import fr.umontpellier.iut.dominion.Player.Skills.discardFromHand
import fr.umontpellier.iut.dominion.Player.Skills.discardListUntilYouStop
import fr.umontpellier.iut.dominion.Player.Skills.discardUntil
import fr.umontpellier.iut.dominion.Player.Skills.discardUntilYouStopAndDo
import fr.umontpellier.iut.dominion.Player.Skills.draw
import fr.umontpellier.iut.dominion.Player.Skills.moveAllAndChooseTheOrder
import fr.umontpellier.iut.dominion.Player.Skills.moveList
import fr.umontpellier.iut.dominion.Player.Skills.moveTo
import fr.umontpellier.iut.dominion.Player.Skills.playCard
import fr.umontpellier.iut.dominion.Player.Skills.reveals
import fr.umontpellier.iut.dominion.Player.Skills.trash
import fr.umontpellier.iut.dominion.Player.Skills.trashAndDo
import fr.umontpellier.iut.dominion.Player.Skills.trashFromList
import fr.umontpellier.iut.dominion.cards.Bonus.Bonus
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.CardConfigurator
import fr.umontpellier.iut.dominion.cards.Events.Event
import fr.umontpellier.iut.dominion.cards.Events.GainType
import fr.umontpellier.iut.dominion.cards.Events.OnMoveEvent
import fr.umontpellier.iut.dominion.cards.builders.attack
import fr.umontpellier.iut.dominion.cards.builders.branchDecision
import fr.umontpellier.iut.dominion.cards.builders.filterNotNull
import fr.umontpellier.iut.dominion.cards.builders.gainFromSupply
import fr.umontpellier.iut.dominion.cards.builders.listIsNotEmpty
import fr.umontpellier.iut.dominion.cards.component.BiEffect
import fr.umontpellier.iut.dominion.cards.component.InteractionRequest
import fr.umontpellier.iut.dominion.cards.component.OnPlayComponent
import fr.umontpellier.iut.dominion.cards.component.Price
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent
import fr.umontpellier.iut.dominion.cards.component.attackOthers
import fr.umontpellier.iut.dominion.cards.component.chooseCardFromHand
import fr.umontpellier.iut.dominion.cards.component.chooseCardFromList
import fr.umontpellier.iut.dominion.cards.component.chooseWhatToDo
import fr.umontpellier.iut.dominion.cards.component.draw
import fr.umontpellier.iut.dominion.cards.component.filter
import fr.umontpellier.iut.dominion.cards.component.gainFromSupply
import fr.umontpellier.iut.dominion.cards.component.gainSpecificCardFromSupply
import fr.umontpellier.iut.dominion.cards.component.lookingAt
import fr.umontpellier.iut.dominion.cards.component.then
import fr.umontpellier.iut.dominion.cards.component.trashCardFromHand
import fr.umontpellier.iut.dominion.cards.count
import fr.umontpellier.iut.dominion.cards.displayCoin
import fr.umontpellier.iut.dominion.cards.factories.activate
import fr.umontpellier.iut.dominion.cards.factories.follow
import fr.umontpellier.iut.dominion.cards.gainCardFromNightSupply
import fr.umontpellier.iut.dominion.cards.gainFromSupply
import fr.umontpellier.iut.dominion.cards.gainSpecificNocturneCard
import fr.umontpellier.iut.dominion.cards.getTopCards
import fr.umontpellier.iut.dominion.cards.plusAssign
import fr.umontpellier.iut.dominion.cards.testTypes
import fr.umontpellier.iut.dominion.game.rules.flipCardFaceDown

object NocturneFactory {

    //region ---KINGDOMS---
    fun Bard() : Card {
        val money = Bonus.money(2)
        return Card.action("Bard", Price.night(4)).addType(CardType.FATE)
            .setup {
                onPlay(money.onPlay()
                    .lookingAt { player, _ -> player.game.receiveNextBoon() }.filterNotNull()
                    .thenWith { player, card -> card.playNocturne(player) }
                    .end()
                )
            }
    }

    fun BlessedVillage() : Card {
        val bonus = Bonus.action(2).draw()
        return Card.action("BlessedVillage", Price.night(4)).addType(CardType.FATE)
            .setup {
                simpleAction(bonus)
                checkGain(BiEffect.empty<Event, Card>()
                    .lookingAt { event, _ -> event.player.game.receiveNextBoon() }
                    .filterNotNull()
                    .chooseWhatToDo { event, _ -> InteractionRequest(
                        instruction = "${event.player}, do you want to play directly your $this ?",
                        cards = listOf(this),
                        buttons = Button.yesOrNo
                    ) }
                    .branch(
                        "y" to {event, _, card -> card.playNocturne(event.player)},
                        "n" to {event, _, card ->
                            event.player.addNextTurnEffect { card.playNocturne(this); }
                        }
                    )
                    .end()
                )
            }
    }

    fun Cemetery() = Card.victory("Cemetery", Price.night(4))
        .setup {
            score { 2 }
            checkGain{event, _ -> event.player.trash(4) }
        }

    fun Changeling() = Card.night("Changeling", Price.night(3))
        .setup {
            onEndSetup {
                addListener<OnMoveEvent>{ event ->
                    if(event.hasMoved) return@addListener
                    val effect = BiEffect.empty<Player, Event>()
                        .lookingAt { _, event -> event.card  }
                        .filterNotNull{ it.isAtLeast(3)}
                        .chooseWhatToDo { _, event -> InteractionRequest(
                            instruction = "${event.player}, do you want to exchange $this for a $scope ?",
                            cards = listOf(this, scope),
                            buttons = Button.yesOrNo
                        ) }
                        .branch("y" to {player, event, card ->
                            val changeling = player.game.getCardFromSupply("Changeling")
                            if(changeling != null) {
                                if(card.replaceInSupply()){ event.updateCard(changeling); event.updateDest(Destination.PlayerZone.Discard) }
                            }
                        })
                        .end()

                    effect(event.player, event)
                }
            }

            onPlay(BiEffect.empty<Player, Card>()
                .then { player, card -> player.trash(card) }
                .gainFromSupply(
                    instruction = {"$player, gain a card you have in play"},
                    filter = {it.name in player.getList(Destination.PlayerZone.InPlay).map { c -> c.name }},
                )
            )
        }


    fun Cobbler() = Card.night("Cobbler", Price.night(5)).addType(CardType.DURATION)
        .setup {
            onDuration {
                onEffect(BiEffect.empty<Player, Card>()
                    .gainFromSupply(
                        instruction = {"$player, gain a card costing up to ${displayCoin(4)} to your hand"},
                        filter = {it isAtMost 4},
                        destination = Destination.PlayerZone.Hand
                    )
                )
            }
        }

    fun Conclave() = Card.action("Conclave", Price.night(4))
        .setup {
            onPlay(Bonus.money(2).onPlay()
                .chooseCardFromHand { player, _ -> InteractionRequest(
                    instruction = "$player, you may play an action card from your hand ",
                    filter = {it.hasType(CardType.ACTION) && it.name !in player.getDistinctCards(Destination.PlayerZone.InPlay).map { c -> c.name }.toSet()},
                    canPass = true
                ) }
                .thenWith { player,  card -> player.playCard(card); player.increment(Item.ACTION) }
                .end()
            )
        }

    fun Crypt() = Card.night("Crypt", Price.night(5)).addType(CardType.DURATION)
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .then {player, card ->
                    player.moveList(
                        instruction = "$player, move any non duration treasures from your play",
                        cards = player.getCopyOf(Destination.PlayerZone.InPlay) ?: mutableListOf(),
                        filter = {it.testTypes { any(CardType.TREASURE); none(CardType.DURATION) }},
                        to = Destination.PlayerZone.Aside,
                        canPass = true,
                        extraAction = { addListToShadowZone(card, it) }
                    )
                }
            )
            onDuration {
                onEffect(BiEffect.empty<Player, Card>()
                    .chooseCardFromList { player, card -> InteractionRequest(
                        instruction = "$player, put one into your hand",
                        cards = player.getShadowList(card)
                    ) }
                    .thenWith { player, self, card ->
                        player.removeCardFromShadowZone(self, card){ moveTo(it, Destination.PlayerZone.Hand) }
                    }
                    .end()
                )
                shouldBeDiscardWhen { player, card -> player.getAllListFromShadowZoneOf(card).isEmpty() }
            }
        }

    fun DenOfSin() = Card.night("Den Of Sin", Price.night(5)).addType(CardType.DURATION)
        .setup {
            simpleDuration(Bonus.draw(2))
            checkGain {
                onEffect{event, _ -> event.updateDest(Destination.PlayerZone.Hand)}
                onCondition { event, _ -> event.initialCameFrom(Destination.PlayerZone.Discard)  }
            }
        }

    fun DevilsWorkshop() = Card.night("Devil's Workshop", Price.night(4))
        .setup {
            onEndSetup { addNocturnePile("Spirit", "Imp") }
            onPlay(BiEffect.empty<Player, Card>()
                .lookingAt { player, _ -> player.cardGainedCurrentTurn.size }
                .branchDecision {
                    on {data >= 2 } then {player, _, _ -> player.gainSpecificNocturneCard("Spirit", "Imp")}
                    on {data == 1} then {player, _, _ -> player.gainFromSupply(
                        instruction = "$player, gain a card costing up to 4",
                        filter = {it isAtMost 4},
                        dest =  Destination.PlayerZone.Discard)
                    }
                    on {data == 0} then {player, _, _ -> player.gainFromSupply("Gold")}
                }
                .end()
            )
        }

    fun Druid() = Card.action("Druid", Price.night(2)).addType(CardType.FATE)
        .setup {
            onEndSetup { getRule<NocturneRules>()?.putAsideForDruid() }
            onPlay(Bonus.Buy.onPlay()
                .chooseCardFromList { player, _ -> InteractionRequest(
                    instruction = "$player, choose a boon from DruidBoons",
                    cards = player.game.chooseDruidBoon()
                ) }
                .thenWith { player, card -> card.play(player) }
                .end()
            )
        }

    fun Exorcist() = Card.night("Exorcist", Price.night(4))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .trashCardFromHand()
                .thenWith { player, card ->
                    player.gainCardFromNightSupply("Spirit",
                        instruction = "$player, gain a spirit costing less than ${card.costInstruction()}",
                        filter = {it.isLessThanWithBonus(card)})
                }
                .end()
            )
        }

    fun FaithfulHound() = Card.action("Faithful Hound", Price.night(2)).addType(CardType.REACTION)
        .setup {
            simpleAction(Bonus.draw(2))
            checkItselfDiscard {
                onEffect(BiEffect.empty<Event, Card>()
                    .chooseWhatToDo { event, card -> InteractionRequest(
                        instruction = "${event.player}, do you want to set aside $card ?",
                        cards = listOf(card),
                        buttons = Button.yesOrNo
                    ) }
                    .branch(
                        "y" to {event, card, _ ->
                            event.player.moveTo(card, Destination.PlayerZone.Aside)
                            event.player.addEndTurnEffect { moveTo(card, Destination.PlayerZone.Hand) }
                        }
                    )
                    .end()
                )
                onCondition { event, _ -> event.isActionDiscard }
            }
        }

    fun fool() = Card.night("Fool", Price.night(3))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .filter { player, _ -> !player.underState("Lost In The Woods") }
                .so { player -> player.game.receiveState(player, "Lost In The Woods") }
                .map { player, _, _ -> player.game.receiveBoons(3)}
                .loop(3){ list, counter ->
                    lookingAt { _, _ -> list }
                        .listIsNotEmpty()
                        .chooseCardFromList { player, _ -> InteractionRequest(
                            instruction = "$player, play the boons in any order (${counter.current}) ",
                            cards = this,
                        ) }
                        .thenDo { player, _, cards, chosen ->
                            cards.remove(chosen)
                            chosen.playNocturne(player)
                        }
                }
                .end()
            )
        }

    fun GhostTown() = Card.night("Ghost Town", Price.night(3)).addType(CardType.DURATION)
        .setup {
            simpleDuration(Bonus.ActionAndDraw)
            checkGain {
                onEffect{event, _ -> event.updateDest(Destination.PlayerZone.Hand)}
                onCondition { event, _ -> event.initialCameFrom(Destination.PlayerZone.Discard)  }
            }
        }

    fun Guardian() = Card.night("Guardian", Price.night(2)).addType(CardType.DURATION)
        .setup {
            simpleDuration(Bonus.Money)
            immunity{
                onEffect(object : TriggerComponent.Immunity { override suspend fun immune(player : Player, self: Card): Boolean { return activate(player, self) } })
            }
            checkGain {
                onEffect{event, _ -> event.updateDest(Destination.PlayerZone.Hand)}
                onCondition { event, _ -> event.initialCameFrom(Destination.PlayerZone.Discard)  }
            }
        }

    fun Idol() : Card {
        val money = Bonus.money(2)
        return Card.treasure("Idol", Price.night(5)).addType(CardType.ATTACK, CardType.FATE)
            .setup {
                onPlay(money.onPlay()
                    .lookingAt{player, _ -> player.getList(Destination.PlayerZone.InPlay).count { hasName("Idol") }}
                    .branchDecision {
                        on {data % 2 == 0} then {player, card, _ -> player.game.processAttack(player, card){it.gainFromSupply("Curse")} }
                        otherwise { player, _, _ ->
                            val c = player.game.receiveNextBoon() ?: return@otherwise
                            c.playNocturne(player)
                        }
                    }
                    .end()
                )
            }
    }

    fun Leprechaun() = Card.action("Leprechaun", Price.night(3)).addType(CardType.DOOM)
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .gainSpecificCardFromSupply(cardName = "Gold")
                .lookingAt { player, _ -> player.sizeOf(Destination.PlayerZone.InPlay) }
                .branchDecision {
                    on {data == 7} then {player, _, _ -> player.gainSpecificNocturneCard("Others", "Wish")}
                    otherwise { player, _, _ ->
                        val c = player.game.receiveNextHex() ?: return@otherwise
                        c.playNocturne(player)
                    }
                }
                .end()
            )
        }

    fun Monastery() = Card.night("Monastery", Price.night(2))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .lookingAt { player, _ -> player.getList(Destination.PlayerZone.Hand) + player.getList(Destination.PlayerZone.InPlay).filter { it.hasName("Copper") } }
                .listIsNotEmpty()
                .thenWith { player, cards -> player.trashFromList(cards, player.cardGainedCurrentTurn.size, true) }
                .end()
            )
        }

    fun Necromancer() = Card.action("Necromancer", Price.night(4))
        .setup {
            onEndSetup {
                val zombies : List<Card> = factory.getMixedCards(CardType.ZOMBIE)?.mapNotNull { factory.createCard(it) } ?: emptyList()
                zombies.forEach{moveCardToTrash(it)}
            }

            onPlay(BiEffect.empty<Player, Card>()
                .chooseCardFromList { player, _ -> InteractionRequest(
                    instruction = "$player, play a non duration action card from trash",
                    cards = player.game.trashedCards
                        .filter { it.testTypes { any(CardType.ACTION); none(CardType.DURATION) } && it.faceDown.not() }
                ) }
                .thenWith { player, card ->
                    player.game.flipCardFaceDown(card)
                    card.set("unable", true)
                    card.play(player)
                }
                .end()
            )
        }

    fun NightWatchman() = Card.night("NightWatchman", Price.night(3))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .lookingAt { player, _-> player.getTopCards(5) }
                .listIsNotEmpty()
                .thenWith { player, cards ->
                    player.discardListUntilYouStop(instructionList = "$player, you may discard any cards from your deck", cards)
                    player.moveAllAndChooseTheOrder(cards, Destination.TempZone.Temp, Destination.PlayerZone.Draw)
                }.end()
            )
            checkGain {
                onEffect{event, _ -> event.updateDest(Destination.PlayerZone.Hand)}
                onCondition { event, _ -> event.initialCameFrom(Destination.PlayerZone.Discard)  }
            }
        }

    fun Pixie() = Card.action("Pixie", Price.night(2)).addType(CardType.FATE)
        .setup {
            onPlay(Bonus.ActionAndDraw.onPlay()
                .lookingAt { player, _ -> player.game.receiveNextBoon() }
                .filterNotNull()
                .chooseWhatToDo { player, card -> InteractionRequest(
                    instruction = "$player, do you want to trash $card, to receive $this twice? ",
                    cards = listOf(card),
                    buttons = Button.yesOrNo,
                ) }
                .branch(
                    "y" to {player, self, card ->
                        if(player.trash(self)) card.playNocturne(player, 2)
                        else card.playNocturne(player)
                    },
                    "n" to {player, _, card -> card.playNocturne(player)}
                )
                .end()
            )
        }


    fun Pooka() = Card.action("Pooka", Price.night(5))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .trashCardFromHand(canPass = true, filter = {! it.hasType(CardType.TREASURE) && it.hasName("Cursed Gold")}, extraInstruction = "treasure (other than Cursed Gold)" )
                .thenWith { player, _ -> player.draw(4) }
                .end()
            )
        }

    fun Raider() = Card.night("Raider", Price.night(5)).addType(CardType.ATTACK, CardType.DURATION)
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .attackOthers { attacker, opponent, _ ->
                    if(opponent.sizeOf(Destination.PlayerZone.Hand) < 5) return@attackOthers
                    val effect = BiEffect.empty<Player, Unit>()
                        .lookingAt { _, _ -> attacker.getList(Destination.PlayerZone.Hand).map { it.name } }
                        .listIsNotEmpty()
                        .chooseCardFromHand { player, _-> InteractionRequest(
                            instruction = "$player, discard a copy that $attacker have in play",
                            filter = {it.name in this}
                        ) }
                        .thenWith { player, card -> player.discard(card)}
                        .otherwise { player, _ -> player.reveals(player.getList(Destination.PlayerZone.Hand)) }
                        .end()

                    effect(opponent, Unit)
                }
            )
            simpleDuration(Bonus.money(3))
        }

    fun SacredGroove() : Card {
        val bonus = Bonus.buy().with(Item.MONEY, 3)
        return Card.action("Sacred Groove", Price.night(5)).addType(CardType.FATE)
            .setup {
                onPlay(bonus.onPlay()
                    .lookingAt { player, _ -> player.game.receiveNextBoon() }
                    .filterNotNull()
                    .filter { player, card ->
                        val oldMoney = player.money
                        card.playNocturne(player)
                        oldMoney == player.money
                    }
                    .attack { _, opponent, _ ->
                        val effect = BiEffect.empty<Player, Card>()
                            .chooseWhatToDo { player, card -> InteractionRequest(
                                instruction = "$player, do you want to receive $card ?",
                                cards = listOf(card),
                                buttons = Button.yesOrNo,
                            ) }
                            .branch("y" to {player, card, _ -> card.playNocturne(player)})
                            .end()

                        effect(opponent, this)
                    }
                    .end()
                )
            }
    }

    fun SecretCave() = Card.action("Secret Cave", Price.night(3)).addType(CardType.DURATION)
        .setup {
            onPlay(Bonus.ActionAndDraw.onPlay()
                .then {player, card ->
                    player.discardAndDo(
                        from = Destination.PlayerZone.Hand,
                        number = 3,
                        canPass = true,
                        instruction = " to gain ${displayCoin(3)} on your next turn",
                        nextAction = {if(it == 3) getFlag("${card.id}_durationMoney") += true}
                    )
                }
            )
            onDuration {
                onEffect{player, _-> player.increment(Item.MONEY, 3)}
                shouldBeDiscardWhen { player, card -> player.isFlagSet("${card.id}_durationMoney") }
            }
        }

    fun Shepherd() = Card.action("Shepherd", Price.night(4))
        .setup {
            onPlay(Bonus.Action.onPlay()
                .then {player, _ ->
                    player.discardUntilYouStopAndDo(
                        from = Destination.PlayerZone.Hand,
                        instruction = " for 2 card per discarded",
                        extraCardInformation = "victory",
                        filter = {it.hasType(CardType.VICTORY)},
                        action = {
                            reveals(it)
                            draw(2)
                        }
                    )
                }
            )
        }

    fun Skulk() = Card.attack("Skulk", Price.night(4)).addType(CardType.DOOM)
        .setup {
            onPlay(Bonus.Buy.onPlay()
                .lookingAt { player, _ -> player.game.receiveNextHex() }
                .filterNotNull()
                .attack { _, opponent, _ -> playNocturne(opponent) }
                .end()
            )
            checkGain{event, _ -> event.player.gainFromSupply("Gold")}
        }

    fun Tormentor() : Card {
        val money = Bonus.money(2)
        return Card.attack("Tormentor", Price.night(5)).addType(CardType.DOOM)
            .setup {
                onEndSetup { addNocturnePile("Spirit", "Imp") }
                onPlay(money.onPlay()
                    .lookingAt { player, card -> player.getList(Destination.PlayerZone.InPlay).none { it.id != card.id } }
                    .branchDecision {
                        on {data} then {player, _, _ -> player.gainSpecificNocturneCard("Spirit", "Imp") }
                        on {!data} then {player, _, _ ->
                            val card = player.game.receiveNextHex() ?: return@then
                            player.game.processAttack(player, card){
                                card.playNocturne(it)
                            }
                        }
                    }
                    .end()
                )
            }
    }

    fun Tracker() = Card.action("Tracker", Price.night(2)).addType(CardType.FATE)
        .setup {
            onPlay(Bonus.Money.onPlay()
                    then {
                        player, card -> player.addCardEffect(card)
                        player.game.receiveNextBoon()?.playNocturne(player)
                    }
            )
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

    fun TragicHero() : Card {
        val bonus = Bonus.buy().draw(3)
        return Card.action("Tragic Hero", Price.night(5))
            .setup {
                onPlay(bonus.onPlay()
                    .filter { player, _ -> player.sizeOf(Destination.PlayerZone.Hand) >= 8 }
                    .thenDo { player, card, _ -> player.trash(card) }
                    .gainFromSupply(
                        instruction = {"$player, gain a treasure"},
                        filter = {it.hasType(CardType.TREASURE)}
                    )
                    .end()
                )
            }
    }

    fun Vampire() = Card.night("Vampire", Price.night(5)).addType(CardType.ATTACK, CardType.DOOM)
        .setup {
            onEndSetup { addNocturnePile("Others", "Bat") }
            onPlay(BiEffect.empty<Player, Card>()
                .lookingAt { player, _ -> player.game.receiveNextHex()  }
                .filterNotNull()
                .attack { _, opponent, _ -> this.playNocturne(opponent) }
                .end()
                .gainFromSupply(
                    instruction = {"$player, gain a card costing up ${displayCoin(5)} other than a vampire"},
                    filter = {it isAtMost 5 && !it.hasSameNameAs(pipeLineCard)}
                )
                .then { player, self ->
                    val card = player.game.getSpecificNightCard("Others", "Bat") ?: return@then
                    if(self.replaceInSupply()) player.moveTo(card, Destination.PlayerZone.Discard)
                }
            )
        }

    fun WereWolf() = Card.action("Werewolf", Price.night(5)).addType(CardType.NIGHT, CardType.ATTACK, CardType.DOOM)
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .lookingAt { player, _ -> player.isFlagSet("NightPhase") }
                .branchDecision {
                    on {data} then {player, card, _ ->
                        val c = player.game.receiveNextHex() ?: return@then
                        player.game.processAttack(player, card) {
                            c.playNocturne(it)
                        }
                    }
                    otherwise { player, _, _ -> player.draw(3) }
                }
                .end()
            )
        }
    //endregion

    //region ---NON SUPPLY---
    fun Bat() = Card.night("Bat", Price.night(2))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .then {player, card ->
                    player.trashAndDo(
                        instruction = "trash up to 2 card to exchange bat for a vampire ",
                        number = 2,
                        from = Destination.PlayerZone.Hand
                    ) {
                        if(it >= 1){
                            val vampire = game.getCardFromSupply("Vampire")
                            vampire?.let { if(card.replaceInSupply()) moveTo(vampire, Destination.PlayerZone.Discard) }
                        }
                    }
                }
            )
        }

    fun Ghost() = Card.night("Ghost", Price.night(4)).addType(CardType.DURATION, CardType.SPIRIT )
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .then {player, card ->
                    player.discardUntil({it.hasType(CardType.ACTION)}){
                        moveTo(it, Destination.PlayerZone.Aside)
                        player.addNextTurnEffect { playCard(it, 2); card.follow(player, it) }
                    }
                }
            )
        }

    //endregion

    //region ---BOONS---
    fun boon(name : String) = Card(name, Price.night(0), CardType.BOON)
    fun TheEarthsGift() = boon("The Earth's Gift")
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .chooseCardFromHand { player, _ -> InteractionRequest(
                    instruction = "$player, you may discard a treasure to gain a card",
                    filter = {it.hasType(CardType.TREASURE)}
                ) }
                .gainFromSupply(
                    instruction = {"gain a card costing up to ${displayCoin(4)}"},
                    filter = {it isAtMost 4}
                ).endParent()
            )
        }

    private fun BiEffect<Player, Card>.discardEffectBoonsAndHex(dest : Destination.NocturneZone) = then {player, card -> player.game.discardTo(dest, card) }

    private fun CardConfigurator.onPlayNocturne(dest : Destination.NocturneZone, onPlayComponent: OnPlayComponent){
        onPlay(onPlayComponent.discardEffectBoonsAndHex(dest))
    }

    fun TheFieldsGift() = boon("The Field's Gift").setup { onPlay(Bonus.action().with(Item.MONEY).onPlay().discardEffectBoonsAndHex(Destination.NocturneZone.Boons)) }
    fun TheFlamesGift() = boon("The Flame's Gift").setup { onPlay{player, card -> player.trash(); player.game.discardTo(Destination.NocturneZone.Boons, card) } }
    fun TheForestsGift() = boon("The Forest's Gift").setup {onPlay(Bonus.buy().with(Item.MONEY).onPlay().discardEffectBoonsAndHex(Destination.NocturneZone.Boons)) }
    fun TheMoonsGift() = boon("The Moon's Gift")
        .setup {
            onPlay{player, card ->
                player.moveTo(
                    Destination.PlayerZone.Discard,
                    Destination.PlayerZone.Draw,
                    instruction = "you may move a card from your discard to your deck",
                    canPass = true
                )
                player.game.discardTo(Destination.NocturneZone.Boons, card)
            }
        }

    fun TheMountainsGift() = boon("The Mountain's Gift").setup {onPlay(BiEffect.empty<Player, Card>().gainSpecificCardFromSupply(cardName = "Silver"))}
    fun TheRiversGift() = boon("The Rivers's Gift").setup {
        onPlay{player, card ->
            player.moveTo(card, Destination.PlayerZone.NocturneZone.Boons)
            player.addCleanUpEffect { game.discardTo(Destination.NocturneZone.Boons, card) }
        }
        onEndTurn(BiEffect.empty<Player, Card>().draw(1))
    }
    fun TheSeasGift() = boon("The Sea's Gift").setup {onPlay(Bonus.draw.onPlay().discardEffectBoonsAndHex(Destination.NocturneZone.Boons)) }
    fun TheSkysGift() = boon("The Sky's Gift").setup {
        onPlayNocturne(Destination.NocturneZone.Boons){player, card -> player.discardAndDo(Destination.PlayerZone.Hand, 3, canPass = true) }
    }
    fun TheSunsGift() = boon("The Sun's Gift").setup {
        onPlayNocturne(Destination.NocturneZone.Boons, BiEffect.empty<Player, Card>()
            .lookingAt { player, _ -> player.getTopCards(4) }.listIsNotEmpty()
            .thenWith { player, cards ->
                player.discardListUntilYouStop(
                    instructionList = "discard any number of them",
                    list = cards,
                ).let { list ->
                    player.moveAllAndChooseTheOrder(list.toMutableList(), Destination.PlayerZone.Draw, Destination.PlayerZone.Draw)
                }
            }
            .end()

        )
    }
    fun TheSwampsGift() = boon("The Swamp's Gift").setup {
        onPlayNocturne(Destination.NocturneZone.Boons){player, card ->
            player.gainSpecificNocturneCard("Spirit", "Will-o'-Wisp")
        }
    }
    fun TheWindsGift() = boon("The Wind's Gift").setup {
        onPlayNocturne(Destination.NocturneZone.Boons, Bonus.draw(2).onPlay().then { discardFromHand(2) })
    }
    //endregion

    //region ---HEXES---

    //endregion

}