package fr.umontpellier.iut.dominion.cards.factories.dominion

import fr.umontpellier.iut.dominion.Annotation.Description
import fr.umontpellier.iut.dominion.Annotation.Dominion_Card
import fr.umontpellier.iut.dominion.Annotation.InSet
import fr.umontpellier.iut.dominion.Annotation.PileType
import fr.umontpellier.iut.dominion.Button
import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.Skills.discard
import fr.umontpellier.iut.dominion.Player.Skills.discardFromHand
import fr.umontpellier.iut.dominion.Player.Skills.discardUntilYouStop
import fr.umontpellier.iut.dominion.Player.Skills.draw
import fr.umontpellier.iut.dominion.Player.Skills.gain
import fr.umontpellier.iut.dominion.Player.Skills.getCardsUntil
import fr.umontpellier.iut.dominion.Player.Skills.moveAll
import fr.umontpellier.iut.dominion.Player.Skills.moveAllAndChooseTheOrder
import fr.umontpellier.iut.dominion.Player.Skills.moveTo
import fr.umontpellier.iut.dominion.Player.Skills.playCard
import fr.umontpellier.iut.dominion.Player.Skills.reveals
import fr.umontpellier.iut.dominion.Player.Skills.trash
import fr.umontpellier.iut.dominion.cards.Bonus.Bonus
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.Description.BonusDescription
import fr.umontpellier.iut.dominion.cards.Description.CardCatalog
import fr.umontpellier.iut.dominion.cards.Description.GainDescription
import fr.umontpellier.iut.dominion.cards.Description.InstructionDescription
import fr.umontpellier.iut.dominion.cards.Description.InteractionDescription
import fr.umontpellier.iut.dominion.cards.Description.ReactionDescription
import fr.umontpellier.iut.dominion.cards.Description.Reward
import fr.umontpellier.iut.dominion.cards.Description.ScoreDescription
import fr.umontpellier.iut.dominion.cards.Description.ScoreType
import fr.umontpellier.iut.dominion.cards.Description.SecondaryEffect
import fr.umontpellier.iut.dominion.cards.Description.TargetPlayer
import fr.umontpellier.iut.dominion.cards.Description.TriggerCondition
import fr.umontpellier.iut.dominion.cards.builders.Context
import fr.umontpellier.iut.dominion.cards.builders.attack
import fr.umontpellier.iut.dominion.cards.builders.branchDecision
import fr.umontpellier.iut.dominion.cards.builders.filterListAndNotEmpty
import fr.umontpellier.iut.dominion.cards.component.InteractionRequest
import fr.umontpellier.iut.dominion.cards.component.OnPlayComponent
import fr.umontpellier.iut.dominion.cards.component.Price
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent
import fr.umontpellier.iut.dominion.cards.component.chooseCardFromHand
import fr.umontpellier.iut.dominion.cards.builders.filterNotNull
import fr.umontpellier.iut.dominion.cards.component.BiEffect
import fr.umontpellier.iut.dominion.cards.component.attack
import fr.umontpellier.iut.dominion.cards.component.benefit
import fr.umontpellier.iut.dominion.cards.component.chooseWhatToDo
import fr.umontpellier.iut.dominion.cards.component.gainFromSupply
import fr.umontpellier.iut.dominion.cards.component.lookingAt
import fr.umontpellier.iut.dominion.cards.component.loop
import fr.umontpellier.iut.dominion.cards.component.spyEffect
import fr.umontpellier.iut.dominion.cards.component.then
import fr.umontpellier.iut.dominion.cards.displayCoin
import fr.umontpellier.iut.dominion.cards.factories.follow
import fr.umontpellier.iut.dominion.cards.forEachSuspend
import fr.umontpellier.iut.dominion.cards.gainFromSupply
import fr.umontpellier.iut.dominion.cards.getTopCards
import fr.umontpellier.iut.dominion.cards.isNotIn
import fr.umontpellier.iut.dominion.cards.moveTo

