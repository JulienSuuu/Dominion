package fr.umontpellier.iut.dominion.domain.setup.card

import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.supply.SupplyPile
import fr.umontpellier.iut.dominion.domain.components.card.CardComponent
import fr.umontpellier.iut.dominion.domain.components.card.bonus.Bonus
import fr.umontpellier.iut.dominion.domain.components.card.bonus.DominionBonus
import fr.umontpellier.iut.dominion.domain.components.card.duration.DurationComponent
import fr.umontpellier.iut.dominion.domain.components.card.duration.Follower
import fr.umontpellier.iut.dominion.domain.components.card.score.MutableScoreComponent
import fr.umontpellier.iut.dominion.domain.components.card.score.ScoreComponent
import fr.umontpellier.iut.dominion.domain.components.card.setup.HeirloomComponent
import fr.umontpellier.iut.dominion.domain.components.card.setup.OnSetup
import fr.umontpellier.iut.dominion.domain.components.card.specification.Specification
import fr.umontpellier.iut.dominion.domain.components.card.trigger.OnPlayComponent
import fr.umontpellier.iut.dominion.domain.components.card.trigger.PokerHandReactionComponent
import fr.umontpellier.iut.dominion.domain.components.card.trigger.TriggerComponent
import fr.umontpellier.iut.dominion.domain.components.card.turn.ExtraTurnComponent
import fr.umontpellier.iut.dominion.domain.model.card.Card
import fr.umontpellier.iut.dominion.domain.model.event.Event
import fr.umontpellier.iut.dominion.domain.model.event.GainType
import fr.umontpellier.iut.dominion.domain.model.game.Game
import fr.umontpellier.iut.dominion.domain.setup.card.factories.DURATION
import fr.umontpellier.iut.dominion.domain.setup.card.factories.EFFECT
import fr.umontpellier.iut.dominion.domain.setup.card.factories.reserveCondition
import fr.umontpellier.iut.dominion.domain.utils.triggerEffect
import java.util.concurrent.atomic.AtomicBoolean

typealias Build<T> = CardConfigurator.Builder<T>.() -> Unit


class CardConfigurator(val scope: Card) {


    open class Builder<T : CardComponent>(val scope: Card) {
        lateinit var effect : T
        var condition : (Event, Player) -> Boolean = { _, _ -> true}


        open fun onEffect(effect: T){ this.effect = effect }
        fun onCondition(condition : (Event, Player) -> Boolean){ this.condition = condition }
        fun reserveCondition(condition : (Event, Player) -> Boolean = { _, _ -> true}){ this.condition = scope.reserveCondition(condition)  }
    }

    inline fun <reified T : CardComponent> build(build : Builder<T>.() -> Unit) {
        val builder = Builder<T>(scope)
        builder.build()
        scope.register<T>(builder.effect, builder.condition)
    }

    class DurationBuilder(scope : Card) : Builder<DurationComponent.Duration>(scope) {
        val duration = DurationComponent(scope = scope)

        fun withTrigger(trigger : (Player, Card) -> Boolean) { duration.setTrigger(trigger) }

        fun withTime(time : Int){ duration.setNumberOfTurns(time) }

        fun infinite(){ duration.setInfinite(true) }

        fun shouldBeDiscardWhen(condition: (Player, Card) -> Boolean) { duration.stayInPlayCondition(condition) }

        override fun onEffect(effect: DurationComponent.Duration) {
            duration.setEffect(effect)
        }
    }

    infix fun onDuration(build : DurationBuilder.() -> Unit): CardConfigurator {
        val duration  = DurationBuilder(scope)
        build(duration)
        scope.register(duration.duration, duration.condition)
        return this
    }

    fun follower(mult : Int = 2): CardConfigurator {
        scope.register(Follower(scope, mult))
        return this
    }

    fun onSetup(neededPlayer : Boolean = false, block : Game.(MutableList<SupplyPile>) -> Unit) {
        val setup = OnSetup(block)
        setup.neededPlayer = neededPlayer
        scope.register(setup)
    }

    fun onEndSetup(block : Game.(MutableList<SupplyPile>) -> Unit) {
        val setup = OnSetup(block)
        setup.neededPlayer = true
        scope.register(setup)
    }

    inline fun <reified T : Specification> addSpecification(component : T){ scope.register(component) }

    fun onStartBuyPhase(builder : Build<TriggerComponent.OnStartBuyPhase>){ build(builder) }
    fun onEndTurn(builder : Build<TriggerComponent.OnEndTurn>) { build(builder) }


