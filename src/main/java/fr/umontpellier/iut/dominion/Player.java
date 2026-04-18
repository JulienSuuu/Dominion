package fr.umontpellier.iut.dominion;


import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import fr.umontpellier.iut.dominion.cards.*;
import fr.umontpellier.iut.dominion.cards.component.*;
import fr.umontpellier.iut.dominion.cards.seaside.SeaSideFactory;
import fr.umontpellier.iut.dominion.gui.Utils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Un joueur de Dominion
 */
public class Player {

    //region --VARIABLES--
    /**
     * Nom du joueur
     */
    private final String name;

    private final EnumMap<Destination, ObservableList<Card>> cardSet = new EnumMap<>(Destination.class);
    private final EnumMap<Item, Integer> items = new EnumMap<>(Item.class);
    private final Map<String, BooleanProperty> flags = new HashMap<>();
    private final Map<String, IntegerProperty> properties = new HashMap<>();

    /**
     * Bonus ou malus sur le nombre de cartes à piocher
     * {@link SeaSideFactory#Outpost()}
     */
    private int drawBonusNextTurn;

    /**
     * La partie en cours
     */
    private final Game game;
    private boolean isSecondTurn = false;
    /**
     * Liste des cartes gagnées au tour précédent
     */
    private List<Card> CardGainedLastTurn;

