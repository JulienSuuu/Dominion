package fr.umontpellier.iut.dominion.cards;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player.Player;
import fr.umontpellier.iut.dominion.cards.Events.Event;
import fr.umontpellier.iut.dominion.cards.component.*;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

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
    private Destination loc;

    private List<Card> location;
    private final Map<Class<? extends TriggerComponent>, BiPredicate<Event, Player>> conditions = new HashMap<>();
    private Predicate<Player> available = (player) -> true;
    private final IntegerProperty price = new SimpleIntegerProperty(0);
    private final Map<Class<? extends CardComponent>, CardComponent> components;
    private final Map<String,Object > properties = new HashMap<>();

    public Map<String,Object> getProperties() {
        return properties;
    }

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
        Collections.addAll(this.types, types);
        price.set(cost.price().get());
        cost.price().bind(Bindings.createIntegerBinding(
                () -> price.get() - GameStat.reduction.get(),
                GameStat.reduction, price));
    }


    public Card copy() {
        Card copy = new Card(name, cost, types.toArray(new CardType[0]));

        copy.components.putAll(components);

        copy.set("unable", true);

        copy.location = new ArrayList<>();
        copy.loc = null;

        copy.available = available;
        copy.conditions.putAll(conditions);

        return copy;
    }


    public int basicPrice() {
        return price.get();
    }


    public <T extends CardComponent> Card addComponent(Class <T> type, T component){
        this.components.put(type, component);
        return this;
    }

    public Card addComponent(CardComponent component){
        this.components.put(component.getClass(), component);
        return this;
    }

    public <T> T set(String property, T value) {
        this.properties.put(property, value);
        return value;
    }

    public <T> Optional<T> get(String property, Class<T> type){
        return Optional.ofNullable(type.cast(this.properties.get(property)));
    }

    public Number getValue(String property){
        Object value = this.properties.get(property);
        if(value instanceof Number){
            return (Number)value ;
        }
        return 0;
    }

    public String getString(String property){
        Object value = this.properties.get(property);
        if(value instanceof String){
            return (String)value;
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getMap(String key) {
        Object val = properties.get(key);
        if (val instanceof Map) {
            return (Map<K, V>) val;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> Collection<T> getCollection(String key) {
        return (Collection<T>) properties.computeIfAbsent(key, k -> new ArrayList<T>());
    }

    public boolean getFlag(String key) {
        Object val = properties.get(key);
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        return false;
    }

    public void clear(){
        this.properties.clear();
    }

    public Card setup(Consumer<CardConfigurator> settings) {
        settings.accept(new CardConfigurator(this));
        return this;
    }


    public <T extends TriggerComponent> void addCondition(BiPredicate<Event, Player> condition, Class<T> clazz){conditions.put(clazz, condition);}

    public Card setAvailable(Predicate<Player> available) {
        this.available = available;
        return this;
    }

    public<T extends CardComponent> boolean canExecute(Event event, Player player, Class<T> clazz) {
        return conditions.getOrDefault(clazz, (event1, player1) -> true).test(event, player);}

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

    public Card addType(CardType type) {
        this.types.add(type);
        return this;
    }

    /**
     * @return {@code true} si la carte est de type {@code type}, {@code false}
     *         sinon
     */
    public boolean hasType(CardType type) {
        return this.types.contains(type);
    }


    public void moveTo(List<Card> newLocation, Destination loc) {
        if (location != null) {
            location.remove(this);
        }
        location = newLocation;
        this.loc = loc;
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
        getComponent(OnPlayComponent.class).ifPresent(o -> o.accept(p, this));
        getComponent(DurationComponent.class).ifPresent(d -> d.activeDuration(this));
    }


    public boolean buyCondition(int potion, int debt){
        return potion <= cost.potion()  &&  debt <= cost.debt().get();
    }

    /**
     * Renvoie la valeur de la carte en points de victoire (c'est cette méthode
     * qui est appelée sur toutes les cartes du deck d'un joueur pour
     * déterminer le score du joueur en fin de partie)
     * <p>
     * Toutes les cartes qui ne sont pas de type Victoire ont une valeur de
     * 0 (la méthode devra donc être redéfinie pour les cartes Victoire)
     */
    public int getVictoryValue(Player player) {return getComponent(ScoreComponent.class).map(s -> s.giveScore(player)).orElse(0);}


    /**
     *
     * @param clazz le composent à chercher dans {@link Card#components}
     * @return un Optional
     * @param <C> le type de la classe à renvoyé
     */
    public <C extends CardComponent> Optional<C> getComponent(Class<C> clazz) {
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
    public Card setPrice(int cost) {
        price.set(cost);
        return this;
    }

    public void removeType(CardType type) {
        types.remove(type);
    }


    public static Card treasure(String name, Price cost){
        return new Card(name, cost, CardType.TREASURE );
    }
    public static Card action(String name, Price cost){
        return new Card(name, cost, CardType.ACTION);
    }
    public static Card Victory(String name, Price cost){
        return new Card(name, cost, CardType.VICTORY);
    }
    public static Card duration(String name, Price cost){
        return new Card(name, cost, CardType.DURATION);
    }

    public boolean isAtMost(int cost, int potion, int debt){
        return getCost() <= cost && buyCondition(potion, debt);
    }
    public boolean isAtMost(int cost){
        return isAtMost(cost, 0, 0);
    }

    public boolean isEqual(int cost, int potion, int debt){
        return getCost() == cost
                && getPotion() == potion
                && getDebt() == debt;
    }

    public boolean isEqual(int cost){
        return isEqual(cost, 0, 0);
    }

    public boolean isEqualWithBonus(Card trashed, int bonusMoney){
        return getCost() == (trashed.getCost() + bonusMoney)
                && getPotion() == trashed.getPotion()
                && getDebt() == trashed.getDebt();
    }

    public boolean isAtMostWithBonus( Card trashed, int bonusMoney) {
        return getCost() <= (trashed.getCost() + bonusMoney)
                && getPotion() <= trashed.getPotion()
                && getDebt() <= trashed.getDebt();
    }

    public boolean isBetween(int lower, int upper) {
        return getCost() >= lower && getCost() <= upper && buyCondition(0,0);
    }

    public CardType getSpecialType(){
        Set<CardType> available = Arrays.stream(CardType.values()).filter(CardType::isSpecial).collect(Collectors.toSet());
        return types.stream().filter(available::contains).findFirst().orElse(null);
    }

    public static Card event(String name, Price price){
        return new Card(name, price, CardType.EVENT);
    }

    public Destination getLocation() {
        return loc;
    }
    public<T extends CardComponent> void removeComponent(Class<T> clazz) {
        components.remove(clazz);
    }
    public boolean hasForLocation(Destination dest) {
        return loc == dest;
    }
    public Set<CardType> getTypes() {return types;}
}