object DominionFactoryKt {
    @Dominion_Card(extension = "Dominion")
    @InSet(value = ["Size Distortion", "Deck Top", "Improvements", "Grand Scheme", "Reach for Tomorrow", "Biggest Money"])
    fun Artisan() = Card.action("Artisan", Price.dominion(6))
        .setup {
            onPlay {
                onEffect(BiEffect.empty<Player, Card>()
                    .gainFromSupply()
                    .chooseCardFromHand(interactionRequest = InteractionRequest(instruction = "Put a card in your draw"))
                    .thenWith { player, chosen ->  player.moveTo(chosen, Destination.PlayerZone.Draw) }
                    .end())
            }
        }

    @Dominion_Card(extension = "Dominion")
    fun Bandit() = Card("Bandit", Price.dominion(5), CardType.ACTION, CardType.ATTACK)
        .setup { 
            onPlay{
                onEffect{player, self ->
                    player.gainFromSupply("Gold")
                    player.game.processAttackWithReveals(
                        attacker = player,
                        attackCard = self,
                        selector = {attacker, victim, options -> attacker.game.chooseCard(victim, options) }
                    )
                }
            }
        }

    @Dominion_Card(extension = "Dominion")
    fun Bureaucrat() = Card("Bureaucrat", Price.dominion(4), *CardType.ActionAndAttack)
        .setup { 
            onPlay {
                onEffect(
                    OnPlayComponent { player, _ -> player.gainFromSupply("Silver", Destination.PlayerZone.Draw) }
                            then { player, self -> player.game.checkHandOrShow(
                        attacker = player,
                        attackCard = self,
                        decision = {victim, options -> victim.chooseCardFromList("Put a card from this list into your deck", options) })
                })
            }
        }

    @Dominion_Card(extension = "Dominion")
    @InSet(value = ["First Game"])
    fun Cellar() = Card.action("Cellar", Price.dominion(2))
        .setup {
            onPlay{
                onEffect(Bonus.Action.onPlay()
                            then { player, _ -> player.discardUntilYouStop(Destination.PlayerZone.Hand, playerAction = player::draw)}
                ) }
        }

    @Dominion_Card(extension = "Dominion")
    fun Chapel() = Card.action("Chapel", Price.dominion(2))
        .setup {
            onPlay {
                onEffect(BiEffect.empty<Player, Card>()
                    .loop(4){counter ->
                        chooseCardFromHand { player, card ->
                            InteractionRequest(
                                instruction = "you may trash ${if(counter.current > 1) "again" else ""} ${counter.remaining} card(s) from your hand",
                                canPass = true)
                        }.thenWith { player, card -> player.trash(card) }
                    })
            }
        }


    @Dominion_Card(extension = "Dominion")
    fun Council_Room() : Card {
        val buyDraw = Bonus.buy().draw(4)
        return Card.action("Council Room", Price.dominion(5))
            .setup {
                onPlay(buyDraw.onPlay().benefit(Player::draw)
            ) }
    }

    @Dominion_Card(extension = "Dominion")
    fun Festival() : Card {
        val actionBuyMoney = Bonus.action(2).with(Item.BUY).with(Item.MONEY)
        return Card.action("Festival", Price.dominion(5))
            .setup { simpleAction(actionBuyMoney) }
    }

    @Dominion_Card(extension = "Dominion", pileType = PileType.VICTORY)
    fun Gardens () = Card.victory("Gardens", Price.dominion(4)).setup { score { player -> player.allOwnedCards.size/10 } }

    @Dominion_Card(extension = "Dominion")
    fun Harbinger() : Card {
        val actionDraw = Bonus.action().draw()
        return Card.action("Harbinger", Price.dominion(3))
            .setup { 
                onPlay(actionDraw.onPlay()
                    .lookingAt { player, _ -> player.getDistinctCards(Destination.PlayerZone.Discard) }
                    .chooseCardFromList { _, _-> InteractionRequest(
                        instruction = "Put a card from your discard to your deck",
                        cards = this,
                        canPass = true )
                    }
                    .thenWith { player, chosen -> player.moveTo(chosen, Destination.PlayerZone.Draw)  }
                    .end()
                )
            }
    }

