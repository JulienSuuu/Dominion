package fr.umontpellier.iut.dominion.cards.factories.Ally;

import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.cards.Card;

import java.util.Optional;

public class AllyArgs {
    private final Object[] args;

    public AllyArgs(Object[] args) {
        this.args = args;
    }

    public Optional<Card> card() {
        return get(0, Card.class);
    }

    public Optional<Integer> integer() {
        return get(0, Integer.class);
    }

    public Optional<Integer> count() {
        return get(1, Integer.class);
    }

    public Optional<Destination> destination() {
        return get(1, Destination.class);
    }

    public <T> Optional<T> get(int index, Class<T> type) {
        if (args == null || index < 0 || index >= args.length) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(type.cast(args[index]));
        } catch (ClassCastException e) {
            return Optional.empty();
        }
    }
}
