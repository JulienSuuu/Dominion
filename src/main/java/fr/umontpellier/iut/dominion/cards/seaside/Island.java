package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;

/**
 * Carte Île (Island)
 * <p>
 * 2 VP
 * Placez cette carte et une carte de votre main sur votre plateau Île (Island
 * Mat).
 */
public class Island extends Card {
    public Island() {
        super("Island", 4, CardType.ACTION, CardType.VICTORY);
    }

    @Override
    public void play(Player p) {
        p.moveTo(this, Destination.ISLAND);
        Card choosen = p.chooseCardFromHand("Choississez une carte de votre main à placer sur l'île", false);
        p.moveTo(choosen, Destination.ISLAND);
        p.log(String.format("Action %s : %s place %s ", getName().toUpperCase(), p.getName(), choosen.getName().toUpperCase())  );
    }


    @Override
    public int getVictoryValue() {
        return 2;
    }
}
