package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent;

/**
 * Carte Singe (Monkey)
 * <p>
 * Jusqu'à votre prochain tour, quand le joueur à votre droite reçoit une
 * carte, +1 Carte.
 * Au début de votre prochain tour, +1 Carte.
 */
public class Monkey extends Card {
    public Monkey() {
        super("Monkey", 3, CardType.ACTION, CardType.DURATION);
        addComponent(new DurationComponent(0, p -> CardUtil.TriggerEffect(p, 0,0,1,0, "Duration", this), this));

        addComponent(TriggerComponent.OnPlayerGain.class, (owner, actor, card) -> {
            if (owner.getGame().onTheRight(owner, actor)) {
                owner.draw(1);}
        });
    }
}
