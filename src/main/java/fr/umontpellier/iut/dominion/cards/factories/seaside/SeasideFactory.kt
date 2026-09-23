package fr.umontpellier.iut.dominion.cards.factories.seaside

import fr.umontpellier.iut.dominion.Annotation.Dominion_Card
import fr.umontpellier.iut.dominion.Button
import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.PlayerComponent.cardGainedCurrentTurn
import fr.umontpellier.iut.dominion.Player.PlayerComponent.cardGainedLastTurn
import fr.umontpellier.iut.dominion.Player.Skills.discard
import fr.umontpellier.iut.dominion.Player.Skills.discardFromHand
import fr.umontpellier.iut.dominion.Player.Skills.draw
import fr.umontpellier.iut.dominion.Player.Skills.drawTo
import fr.umontpellier.iut.dominion.Player.Skills.gain
import fr.umontpellier.iut.dominion.Player.Skills.moveAll
import fr.umontpellier.iut.dominion.Player.Skills.moveAllAndChooseTheOrder
import fr.umontpellier.iut.dominion.Player.Skills.moveTo
import fr.umontpellier.iut.dominion.Player.Skills.playCard
import fr.umontpellier.iut.dominion.Player.Skills.reveals
import fr.umontpellier.iut.dominion.Player.Skills.trash
import fr.umontpellier.iut.dominion.cards.Bonus.Bonus
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.CardConfigurator.Companion.bonus
import fr.umontpellier.iut.dominion.cards.builders.filterPipe
import fr.umontpellier.iut.dominion.cards.builders.listIsNotEmpty
import fr.umontpellier.iut.dominion.cards.component.BiEffect
import fr.umontpellier.iut.dominion.cards.component.Price
import fr.umontpellier.iut.dominion.cards.component.DurationComponent
import fr.umontpellier.iut.dominion.cards.component.InteractionRequest
import fr.umontpellier.iut.dominion.cards.component.OnPlayComponent
import fr.umontpellier.iut.dominion.cards.component.Pair
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent
import fr.umontpellier.iut.dominion.cards.component.chooseCardFromHand
import fr.umontpellier.iut.dominion.cards.component.chooseWhatToDo
import fr.umontpellier.iut.dominion.cards.component.lookingAt
import fr.umontpellier.iut.dominion.cards.component.lookingAtPair
import fr.umontpellier.iut.dominion.cards.component.map
import fr.umontpellier.iut.dominion.cards.component.then
import fr.umontpellier.iut.dominion.cards.executeAmbassador
import fr.umontpellier.iut.dominion.cards.factories.EFFECT
import fr.umontpellier.iut.dominion.cards.factories.activate
import fr.umontpellier.iut.dominion.cards.forEachSuspend
import fr.umontpellier.iut.dominion.cards.gainFromSupply
import fr.umontpellier.iut.dominion.cards.gainMultiplyCardFromSupply
import fr.umontpellier.iut.dominion.cards.getBottomCards
import fr.umontpellier.iut.dominion.cards.getTopCards
import fr.umontpellier.iut.dominion.cards.plusAssign
import fr.umontpellier.iut.dominion.cards.triggerEffect
import java.util.concurrent.atomic.AtomicBoolean

object SeasideFactory {
    @Dominion_Card(extension = "Seaside")
    fun Ambassador() = Card("Ambassador", Price.seaside(3), CardType.ACTION, CardType.ATTACK)
        .setup { onPlay(Player::executeAmbassador) }


    @Dominion_Card(extension = "Seaside")
    fun Astrolabe() : Card {
        val bonus = Bonus().with(Item.BUY, 1).with(Item.MONEY)
        return Card("Astrolabe", Price.seaside(3), CardType.TREASURE, CardType.DURATION )
            .setup { registerSimplePlayAndDuration(bonus) }
    }

    @Dominion_Card(extension = "Seaside")
    fun Bazaar() : Card {
        val action = Bonus().with(Item.ACTION, 2).with(Item.MONEY).draw()
        return Card.action("Bazaar", Price.seaside(5))
            .setup { simpleAction(action) }
    }

