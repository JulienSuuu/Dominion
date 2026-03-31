package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;

/**
 * Carte Village de pêcheurs (Fishing Village)
 * <p>
 * +2 Actions
 * +1 Pièce
 * Au début de votre prochain tour, +1 Action et +1 Pièce.
 */
public class FishingVillage extends Card {
    public FishingVillage() {

        super("Fishing Village", 3, CardType.ACTION, CardType.DURATION);
        addComponent(new DurationComponent(0, player -> CardUtil.TriggerEffect(player, 1,1,0,0,"Duration", this), this));
    }

    public void play(Player p) {
        super.play(p);
        CardUtil.TriggerEffect(p, 1,2,0,0,"Effect", this);
    }



}