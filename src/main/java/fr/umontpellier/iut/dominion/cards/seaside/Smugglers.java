package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;

import java.util.ArrayList;
import java.util.List;

/**
 * Contrebandiers (Smugglers)
 * <p>
 * Recevez un exemplaire d'une carte coûtant jusqu'à 6 Pièces que le joueur
 * à votre droite a reçues à son dernier tour.
 */
public class Smugglers extends Card {
    public Smugglers() {
        super("Smugglers", 3, CardType.ACTION);
    }

    @Override
    public void play(Player p) {
        Player right = p.getGame().onTheRight(p);
        if(right == null) return;
        List<String> choices = new ArrayList<>();
        for(Card c : right.getCardGainedLastTurn()){
            if(c.getCost()>6) continue;
            choices.add("SUPPLY:" + c.getName());
        }
        String choice = p.choose("Choississez une carte de la liste du joueur de votre droite, prix<6" + right.getCardGainedLastTurn(), choices, new ArrayList<>(), false);
        if(choice.isEmpty()) return;
        Card c = p.getCardFromSupply(choice.split(":")[1]);
        if(c!= null){
            p.gain(c, Destination.DISCARD);
            p.log(String.format("Action %s : %s est copié ", getName().toUpperCase(), c.getName()));
        }
    }
}