    @Dominion_Card(extension = "Seaside")
    fun Blockade() : Card {
        return Card("Blockade", Price.seaside(4), CardType.ACTION, CardType.ATTACK, CardType.DURATION)
            .setup {
                onPlay(OnPlayComponent { player, self -> self.set("Players", player.game.scanImmunity(player, self)) }
                        then { player, self -> 
                            player.chooseCardFromSupply("Put a curse on a pile that cost at most 4$", { it.isAtMost(4) })
                                ?.let { 
                                    player.moveTo(it, Destination.PlayerZone.Aside)
                                    player.addCardToShadowZone(self, it)
                                } 
                        }
                )
                onDuration {
                    onEffect(DurationComponent.Duration{_, _ ->}
                        .lookingAt { player, card -> player.getShadowList(card) }.listIsNotEmpty()
                        .thenWith { player, list -> player.moveAll(list, Destination.PlayerZone.Hand) }
                        .sendMessage(Player.clearShadowZone, {_, self, _ -> self})
                        .end()
                    )
                    withTrigger { player, card -> player.getShadowList(card).isNotEmpty() }
                }

                afterGain {
                    onEffect(TriggerComponent.AfterPlayerGain{_, _ -> }
                        .lookingAt { _, event -> event.player  }
                        .thenDo { victim -> victim.gain(victim.getCardFromSupply("Curse")) }.end()
                    )
                    onCondition { event, player ->
                        val blocked = player.getShadowList(scope)
                        if(blocked.isEmpty())return@onCondition false

                        blocked.any { card -> card.hasForLocation(Destination.PlayerZone.Aside) } &&
                                blocked.any { card -> event.card?.hasSameNameAs(card)?:false } &&
                                event.player != player &&
                                event.player.isActive

                    }
                }
            }
    }

    @Dominion_Card(extension = "Seaside")
    fun Caravan() : Card {
        val action = Bonus.action().draw()
        val draw = Bonus().draw()
        return Card("Caravan", Price.seaside(4), CardType.ACTION, CardType.DURATION)
            .setup { registerSimplePlayAndDuration(action, draw) }
    }
    @Dominion_Card(extension = "Seaside")
    fun Corsair() : Card {
        val play = Bonus.money(2)

        return Card("Corsair", Price.seaside(5), CardType.ACTION, CardType.DURATION, CardType.ATTACK)
            .setup { 
                simpleDuration(Bonus.draw)
                onPlay{ player, self ->
                    player.triggerEffect(EFFECT, self, play)
                    self.set("Players", player.game.scanImmunity(player, self))
                }
                onCardPlayed {
                    onEffect{owner, event ->
                        val actor = event.player
                        if(!actor.isFlagSet("${event.scope.id}")){
                            if(event.test { owner.game.isImmune(it, actor) }) return@onEffect
                            event.card.takeIf { actor.trash(it) }?.let {
                                actor.getFlag("${event.scope.id}") += true
                            }
                        }}
                    onCondition { event, player -> player != event.player && (event.cardHasName("Gold") || event.cardHasName("Silver")) }
                }
            }
    }

    @Dominion_Card(extension = "Seaside")
    fun Cutpurse () : Card {
        val money = Bonus.money(2)
        return Card("Cutpurse", Price.seaside(4), CardType.ACTION, CardType.ATTACK)
            .setup { 
                onPlay(bonus(money)
                    .then { player, self ->
                        player.triggerEffect(EFFECT, self, money)
                        player.game.checkHandOrShow(
                            player, self, {it.hasName("Copper")}) {
                            _, cards -> cards.first()
                        }
                    }
                )
            }
    }
    @Dominion_Card(extension = "Seaside")
    fun Embargo () : Card {
        val money = Bonus.money(2)
        return Card.action("Embargo", Price.seaside(2))
        .setup {
            onPlay(bonus(money)
                .chooseWhatToDo { _, self ->
                    InteractionRequest(
                        instruction = "Veux tu écarter cette carte pour poser une malédiction sur une des pile de la réserve",
                        cards = listOf(self),
                        buttons = Button.yesOrNo
                    )
                }.result().filterPipe{"y" == it}
                .so { player, self ->
                    player.chooseCardFromSupply("Curse a pile")
                        ?.let{ card -> if(player.trash(self)) player.game.setToken(card.name) }
                }
                .end()
            )
        }
    }
    @Dominion_Card(extension = "Seaside")
    fun Explorer() : Card =
        Card.action("Explorer", Price.seaside(5))
            .setup { 
                onPlay{ player, _ ->
                    val treasureGain : suspend (String) -> Unit = {string -> player.gainFromSupply(string, Destination.PlayerZone.Hand)}

                    val province = player.getCopyOf(Destination.PlayerZone.Hand)?.firstOrNull { it.hasName("Province") }

                    province?.let { prov ->
                        player.chooseWhatToDo("Reveal a Province", listOf(prov), buttons =  Button.yesOrNo)
                            .takeIf { "y" == it }
                            ?.let {
                                player.reveals(prov)
                                treasureGain("Gold")
                                true
                            }

                    } ?: run {treasureGain("Silver")}
                }
            }

