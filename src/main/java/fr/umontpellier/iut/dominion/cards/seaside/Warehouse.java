package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;

/**
 * Carte Entrepôt (Warehouse)
 * <p>
 * +3 Cartes
 * +1 Action
 * Défaussez 3 cartes.
 */
public class Warehouse extends Card {
    public Warehouse() {
        super("Warehouse", 3, CardType.ACTION);
    }

    @Override
    public void play(Player p) {
        CardUtil.TriggerEffect(p,0,1,3,0,"Effect", this);
        p.discardFromHand(3);
    }
}
