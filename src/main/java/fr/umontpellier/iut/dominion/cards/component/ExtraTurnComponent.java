package fr.umontpellier.iut.dominion.cards.component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExtraTurnComponent implements CardComponent {
    private AtomicBoolean used;

    public ExtraTurnComponent(AtomicBoolean used) {
        this.used = used;
    }
    public Optional<ExtraTurnComponent> canUseExtraTurn() {
        return used.get()? Optional.empty(): Optional.of(this);
    }

    public void consume() {
        used.set(true);
    }

}