    @Dominion_Card(extension = "Seaside")
    fun FishingVillage() : Card {
        val actionMoney = Bonus.action(2).with(Item.MONEY)
        val actionMoneyDuration = Bonus.action().with(Item.MONEY)

        return Card("Fishing Village", Price.seaside(3), CardType.ACTION,  CardType.DURATION)
            .setup {registerSimplePlayAndDuration(actionMoney, actionMoneyDuration)}
    }

    @Dominion_Card(extension = "Seaside")
    fun GhostShip() : Card {
        val draw = Bonus.draw(2)
        return Card("GhostShip", Price.seaside(5), CardType.ACTION, CardType.ATTACK)
        .setup { 
            onPlay(bonus(draw)
                .then { player, self -> player.game.processHandDown(player, self, Destination.PlayerZone.Draw, 3 ) }
            )
        }
    }
    @Dominion_Card(extension = "Seaside")
    fun Haven() : Card {
        val actionDraw = Bonus.action().draw()

        return Card("Haven", Price.seaside(2), CardType.ACTION, CardType.DURATION)
            .setup { 
                onPlay(bonus(actionDraw)
                    .chooseCardFromHand(interactionRequest = InteractionRequest("Choose a card from your hand to set Aside"))
                    .thenWith { player, self, card ->
                        player.addCardToShadowZone(self, card){ moveTo(it, Destination.PlayerZone.Aside) }
                        card.hide()
                    }
                    .end()
                )
                 onDuration {
                     onEffect(BiEffect.empty<Player, Card>()
                         .lookingAt { player, card -> player.getShadowList(card) }
                         .listIsNotEmpty()
                         .thenWith { player, cards -> player.moveAll(cards, Destination.PlayerZone.Hand){it.reveal()} }
                         .sendMessage(Player.clearShadowZone) { _, self, _ -> self }
                         .end()
                     )
                     withTrigger { player, card -> player.getShadowList(card).isNotEmpty() }
                 }
            }
    }
    @Dominion_Card(extension = "Seaside")
    fun Island() : Card = Card("Island", Price.seaside(4), CardType.ACTION, CardType.VICTORY)
        .setup { 
            onPlay(OnPlayComponent{ player, self -> player.moveTo(self, Destination.OtherZone.Island)}
                .chooseCardFromHand(interactionRequest = InteractionRequest("Move a card from your hand to your Island Mat"))
                .thenWith { player, card -> player.moveTo(card, Destination.OtherZone.Island) }
                .end()
            )
            score { 2 }
        }
    @Dominion_Card(extension = "Seaside")
    fun Lighthouse() : Card {
        val actionMoney = Bonus.action().with(Item.MONEY)
        return Card("Lighthouse", Price.seaside(2), CardType.ACTION, CardType.DURATION)
        .setup {
            registerSimplePlayAndDuration(actionMoney, Bonus.Money)
            immunity{
                onEffect(object : TriggerComponent.Immunity{ override suspend fun immune(player : Player, self: Card): Boolean = activate(player, self) })
            }
        }
    }
    @Dominion_Card(extension = "Seaside")
    fun Lookout() : Card  = Card.action("Lookout", Price.seaside(3))
        .setup { 
            onPlay(bonus(Bonus.Action)
                .lookingAt { player, _ -> player.getTopCards(3) }
                .filter(List<Card>::isNotEmpty)
                .thenWith { player, view ->
                    player.chooseCardFromList(instruction = "Trash a card", cards = view)?.let{
                        player.trash(it)
                        view.remove(it)
                    }

                    player.chooseCardFromList(instruction = "Discard a card", cards = view)?.let {
                        player.discard(it)
                        view.remove(it)
                    }

                }
                .end()
            )
        }
    @Dominion_Card(extension = "Seaside")
    fun Merchantship() : Card {
        val money = Bonus.money(2)
        return Card("Merchant Ship", Price.seaside(5), CardType.ACTION, CardType.DURATION)
            .setup{registerSimplePlayAndDuration(money)}
    }
    @Dominion_Card(extension = "Seaside")
    fun Monkey() : Card = Card("Monkey", Price.seaside(3), CardType.ACTION, CardType.DURATION)
        .setup { 
            simpleDuration(Bonus.draw)
            afterGain {
                onEffect{owner, _ -> owner.draw()}
                onCondition { event, player ->  event.player != player &&  player.game.onTheRight(player, event.player) && activate(player, scope)}
            }
        }
    @Dominion_Card(extension = "Seaside")
    fun NativeVillage() : Card {
        val actions = Bonus.action(2)
        return Card.action("Native Village", Price.seaside(2))
            .setup { 
                onPlay(actions.onPlay()
                    .chooseWhatToDo(interactionRequest = { player, _ ->
                        InteractionRequest(
                            instruction = "Add to or retrieve Native Village Mat",
                            cards = player.getCopyOf(Destination.OtherZone.Native)?: emptyList(),
                            buttons = listOf(Button("add", "add"), Button("take", "take"))
                        )
                    }).filter{"add" == it}
                    .so { player -> player.drawTo(Destination.OtherZone.Native)?.hide()}
                    .otherwise { player, _ -> player.getCopyOf(Destination.OtherZone.Native)?.forEach { player.moveTo(it, Destination.PlayerZone.Hand); it.reveal() }  }
                    .end()
            ) }
    }
    @Dominion_Card(extension = "Seaside")
    fun Navigator() : Card {
        val money = Bonus.money(2)
        return Card.action("Navigator", Price.seaside(4))
            .setup { 
                onPlay(bonus(money)
                    .lookingAt { player, _ -> player.getTopCards(5) }
                    .thenWith { player, view ->
                        player.chooseWhatToDo("Discard or replace in any order", list = view, buttons = listOf(Button("discard", "discard"), Button("replace", "replace")))
                            .takeIf { "discard" == it }
                            ?.let {
                                view.forEach { card -> player.discard(card) }
                            }?: run {
                                player.moveAllAndChooseTheOrder(view, Destination.TempZone.Temp, Destination.PlayerZone.Draw )
                        }
                    }
                    .end()
                )
            }
    }
    @Dominion_Card(extension = "Seaside")
    fun Outpost() : Card {
        val b = AtomicBoolean(false)
        return Card("Outpost", Price.seaside(5), CardType.ACTION, CardType.DURATION)
            .setup { 
                onPlay{player, _ ->
                    player.updateDrawBonusValue(-2)
                    b.set(false)
                }
                onExtraTurn(b)
                onDuration{ onEffect{_, _ ->} }
            }

    }
    @Dominion_Card(extension = "Seaside")
    fun PearlDiver(): Card {
        val actionAndDraw = Bonus.action().draw()
        return Card.action("Pearl Diver", Price.seaside(2))
            .setup { 
                onPlay(
                    bonus(actionAndDraw)
                        .map { player, _ -> player.getBottomCards().firstOrNull() }
                        .chooseWhatToDo { _, _ ->
                            InteractionRequest(
                                instruction = "Choice : move this card on the top of your draw pile",
                                cards = listOfNotNull(this),
                                buttons = listOf(Button("onTop", "y"), Button("onBottom", "n")),
                                canPass = true
                            )
                        }
                        .filter { "y" == it }
                        .thenDo { player, _ ->
                            player.getBottomCards().firstOrNull()?.let { card ->
                                player.moveTo(card, Destination.PlayerZone.Draw)
                            }
                        }
                        .end()
                )
            }
    }
    @Dominion_Card(extension = "Seaside")
    fun Pirate() : Card = Card("Pirate", Price.seaside(5), CardType.ACTION, CardType.DURATION, CardType.REACTION)
        .setup {
            onDuration {
                onEffect{player, _ ->
                    player.gainFromSupply(
                        instruction = "Gain a treasure (max 6$)",
                        filter = {it.hasType(CardType.TREASURE) && it.isAtMost(6)},
                        dest = Destination.PlayerZone.Hand
                    )
                }
            }

            afterGain {
                onEffect{owner, event -> owner.playCard(event.scope)}
                onCondition { event, player ->  event.cardHasType(CardType.TREASURE) }
            }
        }
    @Dominion_Card(extension = "Seaside")
    fun PirateShip() : Card = Card("Pirate Ship", Price.seaside(4), CardType.ACTION, CardType.ATTACK)
        .setup { 
            onPlay{ player, self ->
                suspend fun attack(){
                    val treasures = player.game.processAttackWithReveals(
                        attacker = player,
                        attackCard = self,
                        count = 2,
                        filter = {it.hasType(CardType.TREASURE)},
                        selector = {attacker, _, options -> attacker.game.chooseCard(attacker, options) }
                    )
                    treasures.takeIf { it.isNotEmpty() }?.let {player.increment(Item.COIN_TOKEN_SHIP)}
                }

                player.chooseWhatToDo(
                    instruction = "Choose : Gain money or attack others players",
                    list = listOf(self),
                    buttons = listOf(Button("money", "m"), Button("attack", "a"))
                ).takeIf {"m" == it}
                    ?.let { player.increment(Item.MONEY, player.pirateShip) }
                    ?:run { attack() }

            }
        }
    @Dominion_Card(extension = "Seaside")
    fun Sailor() : Card {
        val money = Bonus.money(2)
        return Card("Sailor", Price.seaside(4), CardType.ACTION, CardType.DURATION)
            .setup { 
                simpleAction(Bonus.Action)
                onDuration {
                    onEffect(money.onDuration()
                        .chooseCardFromHand(interactionRequest = InteractionRequest(
                            "You may trash a card from your hand",
                            canPass = true
                        ))
                        .thenWith { player, chosen -> player.trash(chosen) }
                        .end()
                    )
                }
                onGain {
                    onEffect(TriggerComponent.DuringPlayerGain{ _, _ -> }
                        .chooseWhatToDo({_, owner -> owner}) { _, event -> InteractionRequest(
                            instruction = "You may play this card",
                            cards = listOfNotNull(event.card),
                            buttons = listOf(Button("play", "p"), Button("skip", "s")),
                            canPass = true
                        )}.filter { "p" == it }
                        .thenDo { owner, event ->
                            event.card?.let{
                                owner.playCard(it)
                                event.destination = Destination.PlayerZone.InPlay
                                event.scope.set("used", true)
                            }
                        }
                        .end()
                    )

                    onCondition { event, player -> event.card?.hasType(CardType.DURATION) == true
                            && event.player == player
                            && !scope.getFlag("used")
                            && activate(player, scope)
                            && event.notMoved
                    }
                }
            }
    }

