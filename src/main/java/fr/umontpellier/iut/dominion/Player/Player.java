package fr.umontpellier.iut.dominion.Player;


import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import fr.umontpellier.iut.dominion.*;
import fr.umontpellier.iut.dominion.Annotation.AfterAllyTrigger;
import fr.umontpellier.iut.dominion.Annotation.BeforeAllyTrigger;
import fr.umontpellier.iut.dominion.Annotation.Selection_Mode;
import fr.umontpellier.iut.dominion.Interface.Logger;
import fr.umontpellier.iut.dominion.Player.Tokens.JourneyFace;
import fr.umontpellier.iut.dominion.Player.Tokens.Token;
import fr.umontpellier.iut.dominion.Player.Tokens.TokenEffect;
import fr.umontpellier.iut.dominion.cards.*;
import fr.umontpellier.iut.dominion.cards.Events.Discard_Type;
import fr.umontpellier.iut.dominion.cards.Events.Event;
import fr.umontpellier.iut.dominion.cards.component.*;
import fr.umontpellier.iut.dominion.cards.factories.FactorySupplyPile;
import fr.umontpellier.iut.dominion.cards.factories.FactoryUtil;
import fr.umontpellier.iut.dominion.cards.factories.seaside.SeaSideFactory;
import fr.umontpellier.iut.dominion.gui.Utils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Un joueur de Dominion
 */
@Component
@Scope("prototype")
public class Player implements Logger {
    private static final List<String> color = List.of(
            "#e74c3c", // Rouge Alizarin
            "#3498db", // Bleu Peter River
            "#2ecc71", // Vert Emeraude
            "#f1c40f", // Jaune Tournesol
            "#9b59b6", // Améthyste
            "#e67e22", // Carotte
            "#1abc9c"  // Turquoise
    );
    //region --VARIABLES--
    /**
     * Nom du joueur
     */
    private String name;
    private int Id;
    private static int idCounter = 0;
    private final EnumMap<Destination, ObservableList<Card>> cardSet = new EnumMap<>(Destination.class);
    private final EnumMap<Item, IntegerProperty> items = new EnumMap<>(Item.class);
    private final Map<String, BooleanProperty> flags = new HashMap<>();
    private final Map<String, IntegerProperty> properties = new HashMap<>();
    private final List<TriggerComponent.discardHook> discardHooks = new ArrayList<>();
    private final Map<Token, String> myTokens = new EnumMap<>(Token.class);
    private final Map<Class<? extends PlayerComponent>, PlayerComponent>  components = new HashMap<>();
    private final Map<String, BooleanProperty> permanentFlags = new HashMap<>();
    private Player self;

    private final Set<Item> persistent = EnumSet.of(
            Item.DEBT,
            Item.COFFER,
            Item.COIN_TOKEN_SHIP
    );

    /**
     * Bonus ou malus sur le nombre de cartes à piocher
     * {@link SeaSideFactory#Outpost()}
     */
    private int drawBonusNextTurn;

    /**
     * La partie en cours
     */
    private Game game;
    private boolean isSecondTurn = false;
    /**
     * Liste des cartes gagnées au tour précédent
     */
    private List<Card> CardGainedLastTurn;

    /**
     * Liste des cartes gagnées au tour en cours
     */
    private List<Card> CardGainedCurrentTurn = new ArrayList<>();
    private final List<Card> mustBeDiscarded = new ArrayList<>();
    private final List<Card> activeEffect = new ArrayList<>();

    private Player controller = this;
    private boolean mustConductPossessedTurn = false;
    private Player nextPossessor = null;


    public void preparePossession(Player possessor) {
        this.mustConductPossessedTurn = true;
        this.nextPossessor = possessor;
    }

    //endregion

    //region --CONSTRUCTEUR--
    /**
     * Constructeur
     * Initialise les différentes piles de cartes du joueur, place 3 cartes
     * Estate et 7 cartes Copper dans la défausse du joueur puis fait piocher 5
     * cartes en main au joueur.
     *
     */
    public Player() {}

    public void init(String name, Game game, boolean request) {
        this.name = name;
        this.game = game;
        Id = idCounter++;

        CardGainedLastTurn = new ArrayList<>();
        CardGainedCurrentTurn = new ArrayList<>();

        EnumSet<Destination> targets = EnumSet.allOf(Destination.class);
        targets.removeAll(EnumSet.of(Destination.SUPPLY, Destination.TRASH));
        targets.forEach(d -> cardSet.put(d, FXCollections.observableArrayList()));

        for (Item i : Item.values()) {
            items.put(i, new SimpleIntegerProperty(0));
        }

        if(request){
            FactorySupplyPile.createCard("Hovel").moveTo(get(Destination.DISCARD), Destination.DISCARD);
            FactorySupplyPile.createCard("Necropolis").moveTo(get(Destination.DISCARD), Destination.DISCARD);
            FactorySupplyPile.createCard("Overgrown Estate").moveTo(get(Destination.DISCARD), Destination.DISCARD);
        }
        else for (int i = 0; i < 3; i++) getCardFromSupply("Estate").moveTo(get(Destination.DISCARD), Destination.DISCARD);

        for (int i = 0; i < 7; i++) getCardFromSupply("Copper").moveTo(get(Destination.DISCARD), Destination.DISCARD);

        while (!get(Destination.DISCARD).isEmpty()) {
            Collections.shuffle(get(Destination.DISCARD));
            get(Destination.DISCARD).getLast().moveTo(get(Destination.DRAW), Destination.DRAW);
        }
        for (int i = 0; i < 5; i++) {
            get(Destination.DRAW).getLast().moveTo(get(Destination.HAND), Destination.HAND);
        }

        TokenEffect.getTokens().forEach(token -> {
            myTokens.computeIfAbsent(token, t -> "");
        });

        myTokens.computeIfAbsent(Token.ESTATE_TOKEN, t -> "");
        myTokens.computeIfAbsent(Token.CARD_REDUCTION_TOKEN, t -> "");

        addComponent(new GainSkills(self));
        addComponent(new DiscardSkills(self));
        addComponent(new PlaySkills(self));
        addComponent(new BuySkills(self));
        addComponent(new TrashSkills(self));

        listener();
    }

    //endregion

    //region --GETTERS AND SETTERS --
    /**
     * Getters et setters
     */
    public String getName() {
        return name;
    }

    public <T extends PlayerComponent> void addComponent(T component) {
        components.put(component.getClass(), component);
    }

    public <T extends PlayerComponent> Optional<T> getComponent(Class<T> clazz) {
        return Optional.ofNullable(clazz.cast(components.get(clazz)));
    }

    protected Player getController() {
        return controller;
    }