    infix fun onPlay(builder : Build<OnPlayComponent>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun onPlay(component : OnPlayComponent) {
        scope.register(component)
    }

    infix fun onExtraTurn(consumer: AtomicBoolean): CardConfigurator {
        scope.register(ExtraTurnComponent(consumer))
        return this
    }

    infix fun onGain(builder : Build<TriggerComponent.DuringPlayerGain>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun onCardPlayed(builder : Build<TriggerComponent.OnCardPlayed>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun beforeCardPlayed(builder: Build<TriggerComponent.BeforeCardPlayed>): CardConfigurator {
        build(builder)
        return this
    }

    fun withHeirLoom(nameCard : String){
        scope.register(HeirloomComponent(nameCard))
    }

    infix fun afterCardPlayed(builder : Build<TriggerComponent.AfterCardPlayed>): CardConfigurator {
        build(builder)
        return this
    }

    fun immunity(): CardConfigurator {
        scope.register(object : TriggerComponent.Immunity {})
        return this
    }

    infix fun immunity(builder: Build<TriggerComponent.Immunity>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun score(component: ScoreComponent): CardConfigurator {
        scope.register(component)
        return this
    }

    infix fun onEndBuy(builder : Build<TriggerComponent.OnEndBuy>): CardConfigurator {
        build(builder)
        return this
    }

    fun registerSimplePlayAndDuration(playBonus: Bonus, durationBonus: Bonus = playBonus): CardConfigurator {
        simpleAction(playBonus)
        simpleDuration(durationBonus)
        return this
    }

    infix fun simpleAction(playBonus: DominionBonus): CardConfigurator {
        scope.register(playBonus.onPlay())
        return this
    }

    infix fun simpleDuration(durationBonus: Bonus): CardConfigurator {
        scope.register(DurationComponent({ p, c -> p.triggerEffect(DURATION, c, durationBonus) }, scope))
        return this
    }

    infix fun onPokerHand(builder : Build<PokerHandReactionComponent>){
        build(builder)
    }

    infix fun checkItselfDiscard(builder : Build<TriggerComponent.CheckItselfDiscarded>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun onStartTurn(builder : Build<TriggerComponent.OnStartTurn>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun onStartTurn(builder : TriggerComponent.OnStartTurn): CardConfigurator {
        scope.register(builder)
        return this
    }

    infix fun available(consumer: (Player) -> Boolean): CardConfigurator {
        scope.available = consumer
        return this
    }

    infix fun onBuy(builder : Build<TriggerComponent.OnBuy>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun onBuy(builder : TriggerComponent.OnBuy): CardConfigurator {
        scope.register(builder)
        return this
    }

    infix fun overpaid(builder : Build<TriggerComponent.OverPaidCard>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun checkGain(builder : Build<TriggerComponent.CheckItselfGain>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun checkGain(builder : TriggerComponent.CheckItselfGain): CardConfigurator {
        scope.register(builder)
        return this
    }

    infix fun afterGain(builder : Build<TriggerComponent.AfterPlayerGain>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun checkItselfTrash(builder : Build<TriggerComponent.CheckItselfTrashed>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun checkItselfTrash(builder : TriggerComponent.CheckItselfTrashed): CardConfigurator {
        scope.register(builder)
        return this
    }

    infix fun checkItselfDiscard(builder : TriggerComponent.CheckItselfDiscarded): CardConfigurator {
        scope.register(builder)
        return this
    }

    infix fun onEndTurn(component : TriggerComponent.OnEndTurn) {
        scope.register(component)
    }

    fun onSideEffectGain(type : GainType, builder : Build<TriggerComponent.SideEffectGain>) {
        val build = Builder<TriggerComponent.SideEffectGain>(scope)
        build.builder()
        scope.register(build.effect, {event, player -> event.hasGainType(type) && build.condition(event, player) })
    }

    infix fun checkItselfBuy(builder : Build<TriggerComponent.CheckItSelfBuy>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun checkItselfBuy(component : TriggerComponent.CheckItSelfBuy): CardConfigurator {
        scope.register(component)
        return this
    }

    fun mutableScore(baseValue: Int = 0, function : (Player) -> Int = {0}) {
        scope.register<ScoreComponent>(MutableScoreComponent(baseValue, function))
    }

    infix fun onCardTrash(builder : Build<TriggerComponent.OnCardTrashed>): CardConfigurator {
        build(builder)
        return this
    }

    fun get(): Card = scope

    // --- Éléments Statiques (Companion Object) ---
    companion object {

        @JvmStatic
        fun bonus(playBonus: Bonus): OnPlayComponent {
            return OnPlayComponent { player, c -> player.triggerEffect(EFFECT, c, playBonus) }
        }

        @JvmStatic
        fun buyBonus(bonus: Bonus): TriggerComponent.CheckItSelfBuy {
            return TriggerComponent.CheckItSelfBuy { event, c -> event.player.triggerEffect(EFFECT, c, bonus) }
        }
    }
}