package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.Button;
import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;

import java.util.List;

/**
 * Carte Explorateur (Explorer)
 * <p>
 * Vous pouvez dévoiler une Province de votre main. Si vous le faites, recevez
 * un Or (Gold) en main. Sinon, recevez un Argent (Silver) en main.
 */
public class Explorer extends Card {
    public Explorer() {
        super("Explorer", 5, CardType.ACTION);
    }

    @Override
    public void play(Player p) {
        Card province = p.getCardsInHand().stream().filter(c-> c.hasSameNameAs(this)).findFirst().orElse(null);

        boolean revealed = false;
        if (province != null) {
            String choice = p.chooseStringFromButtons("Voulez-vous dévoiler une Province ? ", List.of(new Button(
                    "Oui", "y"
            ), new Button("Non", "n")), false);

            if(choice.equals("y")){
                revealed = true;
            }


        }

        String gain = revealed? "Gold":"Silver";

        Card gainedCard = p.getCardFromSupply(gain);

        if(gainedCard!=null){
            p.gain(gainedCard, Destination.HAND);
        }


    }
}