    @Dominion_Card(extension = "Dominion")
    fun Laboratory() : Card {
        val actionDraw = Bonus.action().draw(2)
        return Card.action("Laboratory", Price.dominion(5))
        .setup { 
            onPlay(actionDraw.onPlay()
                .then { player, _ ->
                    val sideTrack = mutableListOf<Card>()
                    while((player.getCopyOf(Destination.PlayerZone.Hand)?.size?:0) < 7){
                        val drawn: Card = player.getCardFromDeck() ?: break

                        if(drawn.hasType(CardType.ACTION)){
                            player.chooseWhatToDo(
                                instruction = "Choose : put this card aside or take it in your hand",
                                list = listOf(drawn),
                                buttons = listOf(Button("aside", "a"), Button.Hand)
                            ).takeIf { "a" == it }
                                ?.let {
                                    player.moveTo(drawn, Destination.PlayerZone.Aside)
                                    sideTrack.add(drawn)
                                } ?: run { player.moveTo(drawn, Destination.PlayerZone.Hand) }
                        } else player.moveTo(drawn, Destination.PlayerZone.Hand)
                    }

                    sideTrack.forEach { player.moveTo(it, Destination.PlayerZone.Discard) }
                }
            )
        }
    }

    @Dominion_Card(extension = "Dominion")
    @InSet(value = ["First Game"])
    fun Market() : Card {
        val allBasicBonus = Bonus.action().with(Item.BUY).with(Item.MONEY).draw()
        return Card.action("Market", Price.seaside(5))
            .setup { simpleAction(allBasicBonus) }
    }

    @Dominion_Card(extension = "Dominion")
    @InSet(value = ["First Game"])
    fun Merchant() : Card {
        val actionDraw = Bonus.action().draw()
        return Card.action("Merchant", Price.seaside(3))
            .setup { 
                onPlay(actionDraw.onPlay()
                    .then { _, self -> self.set("used", false); self.set("amount", self.getValue("amount").toInt() + 1) }
                )

                onCardPlayed{
                    onEffect { owner, _ ->
                        owner.incrementByAction(Item.MONEY) { scope.getValue("amount").toInt() }
                        scope.set("used", true)
                    }
                    onCondition { event, player -> event.player == player && event.cardHasName("Silver") && !scope.getFlag("used") }
                }
            }
    }

    @Dominion_Card(extension = "Dominion")
    @InSet(value = ["First Game"])
    fun Militia() : Card {
        val money = Bonus.money(2)
        return Card("Militia", Price.seaside(4), *CardType.ActionAndAttack)
            .setup { onPlay( money.onPlay()
                    then { player, self ->
                        player.game.processHandDown(player, self)
                    }
            ) }
    }

    @Dominion_Card(extension = "Dominion")
    @InSet(value = ["First Game"])
    fun Mine() = Card.action("Mine", Price.seaside(5))
        .setup { 
            onPlay(OnPlayComponent{_, _->}
                .chooseCardFromHand(interactionRequest = InteractionRequest(
                    instruction = "Trash a treasure from your hand",
                    filter = {it.hasType(CardType.TREASURE)},
                    canPass = true
                ))
                .filter(Player::trash)
                .thenWith { player, trashed ->
                    player.gainFromSupply(
                        instruction = "Gain a treasure costing up to ${trashed.costValue+3} $",
                        filter = {it.hasType(CardType.TREASURE) && it.isAtMostWithBonus(trashed, 3)},
                        dest = Destination.PlayerZone.Hand
                    )
                }
                .end()
            )
        }

