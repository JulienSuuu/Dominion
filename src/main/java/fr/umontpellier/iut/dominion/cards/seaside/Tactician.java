package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;

/**
 * Carte Tacticien (Tactician)
 * <p>
 * Si vous avez au moins une carte en main, défaussez votre main, et au debut
 * de votre prochain tour, +5 Cartes, +1 Action, et +1 Achat.
 */
public class Tactician extends Card {
    public Tactician() {
        super("Tactician", 5, CardType.ACTION, CardType.DURATION);
        addComponent(new DurationComponent(0, player -> {
            CardUtil.TriggerEffect(player, 0,1,5,1,"Duration", this);
        }, this));

    }

    @Override
    public void play(Player p) {
        if(p.getCardsInHand().isEmpty())return;
        p.getCardsInHand().forEach(card -> p.moveTo(card, Destination.DISCARD));
        p.log(String.format("Action %s : %s défausse toutes ses cartes", getName().toUpperCase(), p.getName()));
        super.play(p);
    }
}
