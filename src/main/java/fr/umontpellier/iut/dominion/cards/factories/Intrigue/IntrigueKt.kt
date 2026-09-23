package fr.umontpellier.iut.dominion.cards.factories.Intrigue

import fr.umontpellier.iut.dominion.Annotation.Dominion_Card
import fr.umontpellier.iut.dominion.Annotation.PileType
import fr.umontpellier.iut.dominion.Button
import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Interface.Logger
import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.*
import fr.umontpellier.iut.dominion.Player.Skills.choose
import fr.umontpellier.iut.dominion.Player.Skills.discard
import fr.umontpellier.iut.dominion.Player.Skills.discardAll
import fr.umontpellier.iut.dominion.Player.Skills.discardFromHand
import fr.umontpellier.iut.dominion.Player.Skills.discardUntil
import fr.umontpellier.iut.dominion.Player.Skills.discardUntilYouStopAndDo
import fr.umontpellier.iut.dominion.Player.Skills.draw
import fr.umontpellier.iut.dominion.Player.Skills.gain
import fr.umontpellier.iut.dominion.Player.Skills.moveAllAndChooseTheOrder
import fr.umontpellier.iut.dominion.Player.Skills.moveTo
import fr.umontpellier.iut.dominion.Player.Skills.moveToTemp
import fr.umontpellier.iut.dominion.Player.Skills.putCardInDraw
import fr.umontpellier.iut.dominion.Player.Skills.reveals
import fr.umontpellier.iut.dominion.Player.Skills.trash
import fr.umontpellier.iut.dominion.Player.Skills.trashAndDo
import fr.umontpellier.iut.dominion.cards.*
import fr.umontpellier.iut.dominion.cards.Bonus.Bonus
import fr.umontpellier.iut.dominion.cards.builders.*
import fr.umontpellier.iut.dominion.cards.component.*
import fr.umontpellier.iut.dominion.cards.factories.ACTION
import fr.umontpellier.iut.dominion.cards.factories.EFFECT
import fr.umontpellier.iut.dominion.cards.factories.TRASHED_ACTION

object IntrigueKt {
    @Dominion_Card(extension = "Intrigue")
    fun Baron() = Card.action("Baron", Price.intrigue(4))
        .setup { 
            onPlay(
                Bonus.Buy.onPlay()
                .chooseCardFromHand(interactionRequest = InteractionRequest(
                    instruction = "You may discard an Estate",
                    filter = {it.hasName("Estate")},
                    canPass = true
                ))
                .thenWith { player, chosen ->
                    player.discard(chosen)
                    player.increment(Item.MONEY, 4)
                }
                .otherwise { player, _ -> player.gainFromSupply(cardName = "Estate") }
                .end()
            )
        }

    @Dominion_Card(extension = "Intrigue")
    fun Bridge() : Card {
        val buyMoney = Bonus.buy().with(Item.MONEY)
        return Card.action("Bridge", Price.intrigue(4))
            .setup {  onPlay(buyMoney.onPlay() then { game.stat.reduction+=1 }) }
    }

    @Dominion_Card(extension = "Intrigue")
    fun Conspirator() = Card.action("Conspirator", Price.intrigue(4))
        .setup { 
            onPlay{player, self ->
                val value = player.getValueOf(Item.ACTION_PLAYED)
                var effect = 0
                if(value >3) effect = 1
                player.triggerEffect(EFFECT, self, Bonus.action(effect).with(Item.MONEY, 2).draw(effect))
            }
        }

    @Dominion_Card(extension = "Intrigue")
    fun Courtier() = Card.action("Courtier", Price.intrigue(5))
        .setup { 
            onPlay(OnPlayComponent{_, _ -> }
                .chooseCardFromHand(interactionRequest = InteractionRequest("Reveal a card"))
                .result().mapPipe { _, chosen -> Transformation(chosen, chosen.types.size) }
                .filterPipeNotNull()
                .thenWithPipe { player, _ ->
                    val count = current.result ?:0
                    val buttons = mutableListOf<Button>().apply {
                        add(Button("+1 Action", "action"))
                        add(Button("+3 Money", "money"))
                        add(Button("+1 Buy", "buy"))
                        add(Button("Gain a Gold", "gold"))
                    }
                    for ( i in 0 until count) {
                        if(buttons.isEmpty())break
                        player.chooseWhatToDo(
                            instruction = "Choice ${i + 1} / $count",
                            list = listOfNotNull(current.self),
                            buttons = buttons,
                        ).also { choice ->
                            when (choice) {
                                "action" -> player.increment(Item.ACTION)
                                "buy" -> player.increment(Item.BUY)
                                "money" -> player.increment(Item.MONEY, 3)
                                "gold" -> player.gainFromSupply(cardName = "Gold")
                            }
                            buttons.removeIf { it.value == choice }
                        }
                    }
                }
                .end()
            )
        }

