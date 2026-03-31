package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;

/**
 * Carte Marée (Tide Pools)
 * <p>
 * +3 Cartes
 * +1 Action
 * Au début de votre prochain tour, défaussez 2 cartes.
 */
public class TidePools extends Card {
    public TidePools() {

        super("Tide Pools", 4, CardType.ACTION, CardType.DURATION);
        addComponent(new DurationComponent(0, player -> player.discardFromHand(2)
        , this));
    }

    @Override
    public void play(Player p) {
        super.play(p);
        CardUtil.TriggerEffect(p, 0,1,3,0, "Effect", this );

    }
}
