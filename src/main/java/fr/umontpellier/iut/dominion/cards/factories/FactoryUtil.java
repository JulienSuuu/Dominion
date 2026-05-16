package fr.umontpellier.iut.dominion.cards.factories;

import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.Events.Event;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;

import java.util.Collection;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class FactoryUtil {
    public static final Predicate<Card> activate = card -> card.getComponent(DurationComponent.class).map(d -> !d.isFinished(card) && card.hasForLocation(Destination.INPLAY)).orElse(false);
    public static final String EFFECT = "Effect";
    public static final String ACTION = "Action";
    public static final String DURATION = "Duration";
    public static final String TRASHED_ACTION = "Trashed Action";
    public static final String GAIN_ACTION = "Gain Action";
    public static final String CG = "Cornucopia_Guilds";
    public static final String DA = "Dark_Ages";

    public static final BiPredicate<Card, Card> lessThan = (purchased, selection) -> {
        boolean exceeds = selection.getCost() > purchased.getCost() ||
                selection.getPotion() > purchased.getPotion() ||
                selection.getDebt() > purchased.getDebt();

        if (exceeds) return false;

        return selection.getCost() < purchased.getCost() ||
                selection.getPotion() < purchased.getPotion() ||
                selection.getDebt() < purchased.getDebt();
    };

    public static BiPredicate<Event, Player> reserveCondition(Card self, BiPredicate<Event, Player> condition) {
       return((event, player) ->{
           boolean isAtTavernMat = self.hasForLocation(Destination.TAVERN);
           return isAtTavernMat && condition.test(event, player) && event.getPlayer() == player;
       });
    }

    public static void linkedCard(Card self, Card toLinked){
        if(activate.test(toLinked)) self.getCollection("LinkedCard").add(toLinked);
    }
    public static Predicate<Card> checkDuration = card -> card.getComponent(DurationComponent.class).map(DurationComponent::checkDuration).orElse(card1 -> false).test(card);
    public static Predicate<Card> checkLink = card -> {
        Collection<Card> check = card.getCollection("LinkedCard");
        return check.stream().noneMatch(activate);
    };


}
