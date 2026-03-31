package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;

/**
 * Carte Caravane (Caravan)
 * <p>
 * +1 Carte
 * +1 Action
 * Au début de votre prochain tour, +1 Carte.
 */
public class Caravan extends Card {
    public Caravan() {

        super("Caravan", 4, CardType.ACTION, CardType.DURATION);
        addComponent(new DurationComponent(0, p -> CardUtil.TriggerEffect(p, 0,0,1,0,"Duration", this), this));
    }

    @Override
    public void play(Player p) {
        super.play(p);
        CardUtil.TriggerEffect(p, 0,1,1,0,"Effect", this);
    }
}
