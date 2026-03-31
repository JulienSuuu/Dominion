package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.Button;
import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Carte Plongeur de perles (Pearl Diver)
 * <p>
 * +1 Carte
 * +1 Action
 * Consultez la carte du bas de votre pioche. Vous pouvez la placer sur le haut.
 */
public class PearlDiver extends Card {
    public PearlDiver() {
        super("Pearl Diver", 2, CardType.ACTION);
    }

    @Override
    public void play(Player p) {
        CardUtil.TriggerEffect(p, 0, 1, 1, 0, "Effect", this);
        Card c = p.getCardsInDraw().getFirst();

        if(c != null) {
            List<Button> buttons = new ArrayList<>();
            buttons.add(new Button("onTop", "y"));
            buttons.add(new Button("onBottom", "n"));
            String choice = p.chooseStringFromButtons("Choix: Placez votre carte au dessus de votre pioche : " + c.getName() , buttons, true);

            if(choice.equals("y")){
                p.moveTo(c, Destination.DRAW);
            };
        }
    }
}
