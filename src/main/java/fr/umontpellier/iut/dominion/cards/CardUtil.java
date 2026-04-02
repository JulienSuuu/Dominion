package fr.umontpellier.iut.dominion.cards;

import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;
import fr.umontpellier.iut.dominion.cards.component.OnPlayComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

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
        if (money > 0) {
            p.incrementMoney(money);
            bonuses.add(String.format("+%d Pièce%s", money, money > 1 ? "s" : ""));
        }
        if (buy > 0) {
            p.incrementBuyActions(buy);
            bonuses.add(String.format("+%d Achat%s", buy, buy > 1 ? "s" : ""));
        }


        builder.append(String.join(", ", bonuses));

        p.log(builder.toString());
    }

    public static Card gainFromSupply(Player p,String message, Predicate<Card> filter, Destination dest, boolean silent) {
        Card supplyCard = p.chooseCardFromSupply(message, filter, false);
        return gainIfPresent(p, supplyCard, dest, silent);
    }

    public static Card gainFromSupply(Player p, String name, Destination dest, boolean silent) {
        Card supplyCard = p.getCardFromSupply(name);
        return gainIfPresent(p, supplyCard, dest, silent);
    }

    public static List<Card> getTopCards(Player p, int count) {
        if (p.getCardsInDraw().size() < count) p.shuffle();
        List<Card> draw = p.getCardsInDraw();
        List<Card> result = new ArrayList<>();
        int actualCount = Math.min(draw.size(), count);

        for (int i = 0; i < actualCount; i++) {
            result.add(draw.get(draw.size() - 1 - i));
        }
        return result;
    }

    public static List<Card> getBottomCards(Player p, int count) {
        if (p.getCardsInDraw().size() < count) p.shuffle();
        List<Card> draw = p.getCardsInDraw();
        List<Card> result = new ArrayList<>();
        int actualCount = Math.min(draw.size(), count);

        for (int i = 0; i < actualCount; i++) {
            result.add(draw.get(i));
        }
        return result;
    }

    public static Card gainIfPresent(Player p, Card target, Destination dest, boolean silent) {
        if (target != null) {
            if (silent) p.gainSilent(target, dest, true);
            else p.gain(target, dest);
        }
        return target;
    }


    public static void addSimpleComponent(Card c, int cardsNow, int actsNow, int buysNow, int coinsNow,
                                          int cardsNext, int actsNext, int buysNext, int coinsNext) {
        addSimpleAction(c, cardsNow, actsNow, buysNow, coinsNow);
        addSimpleDuration(c, cardsNext, actsNext, buysNext, coinsNext);
    }

    public static void addSimpleAction(Card c, int cardsNow, int actsNow, int buysNow, int coinsNow ){
        c.addComponent(OnPlayComponent.class, p -> CardUtil.TriggerEffect(p, coinsNow, actsNow, cardsNow, buysNow, "Effect", c));

    }

    public static void addSimpleDuration(Card c, int cardsNext, int actsNext, int buysNext, int coinsNext) {
        c.addComponent(new DurationComponent(0, p -> CardUtil.TriggerEffect(p, coinsNext, actsNext, cardsNext, buysNext, "Duration", c), c));
    }
}
