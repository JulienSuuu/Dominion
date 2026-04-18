package fr.umontpellier.iut.dominion.cards;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import fr.umontpellier.iut.dominion.SupplyPile;
import fr.umontpellier.iut.dominion.cards.Alchimie.AlchimySet;
import fr.umontpellier.iut.dominion.cards.Intrigue.IntrigueSet;
import fr.umontpellier.iut.dominion.cards.dominion.DominionSet;
import fr.umontpellier.iut.dominion.cards.seaside.*;

/**
 * Classe de fabrication de listes de cartes
 */
public class FactorySupplyPile {

    public record PileConfig(Supplier<Card> cardSupplier, Function<Integer, Integer> countFunction) {
        public static PileConfig kingdom(Supplier<Card> supplier) {
            return new PileConfig(supplier, n -> 10);
        }

        public static PileConfig victory(Supplier<Card> supplier) {return new PileConfig(supplier, n -> n <= 2 ? 8 : 12);}

        public static PileConfig copper(Supplier<Card> supplier) {
            return new PileConfig(supplier, n -> 60 + 7 * n);
        }

        public static PileConfig silver(Supplier<Card> supplier) {
            return new PileConfig(supplier, n -> 40);
        }

        public static PileConfig gold(Supplier<Card> supplier) {
            return new PileConfig(supplier, n -> 30);
        }

        public static PileConfig estate(Supplier<Card> supplier) {return new PileConfig(supplier, n -> n <= 2 ? 8 + 3 * n : 12 + 3 * n);}

        public static PileConfig curse(Supplier<Card> supplier) {
            return new PileConfig(supplier, n -> 10 * (n - 1));
        }

        public static PileConfig potion(Supplier<Card> supplier) {return  new PileConfig(supplier, n -> 20);}

        public static PileConfig platinum(Supplier<Card> supplier) {return  new PileConfig(supplier, n -> 12);}
    }

    private static final Map<String, PileConfig> PILE_CONFIGS = merge(
            CommonSet.get(),
            DominionSet.get(),
            SeasideSet.get(),
            IntrigueSet.get(),
            AlchimySet.get()
    );

    @SafeVarargs
    private static Map<String, PileConfig> merge(Map<String, PileConfig>... maps) {
        Map<String, PileConfig> merged = new HashMap<>();
        for (Map<String, PileConfig> map : maps) {
            merged.putAll(map);
        }
        return Collections.unmodifiableMap(merged);
    }

    /**
     * Renvoie une pile de cartes pour la réserve
     *
     * @param cardName        le nom de la carte
     * @param numberOfPlayers le nombre de joueurs
     * @return une pile de cartes de même type
     */
    public static SupplyPile createSupplyPile(String cardName, int numberOfPlayers) {
        PileConfig config = PILE_CONFIGS.get(cardName);
        return new SupplyPile(config.cardSupplier(), config.countFunction().apply(numberOfPlayers));
    }


}
