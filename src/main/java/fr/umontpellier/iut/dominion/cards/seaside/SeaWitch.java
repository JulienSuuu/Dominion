package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;

/**
 * Carte Sorcière marine (Sea Witch)
 * <p>
 * +2 Cartes
 * Tous vos adversaires reçoivent une Malédiction (Curse).
 * Au début de votre prochain tour, +2 Cartes, puis défaussez 2 cartes.
 */
public class SeaWitch extends Card {
    public SeaWitch() {

        super("Sea Witch", 5, CardType.ACTION, CardType.DURATION, CardType.ATTACK);
        addComponent(new DurationComponent(0, p-> {
            CardUtil.TriggerEffect(p, 0,0,2,0, "Duration", this);
            p.discardFromHand(2);
        }, this));
    }

    @Override
    public void play(Player p) {
        super.play(p);
        CardUtil.TriggerEffect(p, 0,0,2,0, "Effect", this);
        p.getGame().processGain(p, this, Destination.DISCARD, "Curse");

    }
}