    @Dominion_Card(extension = "Intrigue")
    fun Courtyard() : Card {
        val draw = Bonus.draw(3)
        return Card.action("Courtyard", Price.intrigue(2))
            .setup { 
                onPlay(draw.onPlay()
                    .chooseCardFromHand(interactionRequest = InteractionRequest("Put a card onto your deck"))
                    .thenWith { player, card ->  player.moveTo(card, Destination.PlayerZone.Draw) }
                    .end()
                
                )
            }
    }

    @Dominion_Card(extension = "Intrigue")
    fun Diplomat() = Card("Diplomat", Price.intrigue(4), CardType.ACTION, CardType.REACTION)
        .setup { 
            onPlay(OnPlayComponent{_, _ -> }
                .lookingAt { player, _ -> if (player.getList(Destination.PlayerZone.Hand).size <= 5) 2 else 0  }
                .thenDo { player,self, actions ->
                    val bonus = Bonus.draw(2).with(Item.ACTION, actions)
                    player.triggerEffect(EFFECT,self, bonus )
                }
                .end()
            )
            onCardPlayed{
                onEffect{ owner, _ ->
                    owner.reveals(scope)
                    owner.draw(2)
                    owner.discardFromHand(3)
                }

                onCondition { event, player -> event.player != player
                        && event.cardHasType(CardType.ATTACK)
                        && player.getList(Destination.PlayerZone.Hand).size >= 5
                        && scope.hasForLocation(Destination.PlayerZone.Hand)
                }
            }
        }

    @Dominion_Card(extension = "Intrigue", pileType = PileType.VICTORY)
    fun Duke() = Card.victory("Duke", Price.intrigue(5))
        .setup { score { player -> player.getList(Destination.PlayerZone.Hand).count{card -> card.hasName("Duchy")} } }

    @Dominion_Card(extension = "Intrigue", pileType = PileType.VICTORY)
    fun Farm() : Card {
        val money = Bonus.money(2)
        return Card("Farm", Price.intrigue(6), CardType.TREASURE, CardType.VICTORY)
            .setup { 
                onPlay(money.onPlay())
                score { 2 }
            }
    }

    @Dominion_Card(extension = "Intrigue")
    fun Ironworks() = Card.action("Ironworks", Price.intrigue(4))
        .setup { 
            onPlay(OnPlayComponent{_, _ -> }
                .lookingAt { player, _ -> player.gainFromSupply("Gain a card costing to 4$", filter = {it.isAtMost(4)}, Destination.PlayerZone.Discard) }
                .filterNotNull()
                .thenWith { player, card ->
                    val bonus = Bonus.empty().apply {
                        if(card.hasType(CardType.ACTION)) with(Item.ACTION)
                        if(card.hasType(CardType.TREASURE)) with(Item.MONEY)
                        if(card.hasType(CardType.VICTORY)) draw()
                    }

                    player.triggerEffect(EFFECT, scope, bonus)

                }
                .end()

            )
        }

    @Dominion_Card(extension = "Intrigue")
    fun Lurker() = Card.action("Lurker", Price.intrigue(2))
        .setup { 
            onPlay(
                Bonus.Action.onPlay()
                .chooseWhatToDo{_, self -> InteractionRequest(
                    instruction = "Trash an action card from the supply or take one from trash",
                    cards = listOf(self),
                    buttons = listOf(Button.Trash, Button("Gained", "g"))
                )}.filterOrOtherwise { _, _, _, string -> string == "t" }
                .thenDo { player, _, _, _->
                    player.chooseCardFromSupply(
                        instruction = "Trash an action card rom the supply",
                        filter = {it.hasType(CardType.ACTION)}) ?.let{c -> player.trash(c)}
                }
                .otherwise { player, _, _ ->
                    player.game.trashedCards.filter{it.hasType(CardType.ACTION)}
                        .takeIf { it.isNotEmpty() }
                        ?.let { cards -> player.chooseCardFromList(
                            instruction = "Gain an action card from trash",
                            cards = cards
                            ) ?.let{c -> player.gain(c)}
                        } ?: player.log("No trashed Action card found")
                }
                .endParent()
            )
        }

