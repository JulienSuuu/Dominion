package fr.umontpellier.iut.dominion.cards;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import fr.umontpellier.iut.dominion.Annotation.Dominion_Card;
import fr.umontpellier.iut.dominion.Annotation.PileType;
import fr.umontpellier.iut.dominion.SupplyPile;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

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

    private static final Map<String, String> cardToExpansion = new HashMap<>();
    private static final Map<String, PileConfig> PILE_CONFIGS = new HashMap<>();


    @SafeVarargs
    private static Map<String, PileConfig> merge(Map<String, PileConfig>... maps) {
        Map<String, PileConfig> merged = new HashMap<>();
        for (Map<String, PileConfig> map : maps) {
            merged.putAll(map);
        }
        return Collections.unmodifiableMap(merged);
    }

    public static void loadAllCards(){
        System.out.println("--- DÉBUT DU SCAN DES CARTES ---");
        Reflections ref = new Reflections("fr.umontpellier.iut.dominion.cards", Scanners.MethodsAnnotated);
        Set<Method> methods = ref.getMethodsAnnotatedWith(Dominion_Card.class);
        System.out.println("Nombre de méthodes détectées : " + methods.size());
        for (Method method : methods) {

            try {
                System.out.println("Méthode trouvée : " + method.getName());
                Dominion_Card card = method.getAnnotation(Dominion_Card.class);

                Supplier<Card> supplier = () -> {
                    try{return (Card) method.invoke(null);}
                    catch (Exception e){throw new RuntimeException(e);}
                };

                Card sample = supplier.get();
                String name = sample.getName();

                PileConfig pileConfig = createPileConfig(card.pileType(), supplier);

                PILE_CONFIGS.put(name, pileConfig);
                cardToExpansion.put(name, card.extension());


            } catch (RuntimeException e) {
                throw new RuntimeException(e);
            }
            System.out.println("--- FIN DU SCAN ---");
        }
    }

    public static List<String> getExtensions(String... extensions) {
        if (extensions == null) return new ArrayList<>();

        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .forPackage("fr.umontpellier.iut.dominion.cards")
                        .addScanners(Scanners.MethodsAnnotated)
        );

        Set<Method> methods = reflections.getMethodsAnnotatedWith(Dominion_Card.class);

        Set<String> targetExtensions = Arrays.stream(extensions)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return methods.stream()
                .filter(method -> {
                    Dominion_Card annotation = method.getAnnotation(Dominion_Card.class);
                    return annotation != null && targetExtensions.contains(annotation.extension());
                })
                .map(method -> {
                    try {
                        Card c = (Card) method.invoke(null);
                        return (c != null) ? c.getName() : null;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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

    private static PileConfig createPileConfig(PileType type, Supplier<Card> s) {
        return switch (type) {
            case COPPER -> PileConfig.copper(s);
            case ESTATE -> PileConfig.estate(s);
            case VICTORY -> PileConfig.victory(s);
            case KINGDOM -> PileConfig.kingdom(s);
            case SILVER -> PileConfig.silver(s);
            case GOLD -> PileConfig.gold(s);
            case POTION -> PileConfig.potion(s);
            case PLATINUM -> PileConfig.platinum(s);
            case CURSE -> PileConfig.curse(s);
        };
    }

    public static boolean isExpansionRequired(List<String> chosenNames, String expansionName) {
        return chosenNames.stream()
                .map(cardToExpansion::get)
                .filter(Objects::nonNull)
                .anyMatch(exp -> exp.equals(expansionName));
    }


}
