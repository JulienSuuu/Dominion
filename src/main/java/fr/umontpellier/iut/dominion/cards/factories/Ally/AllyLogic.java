package fr.umontpellier.iut.dominion.cards.factories.Ally;

import fr.umontpellier.iut.dominion.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class AllyLogic {
    private final Map<String, BiConsumer<Player, AllyArgs>> hooks = new HashMap<>();

    public AllyLogic on(String eventName, BiConsumer<Player, AllyArgs> action) {
        hooks.put(eventName, action);
        return this;
    }

    public void executeHook(String methodName, Player player, Object[] args) {
        if (hooks.containsKey(methodName)) {
            hooks.get(methodName).accept(player, new AllyArgs(args));
        }
    }

}