    @Dominion_Card(extension = "Intrigue")
    fun Masquerade() : Card {
        val draw = Bonus.draw(2)
        return Card.action("Masquerade", Price.intrigue(3))
            .setup { 
                onPlay(draw.onPlay()
                    .then { player, _ ->
                        val pairs = mutableListOf<Pair<Player, Card>>()
                        player.game.processGlobalEffect(player){
                            it.chooseCardFromHand("Pass a card to your left")
                                ?.let { card ->
                                    pairs.add(Pair(it, card))
                                    it.moveToTemp(card)
                                }
                        }

                        pairs.forEach { pair ->
                            val (p, card) = pair
                            p?.let {
                                val left = player.game.onTheLeft(p)
                                left.moveTo(card, Destination.PlayerZone.Hand)
                            }
                        }
                    }
                    .chooseCardFromHand(interactionRequest = InteractionRequest("You may trash a card from your hand", canPass = true))
                    .thenWith(Player::trash)
                    .end()
                )
            }
    }

    @Dominion_Card(extension = "Intrigue", pileType = PileType.VICTORY)
    fun Mill() : Card {
        val actionDraw = Bonus.action().draw()
        val additionalMoney = Bonus.money(2)
        return Card.action("Mill", Price.intrigue(4)).addType(CardType.VICTORY)
            .setup { 
                onPlay(actionDraw.onPlay()
                    .then {player, self ->
                        player.discardFromHand(2, nextAction = {
                            if(it == 2) player.triggerEffect(EFFECT, self, additionalMoney)
                        }, canPass = player.getList(Destination.PlayerZone.Hand).size < 2)
                    }
                )
                score { 1 }
            }
    }

    @Dominion_Card(extension = "Intrigue")
    fun MiningVillage() : Card {
        val actionDraw = Bonus.action(2).draw()
        val trashAward = Bonus.money(2)
        return Card.action("MiningVillage", Price.intrigue(4))
            .setup { 
                onPlay(actionDraw.onPlay()
                    .lookingAt { _, self -> self.hasForLocation(Destination.PlayerZone.InPlay) }
                    .filter { it }
                    .chooseWhatToDo { _, self -> InteractionRequest(
                        instruction = "You may trash Mining Village to gain 2$",
                        cards = listOf(self),
                        canPass = true
                    ) }
                    .filterWith{player, self, string -> "y" == string && player.trash(self) }
                    .thenDo { player, self -> player.triggerEffect(TRASHED_ACTION, self, trashAward) }
                    .end()
                )

            }
    }

    @Dominion_Card(extension = "Intrigue")
    fun Minion() : Card {
        val money = Bonus.money(2)
        return Card.attack("Minion", Price.intrigue(5))
            .setup { 
                onPlay(
                    Bonus.Action.onPlay()
                    .chooseWhatToDo { _, self -> InteractionRequest(
                        instruction = "Choose : +2 Money or attack others",
                        cards = listOf(self),
                        buttons = listOf(Button("+2$", "m"), Button("attack", "a"))
                    ) }.filterOrOtherwise { _, _, _, string -> string == "m"  }
                    .thenDo { player, card, _, _ -> player.triggerEffect(ACTION, card, money) }
                    .otherwise { player, card, _ ->
                        val action = BiEffect.empty<Logger, Card>()
                            .discardAll(Destination.PlayerZone.Hand)
                            .draw(4)
                            .attack { _, opponent, _ ->
                                if(opponent.getList(Destination.PlayerZone.Hand).size >= 5){
                                opponent.discardAll(Destination.PlayerZone.Hand)
                                opponent.draw(4) }
                            }.end()

                        action(player, card)
                    }
                    .endParent()
                )
            }
    }

