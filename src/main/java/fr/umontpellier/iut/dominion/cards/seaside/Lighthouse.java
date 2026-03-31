package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.component.CardComponent;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent;

/**
 * Carte Phare (Lighthouse)
 * <p>
 * +1 Action
 * Maintenant et au début de votre prochain tour, +1 Pièce.
 * D'ici là, les cartes Attaque jouées par vos adversaires ne vous affectent
 * pas.
 */
public class Lighthouse extends Card {
    public Lighthouse() {
        super("Lighthouse", 2, CardType.ACTION, CardType.DURATION);
        addComponent(new DurationComponent(0, p -> CardUtil.TriggerEffect(p, 1,0,0,0, "Duration", this), this));
        addComponent(TriggerComponent.Immunity.class, new TriggerComponent.Immunity() {});
    }

    @Override
    public void play(Player p) {
        super.play(p);
        CardUtil.TriggerEffect(p, 1, 1, 0, 0, "Effect", this);
    }
}