    protected List<Card> mustBeDiscarded() {
        return mustBeDiscarded;
    }

    protected int getDrawBonus(){
        return drawBonusNextTurn;
    }

    protected void setDrawBonus(int n){
        drawBonusNextTurn = n;
    }

    protected void clearList(){
        CardGainedLastTurn = new ArrayList<>(CardGainedCurrentTurn);
        CardGainedCurrentTurn.clear();
        activeEffect.clear();
        discardHooks.clear();
    }

    private void checkTokens(Event event, Predicate<Token> filter) {
        String cardName = event.getCard().getName();
        myTokens.forEach((token, string) -> {
            boolean isCorrectTarget = string.equals(cardName) || getGame().verifyPileToken(string, cardName);
            if (isCorrectTarget && filter.test(token)) {
                TokenEffect.execute(token, event, self);
            }
        });
    }

    protected void checkPlayToken(Event event) {
        checkTokens(event, Token::isPlayToken);
    }

    protected void checkGainToken(Event event) {
        checkTokens(event, t -> !t.isPlayToken());
    }

    public boolean askReductionToken(String name){
        return myTokens.getOrDefault(Token.CARD_REDUCTION_TOKEN, " ").equals(name);
    }

    /**
     * @return false si c'est pile, true si c'est face
     */
    public JourneyFace flipJourneyToken(){
        BooleanProperty j = getPersistentFlag(Token.JOURNEY_TOKEN.name());
        boolean reverse = !j.get();
        j.set(reverse);
        return j.get()? JourneyFace.FACE_DOWN : JourneyFace.FACE_UP;
    }

    public BooleanProperty getPersistentFlag(String name){
        return permanentFlags.computeIfAbsent(name, k -> new SimpleBooleanProperty(false));
    }
    public String getToken(Token token){
        return myTokens.computeIfAbsent(token, k -> "");
    }
    public void setToken(Token token, String pileName){myTokens.put(token, pileName);}
    public Set<String> getPileName(){
        return new HashSet<>(myTokens.values());
    }

    public void resetFlags() {
        flags.forEach((name, prop) -> {
            if (!prop.isBound()) {
                prop.set(false);
            }
        });
    }

    public void resetProperties() {
        properties.values().forEach(prop ->{
            if(!prop.isBound()){
                prop.set(0);
            }
        });
    }

    public void resetItem() {
        GameStat.reduction.set(0);
        for (Item item : Item.values()) {
            if (!persistent.contains(item)) {
                IntegerProperty prop = items.get(item);
                prop.set(0);
                items.put(item,prop);
            }
        }
    }

    public int getIndex() {
        return game.getPlayerIndex(this);
    }

    public int getMoney() {
        return items.get(Item.MONEY).get();
    }

    public int getNumberOfActions() {return items.get(Item.ACTION).get();}

    public BooleanProperty getFlag(String key) {
        return flags.computeIfAbsent(key, k -> new SimpleBooleanProperty(false));
    }

    public IntegerProperty getProperties(String key) {
        return properties.computeIfAbsent(key, k -> new SimpleIntegerProperty(0));
    }

    public boolean isFlagSet(String key) {
        return getFlag(key).get();
    }

    public boolean isUsed(Card used){
        return getFlag(used.getName()).get();
    }

    @Autowired
    public void setSelf(@Lazy Player self) {
        this.self = self;
    }

    public int getNumberOfBuys() {
        return  items.get(Item.BUY).get();
    }

    public Game getGame() {
        return game;
    }

    public int getDebt() {return items.get(Item.DEBT).get();}

    public int getPotion() {
        return items.get(Item.POTION).get();
    }

    protected List<Card> get(Destination destination) {
        return cardSet.get(destination);
    }

    public int getValueOf (Item item) {return items.get(item).get();}
    public IntegerProperty getPropertyOf(Item item) {return items.get(item);}

    /**
     * @return une copie de {@link Player#CardGainedLastTurn}
     */
    public List<Card> getCardGainedLastTurn() {
        return new ArrayList<>(CardGainedLastTurn);
    }

    /**
     * @return une copie de {@link Player#CardGainedCurrentTurn}
     */
    public List<Card> getCardGainedCurrentTurn() {
        return new  ArrayList<>(CardGainedCurrentTurn);
    }

    /**
     * Renvoie une liste de toutes les cartes possédées par le joueur
     */
    public List<Card> getAllOwnedCards() {
        List<Card> allCards = new ArrayList<>();
        for (Destination d : cardSet.keySet()) {
            allCards.addAll(cardSet.get(d));
        }
        return allCards;
    }

    public List<Card> getCopyOf(Destination destination) {return new ArrayList<>(cardSet.get(destination));}
    public ObservableList<Card> getObservableZone(Destination destination) {
        return cardSet.get(destination);
    }

    /**
     * Renvoie le nombre total de points de victoire du joueur
     * <p>
     * Ce total est calculé en ajoutant les valeurs individuelles de toutes les
     * cartes possédées par le joueur (en utilisant la méthode
     * {@code getVictoryValue()}) des cartes
     */
    public int getVictoryPoints() {
        return getCopyOf(Destination.HAND).stream().mapToInt(card -> card.getVictoryValue(self)).sum() + items.get(Item.VICTORY_TOKEN).get();
    }


    public void increment(Item it, int value){
        if(it == Item.COFFER && getGame().getCoffers() == 0) return;
        BooleanProperty tax = getPersistentFlag(Token.TAX_TOKEN.name());
        if(tax.get() && it == Item.MONEY ){
            value-=1;
            tax.set(false);
        }
        IntegerProperty prop = items.get(it);
        prop.set(prop.get() + value);
        items.put(it,prop);}

    public void decrement(Item it, int value){
        IntegerProperty prop = items.get(it);
        prop.set(prop.get() - value);
        items.put(it, prop)
        ;}


    public void setController(Player controller) {
        this.controller = controller;
    }


    public void updateDrawBonusValue(int value){
        if(drawBonusNextTurn <= -4)return;
        drawBonusNextTurn += value;
    }

    public List<TriggerComponent.discardHook> getDiscardHooks() {
        return discardHooks;
    }

    public void addDiscardHook(TriggerComponent.discardHook discardHook) {
        discardHooks.add(discardHook);
    }


