package fr.umontpellier.iut.dominion.cards;

import fr.umontpellier.iut.dominion.*;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.LongBinding;
import javafx.beans.property.*;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.List;

public class GameStat {
    public static final IntegerProperty charlatanPower = new SimpleIntegerProperty(0);
    public static final IntegerProperty reduction = new SimpleIntegerProperty(0);
    public static final LongProperty emptyPiles = new SimpleLongProperty(0);
    public static final BooleanProperty isFinished = new SimpleBooleanProperty(false);

    public static List<SupplyPile> allCardsInSupply;


    public static void initialize(List<SupplyPile> allCards, ObjectProperty<Player> currentTurnPlayer) {
        allCardsInSupply = allCards;

        charlatanPower.addListener((obs, oldVal, newVal) -> {
            boolean isTreasure = newVal.intValue() > 0;
            allCards.stream()
                    .flatMap(Collection::stream)
                    .filter(c -> "Curse".equals(c.getName()))
                    .forEach(c -> {
                        if (isTreasure) c.addType(CardType.TREASURE);
                        else c.removeType(CardType.TREASURE);
                    });
        });

        LongBinding emptyPilesCount = Bindings.createLongBinding(
                () -> allCards.stream()
                                .filter(AbstractCollection::isEmpty)
                                .count(),
                allCards.toArray(new Observable[0])
        );

        emptyPiles.bind(emptyPilesCount);

        BooleanBinding provinceEmpty = Bindings.createBooleanBinding(
                () -> allCards.stream().anyMatch(p -> p.getName().equals("Province") && p.isEmpty()),
                emptyPiles
        );

        isFinished.bind(provinceEmpty.or(emptyPiles.greaterThanOrEqualTo(3)));
        updatePlayer(currentTurnPlayer);

    }

    public static void updatePlayer(ObjectProperty<Player> current){
        allCardsInSupply.forEach(pile -> {
            pile.forEach(card -> {
                final int baseCost = card.basiquePrice();
                card.getCostProperty().unbind();

                card.getCostProperty().bind(current.flatMap(player -> {
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
            });
        });
    }




}