    @Dominion_Card(extension = "Dominion")
    @InSet(value = ["First Game"])
    fun Moat() : Card {
        val draw = Bonus.draw(2)
        return Card("Moat", Price.seaside(2), CardType.ACTION, CardType.REACTION)
            .setup { 
                simpleAction(draw)
                immunity{
                    onEffect(object : TriggerComponent.Immunity {
                        override suspend fun revealed(player: Player, self: Card): Boolean {
                            player.chooseCardFromHand("reveal your moat to become immune", predicate = {it == self}, canPass = true)
                                ?.let {
                                    self.set("used", true)
                                }
                            return self.getFlag("used")
                        }
                    })
                    onCondition { event, player -> event.card?.hasType(CardType.ATTACK) == true && !scope.getFlag("used") }
                }
            }
    }
    @Dominion_Card(extension = "Dominion")
    fun MoneyLender() = Card.action("Moneylender", Price.seaside(4))
        .setup { 
            onPlay(OnPlayComponent{_, _->}
                .chooseCardFromHand(interactionRequest = InteractionRequest("Trash a copper for 3$", filter = {it.hasName("Copper")}))
                .filter(Player::trash)
                .so { it.increment(Item.MONEY, 3) }
                .end()
            )
        }

    @Dominion_Card(extension = "Dominion")
    fun Poacher() : Card {
        val actionMoneyDraw = Bonus.action().with(Item.MONEY).draw()
        return Card.action("Poacher", Price.seaside(4))
            .setup { onPlay(actionMoneyDraw.onPlay()
                .then { discardFromHand(game.stat.emptyPiles.value) }
            ) }
    }

    @Dominion_Card(extension = "Dominion")
    @InSet(value = ["First Game"])
    fun Remodel() = Card.action("Remodel", Price.seaside(4))
        .setup { 
            onPlay(OnPlayComponent{_, _->}
                .chooseCardFromHand(interactionRequest = InteractionRequest("Trash a card from your hand"))
                .filter(Player::trash)
                .thenWith { player, trashed ->
                    player.gainFromSupply(
                        instruction = "Gain a card costing up to ${trashed.costValue+2} $",
                        filter = {it.isAtMostWithBonus(trashed, 2)},
                        dest = Destination.PlayerZone.Discard
                    )
                }
                .end()
            )
        }

    @Dominion_Card(extension = "Dominion")
    fun Sentry() : Card {
        val actionDraw = Bonus.action().draw()
        return Card.action("Sentry", Price.seaside(5))
            .setup { 
                onPlay(actionDraw.onPlay()
                    .lookingAt { player, _ -> player.getTopCards(2) }
                    .filter { it.isNotEmpty() }
                    .thenWith { player, view ->
                        while (view.isNotEmpty()) {
                            player.chooseWhatToDo(
                                instruction = "Choose : Trash or Discard",
                                list = view,
                                buttons = listOf(Button.Discard, Button.Trash, Button("Done", "x") ),
                                canPass = false
                            ).takeIf { "t" == it || "d" == it }
                                ?.let { str ->
                                    player.chooseCardFromList("Which Card ?", view, canPass = true)
                                        ?.let {
                                            when (str) {
                                                "t" ->  player.trash(it)
                                                "d" ->  player.discard(it)
                                            }
                                            view.remove(it)
                                        }
                                } ?: break
                        }
                    }
                    .filter { it.isNotEmpty()}
                    .thenWith { player, view -> player.moveAllAndChooseTheOrder(view, Destination.TempZone.Temp, Destination.PlayerZone.Draw) }
                    .end()
                
                )
            }
    }

    @Dominion_Card(extension = "Dominion")
    @InSet(value = ["First Game"])
    fun Smithy() : Card {
        val draw = Bonus.draw(3)
        return Card.action("Smithy", Price.seaside(4)).setup { simpleAction(draw) }
    }

    @Dominion_Card(extension = "Dominion")
    fun Throne_Room() = Card.action("Throne Room", Price.seaside(4))
        .setup { 
            onPlay(OnPlayComponent{_, _->}
                .chooseCardFromHand(interactionRequest = InteractionRequest(
                    instruction = "Double a Action card from your hand",
                    filter = {it.hasType(CardType.ACTION)},
                    canPass = true
                ))
                .thenWith { player, self, chosen ->
                    player.playCard(chosen, 2)
                    self.follow(player ,chosen)
                }
                .end()

            )

            follower()
        }

