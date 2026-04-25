package fr.umontpellier.iut.dominion.cards;

import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Events.Event;
import fr.umontpellier.iut.dominion.cards.component.*;
import fr.umontpellier.iut.dominion.cards.factories.FactoryUtil;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class CardConfigurator {
    private final Card card;
    public CardConfigurator(Card card) {
        this.card = card;
    }

    public CardConfigurator onDuration(BiConsumer<Player, Card> consumer) {
        card.addComponent(new DurationComponent(consumer));
        return this;
    }

    public CardConfigurator onDurationWithTrigger(BiConsumer<Player, Card> consumer, Predicate<Card> condition) {
        DurationComponent durationComponent = new DurationComponent(consumer);
        card.addComponent(durationComponent.setTrigger(condition));
        return this;
    }

    public CardConfigurator onPlay(OnPlayComponent consumer) {
        card.addComponent(OnPlayComponent.class, consumer);
        return this;
    }

    public CardConfigurator onExtraTurn(AtomicBoolean consumer) {
        card.addComponent(new ExtraTurnComponent(consumer));
        return this;
    }

    public CardConfigurator onGain(TriggerComponent.OnPlayerGain effect) {
        card.addComponent(TriggerComponent.OnPlayerGain.class, effect);
        return this;
    }

    public CardConfigurator onCardPlayed(TriggerComponent.OnCardPlayed effect) {
        card.addComponent(TriggerComponent.OnCardPlayed.class, effect);
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

    public CardConfigurator onCondition(BiPredicate<Event, Player> condition) {
        card.setCondition(condition);
        return this;
    }

    public CardConfigurator registerSimpleDurationAndPlay(Bonus playBonus, Bonus durationBonus) {
        registerSimpleAction( playBonus);
        registerSimpleDuration( playBonus);
        return this;
    }

    public CardConfigurator registerSimpleAction(Bonus playBonus){
        card.addComponent(OnPlayComponent.class, (p, c) -> CardUtil.TriggerEffect(p, FactoryUtil.EFFECT, c, playBonus));
        return this;
    }

    public CardConfigurator registerSimpleDuration(Bonus durationBonus) {
        card.addComponent(new DurationComponent((p,c) -> CardUtil.TriggerEffect(p, FactoryUtil.EFFECT, c, durationBonus)));
        return this;
    }

    public CardConfigurator onDiscard(TriggerComponent.onCardDiscarded effect) {
        card.addComponent(TriggerComponent.onCardDiscarded.class, effect);
        return this;
    }

    public CardConfigurator onStartTurn(TriggerComponent.onStartTurn effect) {
        card.addComponent(TriggerComponent.onStartTurn.class, effect);
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

    public CardConfigurator checkBuy(TriggerComponent.checkItSelfBuy effect) {
        card.addComponent(TriggerComponent.checkItSelfBuy.class, effect);
        return this;
    }

    public CardConfigurator onTrash(TriggerComponent.onCardTrashed effect) {
        card.addComponent(TriggerComponent.onCardTrashed.class, effect);
        return this;
    }

    public Card get(){
        return card;
    }

}