    public void listener(){
        getFlag(Flags.COPPER_PLAYED).bind(
                Bindings.createBooleanBinding(
                        () -> cardSet.get(Destination.INPLAY).stream().anyMatch(c -> c.hasName("Copper")),
                        cardSet.get(Destination.INPLAY)
                )
        );

        getProperties(fr.umontpellier.iut.dominion.Properties.puddlerReduction).bind(
                Bindings.createIntegerBinding(
                        () -> {
                            Number count = cardSet.get(Destination.INPLAY).stream().filter(c -> c.hasType(CardType.ACTION)).count();
                            return count.intValue()*2;
                        },
                        cardSet.get(Destination.INPLAY)
                )
        );

        if(game.hasCard("Crossroads"))
            getFlag(Flags.playedCrossroads).bind(
                Bindings.createBooleanBinding(
                        () -> cardSet.get(Destination.INPLAY).stream().anyMatch(c -> c.hasName("Crossroads")),
                        cardSet.get(Destination.INPLAY)
                )
            );

        if(game.hasCard("Fool's Gold"))
            getFlag(Flags.playedFoolsGold).bind(
                    Bindings.createBooleanBinding(
                            () -> cardSet.get(Destination.INPLAY).stream().filter(c -> c.hasName("Fool's Gold")).count() >= 2,
                            cardSet.get(Destination.INPLAY)
                    )
            );

        getFlag(Flags.onBuyPhase).bind(
                Bindings.createBooleanBinding(
                        () -> {
                            boolean action = getFlag("Action").get();
                            boolean treasure = getFlag("Treasure").get();
                            return !action && !treasure;
                        },
                        getFlag("Action"),
                        getFlag("Treasure")
                )
        );



    }


    public void setUpTurn(){
        increment(Item.ACTION, 1);
        increment(Item.BUY, 1);
    }

    public int getCoins(){
        return items.get(Item.COIN_TOKEN_SHIP).get();
    }
    //endregion

    //region --METHODES MoveTo--
    /**
     * Déplace une carte dans la main du joueur.
     *
     * @param c la carte à déplacer
     */
    public void moveToHand(Card c) {
        c.moveTo(get(Destination.HAND), Destination.HAND);
    }

    /**
     * Méthode Générique
     * <li>Déplace une carte dans la destination choisit </li>
     * @param c {@link Card} carte à déplacer
     * @param dest {@link Destination} destination de la carte
     * @see Player#gainSilent(Card, Destination, boolean)
     */

    public void moveTo(Card c, Destination dest){
        if(c == null || c.getFlag("unable")) return;
        c.moveTo(get(dest), dest);
    }

    public void moveAll(Destination from, Destination next){
        getCopyOf(from).forEach(card -> moveTo(card, next));
    }
    public void moveAll(List<Card> toMove, Destination next) {toMove.forEach(card -> moveTo(card, next));}



    public void moveAllAndChooseTheOrder(Destination from, Destination next){
        while(!getCopyOf(from).isEmpty()){
            self.chooseCardFromList("Move all your " + from.name().toLowerCase() + " to " + next.name().toLowerCase(), card -> true, getCopyOf(from), false)
                    .ifPresent(card ->  moveTo(card, next));
        }
    }

    public void moveAllAndChooseTheOrder(List<Card> cardlist, Destination from, Destination next){
        while(!cardlist.isEmpty()){
            self.chooseCardFromList("Move those left Card from " + from.name().toLowerCase() + " to " + next.name().toLowerCase(), card -> true, cardlist, false)
                    .ifPresent(card ->{
                        moveTo(card, next);
                        cardlist.remove(card);
                    });
        }
    }
    //endregion

    //region --METHODES DRAW & DISCARD--
    /**
     * Renvoie la carte qui se trouve au sommet de la pioche du joueur.
     * <p>
     * Si la pioche du joueur est vide, on commence par mélanger la défausse
     * et transférer toutes les cartes de la défausse dans la pioche.
     * On renvoie ensuite la première carte de la pioche si elle n'est
     * pas vide (sinon la méthode renvoie {@code null}).
     * <p>
     * Remarque : la carte n'est pas retirée de la pioche.
     *
     * @return la carte piochée, ou {@code null} si aucune carte disponible
     */
    public Card getCardFromDeck() {
        if(get(Destination.DRAW).isEmpty()){
            shuffle();
            if(get(Destination.DRAW).isEmpty())return null;
        }
        return get(Destination.DRAW).getLast();
    }

    /**
     * Mélange la défausse puis remet les cartes dans la pioche
     */
    public void shuffle(){
        Collections.shuffle(get(Destination.DISCARD));
        getCopyOf(Destination.DISCARD).forEach(c -> c.moveTo(get(Destination.DRAW), Destination.DRAW));
    }

    /**
     * @param cardName nom de la carte à obtenir dans la réserve
     * @return la carte du sommet de la pile de réserve correspondant au nom
     *         passé en argument, ou {@code null} si la pile de réserve est vide ou
     *         si le nom ne correspond à aucune pile de la réserve.
     */
    public Card getCardFromSupply(String cardName) {
        return game.getCardFromSupply(cardName);
    }

    /**
     * Pioche une carte et la place directement dans la main du joueur.
     * <p>
     * Cette méthode fait appel à la méthode {@code getCardFromDeck()} pour piocher
     * une carte et la place dans la main du joueur.
     */
    public Card drawToHand() {
        BooleanProperty b = getPersistentFlag(Token.MINUS_ONE_CARD_TOKEN.name());
        if(b.get()){
            b.set(false);
            return null;
        }
        Card c = getCardFromDeck();
        if(c == null)return null;
        moveToHand(c);
        return c;
    }


    /**
     * Pioche une carte et la place directement dans la destiantion choisit
     *
     * <p>Utilise {@link Player#getCardFromDeck()} et {@link Player#moveTo(Card, Destination)}</p>
     * @param dest destination de la carte {@link Destination}
     *
     *
     */
    public void drawTo(Destination dest) {
        BooleanProperty b = getPersistentFlag(Token.MINUS_ONE_CARD_TOKEN.name());
        if(b.get()){
            b.set(false);
            return;
        }

        Card c = getCardFromDeck();
        if(c != null) {
            moveTo(c, dest);
        }
    }

    @AfterAllyTrigger
    public boolean discard(Card c, Discard_Type... discard) {
        return getComponent(DiscardSkills.class).map(d -> d.discard(c, discard)).orElse(false);
    }


    public void discardAll(Destination dest){
        getComponent(DiscardSkills.class).ifPresent(c -> c.discardAll(dest));
    }

    /**
     * Défausse un nombre de cartes définit par les cartes
     * <p> Vérifie que {@code hand} ne soit pas vide </p>
     * <p>
     *     Utilise {@link Player#chooseCardFromHand(String, boolean)}
     *     <p>La carte renvoyé peut etre {@code null}</p>
     *     Puis place la carte dans la discard {@link Player#moveTo(Card, Destination)}
     *
     * </p>
     *
     *
     * @param number nombre de carte de la main à défausser
     */
    public void discardFromHand(int number) {
        getComponent(DiscardSkills.class).ifPresent(s -> s.discardFromHand(number));
    }

