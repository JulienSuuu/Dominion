package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;
import fr.umontpellier.iut.dominion.cards.component.ExtraTurnComponent;

/**
 * Carte Avant-poste (Outpost)
 * <p>
 * Piochez seulement 3 cartes pour votre prochaine main.
 * Jouez un tour supplémentaire après celui-ci (mais pas un troisième
 * consécutif).
 */
public class Outpost extends Card {
    public Outpost() {

        super("Outpost", 5, CardType.ACTION, CardType.DURATION);
        addComponent(new DurationComponent(0, p -> {}, this));
    }

    @Override
    public void play(Player p) {
        super.play(p);
        p.updateDrawBonusValue(-2);
        addComponent(new ExtraTurnComponent());

    }
}
