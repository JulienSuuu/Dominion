package fr.umontpellier.iut.dominion.cards.factories.Prosperity

import fr.umontpellier.iut.dominion.Annotation.Dominion_Card
import fr.umontpellier.iut.dominion.Button
import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Flags
import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.PlayerComponent.addCardEffect
import fr.umontpellier.iut.dominion.Player.Skills.choose
import fr.umontpellier.iut.dominion.Player.Skills.discard
import fr.umontpellier.iut.dominion.Player.Skills.discardAndDo
import fr.umontpellier.iut.dominion.Player.Skills.discardUntil
import fr.umontpellier.iut.dominion.Player.Skills.discardUntilYouStopAndDo
import fr.umontpellier.iut.dominion.Player.Skills.draw
import fr.umontpellier.iut.dominion.Player.Skills.drawByAction
import fr.umontpellier.iut.dominion.Player.Skills.moveAllAndChooseTheOrder
import fr.umontpellier.iut.dominion.Player.Skills.moveTo
import fr.umontpellier.iut.dominion.Player.Skills.playCard
import fr.umontpellier.iut.dominion.Player.Skills.reveals
import fr.umontpellier.iut.dominion.Player.Skills.trash
import fr.umontpellier.iut.dominion.Player.Skills.trashAndEffect
import fr.umontpellier.iut.dominion.Player.Skills.trashUntilYouStopAndDo
import fr.umontpellier.iut.dominion.Properties
import fr.umontpellier.iut.dominion.cards.Bonus.Bonus
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.Events.Event
import fr.umontpellier.iut.dominion.cards.Events.GainType
import fr.umontpellier.iut.dominion.cards.builders.filterListAndNotEmpty
import fr.umontpellier.iut.dominion.cards.builders.filterNotNull
import fr.umontpellier.iut.dominion.cards.builders.flatWith
import fr.umontpellier.iut.dominion.cards.builders.match
import fr.umontpellier.iut.dominion.cards.builders.partition
import fr.umontpellier.iut.dominion.cards.component.BiEffect
import fr.umontpellier.iut.dominion.cards.component.InteractionRequest
import fr.umontpellier.iut.dominion.cards.component.NonNullPair
import fr.umontpellier.iut.dominion.cards.component.Price
import fr.umontpellier.iut.dominion.cards.component.attack
import fr.umontpellier.iut.dominion.cards.component.benefit
import fr.umontpellier.iut.dominion.cards.component.chooseCardFromHand
import fr.umontpellier.iut.dominion.cards.component.chooseWhatToDo
import fr.umontpellier.iut.dominion.cards.component.increment
import fr.umontpellier.iut.dominion.cards.component.lookingAt
import fr.umontpellier.iut.dominion.cards.component.reveal
import fr.umontpellier.iut.dominion.cards.component.then
import fr.umontpellier.iut.dominion.cards.count
import fr.umontpellier.iut.dominion.cards.factories.EFFECT
import fr.umontpellier.iut.dominion.cards.factories.follow
import fr.umontpellier.iut.dominion.cards.gainDifferentCardFromSupply
import fr.umontpellier.iut.dominion.cards.gainFromSupply
import fr.umontpellier.iut.dominion.cards.getTopCards
import fr.umontpellier.iut.dominion.cards.plusAssign
import fr.umontpellier.iut.dominion.cards.triggerEffect

object ProsperityFactoryKt {
    @Dominion_Card(extension = "Prosperity")
    fun Anvil() = Card.treasure("Anvil", Price.prosperity(3))
        .setup {
            onPlay(Bonus.Money.onPlay()
                .chooseCardFromHand(interactionRequest = InteractionRequest(
                    "Discard a treasure from your hand",
                    filter = {it.hasType(
                    CardType.TREASURE)},
                    canPass = true
                ))
                .filter(Player::discard)
                .thenWith { player, _ ->
                    player.gainFromSupply(
                        "$player, gain a card costing up to 4$",
                        filter = {it isAtMost 4},
                        dest = Destination.PlayerZone.Discard
                    )
                }
                .end()
            )
        }

