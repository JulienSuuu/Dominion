package fr.umontpellier.iut.dominion.cards;

import fr.umontpellier.iut.dominion.*;
import fr.umontpellier.iut.dominion.cards.factories.FactorySupplyPile;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.LongBinding;
import javafx.beans.property.*;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameStat {
    public static final BooleanProperty charlatanPower = new SimpleBooleanProperty(false);
    public static final IntegerProperty reduction = new SimpleIntegerProperty(0);
    public static final LongProperty emptyPiles = new SimpleLongProperty(0);
    public static final BooleanProperty isFinished = new SimpleBooleanProperty(false);

    public static Map<String, SupplyPile> allCardsInSupply;


    public static void initialize(Map<String, SupplyPile> allCards, ObjectProperty<Player> currentTurnPlayer) {
        allCardsInSupply = allCards;
        charlatanPower.bind(Bindings.createBooleanBinding(
                () -> allCardsInSupply.containsKey("Charlatan"),
                allCardsInSupply.values().toArray(new Observable[0])
        ));

        if(charlatanPower.get()) {
            allCardsInSupply.get("Curse")
                    .forEach(p -> p.addType(CardType.TREASURE));
        }

        LongBinding emptyPilesCount = Bindings.createLongBinding(
                () -> allCards.values().stream()
                                .filter(AbstractCollection::isEmpty)
                                .count(),
                allCards.values().toArray(new Observable[0])
        );

        emptyPiles.bind(emptyPilesCount);

        BooleanBinding provinceEmpty = Bindings.createBooleanBinding(
                () -> allCards.containsKey("Province") &&  allCards.get("Province").isEmpty(),
                emptyPiles
        );
        BooleanBinding colonyEmpty = Bindings.createBooleanBinding(
                () -> allCards.containsKey("Colony") && allCards.get("Colony").isEmpty(),
                emptyPiles
        );

        isFinished.bind(provinceEmpty.or(colonyEmpty).or(emptyPiles.greaterThanOrEqualTo(3)));
        updatePlayer(currentTurnPlayer);

    }

    public static void updatePlayer(ObjectProperty<Player> current){
        allCardsInSupply.values().forEach(pile -> {

            Card card = pile.getFirst();
            final int baseCost = card.basiquePrice();
            pile.priceProperty().unbind();

            pile.priceProperty().bind(current.flatMap(player -> {
                if (player == null) return Bindings.createIntegerBinding(() -> baseCost);

                var pRedProperty = player.getProperties(Properties.puddlerReduction);
                var pRedQuarry = player.getProperties(Properties.quarryReduction);

                return Bindings.createIntegerBinding(() -> {
                    int redGlobale = GameStat.reduction.get();

                    int quarryRed = card.hasType(CardType.ACTION)? pRedQuarry.get() : 0;

                    int peddlerRed = card.getName().equals("Peddler") ? pRedProperty.get() : 0;

                    return  baseCost - redGlobale - peddlerRed - quarryRed;
                    },
                        pRedProperty, pRedQuarry, GameStat.reduction);
                }));
            pile.update();
            });
    }




}
