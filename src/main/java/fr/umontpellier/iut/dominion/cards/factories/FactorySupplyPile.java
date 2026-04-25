package fr.umontpellier.iut.dominion.cards.factories;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import fr.umontpellier.iut.dominion.Annotation.Dominion_Card;
import fr.umontpellier.iut.dominion.Annotation.ExtraSet;
import fr.umontpellier.iut.dominion.Annotation.InSet;
import fr.umontpellier.iut.dominion.Annotation.PileType;
import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.SupplyPile;
import fr.umontpellier.iut.dominion.cards.Card;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

/**
 * Classe de fabrication de listes de cartes
 */
public class FactorySupplyPile {

    private enum order {
        Dominion,
        Intrigue,
        Seaside,
        Alchemy,
        Prosperity,
        Cornucopia_Guilds,
        Hinterlands
    }

    public static Map<String, List<String>> getCardsByExtension() {
        return cardToExpansion.entrySet()
                .stream()
                .filter(entry -> !entry.getValue().equalsIgnoreCase("Base") && !rewards.contains(entry.getKey()))
                .collect(Collectors.groupingBy(
                        Map.Entry::getValue,
                        () -> new TreeMap<>(Comparator.comparingInt(name -> {
                            try {
                                return order.valueOf(name).ordinal();
                            } catch (IllegalArgumentException e) {
                                return 99;
                            }
                        })),
                        Collectors.mapping(
                                Map.Entry::getKey,
                                Collectors.collectingAndThen(Collectors.toList(), list -> {
                                    list.sort(Comparator.naturalOrder());
                                    return list;
                                })
                        )
                ));
    }

    public static List<String> getAllAvailableCards() {
        List<String> list = new ArrayList<>(PILE_CONFIGS.keySet());
        Collections.sort(list);
        return list;
    }

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

        public static PileConfig rewards(Supplier<Card> supplier) {return  new PileConfig(supplier, n -> n <= 2 ? 1 : 2);}

        public static PileConfig event(Supplier<Card> supplier) {return  new PileConfig(supplier, n -> 1);}

        public static PileConfig mixed(Supplier<Card> supplier) {return new PileConfig(supplier, n -> 1);}
    }

    private static final Map<String, String> cardToExpansion = new HashMap<>();
    private static final Map<String, PileConfig> PILE_CONFIGS = new HashMap<>();
    private static final Map<String, Map<String, List<String>>> preSets = new LinkedHashMap<>();
    private static final List<String > rewards = new ArrayList<>();
    private static final Map<String, List<String>> tempSetCards = new LinkedHashMap<>();
    private static final Map<String, Set<String>> tempSetExpansions = new LinkedHashMap<>();
    private static final Map<String, String> tempSetExtra = new LinkedHashMap<>();

    public static void loadAllCards(){
        System.out.println("--- DÉBUT DU SCAN DES CARTES ---");
        Reflections ref = new Reflections("fr.umontpellier.iut.dominion.cards", Scanners.MethodsAnnotated);
        Set<Method> methods = ref.getMethodsAnnotatedWith(Dominion_Card.class);
        System.out.println("Nombre de méthodes détectées : " + methods.size());
        for (Method method : methods) {

            try {
                Dominion_Card card = method.getAnnotation(Dominion_Card.class);
                InSet inSet = method.getAnnotation(InSet.class);
                ExtraSet extraSet = method.getAnnotation(ExtraSet.class);



                Supplier<Card> supplier = () -> {
                    try{return (Card) method.invoke(null);}
                    catch (Exception e){throw new RuntimeException(e);}
                };

                Card sample = supplier.get();
                String name = sample.getName();
                String extensionName = card.extension();

                PileConfig pileConfig = createPileConfig(card.pileType(), supplier);


                if(sample.hasType(CardType.REWARDS)){
                    rewards.add(name);
                }

                PILE_CONFIGS.put(name, pileConfig);
                cardToExpansion.put(name, card.extension());
                if (inSet != null) {
                    for (String setName : inSet.value()) {
                        tempSetCards.computeIfAbsent(setName, k -> new ArrayList<>()).add(name);
                        tempSetExpansions.computeIfAbsent(setName, k -> new TreeSet<>()).add(extensionName);
                    }
                }

                if (extraSet != null) {
                    for (String setName : extraSet.value()) {
                        tempSetExtra.put(setName, name);
                        tempSetExpansions.computeIfAbsent(setName, k -> new TreeSet<>()).add(card.extension());
                    }
                }
            } catch (RuntimeException e) {
                throw new RuntimeException(e);
            }
        }

        tempSetCards.forEach((setName, cards) -> {
            String combinedKey = String.join(" & ", tempSetExpansions.get(setName));

            String extra = tempSetExtra.get(setName);
            if (extra != null) {
                cards.add(extra);
            }

            preSets.computeIfAbsent(combinedKey, k -> new LinkedHashMap<>())
                    .put(setName, cards);
        });
        System.out.println("--- FIN DU SCAN ---");

    }

    public static Map<String, Map<String, List<String>>> getPreSets() {
        return preSets;
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
            case REWARDS -> PileConfig.rewards(s);
            case EVENT -> PileConfig.event(s);
        };
    }

    public static boolean isExpansionRequired(List<String> chosenNames, String expansionName) {
        return chosenNames.stream()
                .map(cardToExpansion::get)
                .filter(Objects::nonNull)
                .anyMatch(exp -> exp.equals(expansionName));
    }

    public static Map<String, List<String>> getFerrymanOptions(List<String> kingdom) {
        return getGroupedOptions(kingdom, cost -> cost == 3 || cost == 4);
    }

    public static Map<String, List<String>> getYoungWitchOptions(List<String> kingdom) {
        return getGroupedOptions(kingdom, cost -> cost == 2 || cost == 3);
    }


    private static Map<String, List<String>> getGroupedOptions(List<String> kingdom, Predicate<Integer> costPredicate) {
        Set<String> base = cardToExpansion.entrySet().stream()
                .filter(e -> e.getValue().equalsIgnoreCase("Base"))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        return PILE_CONFIGS.entrySet().stream()
                .filter(entry -> !kingdom.contains(entry.getKey()) && !base.contains(entry.getKey()) && !rewards.contains(entry.getKey()))
                .filter(entry -> costPredicate.test(entry.getValue().cardSupplier.get().getCost()))
                .collect(Collectors.groupingBy(
                        entry -> cardToExpansion.get(entry.getKey()),
                        () -> new TreeMap<>(Comparator.comparingInt(name -> {
                            try { return order.valueOf(name).ordinal(); }
                            catch (Exception e) { return 99; }
                        })),
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())
                ));
    }


    public static List<String> getRewards() {
        return rewards;
    }




}
