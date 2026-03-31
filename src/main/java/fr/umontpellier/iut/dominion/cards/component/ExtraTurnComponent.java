package fr.umontpellier.iut.dominion.cards.component;

import java.util.Optional;

public class ExtraTurnComponent implements CardComponent {
    private boolean used = false;

    public Optional<ExtraTurnComponent> canUseExtraTurn() {
        return used? Optional.empty(): Optional.of(this);
    }

    public void consume() {
        used = true;
    }

}
