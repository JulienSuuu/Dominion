package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;

/**
 * Carte Quai (Wharf)
 * <p>
 * Maintenant et au début de votre prochain tour : +2 Cartes et +1 Achat.
 */
public class Wharf extends Card {
    public Wharf() {

        super("Wharf", 5, CardType.ACTION, CardType.DURATION);
        addComponent(new DurationComponent(0,
                player -> CardUtil.TriggerEffect(player,0,0,2,1,"Duration", this),
                this));
    }

    @Override
    public void play(Player p) {
        super.play(p);
        CardUtil.TriggerEffect(p,0,0,2,1,"Effect", this);


    }
}
