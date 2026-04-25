package fr.umontpellier.iut.dominion.cards;

import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Item;
import fr.umontpellier.iut.dominion.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    @Deprecated
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

    public static void TriggerEffect(Player p, String effectName, Card c, Bonus bonus) {
        List<String> logs = new ArrayList<>();

        if (bonus.cardsToDraw() > 0) {
            p.draw(bonus.cardsToDraw());
            logs.add("+" + bonus.cardsToDraw() + " Card(s)");
        }

        bonus.items().forEach((item, qty) -> {
            if (qty > 0) {
                p.increment(item, qty);
                logs.add("+" + qty + " " + item.name());

            }

        });

        p.log(effectName + " " + c.getName().toUpperCase() + " : " + String.join(", ", logs));
    }


    public static Card gainMultiplyCardFromSupply(Player p, String cardName, Destination dest, int numberOfCard) {
        Card c = null;
        for(int index = 0; index < numberOfCard; index++) {
            c = gainFromSupply(p, cardName, dest, false);
            if(c == null)break;
        }
        return c;
    }


    public static Card gainFromSupply(Player p,String message, Predicate<Card> filter, Destination dest, boolean silent) {
        Optional<Card> supplyCard = p.chooseCardFromSupply(message, filter, false);
        return supplyCard.map(card -> gainIfPresent(p, card, dest, silent)).orElse(null);
    }

    public static Card gainFromSupply(Player p, String name, Destination dest, boolean silent) {
        Card supplyCard = p.getCardFromSupply(name);
        return gainIfPresent(p, supplyCard, dest, silent);
    }

    public static Card gainIfPresent(Player p, Card target, Destination dest, boolean silent) {
        if (target != null) {
            if (silent) p.gainSilent(target, dest, true);
            else p.gain(target, dest);
            p.log( p.toLog() + " gained " + target.toLog());
        }
        return target;
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

    public static Card moveIfPresent(Player p, Card target, Destination dest) {
        if (target != null) {
             p.moveTo(target, dest);
        }
        return target;
    }

    public static <T> void executeIfSelected(Supplier<T> selector, Consumer<T> action ) {
        Optional.ofNullable(selector.get()).ifPresent(action);
    }

    public static <T> void executeOrOtherwise(Supplier<T> selector, Predicate<T> filter, Consumer<T> action, Runnable other ) {
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
        p.chooseCardFromHand("Dévoile une carte", false).ifPresent(
                revealed -> {
                    p.log(p.getName() + " dévoile " + revealed.getName());
                    handleReplacements(p, revealed, 2);
                    p.getGame().processGain(p, self, DISCARD, revealed.getName());
                }
        );
    }

    private static void handleReplacements(Player p, Card revealed, int max){
        for (int i = 0; i < max; i++) {
            Optional<Card> c = p.chooseCardFromHand("Remettre en réserve (max " + max + ")", revealed::hasSameNameAs, true);
            if (c.isEmpty()) break;
            Card card = c.get();
            p.getGame().replaceCardInSupply(card, revealed);
        }
    }

}