    @Dominion_Card(extension = "Intrigue", pileType = PileType.VICTORY)
    fun Nobles() = Card.action("Nobles", Price.intrigue(6)).addType(CardType.VICTORY)
        .setup { 
            onPlay(BiEffect.empty<Player, Card>()
                .chooseWhatToDo { _, card -> InteractionRequest(
                    instruction = "Choose : draw 3 cards or 2 actions",
                    cards = listOf(card),
                    buttons = listOf(Button("Draw 3", "d"), Button("+2Action", "a"))
                )}
                .branch(
                    "d" to {player, _, _-> player.draw(3) },
                    "a" to {player, _, _ -> player.increment(Item.ACTION, 2)}
                )
                .end()
            )
            score { 2 }
        }

    @Dominion_Card(extension = "Intrigue")
    fun Patrol() : Card {
        val draw = Bonus.draw(3)
        return Card.action("Patrol", Price.intrigue(5))
            .setup { 
                onPlay(draw.onPlay()
                    .lookingAt { player, _ -> player.getTopCards(4)  }
                    .filterListAndNotEmpty()
                    .revealList()
                    .partition { it.hasType(CardType.CURSE) || it.hasType(CardType.VICTORY) }
                    .thenWith { player, pair ->
                        pair.first.forEach { player.moveTo(it, Destination.PlayerZone.Hand) }
                        player.moveAllAndChooseTheOrder(pair.second.toMutableList(), Destination.TempZone.Temp, Destination.PlayerZone.Draw)
                    }
                    .end()
                )

            }
    }

    @Dominion_Card(extension = "Intrigue")
    fun Pawn() = Card.action("Pawn", Price.intrigue(2))
        .setup { 
            onPlay{player, card ->
                val buttons = mutableListOf<Button>().apply {
                    add(Button("+1 Card", "card"))
                    add(Button("+1 Action", "action"))
                    add(Button("+1 Buy", "buy"))
                    add(Button("+$1", "money"))
                }

                repeat(2){
                    player.chooseWhatToDo("Choose two different options", listOf(card), buttons =  buttons)
                        .let {
                            when(it){
                                "card" -> player.draw()
                                "action" -> player.increment(Item.ACTION)
                                "buy" -> player.increment(Item.BUY)
                                "money" -> player.increment(Item.MONEY)
                            }

                            buttons.removeIf { b -> b.value == it }
                            player.log("${player.toLog} chooses $it")
                        }
                }
            }
        }

    @Dominion_Card(extension = "Intrigue")
    fun Replace() = Card.attack("Replace", Price.intrigue(5))
        .setup { 
            onPlay(BiEffect.empty<Player, Card>()
                .chooseCardFromHand(interactionRequest = InteractionRequest("Trash a card"))
                .filter(Player::trash).result
                .map { player, trashed ->
                    player.gainFromSupply(
                    instruction = "Gain a card costing up to ${trashed.current.costInstruction(2)}",
                    filter = {it.isAtMostWithBonus(trashed.current, 2)},
                    dest = Destination.PlayerZone.Discard
                ) }
                .branchDecision{
                    on { data.hasType(CardType.VICTORY) } then { player, self, _ ->
                        player.game.processAttack(player, self) { v -> v.gainFromSupply("Curse") }
                    }
                    on { player.getList(Destination.PlayerZone.Discard).contains(data)} then {player, _, data ->
                        player.moveTo(data, Destination.PlayerZone.Draw)
                    }
                }
                .end()
            )
        }

    @Dominion_Card(extension = "Intrigue")
    fun SecretPassage() : Card {
        val actionDraw = Bonus.action().draw(2)

        return Card.action("Secret Passage", Price.intrigue(4))
            .setup { 
                onPlay(actionDraw.onPlay()
                    .chooseCardFromHand(interactionRequest = InteractionRequest("Put a card in your deck where you want"))
                    .thenChooseCardFromList { player, _, _, chosen -> InteractionRequest(
                        instruction = "Where ($chosen) ?",
                        cards = player.getList(Destination.PlayerZone.Draw)
                    ) }.result
                    .filterPipeNotNull()
                    .thenWithPipe { player, _ -> player.putCardInDraw(current, extraData) }
                    .end()
                )

            }
    }

    @Dominion_Card(extension = "Intrigue")
    fun ShantyTown() : Card {
        val action = Bonus.action(2)
        val noActionInHand = Bonus.draw(2)
        return Card.action("Shanty Town", Price.intrigue(3))
            .setup { 
                onPlay(action.onPlay()
                    .lookingAt { player, _ -> player.getList(Destination.PlayerZone.Hand).any { it.hasType(CardType.ACTION) } }
                    .filter { !it }
                    .so { player, self -> player.triggerEffect(ACTION, self, noActionInHand) }
                    .end()
                )
            }
    }

