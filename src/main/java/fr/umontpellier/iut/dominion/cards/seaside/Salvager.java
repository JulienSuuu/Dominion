package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;

/**
 * Carte Sauveteur (Salvager)
 * <p>
 * +1 Achat
 * Écartez une carte de votre main. +1 Pièce par Pièce de son coût.
 */
public class Salvager extends Card {
    public Salvager() {
        super("Salvager", 4, CardType.ACTION);
    }

    @Override
    public void play(Player p) {
        CardUtil.TriggerEffect(p, 0,0,0,1, "Effect", this);
        Card c = p.chooseCardFromHand("Choisis une carte à écarter", false);
        p.getGame().moveToTrash(c);
        p.incrementMoney(c.getCost());
    }
}
