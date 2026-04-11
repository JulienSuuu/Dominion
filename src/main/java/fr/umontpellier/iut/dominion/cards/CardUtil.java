package fr.umontpellier.iut.dominion.cards;

import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Item;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;
import fr.umontpellier.iut.dominion.cards.component.OnPlayComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static fr.umontpellier.iut.dominion.Destination.*;

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
            p.increment(Item.ACTION, action);
            bonuses.add(String.format("+%d Action%s", action, action > 1 ? "s" : ""));
        }
        if (money > 0) {
            p.increment(Item.MONEY, money);
            bonuses.add(String.format("+%d Pièce%s", money, money > 1 ? "s" : ""));
        }
        if (buy > 0) {
            p.increment(Item.BUY, buy);
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
        List<Card> draw = getCards(p, count);
        List<Card> result = new ArrayList<>();
        int actualCount = Math.min(draw.size(), count);

        for (int i = 0; i < actualCount; i++) {
            result.add(draw.get(draw.size() - 1 - i));
        }
        return result;
    }




    public static List<Card> getBottomCards(Player p, int count) {
        List<Card> draw = getCards(p, count);
        List<Card> result = new ArrayList<>();
        int actualCount = Math.min(draw.size(), count);

        for (int i = 0; i < actualCount; i++) {
            result.add(draw.get(i));
        }
        return result;
    }

    private static List<Card> getCards(Player p, int count) {
        if (p.getCopyOf(DRAW).size() < count) p.shuffle();
        return p.getCopyOf(DRAW);
    }

    public static Card gainIfPresent(Player p, Card target, Destination dest, boolean silent) {
        if (target != null) {
            if (silent) p.gainSilent(target, dest, true);
            else p.gain(target, dest);
        }
        return target;
    }

    public static Card moveIfPresent(Player p, Card target, Destination dest) {
        if (target != null) {
             p.moveTo(target, dest);
        }
        return target;
    }

    public static <T>   void executeIfSelected(Supplier<T> selector, Consumer<T> action ) {
        Optional.ofNullable(selector.get()).ifPresent(action);
    }

    public static <T> void executeOrOtherWise(Supplier<T> selector, Predicate<T> filter, Consumer<T> action, Runnable other ) {
        Optional.ofNullable(selector.get()).filter(filter).ifPresentOrElse(action, other);
    }

    public static void moveTo(Player p, Supplier<Card> getter, Consumer<Card> setter, Destination dest) {
        Card card = getter.get();
        if (card != null) {
            p.moveTo(card, dest);
            setter.accept(null);
        }
    }

    public static void execute( Runnable... actions) {
        for (Runnable action : actions) {
            action.run();
        }
    }

    public static void executeAmbassador(Player p, Card self){
        CardUtil.executeIfSelected(
                () -> p.chooseCardFromHand("Dévoile une carte", false),
                revealed -> {
                    p.log(p.getName() + " dévoile " + revealed.getName());
                    handleReplacements(p, revealed, 2);
                    p.getGame().processGain(p, self, DISCARD, revealed.getName());
                }
        );
    }

    private static void handleReplacements(Player p, Card revealed, int max){
        for (int i = 0; i < max; i++) {
            Card toReturn = p.chooseCardFromHand("Remettre en réserve (max 2)", revealed::hasSameNameAs, true);
            if (toReturn == null) break;
            p.getGame().replaceCardInSupply(toReturn, revealed);
        }
    }

}
