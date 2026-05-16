package fr.umontpellier.iut.dominion.cards;

import fr.umontpellier.iut.dominion.*;
import fr.umontpellier.iut.dominion.Player.Player;
import fr.umontpellier.iut.dominion.Player.Tokens.Token;
import fr.umontpellier.iut.dominion.Supply.SupplyPile;
import fr.umontpellier.iut.dominion.cards.component.OnPlayComponent;
import fr.umontpellier.iut.dominion.cards.factories.FactorySupplyPile;
import fr.umontpellier.iut.dominion.cards.factories.FactoryUtil;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.LongBinding;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameStat {
    public static final BooleanProperty charlatanPower = new SimpleBooleanProperty(false);
    public static final IntegerProperty reduction = new SimpleIntegerProperty(0);
    public static final LongProperty emptyPiles = new SimpleLongProperty(0);
    public static final BooleanProperty isFinished = new SimpleBooleanProperty(false);

    public static Map<String, SupplyPile> allCardsInSupply;
    public static List<Player> players;

    public static void initialize(Map<String, SupplyPile> allCards, ObjectProperty<Player> currentTurnPlayer, List<Player> player) {
        allCardsInSupply = allCards;
        players = player;
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

    public static void updatePlayer(ObjectProperty<Player> current) {
        allCardsInSupply.values().forEach(pile -> {
            pile.priceProperty().unbind();

            pile.priceProperty().bind(Bindings.createIntegerBinding(() -> {
                        Player player = current.get();
                        Card topCard = pile.isEmpty() ? null : pile.getLast();

                        if (topCard == null) return 0;

                        int baseCost = topCard.basicPrice();
                        int redGlobale = GameStat.reduction.get();

                        if (player == null) return Math.max(0, baseCost - redGlobale);

                        var pRedPeddler = player.getProperties(Properties.puddlerReduction);
                        var pRedQuarry = player.getProperties(Properties.quarryReduction);
                        int pRedToken = player.askReductionToken(topCard.getName())? 2 : 0;
                        int quarryRed = topCard.hasType(CardType.ACTION) ? pRedQuarry.get() : 0;
                        int peddlerRed = topCard.getName().equals("Peddler") ? pRedPeddler.get() : 0;
                        return Math.max(0, baseCost - redGlobale - peddlerRed - quarryRed -  pRedToken);

                    },
                    current,
                    pile,
                    GameStat.reduction
            ));

            pile.update();
        });


        current.addListener((observable, oldValue, newValue) -> {
            if(newValue == null) return;

            List<Card> allEstates = new ArrayList<>(allCardsInSupply.get("Estate"));
            for (Player p : players) {
                allEstates.addAll(p.getAllOwnedCards().stream()
                        .filter(c -> c.hasName("Estate")).toList());
            }

            String inheritedName = newValue.getToken(Token.ESTATE_TOKEN);
            Card template = null;
            if (!inheritedName.isEmpty()) {
                template = newValue.getCopyOf(Destination.ASIDE).stream().filter(c -> c.hasName(inheritedName)).findFirst().orElse(null);
            }

            List<Card> activePlayerCards = newValue.getAllOwnedCards().stream().filter(c -> c.hasName("Estate")).toList();
            final Card finalTemplate = template;
            allEstates.forEach(card -> {
                card.removeType(CardType.ACTION);
                card.removeType(CardType.COMMAND);
                card.removeComponent(OnPlayComponent.class);

                if (finalTemplate != null && activePlayerCards.contains(card)) {
                    card.addType(CardType.ACTION);
                    card.addType(CardType.COMMAND);
                    card.setup(config -> config
                            .onPlay((player, self) ->{
                                Card ephemere = finalTemplate.copy();
                                ephemere.play(player);
                                FactoryUtil.linkedCard(self, ephemere);
                            })
                            .stayInPlayCondition(FactoryUtil.checkLink)
                    );
                }
            });
        });
    }






}
