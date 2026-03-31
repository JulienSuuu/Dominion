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
 * Carte Vigie (Lookout)
 * <p>
 * +1 Action
 * Consultez les 3 premières cartes des votre pioche. Écartez-en une.
 * Défaussez-en une. Placez la carte restante sur le haut de votre pioche.
 */
public class Lookout extends Card {
    public Lookout() {
        super("Lookout", 3, CardType.ACTION);
    }

    @Override
    public void play(Player p) {
        CardUtil.TriggerEffect(p,0,1,0,0,"Effect", this);
        List<Card> view = p.getCardsInDraw().reversed().subList(0,3);
        Card trash = p.chooseCardFromButtons("Choississez une carte à écarter", view, false);
        p.getGame().moveToTrash(trash);
        view.remove(trash);
        Card discard = p.chooseCardFromButtons("Choississez une carte à défaussez", view, false);
        view.remove(discard);
        p.moveTo(discard, Destination.DISCARD);
        p.log(String.format("Action %s : %s défausse %s", getName().toUpperCase(), p.getName(), discard.getName().toUpperCase()));

        if(!view.isEmpty()){
            p.moveTo(view.getFirst(), Destination.DRAW);
        }
    }
}
