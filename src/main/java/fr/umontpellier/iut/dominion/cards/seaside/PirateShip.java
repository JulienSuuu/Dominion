package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.Button;
import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;

import java.util.List;

/**
 * Carte Bateau pirate (Pirate Ship)
 * <p>
 * Choisissez : +1 Pièce par jeton Pièce sur votre plateau Bateau pirate ;
 * ou tous vos adversaires dévoilent les 2 premières cartes de leur pioche,
 * écartent un Trésor (Treasure) dévoilé de votre choix et défaussent le reste,
 * et si au moins un Trésor a été écarté, placez un jeton Pièce sur votre
 * plateau Bateau pirate.
 */
public class PirateShip extends Card {
    public PirateShip() {
        super("Pirate Ship", 4, CardType.ACTION, CardType.ATTACK);
    }

    @Override
    public void play(Player p) {

        String choice = p.chooseStringFromButtons("Choisissez : 1 pièce par Jeton pièce sur le Bateau ou attaque ?", List.of(new Button("Jeton", "j"), new Button("Attaque", "a")), false);
        if(choice.equals("j")) {
            int numberOfCoin = p.getCoins();
            p.incrementMoney(numberOfCoin);
            p.log(String.format("Action %s : %s récupère %d pièce sur son bateau", getName().toUpperCase(), p.getName(), numberOfCoin));
            return;
        }
        if(choice.equals("a")) {
            List<Card> treasureRemoved = p.getGame().processTreasureToTrash(p, this, 2);
            if(treasureRemoved.isEmpty()) return;
            p.incrementCoin(1);
        }
    }
}
