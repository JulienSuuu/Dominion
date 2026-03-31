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
 * Carte Village indigène (Native Village)
 * <p>
 * +2 Actions
 * Choisissez : placez la carte du haut de votre pioche, face cachée, sur votre
 * plateau Village indigène (vous pouvez consulter ces cartes à tout moment);
 * ou prenez en main toutes les cartes du plateau.
 */
public class NativeVillage extends Card {
    public NativeVillage() {
        super("Native Village", 2, CardType.ACTION);
    }

    @Override
    public void play(Player p) {
        CardUtil.TriggerEffect(p, 0,2,0,0,"Effect", this);

        List<Button> buttons = new ArrayList<>();
        buttons.add(new Button("add", "add"));
        buttons.add(new Button("take", "take"));

        String choice = p.chooseStringFromButtons("Choississez entre poser une carte sur votre village ou de récupérer toutes vos cartes", buttons, false);

        if (choice.equals("add")) {
            p.drawTo(Destination.NATIVE);
        }

        if(choice.equals("take")) {
            for (Card c : p.getCardsOnNativeVillageMat()){
                p.moveTo(c, Destination.HAND);
            }
        }

    }
}
