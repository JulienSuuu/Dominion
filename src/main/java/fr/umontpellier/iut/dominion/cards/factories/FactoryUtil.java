package fr.umontpellier.iut.dominion.cards.factories;

import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class FactoryUtil {
    public static final Predicate<Card> activate = card -> card.as(DurationComponent.class).map(d -> !d.isFinished()).orElse(false);
    public static final String EFFECT = "Effect";
    public static final String ACTION = "Action";
    public static final String DURATION = "Duration";
    public static final String TRASHED_ACTION = "Trashed Action";
    public static final String GAIN_ACTION = "Gain Action";
    public static final String CG = "Cornucopia_Guilds";
    public static final String H = "Hinterlands";

    public static final BiPredicate<Card, Card> buyCondition = (purchased, selection) -> {
        boolean exceeds = selection.getCost() > purchased.getCost() ||
                selection.getPotion() > purchased.getPotion() ||
                selection.getDebt() > purchased.getDebt();

        if (exceeds) return false;

        return selection.getCost() < purchased.getCost() ||
                selection.getPotion() < purchased.getPotion() ||
                selection.getDebt() < purchased.getDebt();
    };
}