    public void discardTo(int number) {
        getComponent(DiscardSkills.class).ifPresent(s -> s.discardTo(number));
    }

    public void discardUntilYouStop(Destination from, Consumer<Integer> playerAction ) {
        getComponent(DiscardSkills.class).ifPresent(s -> {s.discardUntilYouStop(from, playerAction);});
    }

    public Card discard(){
         return getComponent(DiscardSkills.class).map(DiscardSkills::discard).orElse(null);
    }
    public List<Card> discardAList(List<Card> toDiscard, int numberToDiscard) {
        return getComponent(DiscardSkills.class).map(t -> t.discardAList(toDiscard, numberToDiscard)).orElse(new ArrayList<>());
    }
    /**
     * Le joueur pioche un nombre de cartes désignée
     * @param number nombre de carte à piocher
     */
    public void draw(int number){
        for(int i=0; i<number; i++){
            drawToHand();
        }
    }

    //endregion

    //region --PlayCard--
    /**
     * Joue une carte de la main du joueur.
     * <p>
     * Cette méthode ne vérifie pas que le joueur a le droit de jouer la
     * carte, ni même que la carte se trouve effectivement dans sa main.
     * La carte est déplacée de la main du joueur dans la liste
     * {@code inPlay} et la méthode {@code play(Player p)} de la
     * carte est exécutée.
     *
     * @param c carte à jouer
     */
    @AfterAllyTrigger
    public void playCard(Card c) {
        getComponent(PlaySkills.class).ifPresent(s -> s.playCard(c));
    }

    @AfterAllyTrigger
    public void playCard(Card c, int amount){
        getComponent(PlaySkills.class).ifPresent(s -> s.playCard(c, amount));
    }

    //endregion

    //region --GainAnCard--
    /**
     * Le joueur gagne une carte et la place dans un emplacement donné
     * <p>Ajoute la carte dans {@link Player#CardGainedCurrentTurn}  </p>
     * <p>Cette méthode applique l'effet des cartes ayant le composant {@link TriggerComponent.DuringPlayerGain}
     *</p>
     *
     *
     * @param card carte gagnée
     * @param dest destination de la carte {@link Destination}
     * @see Player#gainTo(Destination, Card)
     */
    @AfterAllyTrigger
    public void gain(Card card, Destination dest) {
        getComponent(GainSkills.class).ifPresent(component -> component.gain(card, dest, false));
    }


    public void gain(Event event){
        getComponent(GainSkills.class).ifPresent(component -> component.gain(event.getCard(), event.getDest(), true));
    }


    /**
     * Le joueur gagne une carte et la place dans un emplacement donné 
     * <p>Ajoute la carte dans {@link Player#CardGainedCurrentTurn} seulement si {@code gained} est vrai</p>
     * <p>Ne lance aucun effet de cartes adverses </p>
     *
     * @param card carte gagnée
     * @param dest destination de la carte {@link Destination}
     * @param gained boolean pour savoir si le joueur enregistre dans sa main les cartes gagnées durant le tour ({@link Player#moveTo(Card, Destination)})
     *
     *@see Player#gainTo(Destination, Card)
     */
    @AfterAllyTrigger
    public void gainSilent(Card card, Destination dest, boolean gained) {
        getComponent(GainSkills.class).ifPresent(component -> {component.gainSilent(card, dest, gained);});
    }

    /**
     * Place réellement la carte dans l'emplacement donnée
     * @param dest destination {@link Destination}
     * @param card carte gagnée
     */
    public void gainTo(Destination dest, Card card){
        getComponent(GainSkills.class).ifPresent(c -> c.gainTo(dest, card));
    }
    //endregion

    //region --METHODES CHOICES & DISPLAY--


    @Override
    public String toString() {
        String r = String.format("     -- %s --\n", name);
        r += String.format("Actions: %d     Money: %d     Buys: %d     Draw: %d     Discard: %d\n",
               items.get(Item.ACTION).get(),
                items.get(Item.MONEY).get(), items.get(Item.BUY).get(), get(Destination.DRAW).size(), get(Destination.DISCARD).size());
        r += String.format("In play: %s\n", get(Destination.INPLAY).toString());
        r += String.format("Hand: %s\n", get(Destination.HAND).toString());
        return r;
    }

    public String toLog() {
        return "<span class=\"player-name\">" + name + "</span>";
    }

    /**
     * Méthode utilitaire pour l'interface graphique.
     * À NE PAS MODIFIER.
     */
    public String toJSON() {
        StringJoiner joiner = new StringJoiner(", ");
        joiner.add(String.format("\"id\": %d", getId()));
        joiner.add(String.format("\"name\": \"%s\"", getName()));
        joiner.add(String.format("\"actions\": %d", getValueOf(Item.ACTION)));
        joiner.add(String.format("\"money\": %d", getValueOf(Item.MONEY)));
        joiner.add(String.format("\"debt\": %d", getValueOf(Item.DEBT)));
        joiner.add(String.format("\"potion\": %d", getValueOf(Item.POTION)));
        joiner.add(String.format("\"coffre\": %d", getValueOf(Item.COFFER)));
        joiner.add(String.format("\"buys\": %d", getValueOf(Item.BUY)));
        joiner.add(String.format("\"draw\": %s", Utils.toJSON(get(Destination.DRAW))));
        joiner.add(String.format("\"discard\": %s", Utils.toJSON(get(Destination.DISCARD))));
        joiner.add(String.format("\"in_play\": %s", Utils.toJSON(get(Destination.INPLAY))));
        joiner.add(String.format("\"hand\": %s", Utils.toJSON(get(Destination.HAND))));
        joiner.add(String.format("\"color\": \"%s\"", color.get(getId())));
        if(getGame().hasType(CardType.RESERVE, 1)) joiner.add(String.format("\"tavern\": %s", Utils.toJSON(get(Destination.TAVERN))));
        if(getGame().hasExpansion("Adventures", 3)) joiner.add(String.format("\"tokens\": %s", getAdventureTokensData()));
        return "{" + joiner + "}";
    }

