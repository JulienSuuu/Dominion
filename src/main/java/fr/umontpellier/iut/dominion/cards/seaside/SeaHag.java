package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;

/**
 * Carte Sorcière de mer (Sea Hag)
 * <p>
 * Tous vos adversaires défaussent la carte du haut de leur pioche, puis 
 * reçoivent une Malédiction (Curse) sur leur pioche.
 */
public class SeaHag extends Card {

    public SeaHag() {
        super("Sea Hag", 4, CardType.ACTION, CardType.ATTACK);
    }

    @Override
    public void play(Player p) {
        p.getGame().processDiscard(p, this);
        p.getGame().processGain(p, this, Destination.DRAW, "Curse");
    }
}
