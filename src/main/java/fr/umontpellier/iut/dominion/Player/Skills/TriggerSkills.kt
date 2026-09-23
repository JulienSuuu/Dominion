package fr.umontpellier.iut.dominion.Player.Skills

import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Flags
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.PlayerComponent.activeEffect
import fr.umontpellier.iut.dominion.Player.PlayerComponent.mustConductPossessedTurn
import fr.umontpellier.iut.dominion.Player.PlayerComponent.updatePossession
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.Events.Event
import fr.umontpellier.iut.dominion.cards.Events.TriggerEvent
import fr.umontpellier.iut.dominion.cards.Id
import fr.umontpellier.iut.dominion.cards.component.CardComponent
import fr.umontpellier.iut.dominion.cards.component.DurationComponent
import fr.umontpellier.iut.dominion.cards.component.ExtraTurnComponent
import fr.umontpellier.iut.dominion.cards.component.Follower
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent
import fr.umontpellier.iut.dominion.cards.moveTo

suspend fun  Player.triggerDurationCards(){
    val listInPlay = getCopyOf(Destination.PlayerZone.InPlay) ?: emptyList()
    val alreadyUsedEffects = mutableSetOf<Pair<Id, DurationComponent>>()
    chooseOrder<DurationComponent>("play your physical duration cards", { listInPlay }) { card ->
        val duration = card.getComponent<DurationComponent>() ?: return@chooseOrder

        val matchingFollower = listInPlay
            .mapNotNull { it.getComponent<Follower>() }
            .filter { it.contains(duration)  }

        if (matchingFollower.isNotEmpty()) {

            chooseOrder<Follower>("choose the order for your followers", { matchingFollower.map { it.scope } }) { card ->
                val follower = card.getComponent<Follower>() ?: return@chooseOrder
                follower.executeOneDuration(self, duration.scope, duration)
                alreadyUsedEffects += Pair(follower.scope.id, duration)
            }

            duration.consume()

            matchingFollower.forEach {
                if(duration.isFinished(self)) it.removeDuration(duration)
            }

        } else {
            duration.execute(self, card)
            duration.consume()
        }
    }

    val followers = listInPlay.filter { card ->
        val follower = card.getComponent<Follower>()
        follower != null && !follower.inactive && alreadyUsedEffects.none { it.first == card.id } }
    chooseOrder<Follower>("play your remaining active followers", { followers }) { follower ->
        follower.getComponent<Follower>()?.execute(self)
    }
}

inline suspend fun <reified T> Player.triggerActiveEffect(event: Event) where T : TriggerComponent, T: suspend (Player, TriggerEvent) -> Unit {
    chooseOrder<T>("active effects in any order", {activeEffect}, event = event) {card ->
        card.getComponent<T>()?.let {
            val triggerEvent = TriggerEvent(event)
            triggerEvent.updateScope(card)
            it(self, triggerEvent)
        }
    }
}

suspend inline fun <reified T> Player.triggerActiveEffect() where T : TriggerComponent, T: suspend (Player, Card) -> Unit =
    chooseOrder<T>("active effect in any order", {activeEffect}) { card ->
        card.getComponent<T>()?.let {
            it(self, card)
        }
    }


suspend inline fun<reified T> Player.triggerEvent(event : Event) where T : TriggerComponent, T: suspend (Player, TriggerEvent) -> Unit {
    game.notifyTrigger<T>( self, event)
}


suspend inline fun <reified T> Player.triggerEvent() where T : TriggerComponent, T: suspend (Player, Card) -> Unit {
    val list = getCopyOf(Destination.PlayerZone.InPlay)?: emptyList()
    chooseOrder<T>("active effect in Play in any order", {list}){ card ->
        card.getComponent<T>()?.let {
            it(self, card)
        }
    }
}


internal suspend inline fun<reified T> Player.triggerStart() where T : TriggerComponent, T: suspend (Player, Unit) -> Unit {
    chooseOrder<T>("start turn, you may play a card?", {(getList(Destination.PlayerZone.Hand)).filter { it.hasType(CardType.REACTION) }}, true) { card ->
        card.getComponent<T>()?.let{it(self, Unit)}
    }

    triggerStartTavern<T>(Event(self))
}



suspend inline fun<reified T> Player.immunity(attack : Card?): Boolean where T : TriggerComponent.Immunity{
    val inPlay = getCopyOf(Destination.PlayerZone.InPlay)
        ?.any{card -> card.getComponent<T>()?.let { it.immune(self, card) || it.isImmuneAgainst(card, attack) }?:false}?:false

    if(inPlay) return true

    return getCopyOf(Destination.PlayerZone.Hand)?.any{ card -> card.getComponent<T>()?.revealed(self, card) ?: false}?: false
}