    @Dominion_Card(extension = "Intrigue")
    fun Steward() = Card.action("Steward", Price.intrigue(3))
        .setup { 
            onPlay(BiEffect.empty<Player, Card>()
                .chooseWhatToDo { _ , self -> InteractionRequest(
                    instruction = "Choose : +2 draw or +2 money or trash 2 cards",
                    cards = listOf(self),
                    buttons = listOf(Button("+2 Draw", "d"), Button.Money, Button.Trash)
                ) }
                .branch(
                    "d" to {player, _, _ -> player.draw(2)},
                    "m" to {player, _, _ -> player.increment(Item.MONEY, 2)},
                    "t" to {player, _ , _-> player.trash(2)}
                )
                .end()
            )
        }

    @Dominion_Card(extension = "Intrigue")
    fun Swindler() : Card {
        val money = Bonus.money(2)
        return Card.attack("Swindler", Price.intrigue(3))
            .setup { 
                onPlay(money.onPlay()
                    .attack { attacker, opponent, card ->
                        val effect = BiEffect.empty<Player, Card>()
                            .lookingAt { _, _ -> opponent.getCardFromDeck() }
                            .filterNotNull{opponent.trash(it)}
                            .thenWith { player, trashed ->
                                player.forceGainFromSupply(
                                    opponent = opponent,
                                    instruction = "$player, Choose a card for $opponent costing exactly ${trashed.costInstruction()}",
                                    filter = {it.isEqualWithBonus(trashed)},
                                    dest = Destination.PlayerZone.Discard
                                )
                            }.end()

                        effect(attacker, card)

                    }.end()
                )
            }
    }

    @Dominion_Card(extension = "Intrigue")
    fun Torturer() : Card {
        val draw = Bonus.draw(3)
        return Card.attack("Torturer", Price.intrigue(5))
            .setup { 
                onPlay(draw.onPlay()
                    .attack { _, opponent, self ->
                        val action = BiEffect.empty<Player, Card>()
                            .chooseWhatToDo { player, self -> InteractionRequest(
                                instruction = "${player}, Choose : Discard 2 cards or gain a Curse into your hand",
                                cards = listOf(self),
                                buttons = listOf(Button(" 2 Discard", "discard"), Button("Curse", "curse"))
                            )}
                            .branch(
                                "discard" to {player, _, _-> player.discardFromHand(2)},
                                "curse" to {player, _ , _-> player.gainFromSupply("Curse", dest = Destination.PlayerZone.Hand) }
                            )
                            .end()

                        action(opponent, self )
                    }
                    .end()
                )
            }
    }

    @Dominion_Card(extension = "Intrigue")
    fun TradingPost() = Card.action("Trading Post", Price.intrigue(3))
        .setup { 
            onPlay{player, _ ->
                player.trashAndDo(instruction = "to gain a silver", number =  2, from = Destination.PlayerZone.Hand, canPass = false){
                    if(it == 2) player.gainFromSupply("Silver", Destination.PlayerZone.Hand)
                }

            }
        }

    @Dominion_Card(extension = "Intrigue")
    fun Upgrade() : Card {
        val actionDraw = Bonus.action().draw()
        return Card.action("Upgrade", Price.intrigue(5))
            .setup { 
                onPlay(actionDraw.onPlay()
                    .chooseCardFromHand(interactionRequest = InteractionRequest("Trash a card from your hand"))
                    .filter(Player::trash)
                    .thenWith { player, trashed ->
                        player.gainFromSupply(
                            instruction = "$player, Gain to your hand a card costing exactly ${trashed.costInstruction(1)}",
                            filter = {it.isEqualWithBonus(trashed, 1)},
                            dest = Destination.PlayerZone.Hand
                        )
                    }
                    .end()
                )

            }
    }