    public String getAdventureTokensData() {
        StringJoiner categoryJoiner = new StringJoiner(", ");

        StringJoiner supplyJoiner = new StringJoiner(", ");
        myTokens.forEach((token, supplyName) -> {
            supplyJoiner.add(String.format("\"%s\": \"%s\"", token.name().toLowerCase(), supplyName));
        });
        categoryJoiner.add(String.format("\"supplyTokens\": {%s}", supplyJoiner));

        StringJoiner playerJoiner = new StringJoiner(", ");
        playerJoiner.add(String.format("\"MinusOneCoinToken\": %b", getPersistentFlag(Token.TAX_TOKEN.name()).get()));
        playerJoiner.add(String.format("\"MinusOneCardToken\": %b", getPersistentFlag(Token.MINUS_ONE_CARD_TOKEN.name()).get()));
        playerJoiner.add(String.format("\"JourneyToken\": %b", getPersistentFlag(Token.JOURNEY_TOKEN.name()).get()));

        categoryJoiner.add(String.format("\"playerTokens\": {%s}", playerJoiner));

        return "{" + categoryJoiner + "}";
    }


    /**
     * Attend une entrée de la part du joueur (au clavier) et renvoie le choix
     * du joueur.
     * <p>
     * La méthode lit l'entrée clavier jusqu'à ce qu'un choix valide
     * soit entré par l'utilisateur sous la forme d'une chaîne de caractères
     * {@code <TYPE>:<VALEUR>} (par exemple {@code "HAND:Caravan"})
     * correspondant à un élément de {@code choices} ou éventuellement la chaîne
     * vide si l'utilisateur est autorisé à passer. Lorsqu'un choix valide est
     * obtenu, il est renvoyé.
     * <p>
     * @param instruction message à afficher à l'écran pour indiquer au joueur
     *                    la nature du choix qui est attendu
     * @param choices     une liste de {@code String} correspondant aux
     *                    choix valides attendus du joueur.
     * @param buttons     une liste de boutons à afficher à l'écran. Chaque bouton
     *                    correspond à une option de choix qui sera ajoutée à la
     *                    liste des choix valides ({@code "BUTTON:<value>"}).
     * @param canPass     booléen indiquant si le joueur a le droit de passer sans
     *                    faire de choix. S'il est autorisé à passer, c'est la
     *                    chaîne de caractères vide {@code ""} qui signifie qu'il
     *                    désire passer. Remarque : si aucun choix valide n'est
     *                    fourni (la liste {@code choices} est vide), le joueur est
     *                    automatiquement autorisé à passer même si {@code canPass}
     *                    est faux.
     * @return l'objet {@code String} correspondant au choix effectué par
     *         l'utilisateur (un élément de {@code choices} ou une chaîne de la
     *         forme {@code "BUTTON:<value>"} correspondant à un bouton de
     *         {@code buttons} ou éventuellement {@code ""}, si l'utilisateur a
     *         choisi de passer.
     */
    public String choose(String instruction, List<String> choices, List<String> allCards,  List<Button> buttons, boolean canPass) {
        // Ajout des options correspondant aux boutons
        for (Button b : buttons) {
            choices.add("BUTTON:" + b.value());
        }
        // Si aucun choix disponible, le joueur est autorisé à passer
        if (choices.isEmpty()) {
            canPass = true;
        }
        // Si le joueur peut passer, on ajoute l'option ""
        if (canPass) {
            choices.add("");
        }
        // Lit l'entrée de l'utilisateur jusqu'à obtenir un choix valide
        while (true) {
            controller.getGame().prompt(instruction, choices, allCards,  buttons, getIndex());
            String input = controller.getGame().readLine();
            if (choices.contains(input)) {
                return input;
            }
        }
    }

    public String choose(String instruction, boolean canPass) {
        List<String> choices = new ArrayList<>();
        if(canPass) {
            choices.add("");
        }
        while (true) {
            controller.getGame().prompt(instruction, choices, new ArrayList<>(), new ArrayList<>(), getIndex());
            return controller.getGame().readLine();
        }
    }

    public void chooseToken(String instruction,Predicate<Token> filter, boolean canPass) {
        List<Token> tokens = myTokens.keySet().stream().toList();
        List<String> choiceToken = tokens.stream().filter(filter).map(c -> "TOKEN:" + c.name().toLowerCase()).toList();
        String choice = choose(instruction, choiceToken, new ArrayList<>(), new ArrayList<>(), canPass);
        Token selected = null;
        if(choice.startsWith("TOKEN:")) {
            selected = Token.valueOf(choice.split("TOKEN:")[1].toUpperCase());
        }
        if(selected == null) return;

        List<Card> check = getGame().getActionSupplyCards();

        Optional<Card> selectedCard = self.chooseCardFromSupply("Select an Action supply and put your selected Token on it",check::contains, canPass);

        if (selectedCard.isPresent()) {
            self.setToken(selected, selectedCard.get().getName());
        }
    }

    public Optional<Card> chooseCardFromHand(String instruction, Predicate<? super Card> filter, boolean canPass) {
        // ajout des options correspondant aux cartes de la liste
        List<String> choices = get(Destination.HAND).stream().filter(filter).map(c -> "HAND:" + c.getName())
                .collect(Collectors.toList());
        String choice = choose(instruction, choices, new ArrayList<>(), new ArrayList<>(),  canPass);
        if (choice.startsWith("HAND:")) {
            return get(Destination.HAND).stream()
                    .filter(c -> c.hasName(choice.split(":")[1]))
                    .findFirst();
        }
        return Optional.empty();
    }

    /**
     * Version de la méthode {@code chooseCardFromHand} sans prédicat. Toutes les
     * cartes de la main du joueur sont considérées comme choix valides.
     * 
     * @param instruction
     * @param canPass
     * @return a card
     */
    public Optional<Card> chooseCardFromHand(String instruction, boolean canPass) {
        return chooseCardFromHand(instruction, c -> true, canPass);
    }


    /**
     * Fait choisir une carte au joueur parmi une liste spécifique (différente de sa main).
     * Utile pour les cartes révélées, la défausse, ou le dessus du deck.
     * @param instruction Message à afficher au joueur.
     * @param filter      Prédicat pour filtrer les cartes valides dans la liste.
     * @param list        La liste de cartes parmi lesquelles choisir.
     * @param canPass     Si le joueur peut passer.
     * @return un optional.
     */
    @Selection_Mode
    public Optional<Card> chooseCardFromList(String instruction, Predicate<? super Card> filter, List<Card> list, boolean canPass) {
        List<String >choices = computeChoices(filter, list);

        String choice = choose(instruction, choices, choices , new ArrayList<>(),  canPass);

        if (choice != null && choice.startsWith("SELECT_CARD:")) {
            return Optional.of(getCardFromIndex(list, choice));
        }
        return Optional.empty();
    }

