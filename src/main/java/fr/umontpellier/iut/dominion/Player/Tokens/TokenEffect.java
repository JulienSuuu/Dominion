package fr.umontpellier.iut.dominion.Player.Tokens;

import fr.umontpellier.iut.dominion.Item;
import fr.umontpellier.iut.dominion.Player.Player;
import fr.umontpellier.iut.dominion.cards.Events.Event;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class TokenEffect {

    private static final Map<Token, BiConsumer<Event, Player>> effects = Map.ofEntries(
            Map.entry(Token.ONE_MONEY_TOKEN, (event, player) ->applyPlusOne(event, player, Item.MONEY)),
            Map.entry(Token.ONE_ACTION_TOKEN, (event, player) ->applyPlusOne(event, player, Item.ACTION) ),
            Map.entry(Token.ONE_BUY_TOKEN, (event, player) ->applyPlusOne(event, player, Item.BUY) ),
            Map.entry(Token.ONE_CARD_TOKEN, (event, player) -> player.draw(1) ),
            Map.entry(Token.TRASHING_TOKEN, TokenEffect::applyTrashingEffect)
    );

    private static void applyPlusOne(Event event, Player player, Item item) {
        player.increment(item, 1);
    }

    private static void applyTrashingEffect(Event event, Player player) {
        player.chooseCardFromHand("Trashing Token", true).ifPresent(player::trash);
    }

    public static void execute(Token token, Event event, Player player) {
        if (effects.containsKey(token)) {
            effects.get(token).accept(event, player);
        }
    }

    public static Set<Token> getTokens() {
        return effects.keySet();
    }


}