    @Dominion_Card(extension = "Seaside")
    fun Salvager() : Card = Card.action("Salvager", Price.seaside(4))
        .setup { 
            onPlay(Bonus.Buy.onPlay()
                .chooseCardFromHand( interactionRequest = InteractionRequest(instruction = "Trash a card") )
                .thenWith { player, card ->
                    player.trash(card)
                    player.increment(Item.MONEY, card.costValue)
                }.end()
            )
        }

    @Dominion_Card(extension = "Seaside")
    fun SeaChart() : Card{
        val actionAndDraw = Bonus.action().draw()
        return Card.action("Sea Chart", Price.seaside(3))
            .setup {
                onPlay(actionAndDraw.onPlay()
                    .lookingAt { player, _ -> player.getCardFromDeck()  }
                    .thenWith { player, drawn ->
                        val hasCopy = player.getCopyOf(Destination.PlayerZone.InPlay)?.any {drawn?.hasSameNameAs(it) == true }?:false
                        if(hasCopy) player.moveTo(drawn, Destination.PlayerZone.Hand)
                    }.end()
                )
            }
    }

    @Dominion_Card(extension = "Seaside")
    fun SeaHag() = Card("Sea Hag", Price.seaside(4), CardType.ACTION, CardType.ATTACK)
        .setup {
            onPlay { player, self ->
                player.game.processAttack( attacker = player, attackCard = self){
                    it.discardFromHand()
                    it.gainFromSupply("Curse", Destination.PlayerZone.Draw)
                }
            }
        }

