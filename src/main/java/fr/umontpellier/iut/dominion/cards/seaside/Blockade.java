package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent;

/**
 * Carte Blocus (Blockade)
 * <p>
 * Recevez une carte coûtant jusqu'à 4 Pièces, en la mettant de côté.
 * Au début de votre prochain tour, prenez-la en main
 * Tant qu'elle est mise de côté, quand un autre joueur en reçoit un
 * exemplaire durant leur tour, il reçoit une Malédiction (Curse).
 */
public class Blockade extends Card {
    public Blockade() {
        super("Blockade", 4, CardType.ACTION, CardType.DURATION, CardType.ATTACK);
    }

    @Override
    public void play(Player p) {

        Card blocked = p.chooseCardFromSupply("Choississez une carte qui coûte au maximum 4 de pièces", card -> card.getCost() > 0 && card.getCost()<=4, false );

        if(blocked != null){
            p.gainSilent(blocked, Destination.ASIDE, true);
            p.log(String.format("%s bloquée", blocked.getName().toUpperCase()));
        }

        addComponent(new DurationComponent(1,player ->{
            if(blocked != null){
                player.moveTo(blocked, Destination.HAND);
            }
        }, this  ));

        addComponent(TriggerComponent.OnPlayerGain.class, (player, victim, c) -> {
            if(blocked != null && c.hasName(blocked.getName()) && victim != player){
                victim.gain(victim.getCardFromSupply("Curse"), Destination.DISCARD);
                p.log(String.format("TRIGGER %s : %s gagne une malédiction ", this.getName().toUpperCase(), victim.getName()));
            }

        });
    }
}
