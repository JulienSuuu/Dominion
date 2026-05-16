package fr.umontpellier.iut.dominion.cards.component;


import fr.umontpellier.iut.dominion.Button;
import fr.umontpellier.iut.dominion.cards.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class InteractionRequest<X> {
    private final String instruction;
    private final X data;
    private final Predicate<? super Card> filter;
    private final List<Button> buttons;
    private final List<Card> cards;
    private final boolean canPass;

    private InteractionRequest(Builder<X> builder) {
        this.instruction = builder.instruction;
        this.data = builder.data;
        this.filter = builder.filter;
        this.buttons = builder.buttons;
        this.cards = builder.cards;
        this.canPass = builder.canPass;
    }

    public String  instruction() {
        return instruction;
    }
    public X data() {
        return data;
    }

    public Predicate<? super Card> filter() {
        return filter;
    }

    public List<Button> buttons() {
        return buttons;
    }

    public List<Card> cards() {
        return cards;
    }

    public boolean canPass() {
        return canPass;
    }

    public static class Builder<X> {
        private String instruction;
        private X data;
        private Predicate<? super Card> filter = card -> true;
        private List<Button> buttons = new ArrayList<>();
        private List<Card> cards = new ArrayList<>();
        private boolean canPass = false;

        public Builder<X> instruction(String instruction) {
            this.instruction = instruction;
            return this;
        }
        public Builder<X> data(X data) {
            this.data = data;
            return this;
        }

        public Builder<X> filter(Predicate<? super Card> filter) {
            this.filter =  filter;
            return this;
        }

        public Builder<X> buttons(List<Button> buttons) {
            this.buttons = buttons;
            return this;
        }

        public Builder<X> cards(List<Card> cards) {
            this.cards = cards;
            return this;
        }

        public Builder<X> canPass(boolean canPass) {
            this.canPass = canPass;
            return this;
        }

        public InteractionRequest<X> build() {
            return new InteractionRequest<>(this);
        }
    }
}
