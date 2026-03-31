package fr.umontpellier.iut.dominion;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import fr.umontpellier.iut.dominion.cards.Card;

@SuppressWarnings("unchecked")
public class TestUtils {
    /**
     * Renvoie un attribut d'un objet à partir de son nom.
     * La méthode cherche s'il existe un champ déclaré dans la classe de l'objet et
     * si ce n'est pas le cas remonte dans la hiérarchie des classes jusqu'à trouver
     * un champ avec le nom voulu ou renvoie null.
     *
     * @param object objet dont on cherche le champ
     * @param name   nom du champ
     * @return le champ de l'objet, avec un type statique Object
     */
    public static <T> T getAttribute(Object object, String name) {
        Class<?> c = object.getClass();
        while (c != null) {
            try {
                Field field = c.getDeclaredField(name);
                field.setAccessible(true);
                return (T) field.get(object);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    /**
     * Setter générique pour forcer la valeur d'un attribut quelconque.
     * La méthode cherche s'il existe un champ déclaré dans la classe de l'objet et
     * si ce n'est pas le cas remonte dans la hiérarchie des classes jusqu'à trouver
     * un champ avec le nom voulu. Lorsque le champ est trouvé, on lui donne la
     * valeur
     * passée en argument.
     *
     * @param name  nom du champ
     * @param value valeur à donner au champ
     */
    public static void setAttribute(Class<?> c, String name, Object value) {
        while (c != null) {
            try {
                Field field = c.getDeclaredField(name);
                field.setAccessible(true);
                field.set(c, value);
                return;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return;
            }
        }
        throw new RuntimeException("No such field: " + name);
    }

    /**
     * Setter générique pour forcer la valeur d'un attribut quelconque.
     * La méthode cherche s'il existe un champ déclaré dans la classe de l'objet et
     * si ce n'est pas le cas remonte dans la hiérarchie des classes jusqu'à trouver
     * un champ avec le nom voulu. Lorsque le champ est trouvé, on lui donne la
     * valeur
     * passée en argument.
     *
     * @param object objet dont on cherche le champ
     * @param name   nom du champ
     * @param value  valeur à donner au champ
     */
    public static void setAttribute(Object object, String name, Object value) {
        Class<?> c = object.getClass();
        while (c != null) {
            try {
                Field field = c.getDeclaredField(name);
                field.setAccessible(true);
                field.set(object, value);
                return;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return;
            }
        }
        throw new RuntimeException("No such field: " + name);
    }

    public static <T> boolean containsReference(Collection<T> collection, T element) {
        return collection.stream().anyMatch(e -> e == element);
    }

    public static <T> boolean containsReferences(Collection<T> collection, T... elements) {
        if (elements.length != collection.size())
            return false;
        for (T e : elements) {
            if (!containsReference(collection, e))
                return false;
        }
        return true;
    }

    public static <T> boolean containsReferences(Collection<T> collection, Collection<T> elements) {
        if (elements.size() != collection.size())
            return false;
        for (T e : elements) {
            if (!containsReference(collection, e))
                return false;
        }
        return true;
    }

    public static <T> boolean containsReferencesInOrder(List<T> list, T... elements) {
        if (elements.length != list.size()) {
            return false;
        }
        for (int i = 0; i < elements.length; i++) {
            if (elements[i] != list.get(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Convertit une List<Card> en liste de chaînes de caractères (les noms des
     * cartes)
     */
    private static ArrayList<String> cardsToString(List<Card> l) {
        return new ArrayList<>(l.stream().map(Card::getName).toList());
    }

    /**
     * Teste si une List<Card> contient exactement les cartes indiquées dans la
     * chaîne `namesString`
     * (noms de cartes séparées par des virgules). Les deux listes doivent avoir les
     * mêmes éléments,
     * avec les mêmes multiplicités mais l'ordre n'a pas d'importance.
     */
    public static boolean hasCards(List<Card> cards, String... names) {
        if (cards.size() != names.length) {
            return false;
        }
        Arrays.sort(names);
        ArrayList<String> cardNames = cardsToString(cards);
        Collections.sort(cardNames);

        for (int i = 0; i < names.length; i++)
            if (!names[i].equals(cardNames.get(i)))
                return false;
        return true;
    }

    /**
     * Teste si une List<Card> contient au moins le nom indiqué dans chaîne
     * `namesString` (un nom de carte).
     */

    public static boolean hasThisCard(List<Card> cards, String name) {
        List<String> l = cardsToString(cards);
        return l.contains(name);
    }

    /**
     * Renvoie une List<Card> contenant `nb_copies` exemplaires de la carte passée en
     * argument
     *
     * @param c:         classe de carte à instancier
     * @param nb_copies: nombre d'exemplaires à mettre dans la pile
     * @return une liste de cartes
     */
    public static List<Card> makePile(Class<?> c, int nb_copies) {
        List<Card> pile = new ArrayList<>();
        for (int i = 0; i < nb_copies; i++)
            try {
                pile.add((Card) c.getConstructor().newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        return pile;
    }
}
