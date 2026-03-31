package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;

/**
 * Carte Havre (Haven)
 * <p>
 * +1 Carte
 * +1 Action
 * Mettez de côté une carte de votre main face cachée (sous cette carte).
 * Au début de votre prochain tour, prenez-la en main.
 */
public class Haven extends Card {
    public Haven() {
        super("Haven", 2, CardType.ACTION, CardType.DURATION);
    }


    @Override
    public void play(Player p) {
        CardUtil.TriggerEffect(p,0,1,1,0,"Duration", this);

        Card hide = p.chooseCardFromHand("Choissisez une carte de votre main", false );
        if(hide != null) {
            p.moveTo(hide, Destination.ASIDE);

            p.log(String.format("Action %s : %s cache %s", getName().toUpperCase(), p.getName(), hide.getName().toUpperCase()));

            addComponent(new DurationComponent(1, player ->{
                player.moveToHand(hide);
                player.log(String.format("Duration %s : %s récupère %s qui été cachée", getName().toUpperCase(), player.getName(), hide.getName().toUpperCase()));
                }, this));
        }
    }
}
