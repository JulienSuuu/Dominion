package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;

/**
 * Carte Bazar (Bazaar)
 * <p>
 * +1 Carte
 * +2 Actions
 * +1 Pièce
 */
public class Bazaar extends Card {
    public Bazaar() {
        super("Bazaar", 5, CardType.ACTION);
    }

    @Override
    public void play(Player p) {
        CardUtil.TriggerEffect(p, 1, 2, 1, 0 ,"Effect", this);
    }
}
