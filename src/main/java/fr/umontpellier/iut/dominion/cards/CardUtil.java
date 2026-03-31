package fr.umontpellier.iut.dominion.cards;

import fr.umontpellier.iut.dominion.Player;

import java.util.ArrayList;
import java.util.List;

public class CardUtil {
    /**
     * Donne et affique dans les logs, les différents effets de la carte (piece, action, achat et pioche )
     * @param p le joueur (lanceur)
     * @param money  l'argent/s à donner au joueur
     * @param action action/s à donner au joueur
     * @param card carte/s de la pioche à donner au joueur
     * @param buy action/s d'Achat/s à donner au joueur
     * @param name le nom de l'effet ( Effect ou Duration )
     * @param c la carte de l'effet
     */
    public static void TriggerEffect(Player p, int money, int action, int card, int buy, String name, Card c) {
        StringBuilder builder = new StringBuilder(String.format("%s %s : ", name, c.getName().toUpperCase()));
        List<String> bonuses = new ArrayList<>();

        if (card > 0) {
            p.draw(card);
            bonuses.add(String.format("+%d Carte%s", card, card > 1 ? "s" : ""));
        }
        if (action > 0) {
            p.incrementActions(action);
            bonuses.add(String.format("+%d Action%s", action, action > 1 ? "s" : ""));
        }
        if (buy > 0) {
            p.incrementBuyActions(buy);
            bonuses.add(String.format("+%d Achat%s", buy, buy > 1 ? "s" : ""));
        }
        if (money > 0) {
            p.incrementMoney(money);
            bonuses.add(String.format("+%d Pièce%s", money, money > 1 ? "s" : ""));
        }

        builder.append(String.join(", ", bonuses));

        p.log(builder.toString());
    }
}
