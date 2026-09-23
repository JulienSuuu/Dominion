package fr.umontpellier.iut.dominion.gui;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fr.umontpellier.iut.dominion.cards.Card;

public class Utils {
    public static String toLog(List<Card> list) {
        return list.stream()
                .collect(Collectors.groupingBy(Card::getName, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getValue() > 1 ? "%s x%d".formatted(e.getKey(), e.getValue()) : e.getKey())
                .collect(Collectors.joining(", "));
    }

    public static String toString(List<Card> list) {
        return list.stream().map(Card::toString).collect(Collectors.joining(", "));
    }

    public static String toJSON(List<Card> list) {
        return list.stream().map(c -> "\"" + c.getName() + "\"").collect(Collectors.joining(", ", "[", "]"));
    }
}
