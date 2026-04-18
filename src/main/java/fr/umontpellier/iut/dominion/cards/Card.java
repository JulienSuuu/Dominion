package fr.umontpellier.iut.dominion.cards;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Game;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.component.*;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;

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
    private final Price cost;

    private final Set<CardType> types;

    private List<Card> location;
    private BiPredicate<Event, Player> condition = (event, player) ->  true;
    private Predicate<Player> available = (player) -> true;
    private int price;
    private final Map<Class<? extends CardComponent>, CardComponent> components;
    private final Map<String,Object > properties;

    /**
     * Constructeur simple
     *
     * @param name  le nom de la carte
     * @param cost  le coût de la carte
     * @param types les types de la carte
     */
    public Card(String name, Price cost, CardType ...types) {
        this.name = name;
        this.cost = cost;
        this.types = new HashSet<>();
        this.components = new HashMap<>();
        this.properties = new HashMap<>();
        Collections.addAll(this.types, types);
        price = cost.price().get();
        cost.price().bind(Bindings.createIntegerBinding(
                () -> price -GameStat.reduction.get(),
                GameStat.reduction));
    }


    public int basiquePrice() {
        return price;
    }


    public <T extends CardComponent> Card addComponent(Class <T> type, T component){
        this.components.put(type, component);
        return this;
    }

    public Card addComponent(CardComponent component){
        this.components.put(component.getClass(), component);
        return this;
    }

    public void set(String property, Object value){
        this.properties.put(property, value);
    }

    public <T> T getOrDefault(String property, Class<T> type) {
        return type.cast(this.properties.getOrDefault(property,0));
    }
    public <T> T get(String property, Class<T> type){
        return type.cast(this.properties.get(property));
    }

    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getMap(String key) {
        Object val = properties.get(key);
        if (val instanceof Map) {
            return (Map<K, V>) val;
        }
        return null;
    }

    public boolean getFlag(String key) {
        Object val = properties.get(key);
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        return false;
    }
    @SuppressWarnings("unchecked")
    private <T> Consumer<T> getAction(String key){
        Object val = properties.get(key);
        if (val instanceof Consumer) {
            return (Consumer<T>) val;
        }
        return null;
    }

    public void execute(Player p) {
        Consumer<Player>  action = getAction("action");
        if (action != null) {
            action.accept(p);
        }
    }

    public void clear(){
        this.properties.clear();
    }

    public Card setup(Consumer<CardConfigurator> settings) {
        settings.accept(new CardConfigurator(this));
        return this;
    }

    public void setCondition(BiPredicate<Event, Player> condition) {
        this.condition = condition;
    }
    public void setAvailable(Predicate<Player> available) {this.available = available;}

    public boolean canExecute(Event event, Player player) {
        return condition.test(event, player);
    }
    public Predicate<Player> getAvailable() {return this.available;}

    public IntegerProperty getCostProperty() {
        return cost.price();
    }

    public int getCost() {
        return Math.max(0, cost.price().get());
    }

    public int getPotion() {return cost.potion();}
    public int getDebt() {return cost.debt().get();}

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
        as(OnPlayComponent.class).ifPresent(o -> o.accept(p, this));
        as(DurationComponent.class).ifPresent(d -> d.activeDuration(this));
    };


    public boolean buyCondition(int potion, int debt){
        return potion == cost.potion()  &&  debt == cost.debt().get();
    }

    /**
     * Renvoie la valeur de la carte en points de victoire (c'est cette méthode
     * qui est appelée sur toutes les cartes du deck d'un joueur pour
     * déterminer le score du joueur en fin de partie)
     * <p>
     * Toutes les cartes qui ne sont pas de type Victoire ont une valeur de
     * 0 (la méthode devra donc être redéfinie pour les cartes Victoire)
     */
    public int getVictoryValue(Player player) {return as(ScoreComponent.class).map(s -> s.giveScore(player)).orElse(0);}


    /**
     *
     * @param clazz le composent à chercher dans {@link Card#components}
     * @return un Optional
     * @param <C> le type de la classe à renvoyé
     */
    public <C extends CardComponent> Optional<C> as(Class<C> clazz) {
        return Optional.ofNullable(components.get(clazz)).map(clazz::cast);
    }

    public <T extends CardComponent> boolean hasComponent(Class<T> type) {
        return components.containsKey(type);
    }

    public int  numberType(){
        return  types.size();
    }

    public Price getPrice() {
        return cost;
    }

    public void removeType(CardType type) {
        types.remove(type);
    }
}