fun Player.triggerAnotherTurn() : Boolean {
    if (isSecondTurn) {
        isSecondTurn = false
        return false
    }

    if (mustConductPossessedTurn) { return updatePossession() }

    val prop = getPersistentFlag(Flags.expedition)

    if (prop.value) {
        isSecondTurn = true
        prop.value = false
        return true
    }

    return get(Destination.PlayerZone.InPlay)?.value?.asSequence()
        ?.mapNotNull { it.getComponent<ExtraTurnComponent>() }
        ?.firstNotNullOfOrNull { it.canUseExtraTurn() }
        ?.let { extraTurnEffect ->
            extraTurnEffect.consume(self)
            isSecondTurn = true
            true
        } ?: false
}


suspend inline fun <reified T> Player.triggerOneCard(c : Card?, event : Event) where T : TriggerComponent, T: suspend (Event, Card) -> Unit =
    c?.getComponent<T>()?.takeIf { c.canExecute<T>(event, self) }?.let { it(event, c)}

internal suspend inline fun <reified T> Player.triggerPlayerTavern(event: TriggerEvent) where T : TriggerComponent, T : suspend (Player, TriggerEvent) -> Unit =
    triggerTavernMat<T>(event) { card -> card.getComponent<T>()?.let {
        event.updateScope(card)
        it(self, event) }
    }

internal suspend inline fun<reified T> Player.triggerPlayerAndCardTavern(event: Event) where T : TriggerComponent, T : suspend (Player, Card) -> Unit =
    triggerTavernMat<T>(event){card -> card.getComponent<T>()?.let { it(self, card) } }

internal suspend inline fun<reified T> Player.triggerStartTavern(event: Event)where T : TriggerComponent, T : suspend (Player, Unit) ->Unit =
    triggerTavernMat<T>(event){card -> card.getComponent<T>()?.let { it(self, Unit) }}

internal suspend inline fun<reified T> Player.triggerTavernMat(event: Event, action: suspend (Card) -> Unit) where T : CardComponent{
    chooseOrder<T>("Call a card", {getValidTavern()}, true, event) {
        card ->
        card.getComponent<T>()?.let {
            moveTo(card, Destination.PlayerZone.InPlay)
            action(card)
        }
    }
}


internal suspend inline fun<reified T> Player.triggerOthersEvent(event: Event) where T : TriggerComponent, T : suspend (Event, Card) -> Unit{
    val list = getCopyOf(Destination.PlayerZone.InPlay)?.filter { !it.hasType(CardType.REACTION) }?: emptyList()
    chooseOrder<T>("Effect to do :", {list}, event = event){
        card -> card.getComponent<T>()?.let {it(event, card)}
    }

    chooseOrder<T>("Reveal a reaction ?", {getValidReaction()}, event = event){ card ->
        card.getComponent<T>()?.let { it(event, card) }
    }

}




fun Player.getValidReaction(): List<Card> =
    getCopyOf(Destination.PlayerZone.Hand)?.filter { it.hasType(CardType.REACTION) }?: emptyList()


 fun Player.getValidTavern(): List<Card> =
    getCopyOf(Destination.OtherZone.Tavern)?.filter { it.hasType(CardType.RESERVE) }?:emptyList()

suspend inline fun <reified T : CardComponent> Player.chooseOrder(
    instruction: String,
    getter: () -> List<Card>,
    canPass: Boolean = false,
    event: Event = Event(self),
    action: suspend (Card) -> Unit
) {
    if(getter().isEmpty()) return
    
    if(autoPlay<T>(getter(), event, action)) return

    val alreadyUsed = hashSetOf<Card>()
    while (true) {
        val currentChoices = getter().filter { card ->
            card.hasComponent<T>() &&
                    card.canExecute<T>(event, self) &&
                    card !in alreadyUsed
        }

        if (currentChoices.isEmpty()) break

        self.chooseCardFromList(instruction, cards = currentChoices, canPass = canPass)
            ?.let { chosenCard ->
                action(chosenCard)
                alreadyUsed.add(chosenCard)
            }
            ?: break
    }
}

suspend inline fun <reified T> Player.autoPlay(listToCheck : List<Card>, event : Event, action : suspend (Card) -> Unit) : Boolean where T: CardComponent {
    val autoPlayChoices = listToCheck.filter { card ->
        card.hasComponent<T>() &&
                card.canExecute<T>(event, self) &&
                !card.hasType(CardType.REACTION) &&
                !card.hasType(CardType.RESERVE)
    }

    if (autoPlayChoices.size == 1) {
        action(autoPlayChoices.first())
        return true
    }

    return false
}