package fr.umontpellier.iut.dominion.cards;

import java.util.*;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.component.*;

/**
 * Représentation des cartes du jeu Dominion
 */
public class Card {
    /**
     * Le nom de la carte
     */
    private final String name;

    /**
     * Le coût de la carte à l'achat
     */
    private final int cost;

    private final Set<CardType> types;

    private List<Card> location;

    private final Map<Class<? extends CardComponent>, CardComponent> components;

    /**
     * Constructeur simple
     *
     * @param name  le nom de la carte
     * @param cost  le coût de la carte
     * @param types les types de la carte
     */
    public Card(String name, int cost, CardType ...types) {
        this.name = name;
        this.cost = cost;
        this.types = new HashSet<>();
        this.components = new HashMap<>();
        Collections.addAll(this.types, types);
    }

    public <T extends CardComponent> Card addComponent(Class <T> type, T component){
        this.components.put(type, component);
        return this;
    }

    public Card addComponent(CardComponent component){
        this.components.put(component.getClass(), component);
        return this;
    }

    /**
     * Getters et setters
     */
    public int getCost() {
        return cost;
    }

    public String getName() {
        return name;
    }

    public boolean hasName(String name) {
        return this.name.equals(name);
    }

    public boolean hasSameNameAs(Card c) {
        return this.name.equals(c.getName());
    }

    public void addType(CardType type) {
        this.types.add(type);
    }

    /**
     * @return {@code true} si la carte est de type {@code type}, {@code false}
     *         sinon
     */
    public boolean hasType(CardType type) {
        return this.types.contains(type);
    }

    /**
     * Déplace la carte d'un emplacement (location) vers un autre
     * <p>
     * La méthode retire la carte de son emplacement actuel (s'il existe), l'ajoute
     * à {@code newLocation} et met à jour l'attribut {@code location} de la carte
     * 
     * @param newLocation le nouvel emplacement de la carte
     */
    public void moveTo(List<Card> newLocation) {
        if (location != null) {
            location.remove(this);
        }
        location = newLocation;
        newLocation.add(this);
    }

    /**
     * Renvoie une représentation de la carte sous forme de chaîne de caractères
     * (ici la fonction renvoie le nom de la carte)
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Renvoie une représentation de la carte sous forme de chaîne de caractères,
     * pour être affichée dans le log du jeu dans l'interface graphique
     */
    public String toLog() {
        return "<span class=\"card-name\">" + name + "</span>";
    }

    /**
     * Exécute l'effet de la carte, jouée par le joueur {@code p}
     * <p>
     * L'action de cette méthode dépend de la classe de la carte. Vous devrez la
     * redéfinir dans les classes des cartes en fonction de vos besoins...
     * 
     * @param p joueur qui exécute l'effet de la carte
     */
    public void play(Player p) {
        as(OnPlayComponent.class).ifPresent(o -> o.accept(p));
        as(DurationComponent.class).ifPresent(DurationComponent::activeDuration);
    };

    /**
     * Renvoie la valeur de la carte en points de victoire (c'est cette méthode
     * qui est appelée sur toutes les cartes du deck d'un joueur pour
     * déterminer le score du joueur en fin de partie)
     * <p>
     * Toutes les cartes qui ne sont pas de type Victoire ont une valeur de
     * 0 (la méthode devra donc être redéfinie pour les cartes Victoire)
     */
    public int getVictoryValue() {return as(ScoreComponent.class).map(ScoreComponent::getScore).orElse(0);}

    /**
     *
     * @param componentClass le composent à chercher dans {@link Card#components}
     * @return un Optional<
     * @param <C>
     */
    public <C extends CardComponent> Optional<C> as(Class<C> componentClass) {
        return components.values().stream()
                .filter(componentClass::isInstance)
                .map(componentClass::cast)
                .findFirst();
    }

    public <T extends CardComponent> boolean hasComponent(Class<T> type) {
        return components.containsKey(type);
    }
}