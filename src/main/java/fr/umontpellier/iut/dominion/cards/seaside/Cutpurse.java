package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;

/**
 * Carte Coupeur de bourse (Cutpurse)
 * <p>
 * +2 Pièces
 * Tous vos adversaires défaussent un Cuivre (Copper) (ou dévoilent une main
 * sans Cuivre).
 */
public class Cutpurse extends Card {
    public Cutpurse() {
        super("Cutpurse", 4, CardType.ACTION, CardType.ATTACK);
    }

    @Override
    public void play(Player p) {
        CardUtil.TriggerEffect(p, 2,0,0,0,"Effect", this);
        p.getGame().discardOrShowHand(p, "Copper", this);
    }
}
