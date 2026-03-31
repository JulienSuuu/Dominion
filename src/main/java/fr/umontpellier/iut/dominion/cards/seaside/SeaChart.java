package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;

/**
 * Carte marine (Sea Chart)
 * <p>
 * +1 Carte
 * +1 Action
 * Dévoilez la carte du haut de votre pioche. Si vous en avez un exemplaire
 * en jeu, prenez-la en main.
 */
public class SeaChart extends Card {
    public SeaChart() {
        super("Sea Chart", 3, CardType.ACTION);
    }

    @Override
    public void play(Player p) {
        CardUtil.TriggerEffect(p, 0,1,1,0, "Effect", this);
        Card c = p.getCardFromDeck();
        if(c != null){
            boolean hasCopy = p.getCardsInPlay().stream().anyMatch(card -> card.hasSameNameAs(c));
            if(hasCopy) p.moveToHand(c);
        }
    }
}
