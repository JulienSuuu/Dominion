package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.Button;
import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;

import java.util.List;

/**
 * Carte Ambassadeur (Ambassador)
 * <p>
 * Dévoilez une carte de votre main.
 * Replacez, de votre main, à la réserve, jusqu'à 2 exemplaires de cette carte.
 * Ensuite, tous vos adversaires reçoivent un exemplaire de cette carte.
 */
public class Ambassador extends Card {
    public Ambassador() {
        super("Ambassador", 3, CardType.ACTION, CardType.ATTACK);
    }

    @Override
    public void play(Player p) {
        Card choice = p.chooseCardFromHand("Dévoile une carte de ta main", false);

        if(choice != null){
            p.log(p.getName() + " a dévoilé " + choice.getName());
            String exemplaire = p.chooseStringFromButtons("Choisi, 0, 1 ou 2 exemplaires de la carte" + choice.getName(), List.of(new Button("1", "1"), new Button("2", "2")), true);
            if(!exemplaire.isEmpty()){
                for(int i = 0; i < Integer.parseInt(exemplaire); i++){
                    Card choosed = p.chooseCardFromHand("Choisi la même carte à remetttre dans la réserve", false);
                    if(!p.getGame().replaceCardInSupply(choosed, choice)){
                        i--;
                    }
                }
            }
            p.getGame().processGain(p, this, Destination.DISCARD, choice.getName());
        }
    }
}
