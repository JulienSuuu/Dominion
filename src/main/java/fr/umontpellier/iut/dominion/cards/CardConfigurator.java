package fr.umontpellier.iut.dominion.cards;

import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.component.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
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


    public CardConfigurator registerSimpleComponent( int cardsNow, int actsNow, int buysNow, int coinsNow,
                                               int cardsNext, int actsNext, int buysNext, int coinsNext) {
        registerSimpleAction( cardsNow, actsNow, buysNow, coinsNow);
        registerSimpleDuration( cardsNext, actsNext, buysNext, coinsNext);
        return this;
    }

    public CardConfigurator registerSimpleAction( int cardsNow, int actsNow, int buysNow, int coinsNow ){
        card.addComponent(OnPlayComponent.class, (p, c) -> CardUtil.TriggerEffect(p, coinsNow, actsNow, cardsNow, buysNow, "Effect", c));
        return this;
    }

    public CardConfigurator registerSimpleDuration( int cardsNext, int actsNext, int buysNext, int coinsNext) {
        card.addComponent(new DurationComponent((p,c) -> CardUtil.TriggerEffect(p, coinsNext, actsNext, cardsNext, buysNext, "Duration", c)));
        return this;
    }

    public Card get(){
        return card;
    }

}
