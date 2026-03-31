package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;
import fr.umontpellier.iut.dominion.cards.component.TriggerEffect;

/**
 * Carte Astrolabe
 * <p>
 * Maintenant et au début de votre prochain tour :
 * +1 Pièce
 * +1 Achat
 */
public class Astrolabe extends Card {
    public Astrolabe() {
        super("Astrolabe", 3, CardType.TREASURE, CardType.DURATION);
        addComponent(new DurationComponent(0, player -> CardUtil.TriggerEffect(player, 1,0,0,1, "Duration", this)
,this));
    }
    @Override
    public void play(Player p) {
        super.play(p);
        CardUtil.TriggerEffect(p, 1,0,0,1, "Effect", this);
    }

}
