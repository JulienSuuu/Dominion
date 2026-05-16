package fr.umontpellier.iut.dominion.cards.component;

import fr.umontpellier.iut.dominion.Interface.Logger;
import fr.umontpellier.iut.dominion.cards.Card;

import java.util.function.BiFunction;
import java.util.function.Function;

public class TargetedBuilder<T extends BiEffect<U, V, T> & CardComponent, U extends Logger, V, X>{
    private final ContextBuilder<T, U, V, X> parent;
    private final BiFunction<U, V ,Logger> targetPicker;

    public TargetedBuilder(ContextBuilder<T, U, V, X> parent, BiFunction<U, V ,Logger> targetPicker) {
        this.parent = parent;
        this.targetPicker = targetPicker;
    }

    public ContextBuilder<T, U, V, Card> chooseCardFromHand(BiFunction<U, V, InteractionRequest<X>> config) {
        return parent.internalChoose(config, targetPicker, Logger::chooseCardFromHand);
    }

    public ContextBuilder<T, U, V, String> chooseWhatToDo(BiFunction<U, V, InteractionRequest<X>> config) {
        return parent.internalChoose(config, targetPicker, Logger::chooseWhatToDo);
    }

    public ContextBuilder<T, U, V, Card> chooseCardFromList(BiFunction<U, V, InteractionRequest<X>> config) {
        return parent.internalChoose(config, targetPicker, Logger::chooseCardFromList);
    }

}