    @Dominion_Card(extension = "Seaside")
    fun SeaWitch() : Card {
        val draw = Bonus.draw(2)
        return Card("Sea Witch", Price.seaside(5), CardType.ACTION, CardType.DURATION, CardType.ATTACK)
            .setup { 
                onPlay (draw.onPlay() then { player, self ->
                    player.game.processAttack(attacker = player, attackCard = self) {
                        it.gainFromSupply("Curse", Destination.PlayerZone.Discard)
                    }
                })
                onDuration { onEffect( draw.onDuration() then {discardFromHand(2)} ) }
            }
    }

    @Dominion_Card(extension = "Seaside")
    fun Smugglers() = Card.action("Smugglers", Price.seaside(3))
        .setup { 
            onPlay(OnPlayComponent{_, _ -> }
                .lookingAt { player, _ ->
                    val right = player.game.onTheRight(player)
                    right.cardGainedLastTurn
                }
                .filter{it.isNotEmpty()}
                .chooseCardFromList(config = {_, _ -> InteractionRequest(
                    instruction = "Copy a card from this list and gain it",
                    cards = this,
                    filter = { card -> card.isAtMost(6) },
                    canPass = true
                ) } )
                .thenWith { player, card ->
                    player.gain(card)
                }
                .end()

            )

        }

