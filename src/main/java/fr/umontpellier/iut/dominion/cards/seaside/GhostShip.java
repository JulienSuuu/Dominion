package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;

/**
 * Carte Vaisseau fantôme (Ghost Ship)
 * <p>
 * +2 Cartes
 * Tous vos adversaires ayant au moins 4 cartes en main placent des cartes
 * de leur main sur leur pioche jusqu'à avoir 3 cartes en main.
 */
public class GhostShip extends Card {
    public GhostShip() {
        super("Ghost Ship", 5, CardType.ACTION, CardType.ATTACK);
    }

    @Override
    public void play(Player p) {
        CardUtil.TriggerEffect(p,0,0,2,0,"Effect", this);
        p.getGame().processMoveTo(p, this, Destination.DRAW);

    }
}