    @Dominion_Card(extension = "Intrigue")
    fun Wishing_Well() : Card {
        val actionDraw = Bonus.action().draw()
        return Card.action("Wishing Well", Price.intrigue(3))
            .setup { 
                onPlay(actionDraw.onPlay()
                    .then {
                        getCardFromDeck()?.let { card ->
                            val s = choose("In Channel, write a card name").lowercase().replaceFirstChar { it.uppercase() }
                            if(card.hasName(s)) moveTo(card, Destination.PlayerZone.Hand)
                        }
                    }
                )
            }
    }
    @Dominion_Card(extension = "Intrigue")
    fun Coppersmith() = Card.action("Coppersmith", Price.intrigue(4))
        .setup { 
            onPlay{_, self ->
                val i = self.getValue("copperIncrement").toInt()
                self.set("copperIncrement", i+1)
            }
            onCardPlayed {
                onEffect { owner, _ -> owner.increment(Item.MONEY, scope.getValue("copperIncrement").toInt()) }
                onCondition { event, player -> player == event.player && event.cardHasName("Copper") }
            }
        }

    @Dominion_Card(extension = "Intrigue", pileType = PileType.VICTORY)
    fun GreatHall() : Card {
        val actionDraw = Bonus.action().draw()
        return Card.victory("Great Hall", Price.intrigue(3)).addType(CardType.ACTION)
            .setup {
                onPlay(actionDraw.onPlay())
                score {1}
            }
    }
    @Dominion_Card(extension = "Intrigue")
    fun Saboteur() = Card.attack("Saboteur", Price.intrigue(5))
        .setup { 
            onPlay(BiEffect.empty<Player, Card>()
                .attack { _, opponent, _ ->
                    opponent.discardUntil({it.isAtLeast(3)}){
                        opponent.reveals(it)
                        opponent.trash(it)
                        opponent.gainFromSupply(
                            instruction = "$opponent, You may gain a card costing up to ${it.costInstruction(-2)}",
                            filter = {card -> card.isAtMostWithBonus(it, -2)},
                            dest = Destination.PlayerZone.Discard,
                            canPass = true
                            )
                    }
                }.end()
            )
        }
    @Dominion_Card(extension = "Intrigue")
    fun Scout() = Card.action("Scout", Price.intrigue(4))
        .setup { 
            onPlay(
                Bonus.Action.onPlay()
                .lookingAt { player, _ -> player.getTopCards(4) }.filterListAndNotEmpty()
                .partition { it.hasType(CardType.VICTORY) }
                .thenWith { player, pair ->
                    val(victory, others) = pair
                    victory.forEach { player.moveTo(it, Destination.PlayerZone.Hand) }
                    player.moveAllAndChooseTheOrder(others.toMutableList(), Destination.TempZone.Temp, Destination.PlayerZone.Draw)
                }
                .end()
            )
        }

    @Dominion_Card(extension = "Intrigue")
    fun SecretChamber() = Card.action("Secret Chamber", Price.intrigue(2)).addType(CardType.REACTION)
        .setup { 
            onPlay{ player, _ -> player.discardUntilYouStopAndDo(Destination.PlayerZone.Hand){ increment(Item.MONEY) } }
            onCardPlayed {
                onEffect( TriggerComponent.OnCardPlayed{_, event -> scope.set("LAST_ID", event.id)}
                    .draw(2)
                    .then { moveTo(Destination.PlayerZone.Hand, Destination.PlayerZone.Draw, "$this, Put 2 cards in your draw", 2) })
                onCondition { event, player -> !event.isSamePlayer(player) && event.cardHasType(CardType.ATTACK) && !event.isSameId(scope.getValue("LAST_ID").toInt()) }
            }
        }

    @Dominion_Card(extension = "Intrigue")
    fun Tribute() = Card.action("Tribute", Price.intrigue(5))
        .setup { 
            onPlay(BiEffect.empty<Player, Card>()
                .lookingAt { player, _ -> player.game.onTheLeft(player) }
                .map { _, _, left -> NonNullPair(left, left.getTopCards(2))  }
                .thenWith { player, pair ->
                    val(left, cards) = pair
                    val processedNames = mutableSetOf<String>()
                    cards.forEach {
                        left.moveTo(it, Destination.PlayerZone.Discard)

                        if(it.name !in processedNames){
                        processedNames += it.name
                        if(it.hasType(CardType.VICTORY)) player.draw(2)
                        if(it.hasType(CardType.ACTION)) player.increment(Item.ACTION, 2)
                        if(it.hasType(CardType.TREASURE)) player.increment(Item.MONEY, 2)
                        }
                    }
                }
                .end()
            )
        }
}