package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.Button;
import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;

import java.util.List;

/**
 * Carte Navigateur (Navigator)
 * <p>
 * +2 Pièces
 * Consultez les 5 premières cartes de votre pioche.
 * Défaussez-les toutes ou replacez-les sur votre pioche dans l'ordre de
 * votre choix.
 */
public class Navigator extends Card {
    public Navigator() {
        super("Navigator", 4, CardType.ACTION);
    }

    @Override
    public void play(Player p) {
        CardUtil.TriggerEffect(p, 2,0,0,0,"Effect", this);

        List<Card> view = p.getCardsInDraw().subList(0, Math.min(p.getCardsInDraw().size(), 5));


        String choice = p.chooseStringFromButtons("Défausse tout ou replace les cartes dans l'ordre que tu veux", List.of(new Button("discard", "d"), new Button("replace", "r")), false);
        if(choice.equals("d")) {
            for(Card c : view) {
                p.moveTo(c, Destination.DISCARD);
            }
        }else if(choice.equals("r")) {
            while(!view.isEmpty()) {
                Card c = p.chooseCardFromButtons("Remet les cartes dans l'ordre que tu veux", view, false );
                if(c != null) {
                    view.remove(c);
                    p.moveTo(c, Destination.DRAW);
                }
            }
        }
    }
}
