package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent;

/**
 * Carte Pirate
 * <p>
 * Au début de votre prochain tour, recevez en main un Trésor coûtant jusqu'à
 * 6 Pièces.
 * Quand un joueur reçoit un Trésor, vous pouvez jouer cette carte depuis votre
 * main.
 */
public class Pirate extends Card {
    public Pirate() {
        super("Pirate", 5, CardType.ACTION, CardType.DURATION, CardType.REACTION);

        addComponent(new DurationComponent(0, p -> {
            Card c = p.chooseCardFromSupply(
                    "Choississez un trésor valant au maximum 6 pièces",
                    card -> card.hasType(CardType.TREASURE) && card.getCost() <= 6,
                    false
                    );
            if(c != null) {
                p.gain(c, Destination.HAND);
                p.log(String.format("Action %s : %s récupère %s coutant %d pièces", getName().toUpperCase(), p.getName(), c.getName().toUpperCase(), c.getCost()));
            }

        }, this));

        addComponent(TriggerComponent.OnPlayerGain.class, ((owner, victims, c) -> {
            if(c.hasType(CardType.TREASURE) && owner.getCardsInHand().contains(this)) {
                Card choice = owner.chooseCardFromHand("Veux tu jouer ton pirate ?", card -> card.hasSameNameAs(this), true);
                if(choice != null ){
                    owner.playCard(this);
                    owner.log(String.format("Reaction %s", getName().toUpperCase()));
                }

            }
        }
                ));
    }
}