    private List<String> computeChoices(Predicate<? super Card> filter, List<Card> list) {
        List<String> choices = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            Card c = list.get(i);
            if (filter.test(c)) {
                choices.add("SELECT_CARD:" + i + ":" + c.getName());
            }
        }
        return choices;
    }

    public Optional<String> chooseWhatToDoOpt(String instruction, List<Card> card, List<Button> buttons, boolean canPass) {
        return Optional.ofNullable(self.chooseWhatToDo(instruction, card, buttons, canPass));
    }

    @Selection_Mode
    public String chooseWhatToDo(String instruction, Predicate<Card> filter,  List<Card> list, List<Button> buttons,  boolean canPass) {
        List<String> all = computeChoices(card -> true, list);
        List<String>choices = computeChoices(filter, list);
        String choice = choose(instruction, choices, all, buttons, canPass);
        if (choice.startsWith("SELECT_CARD:")) {
            return getCardFromIndex(list, choice).getName();
        }
        if(choice.isEmpty()) return "";
        return choice.split(":")[1];
    }

    @Selection_Mode
    public String chooseWhatToDo(String instruction,List<Card> list ,List<Button> buttons, boolean canPass) {
        return chooseWhatToDo(instruction, card -> false ,list, buttons, canPass);
    }

    public Card getCardFromIndex(List<Card> cards, String choice) {
        int selectedIndex = Integer.parseInt(choice.split(":")[1]);
        return cards.get(selectedIndex);
    }

    /**
     * Attend une entrée de la part du joueur et renvoie le choix du joueur.
     * <p>
     * Cette méthode est similaire à {@code chooseCardFromHand} mais elle fait
     * choisir une carte parmi les cartes disponibles dans la réserve du jeu
     * (uniquement les piles de réserve contenant au moins une carte).
     * <p>
     * Exemple d'utilisation pour faire choisir une carte sur le dessus d'une pile
     * de réserve qui coûte 4 pièces ou moins (dans cet exemple le joueur est
     * autorisé à passer s'il ne veut pas choisir de carte) :
     * 
     * <pre>
     * Card choice = p.chooseCardFromSupply(
     *         "Choose a card costing up to 4",
     *         c -> c.getCost() <= 4,
     *         true);
     * </pre>
     * 
     * @param instruction message à afficher à l'écran pour indiquer au joueur
     *                    la nature du choix qui est attendu
     * @param filter      prédicat permettant de filtrer les cartes disponibles dans
     *                    la réserve du jeu. Seules les cartes pour lesquelles le
     *                    prédicat renvoie {@code true} seront considérées comme
     *                    choix valides.
     * @param canPass     booléen indiquant si le joueur a le droit de passer sans
     *                    faire de choix.
     * @return la carte du dessus de la pile de réserve choisie par le joueur ou
     *         {@code null} si le joueur a choisi de passer ou s'il n'avait aucune
     *         carte valide dans sa main.
     */
    public Optional<Card> chooseCardFromSupply(String instruction, Predicate<Card> filter, boolean canPass) {
        List<String> choices = game.getAvailableSupplyCards().stream()
                .filter(filter)
                .map(c -> "SUPPLY:" + c.getName())
                .collect(Collectors.toList());

        String choice = choose(instruction, choices, new ArrayList<>(), new ArrayList<>(), canPass);
        if (choice.startsWith("SUPPLY:")) {
            return Optional.ofNullable(getCardFromSupply(choice.split(":")[1]));
        }
        return Optional.empty();
    }

    /**
     * Demande au joueur de choisir une option parmi une liste de boutons affichés à
     * l'écran.
     * 
     * @param instruction message à afficher à l'écran pour indiquer au joueur la
     *                    nature du choix qui est attendu
     * @param buttons     liste des boutons à afficher à l'écran. Chaque bouton
     *                    correspond à une option de choix qui sera ajoutée à la
     *                    liste des choix valides ({@code "BUTTON:<value>"}).
     * @param canPass     booléen indiquant si le joueur a le droit de passer sans
     *                    faire de choix.
     * @return la valeur du bouton choisi par le joueur ou {@code null} si le joueur
     *         a choisi de passer ou si la liste de boutons était vide.
     */
    public String  chooseStringFromButtons(String instruction, List<Button> buttons, boolean canPass) {
        String choice = choose(instruction, new ArrayList<>(),new ArrayList<>(), buttons, canPass);
        if (choice.startsWith("BUTTON:")) {
            return choice.split(":")[1];
        }
        return null;
    }

    /**
     * Ajoute un message dans le log du jeu qui est affiché dans l'interface
     * graphique.
     * 
     * @param message message à ajouter au log du jeu (peut contenir du HTML pour le
     *                formatage)
     */
    public void log(String message) {
        game.log(message);
    }
    //endregion

    //region --PlayerLoop--
    /**
     * Exécute le tour d'un joueur
     * <p>
     * Cette méthode exécute successivement les phases du tour d'un joueur:
     * <p>
     * 1. (Préparation) initialise les compteurs d'actions, d'achats et d'argent du
     * joueur
     * <p>
     * 2. (Action, Trésor et Achat) Le joueur peut jouer des cartes Action et Trésor
     * de sa main, et acheter des cartes de la réserve. Cependant, dès qu'il joue
     * une carte Trésor, il ne peut plus jouer de carte Action pendant le reste de
     * son tour. De même, dès qu'il achète une carte, il ne peut plus jouer de carte
     * Action ni de carte Trésor pendant le reste de son tour.
     * <p>
     * Le joueur peut passer pour terminer son tour. Pour fluidifier le jeu, le tour
     * se termine également automatiquement lorsque le joueur n'a plus d'achat
     * disponible.
     */
    @BeforeAllyTrigger
    public void playTurn() {
        getComponent(PlaySkills.class).ifPresent(PlaySkills::playTurn);
    }

    public void repayDebt(){
        getComponent(BuySkills.class).ifPresent(BuySkills::repayDebt);
    }

    public void useCoffer(){
        getComponent(BuySkills.class).ifPresent(BuySkills::useCoffre);
    }

    public boolean canBuy(Card c) {
        return getComponent(BuySkills.class).map(b -> b.canBuy(c)).orElse(false);
    }

    @AfterAllyTrigger
    public void buyCard(Card c) {
        getComponent(BuySkills.class).ifPresent(b -> b.buyCard(c));
    }


    /**
     * Fin du tour du joueur
     * <p>
     * Cette méthode exécute la phase de "Clean-up" à la fin du tour d'un joueur:
     * - Les compteurs d'actions, argent et achats du joueur sont remis à 0
     * - Les cartes en main et en jeu sont défaussées (sauf les cartes Duration qui
     * ont encore un effet)
     * - Le joueur pioche les cartes de sa prochaine main (normalement 5 cartes,
     * mais parfois moins selon les effets de certaines cartes)
     */
    @BeforeAllyTrigger
    public void cleanup() {
        getComponent(PlaySkills.class).ifPresent(PlaySkills::cleanup);
    }


    //endregion

    //region --TriggerComponent--
    /**
     *Utilisé dans {@link Player#playTurn()} en début de tour.
     * <p>Cette méthode parcourt le inplay,  puis lance l'action du composant {@link DurationComponent},
     * si l'{@link Optional} est vide la méthode ne fait rien et passe à la prochaine carte
     * </p>
     * @see Card#getComponent(Class)
     */

    public void triggerDurationCard() {
        getCopyOf(Destination.INPLAY)
                .forEach(c -> c.getComponent(DurationComponent.class).ifPresent(d ->{
                    d.execute(self, c);
                    d.consume();
                }));
    }

    public <T extends TriggerComponent & BiConsumer<Event, Player>> void triggerActiveEffect(Class<T> clazz, Event event){
        new ArrayList<>(activeEffect).stream().filter(card -> card.canExecute(event, self, clazz) && !get(Destination.INPLAY).contains(card)).forEach(
                card -> card.getComponent(clazz).ifPresent(e -> e.accept(event, self))
        );
    }

    public <T extends TriggerComponent & BiConsumer<Player, Card>> void triggerActiveEffect(Class<T> clazz){
        Event event = new Event(self);
        new ArrayList<>(activeEffect).stream().filter(card -> card.canExecute(event, self, clazz) && !get(Destination.INPLAY).contains(card)).forEach(
                card -> card.getComponent(clazz).ifPresent(e -> e.accept(self, card))
        );
    }




    /**
     * Lance l'effet (Trigger) de la carte sur tout les joueurs ( qui peut être soit-même)
     *
     * @param type classe du composant (triggerComponent & TriggerEffect
     * @param event l'evenement qui a été déclenché par une action d'un joueur
     * @param <T> le composant héritant de TriggerComponent et TriggerEffect qui sont des {@link FunctionalInterface}
     */
    public<T extends TriggerComponent & BiConsumer<Event, Player>> void triggerEvent(Class<T> type, Event event){
        getGame().notifyTrigger(type, self, event);
    }

    /**
     * Lance l'effet (Trigger) des cartes en jeu sur soit-même
     *
     * @param type classe du composant (triggerComponent & TriggerEffect
     * @param <T> le composant héritant de TriggerComponent et TriggerEffect qui sont des {@link FunctionalInterface}
     */
    public<T extends TriggerComponent & BiConsumer<Player, Card>> void triggerEvent(Class<T> type){
        for(Card c : getCopyOf(Destination.INPLAY)){
            c.getComponent(type).ifPresent(d -> d.accept(self, c));
        }
    }

    /**
     *Utilisé dans {@link Player#playTurn()} en début de tour.
     * <p>Cette méthode parcourt le inplay , puis lance l'action du composant {@link TriggerComponent.onStartTurn},
     * si l'{@link Optional} est vide la méthode ne fait rien et passe à la prochaine carte
     * </p>
     * @param type la classe du composant
     * @see Card#getComponent(Class)
     */
    public<T extends TriggerComponent & Consumer<Player>> void triggerStart(Class<T> type){
        Set<Card> alreadyRevealed = new HashSet<>();
        while (true) {
            List<Card> validReactions = this.getCopyOf(Destination.HAND).stream()
                    .filter(c -> c.hasType(CardType.REACTION)
                            && c.hasComponent(type)
                            && !alreadyRevealed.contains(c))
                    .toList();

            if (validReactions.isEmpty()) break;

            Optional<Card> chosen = self.chooseCardFromList("Start turn, you may play a Card?", card -> true, validReactions, true);

            if (chosen.isEmpty()) break;
            alreadyRevealed.add(chosen.get());
            chosen.get().getComponent(type).ifPresent(s -> s.accept(self));

        }

        triggerStartTavern(TriggerComponent.onStartTurn.class, new Event(self));

    }

    /**
     * Lance une vérification sur l'état du joueur et si il est immunisé contre le type {@link CardType#ATTACK}
     *
     * @param type la classe du composant {@link fr.umontpellier.iut.dominion.cards.component.TriggerComponent.Immunity}
     * @return l'état du joueur {@code False} si il n'est pas immunisé {@code True} si il l'est
     * @param <T> le composant
     *           
     * @see Card#hasComponent(Class)
     */
    public <T extends TriggerComponent.Immunity> boolean immunity(Class<T> type, Card attack) {

        boolean inPlay = getCopyOf(Destination.INPLAY).stream()
                .anyMatch(card -> card.getComponent(type).map(t -> t.immune(card) || t.isImmuneAgainst(card, attack)).orElse(false));

        if (inPlay) return true;

        return getCopyOf(Destination.HAND).stream().anyMatch(card -> card.hasComponent(type) && card.getComponent(type).map(i -> i.revealed(self, card)).orElse(false));
    }

    /**
     * Regarde dans la main en jeu, si la carte comporte le composant {@link ExtraTurnComponent}
     * <p>Dans l'ordre</p>
     * <li> {@code flatMap()} Vérifie les cartes qui contiennent un {@code ExtraTurnComponent}
     * une par une  et Stream le résultat</li>
     * <li> {@code flatMap()} Vérifie si la carte peut déclencher l'effet {@link ExtraTurnComponent#canUseExtraTurn()} et retoune un nouveau Stream}</li>
     * <li> {@code findFirst()} Récupère le premier qui peut déclencher le résultat en tant que {@code Optional<ExtraTurnComponent>} </li>
     * <li> {@code map()} si L'Optional existe {@code Non Vide}, la map déclenche {@link ExtraTurnComponent#consume(Player)} puis renvoie true}</li>
     * <li> {@code orElse()} retourne false si l'Optional est vide</li>
     *
     * @return si le joueur peut faire un tour en plus {@link SeaSideFactory#Outpost()}
     *
     * @see Card#getComponent(Class)
     */
    public boolean triggerAnotherTurn() {
        if(isSecondTurn){
            isSecondTurn = false;
            return false;
        }

        if (this.mustConductPossessedTurn) {
            this.setController(nextPossessor);
            this.mustConductPossessedTurn = false;
            isSecondTurn = true;
            return true;
        }

        if(this.isFlagSet(Flags.expedition)){
            isSecondTurn = true;
            return true;
        }

        return getCopyOf(Destination.HAND).stream()
                .flatMap(c -> c.getComponent(ExtraTurnComponent.class).stream())
                .flatMap(comp -> comp.canUseExtraTurn().stream())
                .findFirst()
                .map(e -> {
                    e.consume(self);
                    isSecondTurn = true;
                    return true;
                }).orElse(false);
    }


    public <T extends TriggerComponent & BiConsumer<Event, Card>> void triggerOneCard(Class<T> clazz, Card c, Event event){
        if(!c.canExecute(event, self, clazz))return;
        c.getComponent(clazz).ifPresent(t -> t.accept(event, c));
    }

    public <T extends TriggerComponent & BiConsumer<Event, Card>> void triggerCardTavern(Class<T> clazz, Event event){
       triggerTavernMat(clazz, event, card -> card.getComponent(clazz).ifPresent(e -> e.accept(event, card)));
    }

    public <T extends TriggerComponent & BiConsumer<Event, Player>> void triggerPlayerTavern(Class<T> clazz, Event event){
        triggerTavernMat(clazz, event, card -> card.getComponent(clazz).ifPresent(e -> e.accept(event, self)));
    }

    public <T extends TriggerComponent & BiConsumer<Player, Card>> void triggerPlayerAndCardTavern(Class<T> clazz, Event event){
        triggerTavernMat(clazz, event, card -> card.getComponent(clazz).ifPresent(e -> e.accept(self, card)));
    }

    public <T extends TriggerComponent & Consumer<Player>> void triggerStartTavern(Class<T> clazz, Event event){
        triggerTavernMat(clazz, event, card -> card.getComponent(clazz).ifPresent(c -> c.accept(self)));
    }


    public <T extends TriggerComponent> void triggerTavernMat(Class<T> clazz, Event event, Consumer<Card> consumer){
        Set<Card> alreadyCall = new HashSet<>();
        while(true){
            List<Card> tavern = getValidTavern(clazz, event, alreadyCall);
            if(tavern.isEmpty()) break;
            Optional<Card> call = self.chooseCardFromList("Call a card", card -> true, tavern, true);
            if (call.isEmpty()) break;
            alreadyCall.add(call.get());
            Card c = call.get();
            c.moveTo(get(Destination.INPLAY), Destination.INPLAY);
            consumer.accept(c);
        }
    }

    public <T extends TriggerComponent> List<Card> getValidTavern(Class<T> clazz, Event event, Set<Card> alreadyCall){
        return getCopyOf(Destination.TAVERN).stream()
                .filter(c -> c.hasComponent(clazz) && c.hasType(CardType.RESERVE) && !alreadyCall.contains(c) && c.canExecute(event, self, clazz))
                .toList();
    }

    public <T extends TriggerComponent & BiConsumer<Event, Card>> void triggerOthersEvent(Class<T> clazz, Event event){
        getCopyOf(Destination.INPLAY).stream()
                .filter(c ->( !c.hasType(CardType.REACTION) && c.hasComponent(clazz) && c.canExecute(event, self, clazz)))
                .forEach(c -> c.getComponent(clazz).ifPresent(t -> t.accept(event, c)));

        Set<Card> alreadyRevealed = new HashSet<>();

        while (true) {
            List<Card> validReactions= getValidReactions(clazz, event, alreadyRevealed);
            if (validReactions.isEmpty()) break;
            Optional<Card> chosen = self.chooseCardFromList("Reveal a Reaction?", card -> true, validReactions, true);
            if (chosen.isEmpty()) break;
            alreadyRevealed.add(chosen.get());
            chosen.get().getComponent(clazz).ifPresent(t -> t.accept(event, chosen.get()));
        }
    }


    public <T extends TriggerComponent> List<Card> getValidReactions(Class<T> clazz, Event event, Collection<Card> container) {
        return getCopyOf(Destination.HAND).stream()
                .filter(c -> c.hasType(CardType.REACTION)
                        && c.hasComponent(clazz)
                        && c.canExecute(event, self, clazz)
                        && !container.contains(c))
                .toList();
    }


    public boolean trash(int number){
        return getComponent(TrashSkills.class).map(trash -> trash.trash(number)).orElse(false);
    }

    public Card trash(){
        return getComponent(TrashSkills.class).map(TrashSkills::trash).orElse(null);
    }

    public void trashWithCondition(int number, Predicate<Card> condition, Destination from){
        getComponent(TrashSkills.class).ifPresent(trash -> trash.trashWithCondition(number, condition, from));
    }

    @AfterAllyTrigger
    public boolean trash(Card c) {
       return getComponent(TrashSkills.class).map(t -> t.trash(c)).orElse(false);
    }

    @AfterAllyTrigger
    public void trashAll(Destination sourceZone) {
        getComponent(TrashSkills.class).ifPresent(t -> t.trashAll(sourceZone));
    }

    //--endregion


    public void putACardInDraw(Card card, Card selected) {
        List<Card> drawPile = get(Destination.DRAW);
        List<Card> tempAside = new ArrayList<>();

        if (selected == null) {
            moveTo(card, Destination.DRAW);
        } else {

            while (!drawPile.isEmpty()) {
                Card top = drawPile.getLast();
                if (top.equals(selected)) {
                    break;
                }
                top.moveTo(tempAside, null);
            }

            moveTo(card, Destination.DRAW);

            for (int j = tempAside.size() - 1; j >= 0; j--) {
                moveTo(tempAside.get(j), Destination.DRAW);
            }
        }
    }

    public void addCardEffect(Card c){
        if(activeEffect.contains(c))return;
        activeEffect.add(c);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Player player)) return false;
        return Objects.equals(getId(), player.getId());
    }

    public int getId(){
        return Id;
    }


    public List<Card> getDistinctCards(Destination dest){
        if(dest == Destination.TRASH){
            return getGame().getTrashCards().stream()
                    .collect(Collectors.collectingAndThen(
                            Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Card::getName))),
                            ArrayList::new
                    ));
        }

        return getCopyOf(dest).stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Card::getName))),
                        ArrayList::new
                ));
    }


    public void generalCleanUp(){
        self.getCopyOf(Destination.INPLAY).stream().filter(c->
                        c.getComponent(DurationComponent.class)
                                .map(d -> d.isFinished(c))
                                .orElse(true))
                .toList()
                .forEach(c ->{
                    self.discard(c, Discard_Type.CLEANUP);
                    c.clear();
                });

        resetItem();
        resetFlags();
        CardGainedCurrentTurn.clear();
        discardHooks.clear();
        activeEffect.clear();
        

    }


    public void revealsUntil(Predicate<Card> check, Consumer<Card> action){
        getComponent(DiscardSkills.class).ifPresent(d -> d.discardUntil(check, action));
    }

    public void reveals(Card c){
        if(c == null) return;
        self.log("Reveals " + c.toLog());
    }

    public void reveals(List<Card> cards){
        self.log("Reveals " + cards);
    }


    public void shuffling(Destination dest){
        Collections.shuffle(get(dest));
    }

    public boolean isActive(){
        return self.equals(game.getCurrentTurnPlayer());
    }

}