    /**
     * Liste des cartes gagnées au tour en cours
     */
    private final List<Card> CardGainedCurrentTurn;
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
     * @param name: le nom du joueur
     * @param game: le jeu en cours
     *
     */
    public Player(String name, Game game) {
        this.name = name;
        this.game = game;

        CardGainedLastTurn = new ArrayList<>();
        CardGainedCurrentTurn = new ArrayList<>();

        for (Destination d : Destination.values()) {
            cardSet.put(d, FXCollections.observableArrayList());
        }

        for (Item i : Item.values()) {
            items.put(i, 0);
        }

        // Ajoute 3 Estate et 7 Copper (pris dans la réserve du jeu) dans la
        // défausse du joueur
        for (int i = 0; i < 3; i++)
            getCardFromSupply("Estate").moveTo(get(Destination.DISCARD));
        for (int i = 0; i < 7; i++)
            getCardFromSupply("Copper").moveTo(get(Destination.DISCARD));

        // Mélange la défausse, construit la pioche et pioche 5 cartes en main
        Collections.shuffle(get(Destination.DISCARD));
        while (!get(Destination.DISCARD).isEmpty()) {
            get(Destination.DISCARD).getLast().moveTo(get(Destination.DRAW));
        }
        for (int i = 0; i < 5; i++) {
            get(Destination.DRAW).getLast().moveTo(get(Destination.HAND));
        }

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

    public int getIndex() {
        return game.getPlayerIndex(this);
    }

    public int getMoney() {
        return items.get(Item.MONEY);
    }

    public int getNumberOfActions() {return items.get(Item.ACTION);}

    public BooleanProperty getFlag(String key) {
        return flags.computeIfAbsent(key, k -> new SimpleBooleanProperty(false));
    }

    public IntegerProperty getProperties(String key) {
        return properties.computeIfAbsent(key, k -> new SimpleIntegerProperty(0));
    }

    public boolean isFlagSet(String key) {
        return getFlag(key).get();
    }

    public void resetFlags() {
        flags.values().forEach(prop ->{
                    if(!prop.isBound()){
                        prop.set(false);}
                });
    }

    public void resetProperties() {
        properties.values().forEach(prop ->{
            if(!prop.isBound()){
                prop.set(0);
            }
        });
    }

    public int getNumberOfBuys() {
        return  items.get(Item.BUY);
    }

    public Game getGame() {
        return game;
    }

    public int getDebt() {return items.get(Item.DEBT);}

    public int getPotion() {
        return items.get(Item.POTION);
    }

    private List<Card> get(Destination destination) {
        return cardSet.get(destination);
    }
    public int getValueOf (Item item) {return items.get(item);}

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
        for (Destination d : Destination.values()) {
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
        return getCopyOf(Destination.HAND).stream().mapToInt(card -> card.getVictoryValue(this)).sum() + items.get(Item.VICTORY_TOKEN);
    }


    public void increment(Item it, int value){items.put(it, items.get(it) + value);}
    public void decrement(Item it, int value){items.put(it, items.get(it) - value);}

    public void resetItem() {
        Set<Item> persistent = EnumSet.of(
                Item.DEBT,
                Item.COFFER,
                Item.COIN_TOKEN_SHIP
        );
        GameStat.reduction.set(0);
        for (Item item : Item.values()) {
            if (!persistent.contains(item)) {
                items.put(item, 0);
            }
        }
    }

    public Player getController() {
        return controller;
    }
    public void setController(Player controller) {
        this.controller = controller;
    }


    public void updateDrawBonusValue(int value){
        if(drawBonusNextTurn == -4)return;
        drawBonusNextTurn += value;
    }

    public void listener(){
        getFlag(Flags.COPPER_PLAYED).bind(
                Bindings.createBooleanBinding(
                        () -> cardSet.get(Destination.INPLAY).stream().anyMatch(c -> c.hasName("Copper")),
                        cardSet.get(Destination.INPLAY)
                )
        );
        getProperties(Properties.puddlerReduction).bind(
                Bindings.createIntegerBinding(
                        () -> {
                            Number count = cardSet.get(Destination.INPLAY).stream().filter(c -> c.hasType(CardType.ACTION)).count();
                            return count.intValue()*2;
                        },
                        cardSet.get(Destination.INPLAY)
                )
        );
    }

    public int getCoins(){
        return items.get(Item.COIN_TOKEN_SHIP);
    }
    //endregion

    //region --METHODES MoveTo--
    /**
     * Déplace une carte dans la main du joueur.
     *
     * @param c la carte à déplacer
     */
    public void moveToHand(Card c) {
        c.moveTo(get(Destination.HAND));
    }

    public void discard(Card c){
        if(c==null) return;
        Event event = new Event(c, Destination.DISCARD, this );
        c.as(TriggerComponent.onCardDiscarded.class).ifPresent(t -> t.accept(this, event));
        gainSilent(c, event.getDest(), false);
    }

    /**
     * Méthode Générique
     * <li>Déplace une carte dans la destination choisit </li>
     * @param c {@link Card} carte à déplacer
     * @param dest {@link Destination} destination de la carte
     * @see Player#gainSilent(Card, Destination, boolean)
     */
    public void moveTo(Card c, Destination dest){
        gainSilent(c, dest, false);
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
        getCopyOf(Destination.DISCARD).forEach(c -> c.moveTo(get(Destination.DRAW)));
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
        Card c = getCardFromDeck();
        if(c != null) {
            moveTo(c, dest);
        }
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
        if (get(Destination.HAND).isEmpty()) return;

        int target = Math.min(number, get(Destination.HAND).size());
        int discardedCount = 0;

        while (discardedCount < target) {
            Card c = controller.chooseCardFromHand("Défausse encore " + (target - discardedCount) + " carte(s)", false);
            if (c != null) {
                discard(c);
                discardedCount++;
            } else {
                break;
            }
        }
    }

    public Card discard(){
        Card c = chooseCardFromHand("Défausse une carte ", true );
        if(c != null){
            discard(c);
           return c;}

        return null;
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
    public void playCard(Card c) {
        c.moveTo(get(Destination.INPLAY));
        if(c.hasType(CardType.ACTION))increment(Item.ACTION_PLAYED, 1);
        Event event = new Event(c, null, this);
        triggerEvent(TriggerComponent.OnCardPlayed.class, event);
        c.play(this);
    }

    /**
     * Joue une carte de la main du joueur.
     * <p>
     * S'il existe une carte dans la main du joueur dont le nom est
     * égal au paramètre, la carte est jouée à l'aide de la méthode
     * {@code playCard(Card c)}. Si aucune carte ne correspond, la
     * méthode ne fait rien.
     *
     * @param cardName nom de la carte à jouer
     */
    public void playCard(String cardName) {
        get(Destination.HAND).stream()
                .filter(card -> card.getName().equalsIgnoreCase(cardName))
                .findFirst()
                .ifPresent(this::playCard);
    }
    //endregion

    //region --GainAnCard--
    /**
     * Le joueur gagne une carte et la place dans un emplacement donné (main,
     * défausse, etc.)
     * <p>
     * Si la carte n'est pas {@code null}, elle est déplacée dans l'emplacement
     * indiqué
     *
     * @param gainedCard carte à gagner (éventuellement {@code null})
     */
    public void gainTo(Card gainedCard, List<Card> location) {
        if(gainedCard == null)return;
        gainedCard.moveTo(location);
        int i = game.tradeRoute(gainedCard);
        increment(Item.COIN_TOKEN_ROUTE, i);
    }

    /**
     * Le joueur gagne une carte et la place dans un emplacement donné
     * <p>Ajoute la carte dans {@link Player#CardGainedCurrentTurn}  </p>
     * <p>Cette méthode applique l'effet des cartes ayant le composant {@link fr.umontpellier.iut.dominion.cards.component.TriggerComponent.OnPlayerGain}
     *</p>
     *
     *
     * @param card carte gagnée
     * @param dest destination de la carte {@link Destination}
     * @see Player#gainTo(Destination, Card)
     */
    public void gain(Card card, Destination dest){
        if(card == null)return;
        CardGainedCurrentTurn.add(card);

        if(card.getFlag("haveSpecialEffect")){
            card.execute(this);
        }

        Event event = new Event(card, dest, this);
        triggerEvent(TriggerComponent.OnPlayerGain.class, event);
        triggerActiveEffect(TriggerComponent.OnPlayerGain.class, event);


        if(controller != this){
            controller.gainTo(Destination.DISCARD, card);
            return;
        }
        gainTo(event.getDest(), event.getCard());

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
    public void gainSilent(Card card, Destination dest, boolean gained) {
        if(gained)CardGainedCurrentTurn.add(card);

        if(card.getFlag("haveSpecialEffect")){
            card.execute(this);
        }

        if(controller != this){
            controller.gainTo(Destination.DISCARD, card);
        }

        gainTo(dest, card);
    }

    /**
     * Place réellement la carte dans l'emplacement donnée
     * @param dest destination {@link Destination}
     * @param card carte gagnée
     * @see Player#gainTo(Card, List)
     */
    public void gainTo(Destination dest, Card card){
        if(dest == null)return;
        gainTo(card, get(dest));
    }
    //endregion

    //region --METHODES CHOICES & DISPLAY--
    /**
     * Renvoie une représentation de l'état du joueur sous forme d'une chaîne
     * de caractères.
     * <p>
     * Cette représentation comporte
     * - le nom du joueur
     * - le nombre d'actions, de pièces et d'achats du joueur
     * - le nombre de cartes dans la pioche et dans la défausse du joueur
     * - la liste des cartes en jeu du joueur
     * - la liste des cartes dans la main du joueur
     * <p>
     * On pourrait par exemple avoir l'affichage suivant:
     * <p>
     * -- Toto --
     * Actions: 2 Money: 4 Buys: 1 Draw: 7 Discard: 3
     * In play: Caravan, Copper, Silver, Copper
     * Hand: Estate, Province
     */
    @Override
    public String toString() {
        String r = String.format("     -- %s --\n", name);
        r += String.format("Actions: %d     Money: %d     Buys: %d     Draw: %d     Discard: %d\n",
               items.get(Item.ACTION),
                items.get(Item.MONEY), items.get(Item.BUY), get(Destination.DRAW).size(), get(Destination.DISCARD).size());
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
        joiner.add(String.format("\"name\": \"%s\"", name));
        joiner.add(String.format("\"actions\": %d", items.get(Item.ACTION)));
        joiner.add(String.format("\"money\": %d", items.get(Item.MONEY)));
        joiner.add(String.format("\"debt\": %d", items.get(Item.DEBT)));
        joiner.add(String.format("\"potion\": %d", items.get(Item.POTION)));
        joiner.add(String.format("\"coffre\": %d", items.get(Item.COFFER)));
        joiner.add(String.format("\"buys\": %d", items.get(Item.BUY)));
        joiner.add(String.format("\"draw\": %s", Utils.toJSON(get(Destination.DRAW))));
        joiner.add(String.format("\"discard\": %s", Utils.toJSON(get(Destination.DISCARD))));
        joiner.add(String.format("\"in_play\": %s", Utils.toJSON(get(Destination.INPLAY))));
        joiner.add(String.format("\"hand\": %s", Utils.toJSON(get(Destination.HAND))));
        return "{" + joiner + "}";
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
     * Exemple d'utilisation pour demander à un joueur de choisir le nom d'une
     * carte de sa main (ici il n'a pas le droit de passer s'il a au moins une carte
     * en main). Dans l'exemple la méthode renvoie une chaîne de caractères de la
     * forme {@code "HAND:<cardName>"} où {@code <cardName>} est le nom de la carte
     * choisie par le joueur parmi les cartes de sa main.
     * 
     * <pre>
     * {@code
     * List<String> choices = new ArrayList<>();
     * for (Card c : hand) {
     *     choices.add("HAND:" + c.getName());
     * }
     * String choice = p.choose("Choose a card", choices, new ArrayList<>(), false);
     * }
     * </pre>
     * 
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
    public String choose(String instruction, List<String> choices, List<Button> buttons, boolean canPass) {
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
            controller.game.prompt(instruction, choices, buttons, getIndex());
            String input = controller.game.readLine();
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
            controller.game.prompt(instruction, choices, new ArrayList<>(), getIndex());
             return controller.game.readLine();
        }
    }

    /**
     * Attend une entrée de la part du joueur et renvoie le choix du joueur.
     * <p>
     * Dans cette méthode, la liste des choix est donnée sous la forme d'un prédicat
     * permettant de filtrer les cartes de la main du joueur. Le résultat renvoyé
     * est la carte choisie ou {@code null} si le joueur a choisi de passer.
     * <p>
     * La méthode commence par construire une liste de tous les noms des cartes
     * dans {@code hand} qui vérifient le prédicat, puis appelle la méthode
     * {@code choose} pour faire choisir un nom parmi cette liste à l'utilisateur.
     * <p>
     * Exemple d'utilisation pour faire choisir le nom d'une carte Action de sa
     * main à un joueur (dans cet exemple le joueur n'a pas le droit de passer
     * s'il a au moins une carte Action en main, mais la méthode peut quand
     * même renvoyer {@code null} s'il n'a aucune carte Action en main) :
     * 
     * <pre>
     * Card choice = p.chooseCardFromHand(
     *         "Choose an Action card",
     *         c -> c.hasType(CardType.ACTION),
     *         false);
     * </pre>
     * 
     * @param instruction message à afficher à l'écran pour indiquer au joueur
     *                    la nature du choix qui est attendu
     * @param filter      prédicat permettant de filtrer les cartes de la main
     *                    du joueur. Seules les cartes pour lesquelles le prédicat
     *                    renvoie {@code true} seront considérées comme choix
     *                    valides.
     * @param canPass     booléen indiquant si le joueur a le droit de passer sans
     *                    faire de choix.
     * @return la carte choisie par le joueur ou {@code null} si le joueur a choisi
     *         de passer ou s'il n'avait aucune carte valide dans sa main.
     */
    public Card chooseCardFromHand(String instruction, Predicate<Card> filter, boolean canPass) {
        // ajout des options correspondant aux cartes de la liste
        List<String> choices = get(Destination.HAND).stream().filter(filter).map(c -> "HAND:" + c.getName())
                .collect(Collectors.toList());
        String choice = choose(instruction, choices, new ArrayList<>(), canPass);
        if (choice.startsWith("HAND:")) {
            return get(Destination.HAND).stream()
                    .filter(c -> c.hasName(choice.split(":")[1]))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    /**
     * Version de la méthode {@code chooseCardFromHand} sans prédicat. Toutes les
     * cartes de la main du joueur sont considérées comme choix valides.
     * 
     * @param instruction
     * @param canPass
     * @return a card
     */
    public Card chooseCardFromHand(String instruction, boolean canPass) {
        return chooseCardFromHand(instruction, c -> true, canPass);
    }


    /**
     * Fait choisir une carte au joueur parmi une liste spécifique (différente de sa main).
     * Utile pour les cartes révélées, la défausse, ou le dessus du deck.
     * * @param instruction Message à afficher au joueur.
     * @param filter      Prédicat pour filtrer les cartes valides dans la liste.
     * @param list        La liste de cartes parmi lesquelles choisir.
     * @param canPass     Si le joueur peut passer.
     * @return La carte choisie ou null.
     */
    public Card chooseCardFromList(String instruction, Predicate<Card> filter, List<Card> list, boolean canPass) {
        List<String> choices = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            Card c = list.get(i);
            if (filter.test(c)) {
                choices.add("SELECT_CARD:" + i + ":" + c.getName());
            }
        }

        String choice = choose(instruction, choices, new ArrayList<>(), canPass);

        if (choice != null && choice.startsWith("SELECT_CARD:")) {
            int selectedIndex = Integer.parseInt(choice.split(":")[1]);
            return list.get(selectedIndex);
        }
        return null;
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
    public Card chooseCardFromSupply(String instruction, Predicate<Card> filter, boolean canPass) {
        List<String> choices = game.getAvailableSupplyCards().stream()
                .filter(filter)
                .map(c -> "SUPPLY:" + c.getName())
                .collect(Collectors.toList());

        String choice = choose(instruction, choices, new ArrayList<>(), canPass);
        if (choice.startsWith("SUPPLY:")) {
            return getCardFromSupply(choice.split(":")[1]);
        }
        return null;
    }

    /**
     * Demande au joueur de choisir une carte parmi une liste passée en argument.
     * 
     * @param instruction message à afficher à l'écran pour indiquer au joueur la
     *                    nature du choix qui est attendu
     * @param cards       liste des cartes parmi lesquelles le joueur doit choisir.
     *                    Pour chaque carte de la liste, un bouton portant le nom de
     *                    la carte est affiché à l'écran.
     * @param canPass     booléen indiquant si le joueur a le droit de passer sans
     *                    faire de choix.
     * @return la carte choisie par le joueur ou {@code null} si le joueur a choisi
     *         de passer ou si la liste de cartes était vide.
     */
    public Card chooseCardFromButtons(String instruction, List<Card> cards, boolean canPass) {
        // liste de noms de cartes
        List<Button> buttons = new ArrayList<>();
        // ajout des options correspondant aux cartes de la liste
        for (Card c : cards)
            buttons.add(new Button(c.getName(), c.getName()));

        String choice = choose(instruction, new ArrayList<>(), buttons, canPass);
        if (choice.startsWith("BUTTON:")) {
            for (Card c : cards) {
                if (c.hasName(choice.split(":")[1])) {
                    return c;
                }
            }
        }
        return null;
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
    public String chooseStringFromButtons(String instruction, List<Button> buttons, boolean canPass) {
        String choice = choose(instruction, new ArrayList<>(), buttons, canPass);
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

    public void playTurn() {
        items.put(Item.ACTION, 1);
        items.put(Item.BUY, 1);

        boolean canPlayAction = true;
        boolean canPlayTreasure = true;

        triggerDurationCard();
        triggerStart(TriggerComponent.onStartTurn.class);
        while (true) {
            List<String> choices = new ArrayList<>();
            computeChoices(choices, canPlayAction, canPlayTreasure);
            List<Button> buttons = computeButtons();
            String instruction = computeInstruction(canPlayAction, canPlayTreasure);

            String playCard = choose(instruction, choices, buttons, true);

            if (playCard.isEmpty()) break;


            if(playCard.startsWith("BUTTON:")) {
                String choice =  playCard.split(":")[1];
                if(choice.equals("REMBOURSER")) {
                    repayDebt();
                    continue;
                }
                if(choice.equals("COFFRE")) {
                    useCoffre();
                    continue;
                }
            }

            if (playCard.startsWith("HAND:")) {
                Card play = get(Destination.HAND).stream()
                        .filter(c -> c.hasName(playCard.split(":")[1]))
                        .findFirst()
                        .orElse(null);
                
                if (play == null) continue;

                if (play.hasType(CardType.ACTION)) {
                    increment(Item.ACTION, -1);

                }
                if (play.hasType(CardType.TREASURE)) {
                    canPlayAction = false;
                }

                playCard(play);

                if (items.get(Item.ACTION) == 0) {
                    canPlayAction = false;
                }
            }

            if (playCard.startsWith("SUPPLY:")) {
                Card play = getCardFromSupply(playCard.split(":")[1]);
                if(play == null) continue;
                increment(Item.BUY, -1);
                canPlayAction = false;
                canPlayTreasure = false;
                buyCard(play);
                if (items.get(Item.BUY) == 0) {
                    break;
                }
            }
        }
    }

    public void repayDebt(){
        int toRepay = Math.min(items.get(Item.MONEY), items.get(Item.DEBT));
        decrement(Item.DEBT, toRepay);
        decrement(Item.MONEY, toRepay);
    }

    public void useCoffre(){
        if(items.get(Item.COFFER) > 0){
            decrement(Item.COFFER, 1);
            decrement(Item.MONEY, 1);
        }
    }

    public boolean canBuy(Card c) {
        boolean enoughMoney = items.get(Item.MONEY) >= c.getCost();
        boolean enoughPotion = items.get(Item.POTION) >= c.getPotion();
        boolean isNotDebted = items.get(Item.DEBT) == 0;
        boolean available = c.getAvailable().test(this);
        boolean isNotContraband = !getGame().getNamedCardsThisTurn("contraband").contains(c.getName());
        return enoughMoney && enoughPotion && isNotDebted && available && isNotContraband;
    }

    public void buyCard(Card c) {
        decrement(Item.MONEY, c.getCost());
        decrement(Item.POTION, c.getPotion());
        decrement(Item.DEBT, c.getDebt());

        log(toLog() + " bought " + c.toLog());
        triggerBuy(c);
        gain(c, Destination.DISCARD);
        onCursePile(c);
    }

    public void triggerBuy(Card c){
        getCopyOf(Destination.INPLAY)
                .forEach(card -> card.as(TriggerComponent.onBuy.class).ifPresent(d -> d.accept(this, c)));

    }

    public void onCursePile(Card c){
        if(getGame().hasToken(c.getName())){
            for (int i = 0; i <  getGame().getToken(c.getName()); i++ ){
            gain(getCardFromSupply("Curse"),Destination.DISCARD);
            }
        }
    }
    /**
     * Remplis la liste des choix à proposer au joueur pendant son tour
     *
     * @param choices List des choix à remplir
     * @param canPlayAction si le joueur peut jouer une carte Action
     * @param canPlayTreasure si le joueur peut jouer une carte Treasure
     */
    private void computeChoices(List<String> choices, boolean canPlayAction, boolean canPlayTreasure) {
        for (Card c : get(Destination.HAND)) {
            if(canPlayAction && c.hasType(CardType.ACTION) && items.get(Item.ACTION) > 0){
                choices.add("HAND:" + c.getName());
            }
            if(canPlayTreasure && c.hasType(CardType.TREASURE)) {
                choices.add("HAND:" + c.getName());
            }
        }
        for(Card c : game.getAvailableSupplyCards()){
            if(!canBuy(c)) continue;
            choices.add("SUPPLY:" + c.getName());
        }
    }

    private List<Button> computeButtons() {
        List<Button> buttons = new ArrayList<>();
        if(items.get(Item.DEBT) > 0){
            buttons.add(new Button("Rembourser la dette", "REMBOURSER"));
        }
        if(items.get(Item.COFFER) > 0){
            buttons.add(new Button("Coffre (" + items.get(Item.COFFER) + ")", "COFFRE"));
        }
        return buttons;
    }

    /**
     * l'instruction complète à donnée au joueur pour ces choix (Action, Treasure, Buy)
     *
     * @param canPlayAction si le joueur peut jouer une carte Action
     * @param canPlayTreasure si le joueur peut jouer une carte Treasure
     * @return l'instruction
     */
    private String computeInstruction(boolean canPlayAction, boolean canPlayTreasure) {
        StringJoiner instructions = new StringJoiner(" | ");
        instructions.add("CHOOSE AN EVENT: ");
        if(canPlayAction){
            instructions.add("ACTION");
        }
        if(canPlayTreasure){
            instructions.add("TREASURE");
        }
        instructions.add("BUY ");
        return instructions.toString();
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
    public void cleanup() {
        triggerEvent(TriggerComponent.onEndBuy.class);
        resetItem();
        getCopyOf(Destination.HAND).forEach(c -> {c.moveTo(get(Destination.DISCARD)); c.clear();});

        getCopyOf(Destination.INPLAY).stream().filter(c->
                        c.as(DurationComponent.class)
                                .map(DurationComponent::isFinished)
                                .orElse(true))
                .toList()
                .forEach(c ->{
                    c.moveTo(get(Destination.DISCARD));
                    if(c.hasName("Charlatan")){
                        GameStat.charlatanPower.set(GameStat.charlatanPower.getValue() - 1);
                    }
                    c.clear();
                });

        if (this.controller != this) {
            this.setController(this);
            mustBeDiscarded.forEach(c -> {c.moveTo(get(Destination.DISCARD)); c.clear();});
        }


        int numberOfDraw = Math.max(5 + drawBonusNextTurn,0) ;
        draw(numberOfDraw);
        drawBonusNextTurn = 0;

        CardGainedLastTurn = new ArrayList<>(CardGainedCurrentTurn);
        CardGainedCurrentTurn.clear();
        activeEffect.clear();
        resetFlags();
        resetProperties();
    }


    //endregion

    //region --TriggerComponent--
    /**
     *Utilisé dans {@link Player#playTurn()} en début de tour.
     * <p>Cette méthode parcourt le inplay,  puis lance l'action du composant {@link DurationComponent},
     * si l'{@link Optional} est vide la méthode ne fait rien et passe à la prochaine carte
     * </p>
     * @see Card#as(Class)
     */

    private void triggerDurationCard() {
        getCopyOf(Destination.INPLAY)
                .forEach(c -> c.as(DurationComponent.class).ifPresent(d ->{
                    d.execute(this, c);
                    d.consume();
                }));
    }

    private <T extends TriggerComponent & TriggerEffect> void triggerActiveEffect(Class<T> clazz, Event event){
        new ArrayList<>(activeEffect).stream().filter(card -> card.canExecute(event, this)).forEach(
                card -> card.as(clazz).ifPresent(e -> e.execute(this, this, event))
        );

    }


    /**
     * Lance l'effet (Trigger) de la carte sur tout les joueurs ( qui peut être soit-même)
     *
     * @param type classe du composant (triggerComponent & TriggerEffect
     * @param event l'evenement qui a été déclenché par une action d'un joueur
     * @param <T> le composant héritant de TriggerComponent et TriggerEffect qui sont des {@link FunctionalInterface}
     */
    public<T extends TriggerComponent & TriggerEffect> void triggerEvent(Class<T> type, Event event){
        getGame().notifyTrigger(type, this, event);
    }

    /**
     * Lance l'effet (Trigger) des cartes en jeu sur soit-même
     *
     * @param type classe du composant (triggerComponent & TriggerEffect
     * @param <T> le composant héritant de TriggerComponent et TriggerEffect qui sont des {@link FunctionalInterface}
     */
    public<T extends TriggerComponent & BiConsumer<Player, Card>> void triggerEvent(Class<T> type){
        for(Card c : getCopyOf(Destination.INPLAY)){
            c.as(type).ifPresent(d -> d.accept(this, c));
        }


    }

    /**
     *Utilisé dans {@link Player#playTurn()} en début de tour.
     * <p>Cette méthode parcourt le inplay , puis lance l'action du composant {@link TriggerComponent.onStartTurn},
     * si l'{@link Optional} est vide la méthode ne fait rien et passe à la prochaine carte
     * </p>
     * @param type la classe du composant
     * @see Card#as(Class)
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

            Card chosen = chooseCardFromList("Start turn, you may play a Card?", card -> true, validReactions, true);

            if (chosen == null) break;

            alreadyRevealed.add(chosen);

            chosen.as(type).ifPresent(s -> s.accept(this));

        }
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
    public <T extends TriggerComponent.Immunity> boolean immunity(Class<T> type) {

        boolean inPlay = getCopyOf(Destination.INPLAY).stream()
                .anyMatch(card -> card.as(type).map(t -> t.immune(card)).orElse(false));

        if (inPlay) return true;

        return getCopyOf(Destination.HAND).stream().anyMatch(card -> card.hasComponent(type) && card.as(type).map(i -> i.revealed(this, card)).orElse(false));
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
     * @see Card#as(Class)
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

        return getCopyOf(Destination.HAND).stream()
                .flatMap(c -> c.as(ExtraTurnComponent.class).stream())
                .flatMap(comp -> comp.canUseExtraTurn().stream())
                .findFirst()
                .map(e -> {
                    e.consume(this);
                    isSecondTurn = true;
                    return true;
                }).orElse(false);
    }

    public void moveToTrash(Card c) {
        log(String.format("Trash %s ", c.getName()));
        //TODO mettre en place le trashTrigger ici

        if(controller != this){
            c.moveTo(get(Destination.ASIDE));
            mustBeDiscarded.add(c);
            return;
        }
        getGame().moveCardToTrash(c);
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
                top.moveTo(tempAside);
            }

            moveTo(card, Destination.DRAW);

            for (int j = tempAside.size() - 1; j >= 0; j--) {
                moveTo(tempAside.get(j), Destination.DRAW);
            }
        }
    }

    public void addCardEffect(Card c){
        activeEffect.add(c);
    }


}
