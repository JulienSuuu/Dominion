package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;

/**
 * Carte Navire marchand (Merchant Ship)
 * <p>
 * Maintenant et au début de votre prochain tour, +2 Pièces.
 */
public class MerchantShip extends Card {
    public MerchantShip() {
        super("Merchant Ship", 5, CardType.ACTION, CardType.DURATION);
        addComponent(new DurationComponent(0, player -> CardUtil.TriggerEffect(player,2,0,0,0,"Duration", this)
, this));
    }

    @Override
    public void play(Player p) {
        super.play(p);
        CardUtil.TriggerEffect(p,2,0,0,0,"Effect", this);
    }

}
