package fr.umontpellier.iut.dominion.cards.component

import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.Events.Event
import fr.umontpellier.iut.dominion.cards.Events.TriggerEvent

interface TriggerComponent : SingleCardComponent {

    /**
     * Triggers se déclenchant lors du gain d'une carte
     */
    fun interface DuringPlayerGain: BiEffect<Player, TriggerEvent>, TriggerComponent
    fun interface AfterPlayerGain : BiEffect<Player, TriggerEvent>, TriggerComponent
    fun interface SideEffectGain : BiEffect<Player, TriggerEvent>, TriggerComponent

    /**
     * Trigger se déclenchant lorsqu'un joueur joue une carte
     */
    fun interface OnCardPlayed : BiEffect<Player, TriggerEvent>, TriggerComponent
    fun interface BeforeCardPlayed : BiEffect<Player, TriggerEvent>, TriggerComponent
    fun interface AfterCardPlayed : BiEffect<Player, TriggerEvent>, TriggerComponent

    /**
     * Trigger se déclenchant en début de tour
     */
    fun interface OnStartTurn : BiEffect<Player, Unit>, TriggerComponent
    fun interface OnEndTurn : BiEffect<Player, Card>, TriggerComponent

    /**
     * Trigger se déclenchant au moment où le joueur peut être immunisé
     */
    interface Immunity : TriggerComponent {
        suspend fun revealed(player: Player, self: Card): Boolean = false
        suspend fun immune(player : Player, self: Card): Boolean = false
        suspend fun isImmuneAgainst(self: Card, attack: Card?): Boolean = false
    }

    /**
     * Trigger se déclenchant en fin de la phase d'achat ( début clean Up )
     */
    fun interface OnEndBuy : BiEffect<Player, Card>, TriggerComponent

    /**
     * Trigger se déclenchant sur une carte défaussé ( elle-même )
     */
    fun interface CheckItselfDiscarded : BiEffect<Event, Card>, TriggerComponent

    /**
     * Trigger se déclenchant sur une carte écarte ( elle-même )
     */
    fun interface CheckItselfTrashed : BiEffect<Event, Card>, TriggerComponent
    fun interface OnCardTrashed : BiEffect<Event, Card>, TriggerComponent

    /**
     * Trigger se déclenchant au moment de l'achat d'une carte ( avant son déplacement physique )
     */
    fun interface OnBuy : BiEffect<Player, Card>, TriggerComponent

    fun interface CheckItSelfBuy : BiEffect<Event, Card>, TriggerComponent {
        fun setFlag(name: String): CheckItSelfBuy {
            return CheckItSelfBuy { event, card ->
                this(event, card)
                event.player.getFlag(name).value = true
            }
        }
    }

    fun interface OnStartBuyPhase : BiEffect<Player, Card>, TriggerComponent

    /**
     * Trigger se déclenchant sur la carte gagné ( elle-même)
     */
    fun interface CheckItselfGain : BiEffect<Event, Card>, TriggerComponent

    /**
     * Trigger se déclenchant sur une carte qui peut être payer plus chère
     */
    fun interface OverPaidCard : BiEffect<Player, Card>, TriggerComponent

    /**
     * Trigger personnel du joueur sur les carte défaussé
     */
    fun interface discardHook : BiEffect<Event, Unit>, TriggerComponent
}