    @Dominion_Card(extension = "Dominion")
    fun Vassal() : Card {
        val money = Bonus.money(2)
        return Card.action("Vassal", Price.seaside(3))
            .setup { 
                onPlay(money.onPlay()
                    .lookingAt { player, _ -> player.getCardFromDeck() }
                    .filterNotNull{!it.hasType(CardType.ACTION)}
                    .chooseWhatToDo { _, _ ->
                        InteractionRequest(
                            instruction = "Do you want to play ${this.name}?",
                            cards = listOf(this),
                            buttons = Button.yesOrNo
                        )
                    }
                    .branchDecision {
                        on {choice == "y"} then {player, _, card -> player.playCard(card)}
                        otherwise { player, _, card -> player.discard(card) }
                    }
                    .endParent()
                )
            }
    }

    @Dominion_Card(extension = "Dominion")
    @InSet(value = ["First Game"])
    fun Village() : Card {
        val actionDraw = Bonus.action(2).draw()
        return Card.action("Village", Price.seaside(3))
            .setup {simpleAction(actionDraw) }
    }

    @Dominion_Card(extension = "Dominion")
    fun Witch() : Card {
        val draw = Bonus.draw(2)
        return Card.attack("Witch", Price.seaside(5))
            .setup { 
                onPlay(draw.onPlay()
                    .attack { _, opponent, _ -> opponent.gainFromSupply("Curse")  }
                    .end()
                )
            }
    }

    @Dominion_Card(extension = "Dominion")
    @InSet(value = ["First Game"])
    fun Workshop() = Card.action("Workshop", Price.seaside(3))
        .setup { 
            onPlay{ player, _ ->
                player.gainFromSupply(
                    instruction = "Gain a card costing up to 4",
                    filter = {it.isAtMost(4)},
                    dest = Destination.PlayerZone.Discard )
        }}

    @Dominion_Card(extension = "Dominion")
    fun Adventurer() = Card.action("Adventurer", Price.seaside(6))
        .setup { 
            onPlay(OnPlayComponent{_, _->}
                .lookingAt { player, _ -> player.getCardsUntil(count = 2){it.hasType(CardType.TREASURE)} }
                .thenWith { player, cards ->
                    player.reveals(cards)
                    val (treasures, others) = cards.partition { it.hasType(CardType.TREASURE) }
                    treasures.forEach { player.moveTo(it, Destination.PlayerZone.Hand) }
                    others.forEach { player.discard(it) }
                }
                .end()
            )
        }

    @Dominion_Card(extension = "Dominion")
    fun Chancellor() : Card {
        val money = Bonus.money(2)
        return Card.action("Chancellor", Price.seaside(3))
            .setup { 
                onPlay(money.onPlay()
                    .chooseWhatToDo { _, self -> InteractionRequest(
                        instruction = "Do you want to put your draw into your discard ?",
                        cards = listOf(self),
                        buttons = Button.yesOrNo,
                        canPass = true
                    ) }.filter { "y" == it }
                    .so { player -> player.moveAll(Destination.PlayerZone.Draw, Destination.PlayerZone.Discard) }
                    .end()
                )
            }
    }

    @Dominion_Card(extension = "Dominion")
    fun Feast() = Card.action("Feast", Price.seaside(4))
        .setup { 
            onPlay{player, self ->
                player.trash(self)
                player.gainFromSupply(instruction = "Gain a card costing up to 5", filter = {it.isAtMost(5)}, dest = Destination.PlayerZone.Discard)
            }

        }

