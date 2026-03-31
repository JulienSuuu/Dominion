package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.Button;
import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;

import java.util.List;
import java.util.Objects;

/**
 * Carte Embargo
 * <p>
 * +2 Pièces
 * Écartez ceci pour placer un jeton Embargo sur une pile de la réserve.
 * (Pendant le reste de la partie, quand un joueur achète une carte de cette
 * pile, il reçoit une Malédiction (Curse).)
 */
public class Embargo extends Card {
    public Embargo() {
        super("Embargo", 2, CardType.ACTION);
    }

    @Override
    public void play(Player p) {
        CardUtil.TriggerEffect(p, 2,0,0,0,"Effect", this);

        String choice = p.chooseStringFromButtons("Veux tu écarter cette carte pour poser une malédiction sur une pile", List.of(
                new Button("Oui", "y"), new Button("Non", "n")
        ), false);

        if(choice.equals("y")) {
            p.getGame().moveToTrash(this);
            Card chosen = p.chooseCardFromSupply("Choisissez une pile de la réserve à maudire", Objects::nonNull, false);
            p.getGame().setToken(chosen.getName());
            p.log(String.format("Curse %s : +%s a été maudit", getName().toUpperCase(), chosen.getName().toUpperCase()));

        }

    }
}


