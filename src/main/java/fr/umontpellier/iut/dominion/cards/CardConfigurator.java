package fr.umontpellier.iut.dominion.cards;

import fr.umontpellier.iut.dominion.Interface.Logger;
import fr.umontpellier.iut.dominion.Player.Player;
import fr.umontpellier.iut.dominion.cards.Events.Event;
import fr.umontpellier.iut.dominion.cards.component.*;
import fr.umontpellier.iut.dominion.cards.factories.FactoryUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class CardConfigurator {
    private final Card card;
    public CardConfigurator(Card card) {
        this.card = card;
    }

    public CardConfigurator onDuration(DurationComponent.duration consumer) {
        card.addComponent(new DurationComponent(consumer));
        return this;
    }

    public CardConfigurator onDurationWithTrigger(DurationComponent.duration consumer, Predicate<Card> condition) {
        DurationComponent durationComponent = new DurationComponent(consumer);
        card.addComponent(durationComponent.setTrigger(condition));
        return this;
    }

    public CardConfigurator onDurationWithTime(DurationComponent.duration consumer, int time) {
        card.addComponent(new DurationComponent(consumer).setNumberOfTurns(time));
        return this;
    }

    public CardConfigurator onInfiniteDuration(DurationComponent.duration consumer) {
        DurationComponent durationComponent = new DurationComponent(consumer);
        card.addComponent(durationComponent.setInfinite(true));
        return this;
    }

    public CardConfigurator stayInPlayCondition(Predicate<Card> condition) {
        DurationComponent duration = card.getComponent(DurationComponent.class)
                .orElseGet(() -> {
                    DurationComponent newComp = new DurationComponent((p, c) -> {});
                    card.addComponent(newComp);
                    return newComp;
                });

        duration.stayInPlayCondition(condition);
        return this;
    }

    public CardConfigurator durationCondition(Predicate<Card> condition) {
        card.getComponent(DurationComponent.class).ifPresent(d -> d.thingToDo(condition));
        return this;
    }

    public CardConfigurator onPlay(OnPlayComponent consumer) {
        card.addComponent(OnPlayComponent.class, consumer);
        return this;
    }

    public CardConfigurator onPlayWithBonus(Bonus bonus, OnPlayComponent consumer) {
        card.addComponent(OnPlayComponent.class, bonus(bonus).then(consumer));
        return this;
    }

    public CardConfigurator onExtraTurn(AtomicBoolean consumer) {
        card.addComponent(new ExtraTurnComponent(consumer));
        return this;
    }

    public CardConfigurator onGain(TriggerComponent.DuringPlayerGain effect) {
        card.addComponent(TriggerComponent.DuringPlayerGain.class, effect);
        return this;
    }

    public CardConfigurator onCardPlayed(TriggerComponent.OnCardPlayed effect) {
        card.addComponent(TriggerComponent.OnCardPlayed.class, effect);
        return this;
    }

    public CardConfigurator beforeCardPlayed(TriggerComponent.beforeCardPlayed effect) {
        card.addComponent(TriggerComponent.beforeCardPlayed.class, effect);
        return this;
    }

    public CardConfigurator afterCardPlayed(TriggerComponent.afterCardPlayed effect) {
        card.addComponent(TriggerComponent.afterCardPlayed.class, effect);
        return this;
    }

    public CardConfigurator immunity(){
        card.addComponent(TriggerComponent.Immunity.class, new TriggerComponent.Immunity() {});
        return this;
    }

    public CardConfigurator immunity(TriggerComponent.Immunity consumer) {
        card.addComponent(TriggerComponent.Immunity.class, consumer);
        return this;
    }

    public CardConfigurator score(ScoreComponent component){
        card.addComponent(ScoreComponent.class, component);
        return this;
    }

    public CardConfigurator onEndBuy(TriggerComponent.onEndBuy effect) {
        card.addComponent(TriggerComponent.onEndBuy.class, effect);
        return this;
    }

    public CardConfigurator onEndBuyCondition(BiPredicate<Event, Player> condition) {
        card.addCondition(condition,TriggerComponent.onEndBuy.class);
        return this;
    }

    public CardConfigurator duringGainCondition(BiPredicate<Event, Player> condition) {
        card.addCondition(condition, TriggerComponent.DuringPlayerGain.class);
        return this;
    }

    public CardConfigurator itselfGainCondition(BiPredicate<Event, Player> condition) {
        card.addCondition(condition, TriggerComponent.checkItselfGain.class);
        return this;
    }

    public CardConfigurator afterGainCondition(BiPredicate<Event, Player> condition) {
        card.addCondition(condition, TriggerComponent.AfterPlayerGain.class);
        return this;
    }

    public CardConfigurator onTrashCondition(BiPredicate<Event, Player> condition) {
        card.addCondition(condition, TriggerComponent.onCardTrashed.class);
        return this;
    }

    public CardConfigurator itselfTrashCondition(BiPredicate<Event, Player> condition) {
        card.addCondition(condition, TriggerComponent.checkItselfTrashed.class);
        return this;
    }

    public CardConfigurator afterDiscardCondition(BiPredicate<Event, Player> condition) {
        return this;
    }

    public CardConfigurator itselfDiscardCondition(BiPredicate<Event, Player> condition) {
        card.addCondition(condition, TriggerComponent.checkItselfDiscarded.class);
        return this;
    }

    public CardConfigurator cardPlayedCondition(BiPredicate<Event, Player> condition) {
        card.addCondition(condition, TriggerComponent.OnCardPlayed.class);
        return this;
    }

    public CardConfigurator beforeCardPlayedCondition(BiPredicate<Event, Player> condition) {
        card.addCondition(condition, TriggerComponent.beforeCardPlayed.class);
        return this;
    }

    public CardConfigurator afterCardPlayedCondition(BiPredicate<Event, Player> condition) {
        card.addCondition(condition, TriggerComponent.afterCardPlayed.class);
        return this;
    }

    public CardConfigurator ImmunityCondition(BiPredicate<Event, Player> condition) {
        card.addCondition(condition, TriggerComponent.Immunity.class);
        return this;
    }

    public CardConfigurator registerSimplePlayAndDuration(Bonus playBonus, Bonus durationBonus) {
        registerSimpleAction( playBonus);
        registerSimpleDuration(durationBonus);
        return this;
    }

    public CardConfigurator registerSimpleAction(Bonus playBonus){
        card.addComponent(OnPlayComponent.class, bonus(playBonus));
        return this;
    }

    public CardConfigurator registerSimpleDuration(Bonus durationBonus) {
        card.addComponent(new DurationComponent((p,c) -> CardUtil.TriggerEffect(p, FactoryUtil.DURATION, c, durationBonus)));
        return this;
    }

    public CardConfigurator checkItselfDiscard(TriggerComponent.checkItselfDiscarded effect) {
        card.addComponent(TriggerComponent.checkItselfDiscarded.class, effect);
        return this;
    }

    public CardConfigurator onStartTurn(TriggerComponent.onStartTurn effect) {
        card.addComponent(TriggerComponent.onStartTurn.class, effect);
        return this;
    }

    public CardConfigurator onStartTurnCondition(BiPredicate<Event, Player> condition) {
        card.addCondition(condition, TriggerComponent.onStartTurn.class);
        return this;
    }

    public CardConfigurator available(Predicate<Player> consumer) {
        card.setAvailable(consumer);
        return this;
    }

    public CardConfigurator onBuy(TriggerComponent.onBuy effect) {
        card.addComponent(TriggerComponent.onBuy.class, effect);
        return this;
    }

    public CardConfigurator overpaid(TriggerComponent.overPaidCard effect) {
        card.addComponent(TriggerComponent.overPaidCard.class, effect);
        return this;
    }


    public CardConfigurator checkGain(TriggerComponent.checkItselfGain effect) {
        card.addComponent(TriggerComponent.checkItselfGain.class, effect);
        return this;
    }

    public CardConfigurator afterGain(TriggerComponent.AfterPlayerGain effect) {
        card.addComponent(TriggerComponent.AfterPlayerGain.class, effect);
        return this;
    }

    public CardConfigurator checkBuy(TriggerComponent.checkItSelfBuy effect) {
        card.addComponent(TriggerComponent.checkItSelfBuy.class, effect);
        return this;
    }

    public CardConfigurator checkItselfTrash(TriggerComponent.checkItselfTrashed effect) {
        card.addComponent(TriggerComponent.checkItselfTrashed.class, effect);
        return this;
    }

    public CardConfigurator checkItselfBuy(TriggerComponent.checkItSelfBuy effect) {
        card.addComponent(TriggerComponent.checkItSelfBuy.class, effect);
        return this;
    }

    public CardConfigurator checkItselfBuyCondition(BiPredicate<Event, Player> condition) {
        card.addCondition(condition, TriggerComponent.checkItSelfBuy.class);
        return this;
    }

    public CardConfigurator onCardTrash(TriggerComponent.onCardTrashed effect) {
        card.addComponent(TriggerComponent.onCardTrashed.class, effect);
        return this;
    }


    public static OnPlayComponent bonus(Bonus playBonus) {
        return (player, c) ->{CardUtil.TriggerEffect(player, FactoryUtil.EFFECT, c, playBonus);};
    }

    public static TriggerComponent.checkItSelfBuy buyBonus(Bonus bonus) {
        return (event, c) -> CardUtil.TriggerEffect(event.getPlayer(), FactoryUtil.EFFECT, c, bonus);
    }

    public static <T extends CardComponent> T run(Class<T> clazz , T effect) {
        return effect;
    }

    public static OnPlayComponent run(OnPlayComponent effect) {
        return run(OnPlayComponent.class, effect);
    }

    @SuppressWarnings("unchecked")
    public static <U extends Logger, V, T extends BiEffect<U, V, T> & CardComponent> T empty(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class<?>[]{clazz},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "toString" -> { return "EmptyComponent[" + clazz.getSimpleName() + "]"; }
                        case "hashCode" -> { return System.identityHashCode(proxy); }
                        case "equals"   -> { return proxy == args[0]; }
                    }


                    if (method.isDefault()) {
                        return InvocationHandler.invokeDefault(proxy, method, args);
                    }


                    if (method.getReturnType().isAssignableFrom(clazz)) {
                        return proxy;
                    }

                    if (method.getReturnType().equals(Void.TYPE)) {
                        return null;
                    }

                    return null;
                }
        );
    }

    public Card get(){
        return card;
    }

}