    @Dominion_Card(extension = "Dominion")
    fun Spy() : Card {
        val actionDraw = Bonus.action().draw()
        return Card.attack("Spy", Price.seaside(4))
            .setup { 
                onPlay(actionDraw.onPlay()
                    .spyEffect { player, _ -> player }
                    .attack { player, opponent, card ->
                        val pipeLine = BiEffect<Player, Card> { _, _ -> }.spyEffect { _, _ -> opponent }
                        val Context = Context(player, card, Unit)
                        pipeLine.function(Context)
                    }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = "Dominion")
    fun Thief() : Card = Card.attack("Thief", Price.seaside(4))
        .setup { 
            onPlay(OnPlayComponent { _, _ ->}
                .attack { attacker, opponent, card ->
                    val action = BiEffect.empty<Player, Card>()
                        .lookingAt { _, _ -> opponent.getTopCards(2)  }
                        .thenDo { cards -> opponent.reveals(cards)  }
                        .filterListAndNotEmpty { it.hasType(CardType.TREASURE) }
                        .chooseCardFromList { _, _ -> InteractionRequest(
                            instruction = "Trash a card from ${opponent.name}",
                            cards = this,
                            canPass = true
                        ) }
                        .filter(opponent::trash)
                        .thenChooseWhatToDo { _, _, _,  trashed -> InteractionRequest(
                            instruction = "Do you want to gain this card ?",
                            cards = listOf(trashed),
                            buttons = Button.yesOrNo,
                            canPass = true
                        ) }.filterExtraData { "y" == it }
                        .alsoDo {_ , _,  cards, _ -> cards.filter { it.isNotIn(Destination.Trash) }.forEachSuspend(opponent::discard) }
                        .thenWith { player, card -> player.gain(card) }
                        .end()

                    action(attacker, card)
                }
                .end()
            )
        }


    @Dominion_Card(extension = "Dominion")
    fun  Woodcutter() : Card {
        val buyMoney = Bonus.buy().with(Item.MONEY, 2)
        return Card.action("Woodcutter", Price.seaside(3))
            .setup { simpleAction(buyMoney) }
    }

    @Description
    fun dominionDescription(){
        CardCatalog.apply {
            configure("Artisan"){
                + InstructionDescription().top {
                    +GainDescription()
                        .text("Gain a card to your hand costing up to ${displayCoin(5)}")
                        .upToCost(5)
                        .to(Destination.PlayerZone.Hand)

                    +InteractionDescription()
                        .text("Put a card from your hand onto your deck")
                        .from(Destination.PlayerZone.Hand)
                        .moveTo(Destination.PlayerZone.Draw)
                        .amount(1)
                }
            }


            configure("Bandit"){
                + InstructionDescription().top {
                    +GainDescription()
                        .text("Gain a Gold.")
                        .card("Gold")

                    +InteractionDescription()
                        .text("Each other player reveals the top 2 cards of their deck, trashes a revealed Treasure other than Copper, and discards the rest.")
                        .target(TargetPlayer.OTHER_PLAYERS)
                        .from(Destination.PlayerZone.Draw)
                        .amount(2)
                        .filter(CardType.TREASURE)
                        .exclude("Copper")
                        .moveTo(Destination.Trash)
                        .secondaryEffect(SecondaryEffect.DiscardRemaining)
                }
            }

            configure("Bureaucrat"){
                + InstructionDescription().top {
                    +GainDescription()
                        .text("Gain a Silver into your deck.")
                        .card("Silver")
                        .to(Destination.PlayerZone.Draw)

                    +InteractionDescription()
                        .text("Each other player reveals a Victory card from their hand and puts it onto their deck (or reveals a hand with no Victory cards).")
                        .target(TargetPlayer.OTHER_PLAYERS)
                        .filter(CardType.VICTORY)
                        .from(zone = Destination.PlayerZone.Hand)
                        .moveTo(Destination.PlayerZone.Draw)
                        .secondaryEffect(SecondaryEffect.RevealHandIfNoMatch())
                }
            }

            configure("Cellar"){
                + BonusDescription.Action
                + InstructionDescription().top {
                    +InteractionDescription()
                        .text("Discard any number of cards. +1 Card per card discarded")
                        .from(Destination.PlayerZone.Hand)
                        .moveTo(Destination.PlayerZone.Discard)
                        .optional(true)
                        .secondaryEffect(SecondaryEffect.DrawMatchingAmount())
                }
            }

            configure("Chapel"){
                + InstructionDescription().top {
                    +InteractionDescription()
                        .text("Trash up to 4 cards from your hand.")
                        .from(Destination.PlayerZone.Hand)
                        .moveTo(Destination.Trash)
                        .amount(4)
                        .optional(true)
                }
            }

            configure("Council Room"){
                +BonusDescription()
                    .cards(4)
                    .buys(1)
                +InstructionDescription().top {
                    +InteractionDescription()
                        .text("Each other player draws a card.")
                        .target(TargetPlayer.OTHER_PLAYERS)
                        .amount(1)
                        .secondaryEffect(SecondaryEffect.DrawCards())
                }
            }

            configure("Festival"){
                +BonusDescription()
                    .actions(2)
                    .buys(1)
                    .money(2)
            }

            configure("Gardens"){
                +ScoreDescription()
                    .text("Worth 1  VP per 10 cards you have (round down)")
                    .type(ScoreType.PerCardsCount(vp = 1, cardsStep = 10))
            }

            configure("Harbinger"){
                +BonusDescription.ActionAndDraw
                +InstructionDescription().top {
                    +InteractionDescription()
                        .text("Look through your discard pile. You may put a card from it onto your deck.")
                        .from(Destination.PlayerZone.Discard)
                        .moveTo(Destination.PlayerZone.Draw)
                        .optional(true)
                        .amount(1)
                }
            }

            configure("Laboratory"){
                +BonusDescription()
                    .cards(2)
                    .actions(1)
            }

            configure("Library"){
                +InstructionDescription().top {
                    +InteractionDescription()
                        .text("Draw until you have 7 cards in hand, skipping any Action cards you choose to; set those aside, discarding them afterwards.")
                        .secondaryEffect(
                            SecondaryEffect.DrawUntil(
                                targetHandSize = 7,
                                skippableType = CardType.ACTION,
                                skippedDestination = Destination.PlayerZone.Discard
                            )
                        )
                }
            }

            configure("Market"){
                +BonusDescription()
                    .cards(1)
                    .actions(1)
                    .buys(1)
                    .money(1)
            }

            configure("Merchant") {
                + BonusDescription.ActionAndDraw

                + InstructionDescription().top {
                    + ReactionDescription()
                        .text("The first time you play a Silver this turn, ${displayCoin(1)}.")
                        .trigger(TriggerCondition.FirstCardPlayed(cardName = "Silver"))
                        .effect(Reward.BonusCoins(1))
                }
            }

            configure("Militia"){
                +BonusDescription()
                    .money(2)
                + InstructionDescription().top {
                    + InteractionDescription()
                        .text("Each other player discards down to 3 cards in hand.")
                        .target(TargetPlayer.OTHER_PLAYERS)
                        .from(Destination.PlayerZone.Hand)
                        .moveTo(Destination.PlayerZone.Discard)
                        .secondaryEffect(SecondaryEffect.DiscardDownTo(targetHandSize = 3))
                }
            }

            configure("Mine"){
                +InstructionDescription().top {
                    +InteractionDescription()
                        .text("You may trash a Treasure from your hand.")
                        .from(Destination.PlayerZone.Hand)
                        .moveTo(Destination.Trash)
                        .optional(true)
                        .amount(1)
                        .filter(CardType.TREASURE)

                    +GainDescription()
                        .text("Gain a Treasure to your hand costing up to +${displayCoin(3)} more than it.")
                        .to(Destination.PlayerZone.Hand)
                        .count(1)
                        .relativeCost(3)
                        .ofType(CardType.TREASURE)
                }
            }

            configure("Moat"){
                +BonusDescription()
                    .cards(2)
                +InstructionDescription().bottom {
                    
                }
            }

            configure("Moneylender"){
                + InstructionDescription().top {
                    +InteractionDescription()
                        .text("You may trash a Copper from your hand for +${displayCoin(3)}.")
                        .from(Destination.PlayerZone.Hand)
                        .moveTo(Destination.Trash)
                        .card("Copper")
                        .optional(true)
                        .amount(1)
                        .secondaryEffect(Reward.BonusCoins(3))
                }
            }
        }
    }



}