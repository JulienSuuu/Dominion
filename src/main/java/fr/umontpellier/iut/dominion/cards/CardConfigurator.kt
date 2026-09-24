package fr.umontpellier.iut.dominion.cards

import fr.umontpellier.iut.dominion.game.Game
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.cards.Bonus.Bonus
import fr.umontpellier.iut.dominion.cards.Bonus.DominionBonus
import fr.umontpellier.iut.dominion.cards.Events.Event
import fr.umontpellier.iut.dominion.cards.Events.GainType
import fr.umontpellier.iut.dominion.cards.Events.PokerEvent
import fr.umontpellier.iut.dominion.cards.component.*
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent.*
import fr.umontpellier.iut.dominion.cards.factories.DURATION
import fr.umontpellier.iut.dominion.cards.factories.EFFECT
import fr.umontpellier.iut.dominion.cards.factories.Futaba.EvaluatedPokerHand
import fr.umontpellier.iut.dominion.cards.factories.Futaba.PokerHand
import fr.umontpellier.iut.dominion.cards.factories.reserveCondition
import javafx.geometry.Side
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicBoolean


typealias Build<T> = CardConfigurator.Builder<T>.() -> Unit


class CardConfigurator(val scope: Card) {


    open class Builder<T : CardComponent>(val scope: Card) {
        lateinit var effect : T
        var condition : (Event, Player) -> Boolean = {_, _ -> true}


        open fun onEffect(effect: T){ this.effect = effect }
        fun onCondition(condition : (Event, Player) -> Boolean){ this.condition = condition }
        fun reserveCondition(condition : (Event, Player) -> Boolean = {_, _ -> true}){ this.condition = scope.reserveCondition(condition)  }
    }

    inline fun <reified T : CardComponent> build(build : Builder<T>.() -> Unit) {
        val builder = Builder<T>(scope)
        builder.build()
        scope.register<T>(builder.effect, builder.condition)
    }

    private fun duration(build : Builder<DurationComponent.Duration>.() -> Unit) : DurationComponent {
        val builder = Builder<DurationComponent.Duration>(scope)
        builder.build()
        val duration = DurationComponent(builder.effect, scope)
        scope.register(duration, builder.condition)

        return duration
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

    fun onSetup(neededPlayer : Boolean = false, block : Game.() -> Unit) {
        val setup = OnSetup(block)
        setup.neededPlayer = neededPlayer
        scope.register(setup)
    }

    fun onEndSetup(block : Game.() -> Unit) {
        val setup = OnSetup(block)
        setup.neededPlayer = true
        scope.register(setup)
    }

    fun onStartBuyPhase(builder : Build<OnStartBuyPhase>){ build(builder) }
    fun onEndTurn(builder : Build<OnEndTurn>) { build(builder) }


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

    infix fun onGain(builder : Build<DuringPlayerGain>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun onCardPlayed(builder : Build<OnCardPlayed>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun beforeCardPlayed(builder: Build<BeforeCardPlayed>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun afterCardPlayed(builder : Build<AfterCardPlayed>): CardConfigurator {
        build(builder)
        return this
    }

    fun immunity(): CardConfigurator {
        scope.register(object : TriggerComponent.Immunity {})
        return this
    }

    infix fun immunity(builder: Build<Immunity>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun score(component: ScoreComponent): CardConfigurator {
        scope.register(component)
        return this
    }

    infix fun onEndBuy(builder : Build<OnEndBuy>): CardConfigurator {
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
        scope.register(DurationComponent ({ p, c -> p.triggerEffect( DURATION, c, durationBonus) }, scope))
        return this
    }

    infix fun onPokerHand(builder : Build<PokerHandReactionComponent>){
        build(builder)
    }

    infix fun checkItselfDiscard(builder : Build<CheckItselfDiscarded>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun onStartTurn(builder : Build<OnStartTurn>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun onStartTurn(builder : OnStartTurn): CardConfigurator {
        scope.register(builder)
        return this
    }

    infix fun available(consumer: (Player) -> Boolean): CardConfigurator {
        scope.available = consumer
        return this
    }

    infix fun onBuy(builder : Build<OnBuy>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun onBuy(builder : OnBuy): CardConfigurator {
        scope.register(builder)
        return this
    }

    infix fun overpaid(builder : Build<OverPaidCard>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun checkGain(builder : Build<CheckItselfGain>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun checkGain(builder : CheckItselfGain): CardConfigurator {
        scope.register(builder)
        return this
    }



    infix fun afterGain(builder : Build<AfterPlayerGain>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun checkItselfTrash(builder : Build<CheckItselfTrashed>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun checkItselfTrash(builder : CheckItselfTrashed): CardConfigurator {
        scope.register(builder)
        return this
    }

    infix fun checkItselfDiscard(builder : CheckItselfDiscarded): CardConfigurator {
        scope.register(builder)
        return this
    }

    infix fun onEndTurn(component : OnEndTurn) {
        scope.register(component)
    }

    fun onSideEffectGain(type : GainType ,builder : Build<SideEffectGain>) {
        val build = Builder<SideEffectGain>(scope)
        build.builder()
        scope.register(build.effect, {event, player -> event.hasGainType(type) && build.condition(event, player) })
    }

    infix fun checkItselfBuy(builder : Build<CheckItSelfBuy>): CardConfigurator {
        build(builder)
        return this
    }

    infix fun checkItselfBuy(component : CheckItSelfBuy): CardConfigurator {
        scope.register(component)
        return this
    }




    infix fun onCardTrash(builder : Build<OnCardTrashed>): CardConfigurator {
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

        @JvmStatic
        fun <T : CardComponent> run(clazz: Class<T>, effect: T): T = effect

        @JvmStatic
        fun run(effect: OnPlayComponent): OnPlayComponent = run(OnPlayComponent::class.java, effect)

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        inline fun <reified T: CardComponent> empty(): T {
            val clazz = T::class.java

            return Proxy.newProxyInstance(
                clazz.classLoader,
                arrayOf(clazz),
                InvocationHandler { proxy, method, args ->
                    when (method.name) {
                        "toString" -> return@InvocationHandler "EmptyComponent[${clazz.simpleName}]"
                        "hashCode" -> return@InvocationHandler System.identityHashCode(proxy)
                        "equals" -> return@InvocationHandler proxy === args[0]
                    }

                    if (method.isDefault) {
                        return@InvocationHandler InvocationHandler.invokeDefault(proxy, method, args)
                    }

                    if (method.returnType.isAssignableFrom(clazz)) {
                        return@InvocationHandler proxy
                    }

                    if (method.returnType == Void.TYPE) {
                        return@InvocationHandler null
                    }

                    null
                }
            ) as T
        }
    }
}