package fr.umontpellier.iut.dominion.Player.PlayerComponent

import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.cards.Card
import kotlin.collections.remove

class TurnHistoryComponent(private val self: Player) : PlayerComponent {
    internal val cardGainedLastTurn = mutableListOf<Card>()
    internal val cardGainedCurrentTurn = mutableListOf<Card>()
    internal val activeEffect = mutableListOf<Card>()

    override fun reset() {
        cardGainedLastTurn.clear()
        cardGainedLastTurn.addAll(cardGainedCurrentTurn)
        cardGainedCurrentTurn.clear()
    }

    fun addCardEffect(c :Card?){
        if(c == null) return
        if(activeEffect.contains(c)) return
        activeEffect.add(c)
    }

    fun removeCardEffect(c : Card?){
        if(c == null) return
        activeEffect.remove(c)
    }

    fun addCardCurrentTurn(c :Card?){
        if(c == null) return
        if(cardGainedCurrentTurn.contains(c)) return
        cardGainedCurrentTurn.add(c)
    }
}

val Player.cardGainedLastTurn get() = getComponent<TurnHistoryComponent>()?.cardGainedLastTurn ?: emptyList()
val Player.cardGainedCurrentTurn get() = getComponent<TurnHistoryComponent>()?.cardGainedCurrentTurn ?: emptyList()
val Player.activeEffect get() = getComponent<TurnHistoryComponent>()?.activeEffect ?: emptyList()

fun Player.addCardEffect(c :Card?) = getComponent<TurnHistoryComponent>()?.addCardEffect(c)
fun Player.removeCardEffect(c :Card?) = getComponent<TurnHistoryComponent>()?.removeCardEffect(c)
fun Player.addCardCurrentTurn(c :Card?) = getComponent<TurnHistoryComponent>()?.addCardCurrentTurn(c)