    @Dominion_Card(extension = "Seaside")
    fun Tactician() : Card {
        val draw_action_buy = Bonus.action().with(Item.BUY).draw(5)
        return Card("Tactician", Price.seaside(5), CardType.ACTION, CardType.DURATION)
            .setup {
                onPlay { player, self ->
                    val hand = player.getCopyOf(Destination.PlayerZone.Hand)

                    hand?.takeIf { it.isNotEmpty() }
                        ?.forEachSuspend { player.discard(it) }
                        ?: self.set("activated", true)
                }
                onDuration {
                    onEffect( draw_action_buy.onDuration())
                    withTrigger { _, card -> !card.getFlag("activated") }
                }
            }
    }

    @Dominion_Card(extension = "Seaside")
    fun TidePools() : Card {
        val actionDraw = Bonus.action().draw(3)
        return Card("Tide Pools", Price.seaside(4), CardType.ACTION, CardType.DURATION)
            .setup { 
                simpleAction(actionDraw)
                onDuration { onEffect{player, _ -> player.discardFromHand(2)} }
            }
    }
    @Dominion_Card(extension = "Seaside")
    fun TreasureMap() = Card.action("Treasure Map", Price.seaside(4))
        .setup { 
            onPlay(OnPlayComponent { _, _ -> }
                .lookingAtPair { player, self -> Pair(
                    player.trash(self),
                    player.getCopyOf(Destination.PlayerZone.Hand)?.firstOrNull { it.hasSameNameAs(self) }) }
                .filter { it.first == true && it.second != null }
                .thenWith { player, pair ->
                    if(player.trash(pair.second)) player.gainMultiplyCardFromSupply("Gold", Destination.PlayerZone.Draw, 4)  }
                .end()
            )
        }
    @Dominion_Card(extension = "Seaside")
    fun Treasury() : Card {
        val actionBuyMoney = Bonus.action().with(Item.BUY).with(Item.MONEY)
        return Card.action("Treasury", Price.seaside(5))
            .setup { 
                simpleAction(actionBuyMoney)
                    onEndBuy {
                        onEffect(TriggerComponent.OnEndBuy{_, _ ->}
                            .lookingAt { player, _ -> player.cardGainedCurrentTurn.any{it.hasType(CardType.TREASURE)} }
                            .filter { !it }
                            .chooseWhatToDo(config = {_, self ->
                                InteractionRequest(
                                    instruction = "Do you want to put your Treasury on your deck?",
                                    cards = listOf(self),
                                    buttons = Button.DeckOrDiscard,
                                    canPass = true)
                            } )
                            .filter { "deck" == it }
                            .thenDo { player, self -> player.moveTo(self, Destination.PlayerZone.Draw) }
                            .end()
                        )
                    }
            }
    }
    @Dominion_Card(extension = "Seaside")
    fun WareHouse() : Card {
        val action_draw = Bonus.action().draw(3)
        return Card.action("Warehouse", Price.seaside(3))
            .setup { 
                onPlay(action_draw.onPlay()
                    .then { player, _ -> player.discardFromHand(3) }
                )
            }
    }
    @Dominion_Card(extension = "Seaside")
    fun Wharf() : Card {
        val buy_draw = Bonus.buy().draw(2)
        return Card("Wharf", Price.seaside(5), CardType.ACTION, CardType.DURATION)
            .setup { registerSimplePlayAndDuration(buy_draw) }
    }
}