    @Dominion_Card(extension = "Prosperity")
    fun Bank() = Card.treasure("Bank", Price.prosperity(7))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .lookingAt { player, _ -> 1 + player.getList(Destination.PlayerZone.Hand).count { hasType(CardType.TREASURE) } }
                .thenDo { player, self, number ->
                    val bonus = Bonus.money(number)
                    player.triggerEffect(EFFECT, self, bonus )
                }
                .end()
            )
        }
    @Dominion_Card(extension = "Prosperity")
    fun Bishop() = Card.action("Bishop", Price.prosperity(4))
        .setup {
            onPlay(Bonus.Money.onPlay()
                .increment(Item.VICTORY_TOKEN)
                .chooseCardFromHand(interactionRequest = InteractionRequest("Trash a card from your hand"))
                .filter(Player::trash)
                .thenWith { player, card -> player.increment(Item.VICTORY_TOKEN, card.costValue/2) }
                .end()
                .benefit {
                    it.chooseCardFromHand("$it,you may trash a card from your hand", canPass = true)
                    ?.let{card -> it.trash(card)}
                }
            )
        }
    @Dominion_Card(extension = "Prosperity")
    fun Charlatan() : Card {
        val money = Bonus.money(3)
        return Card.attack("Charlatan", Price.prosperity(5))
            .setup {
                onPlay(money.onPlay()
                    .attack { _, opponent, _ -> opponent.gainFromSupply("Curse") }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = "Prosperity")
    fun City() : Card {
        val actionDraw = Bonus.action(2).draw()
        return Card.action("City", Price.prosperity(5))
            .setup {
                onPlay(actionDraw.onPlay()
                    .lookingAt { player, _ -> player.game.stat.emptyPiles.value }
                    .match {
                        on {data >= 1} then {player, _, _ -> player.draw()}
                        on {data >= 2} then {player, _, _ ->
                            player.increment(Item.BUY)
                            player.increment(Item.MONEY)
                        }
                    }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = "Prosperity")
    fun Clerk() : Card {
       val money = Bonus.money(2)
       return Card.attack("Clerk", Price.prosperity(4)).addType(CardType.REACTION)
           .setup{
               onPlay(money.onPlay()
                   .attack { _, opponent, _ ->
                       if(opponent.getList(Destination.PlayerZone.Hand).size >=5 ){
                           opponent.chooseCardFromHand("$opponent, move a card from your hand to your draw")
                               ?.let { opponent.moveTo(it, Destination.PlayerZone.Draw) }
                       }
                   }
                   .end()
               )
               onStartTurn { onEffect {p, _ -> p.playCard(scope)} }
           }
    }
    @Dominion_Card(extension = "Prosperity")
    fun Collection() : Card {
        val buyMoney = Bonus.buy().with(Item.MONEY, 2)
        return Card.treasure("Collection", Price.prosperity(5))
            .setup {
                onPlay(buyMoney.onPlay()
                    .then { player, self -> player.addCardEffect(self) }
                )
                onSideEffectGain(type = GainType.AFTER) {
                    onEffect{owner, _ -> owner.increment(Item.VICTORY_TOKEN)}
                    onCondition { event, player -> event.isSamePlayer(player) && event.cardHasType(CardType.ACTION) && event.isSameCard }
                }
            }
    }

    @Dominion_Card(extension = "Prosperity")
    fun CrystalBall() = Card.treasure("Crystal Ball", Price.prosperity(5))
        .setup {
            onPlay(Bonus.Money.onPlay()
                .lookingAt { player, _ -> player.getCardFromDeck()  }.filterNotNull()
                .map { _, _, deck ->
                    val button = mutableListOf<Button>().apply {
                        add(Button.Trash)
                        add(Button.Discard)
                        if(deck.hasType(CardType.ACTION) || deck.hasType(CardType.TREASURE)) add(Button("Play", "p"))
                    }
                    NonNullPair(deck, button)
                }
                .chooseWhatToDo { player, _ ->
                    val (top, buttons) = this
                    val instruction = "$player, Choose : You may Trash, Discard ${if (buttons.size == 3) "or Play" else ""} this card"
                    InteractionRequest(instruction = instruction, cards = listOf(top), buttons = buttons, canPass = true) }
                .branch(
                        "t" to {player, _, pair -> player.trash(pair.first)},
                        "d" to {player, _, pair -> player.discard(pair.first)},
                        "p" to {player, _, pair ->
                            player.playCard(pair.first)
                        }
                    )
                .end()
            )
        }
    @Dominion_Card(extension = "Prosperity")
    fun Expand() = Card.action("Expand", Price.prosperity(7))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .chooseCardFromHand(interactionRequest = InteractionRequest("Trash a card"))
                .filter(Player::trash)
                .thenWith { player, trashed ->
                    player.gainFromSupply(
                        instruction = "$player, gain a card costing up to ${trashed.costInstruction(3)}",
                        filter = {it.isAtMostWithBonus(trashed, 3)},
                        dest = Destination.PlayerZone.Discard
                    )
                }
                .end()
            )
        }
    @Dominion_Card(extension = "Prosperity")
    fun Forge() = Card.action("Forge", Price.prosperity(7))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .then { player, _ ->
                    player.trashUntilYouStopAndDo(from = Destination.PlayerZone.Hand){
                        player.gainFromSupply(
                            instruction = "$$player, gain a card costing exactly $it$",
                            filter = {card -> card.isEqual(it)},
                            dest = Destination.PlayerZone.Discard
                        )
                    }
                }
            )
        }
    @Dominion_Card(extension = "Prosperity")
    fun GrandMarket() : Card {
        val all = Bonus.action().with(Item.BUY).with(Item.MONEY, 2).draw()
        return Card.action("Grand Market", Price.prosperity(6))
            .setup {
                simpleAction(all)
                available { !it.isFlagSet(Flags.COPPER_PLAYED) }
            }
    }
    @Dominion_Card(extension = "Prosperity")
    fun Hoard() : Card {
        val money = Bonus.money(2)
        return Card.treasure("Hoard", Price.prosperity(6))
            .setup {
                simpleAction(money)
                onBuy { onEffect{player, card -> if(card.hasType(CardType.VICTORY)) player.gainFromSupply("Gold")} }
            }
    }
    @Dominion_Card(extension = "Prosperity")
    fun Investment() = Card.treasure("Investment", Price.prosperity(4))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .then {trash()}
                .chooseWhatToDo { player, self -> InteractionRequest(
                    instruction = "$player, Choose : +1 coin or trash this card for Victory tokens",
                    cards = listOf(self),
                    buttons = listOf(Button.Money, Button.Trash)
                ) }
                .branch(
                    "t" to {player, self, _ ->
                        if(player.trash(self)){
                            player.reveals(player.getList(Destination.PlayerZone.Hand))
                            player.incrementByAction(Item.VICTORY_TOKEN){ getList(Destination.PlayerZone.Hand).count{ hasType(CardType.TREASURE)} }
                        }
                    },
                    "m" to {player, _, _ -> player.increment(Item.MONEY)}
                )
                .end()
            )
        }

    @Dominion_Card(extension = "Prosperity")
    fun KingsCourt() = Card.action("King's Court", Price.prosperity(7))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .chooseCardFromHand(interactionRequest = InteractionRequest(
                    instruction = "You may play an action card three time in a row",
                    filter = {it.hasType(CardType.ACTION)},
                    canPass = true
                ))
                .thenWith { player, scope, chosen ->
                    player.playCard(chosen, 3)
                    scope.follow(player, chosen)
                }
                .end()
            )

            follower(3)
        }
    @Dominion_Card(extension = "Prosperity")
    fun Magnate() = Card.action("Magnate", Price.prosperity(5))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .reveal{getList(Destination.PlayerZone.Hand)}
                .then { drawByAction { getList(Destination.PlayerZone.Hand).count { hasType(CardType.TREASURE) } } }

            )
        }

    @Dominion_Card(extension = "Prosperity")
    fun Mint() = Card.action("Mint", Price.prosperity(5))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .chooseCardFromHand(interactionRequest = InteractionRequest(
                    instruction = "You may reveal a treasure from your hand",
                    filter = {it.hasType(CardType.TREASURE)},
                    canPass = true
                ))
                .thenWith { player, card ->
                    player.reveals(card)
                    player.gainFromSupply(card.name)
                }
                .end()
            )
            checkGain {
                onEffect(BiEffect.empty<Event, Card>()
                    .lookingAt { event, _ -> event.player.getList(Destination.PlayerZone.InPlay) }
                    .filterListAndNotEmpty{!it.hasType(CardType.DURATION) && it.hasType(CardType.TREASURE)}
                    .flatWith { event, _, card -> event.player.trash(card)  }
                    .end())
            }
        }
    @Dominion_Card(extension = "Prosperity")
    fun Monument() : Card {
        val money = Bonus.money(2)
        return Card.action("Monument", Price.prosperity(4))
            .setup { onPlay(money.onPlay().increment(Item.VICTORY_TOKEN)) }
    }
    @Dominion_Card(extension = "Prosperity")
    fun Peddler() : Card {
        val actionDrawMoney = Bonus.action().with(Item.MONEY).draw()
        return Card.action("Peddler", Price.prosperity(8))
            .setup { simpleAction(actionDrawMoney) }
    }
    @Dominion_Card(extension = "Prosperity")
    fun Quarry() = Card.treasure("Quarry", Price.prosperity(4))
        .setup { onPlay(Bonus.Money.onPlay() .then { getProperties(Properties.quarryReduction) += 2 }) }
    @Dominion_Card(extension = "Prosperity")
    fun Rabble() : Card {
        val draw = Bonus.draw(3)
        return Card.attack("Rabble", Price.prosperity(5))
            .setup {
                onPlay(draw.onPlay()
                    .attack { attacker, opponent, _ ->
                        val effect = BiEffect.empty<Player, Player>()
                            .lookingAt { opp, _ -> opp.getTopCards(3) }
                            .partition { it.hasType(CardType.TREASURE) || it.hasType(CardType.ACTION) }
                            .thenWith { opp, pair ->
                                val(toTrash, others) = pair
                                toTrash.forEach{c -> opp.trash(c)}
                                opp.moveAllAndChooseTheOrder(others.toMutableList(), Destination.TempZone.Temp, Destination.PlayerZone.Draw)
                            }.end()

                        effect(opponent, attacker)

                    }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = "Prosperity")
    fun Tiara() = Card.treasure("Tiara", Price.prosperity(4))
        .setup {
            onPlay(Bonus.Money.onPlay()
                .then { player, self -> player.addCardEffect(self) }
                .chooseCardFromHand(interactionRequest = InteractionRequest(
                    instruction = "You may play a treasure card 2 times",
                    filter = {it.hasType(CardType.TREASURE)},
                    canPass = true
                ))
                .thenDo { player, self, _, card ->
                    player.playCard(card, 2)
                    self.follow(player, card)
                }
                .end()
            )

            onSideEffectGain(GainType.DURING){
                onEffect(BiEffect.empty<Player, Event>()
                    .chooseWhatToDo { owner, event -> InteractionRequest(
                        instruction = "$owner Do you want to put thi card into your draw ?",
                        cards = listOfNotNull(event.card),
                        buttons = Button.yesOrNo,
                        canPass = true
                    ) }
                    .branch("y" to {_, event, _ -> event.destination = Destination.PlayerZone.Draw})
                    .end()
                )
                onCondition { event, player -> event.isSamePlayer(player) }
            }
            follower()
        }
    @Dominion_Card(extension = "Prosperity")
    fun Vault() : Card {
        val draw = Bonus.draw(2)
        return Card.action("Vault", Price.prosperity(5))
            .setup {
                onPlay(draw.onPlay()
                    .then {
                        discardUntilYouStopAndDo(
                            from = Destination.PlayerZone.Hand,
                            instruction = " to gain 1$ per card discarded"
                        ) { increment(Item.MONEY) }
                    }
                    .attack { _, opponent, _ ->
                        opponent.discardAndDo(
                            from = Destination.PlayerZone.Hand,
                            number = 2,
                            canPass = true,
                            instruction = " to draw a card"
                        ){if(it==2) draw()}
                    }.end()
                )
            }
    }

    suspend fun Player.nameACard(key : String){
        val left = game.onTheLeft(this)
        left.choose("$left, write a name of a card ( pref existing), in box channel")
            .let { game.getNamedCardsThisTurn(key).add(it) }

    }
    @Dominion_Card(extension = "Prosperity")
    fun WarChest() = Card.treasure("War Chest", Price.prosperity(5))
        .setup {
            onPlay(BiEffect.empty<Player, Card>()
                .then { nameACard("War Chest") }
                .lookingAt { player, _ -> player.game.getNamedCardsThisTurn("War Chest") }
                .thenWith { player, cards ->
                    player.gainFromSupply(
                        instruction = "$player, gain a card costing up to 5$ and was not named this turn",
                        filter = {it isAtMost 5 && it.name !in cards},
                        dest = Destination.PlayerZone.Discard
                    )
                }
                .end()
            )
        }
    @Dominion_Card(extension = "Prosperity")
    fun Watchtower() = Card.action("Watchtower", Price.prosperity(5)).addType(CardType.REACTION)
        .setup {
            onPlay{player, _ -> player.drawByAction { 6 - getList(Destination.PlayerZone.Hand).size  } }
            onGain {
                onEffect(
                    BiEffect.empty<Player, Event>()
                        .chooseWhatToDo { player, event -> InteractionRequest(
                            instruction = "$player, Choose : Trash or put in draw this card",
                            cards = listOfNotNull(event.card),
                            buttons = Button.TrashOrDeck,
                        )  }
                        .branch(
                            "t" to {player, event, _ -> if(player.trash(event.card)) event.destination = Destination.Trash },
                            "d" to {_, event, _ -> event.destination = Destination.PlayerZone.Draw}
                        )
                        .end()
                )
                onCondition { event, player -> event.isSamePlayer(player) && event.isSameCard }
            }

        }
    @Dominion_Card(extension = "Prosperity")
    fun WorkersVillage() : Card {
        val actionBuyDraw = Bonus.action(2).with(Item.BUY).draw()
        return Card.action("Worker's Village", Price.prosperity(4)).setup { simpleAction(actionBuyDraw) }
    }
    @Dominion_Card(extension = "Prosperity")
    fun Contraband() : Card {
        val moneyBuy = Bonus.buy().with(Item.MONEY, 3)
        return Card.treasure("Contraband", Price.prosperity(5))
            .setup { onPlay(moneyBuy.onPlay() then { nameACard("Contraband") }) }
    }
    @Dominion_Card(extension = "Prosperity")
    fun Goons() : Card {
        val moneyDraw = Bonus.money(2).draw()
        return Card.attack("Goons", Price.prosperity(6))
            .setup {
                onPlay(moneyDraw.onPlay()
                    .then { player, self ->
                        player.game.processHandDown(
                            p = player,
                            c = self,
                            toReach = 3,
                            mayDiscard = true,
                        )
                    }
                )
                onBuy { onEffect{player, _ -> player.increment(Item.VICTORY_TOKEN)} }
            }
    }
    @Dominion_Card(extension = "Prosperity")
    fun Loan() = Card.treasure("Loan", Price.prosperity(3))
        .setup {
            onPlay(Bonus.Money.onPlay()
                .then {
                    discardUntil({it.hasType(CardType.TREASURE)}){
                        val effect = BiEffect.empty<Player, Card>()
                            .chooseWhatToDo { player, card -> InteractionRequest(
                                instruction = "$player, Choose : Trash or discard this card",
                                cards = listOf(card),
                                buttons = Button.DiscardOrTrash,
                            ) }
                            .branch(
                                "t" to {player, card, _ -> player.trash(card)},
                                "d" to {player, card, _ -> player.discard(card)},
                            )
                            .end()

                        effect(this, it)
                    }
                }
            )
        }
    @Dominion_Card(extension = "Prosperity")
    fun Mountebank() : Card {
        val money = Bonus.money(2)
        return Card.attack("Mountebank", Price.prosperity(5))
            .setup {
                onPlay(money.onPlay()
                    .attack { _, opponent, _ ->
                        opponent.chooseCardFromHand(
                            instruction =  "$opponent, You may discard a curse",
                            canPass = true
                        ) {it.hasType(CardType.CURSE)}
                            ?.let{c -> opponent.discard(c)}
                            ?:run{ opponent.gainDifferentCardFromSupply("Curse", "Copper") }
                    }.end()
                )
            }
    }
    @Dominion_Card(extension = "Prosperity")
    fun RoyalSeal() : Card {
        val money = Bonus.money(2)
        return Card.treasure("Royal Seal", Price.prosperity(5))
            .setup {
                simpleAction(money)
                onGain {
                    onEffect(BiEffect.empty<Player, Event>()
                        .chooseWhatToDo { player, event -> InteractionRequest(
                            instruction = "$player, do you want to put ${event.card} into your draw ? ",
                            cards = listOfNotNull(event.card),
                            buttons = Button.yesOrNo
                        ) }
                        .branch("y" to {_, event, _ -> event.destination = Destination.PlayerZone.Draw})
                        .end()
                    )
                    onCondition { event, player -> event.isSamePlayer(player) }
                }
            }
    }
    @Dominion_Card(extension = "Prosperity")
    fun Talisman() = Card.treasure("Talisman", Price.prosperity(4))
        .setup {
            simpleAction(Bonus.Money)
            onBuy {
                onEffect{player, gained ->
                    if(!gained.hasType(CardType.VICTORY) && gained isAtMost 4) {
                        player.gainFromSupply(gained.name)
                    }
                }
            }
        }
    @Dominion_Card(extension = "Prosperity")
    fun TradeRoute() = Card.action("Trade Route", Price.prosperity(3))
        .setup {
            onPlay(Bonus.Buy.onPlay() then { trashAndEffect(from = Destination.PlayerZone.Hand){ increment(Item.MONEY, tradeCoin) } })
            onSetup(true) {
                supplyPiles.values.forEach {
                    if(it.last().hasType(CardType.VICTORY)&& !it.last().hasType(CardType.KNIGHT)){
                        it.token++
                    }
                }
            }
        }
    @Dominion_Card(extension = "Prosperity")
    fun Venture() = Card.treasure("Venture", Price.prosperity(5))
        .setup {
            onPlay(Bonus.Money.onPlay()
                .then { discardUntil({it.hasType(CardType.TREASURE)}){treasure ->
                    playCard(treasure)
                } }
            )
        }
}