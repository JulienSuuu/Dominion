package fr.umontpellier.iut.dominion;


import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;
import fr.umontpellier.iut.dominion.cards.component.ExtraTurnComponent;
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent;
import fr.umontpellier.iut.dominion.cards.component.TriggerEffect;
import fr.umontpellier.iut.dominion.cards.seaside.*;
import fr.umontpellier.iut.dominion.gui.Utils;

/**
 * Un joueur de Dominion
 */
public class Player {

    //region --VARIABLES--
    /**
     * Nom du joueur
     */
    private final String name;

    /**
     * Nombre d'actions disponibles
     */
    private int numberOfActions;

    /**
     * Nombre d'achats disponibles
     */
    private int numberOfBuys;

    /**
     * Nombre de pièces disponibles pour acheter des cartes
     */
    private int money;

    /**
     * Bonus ou malus sur le nombre de cartes à piocher
     * {@link fr.umontpellier.iut.dominion.cards.seaside.Outpost}
     */
    private int drawBonusNextTurn;


    /**
     * Jeton pièces du joueur {@link fr.umontpellier.iut.dominion.cards.seaside.PirateShip}
     */
    private int coins;

    /**
     * La partie en cours
     */
    private final Game game;

    /**
     * Liste des cartes dans la main du joueur
     */
    private final List<Card> hand;

    /**
     * Liste des cartes dans la défausse du joueur
     */
    private final List<Card> discard;

    /**
     * Liste des cartes dans la pioche du joueur (on considère que le dessus de
     * la pioche est à la fin de la liste)
     */
    private final List<Card> draw;

    /**
     * Listes des cartes qui ont été jouées pendant le tour courant
     */
    private final List<Card> inPlay;

    /**
     * Liste des cartes actuellement mises de côté par une action (par exemple Haven
     * of Blockade) qui ne sont pas considérées comme "in play" ni sur aucun des
     * tapis spéciaux du joueur (island mat, native village mat, etc.)
     */
    private final List<Card> cardsSetAside;

    /**
     * Liste des cartes mises de côté sur le plateau île (island mat) du joueur
     */
    private final List<Card> islandMat;

    /**
     * Liste des cartes mises de côté sur le plateau correspondant à la carte Native
     * Village du joueur
     */
    private final List<Card> nativeVillageMat;


    /**
     * Liste des cartes gagnées au tour précédent
     */
    private List<Card> CardGainedLastTurn;

    /**
     * Liste des cartes gagnées au tour en cours
     */
    private final List<Card> CardGainedCurrentTurn;
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
        // Prépare les listes de cartes
        hand = new ArrayList<>();
        discard = new ArrayList<>();
        draw = new ArrayList<>();
        inPlay = new ArrayList<>();
        cardsSetAside = new ArrayList<>();
        islandMat = new ArrayList<>();
        nativeVillageMat = new ArrayList<>();
        CardGainedLastTurn = new ArrayList<>();
        CardGainedCurrentTurn = new ArrayList<>();

        // Ajoute 3 Estate et 7 Copper (pris dans la réserve du jeu) dans la
        // défausse du joueur
        for (int i = 0; i < 3; i++)
            getCardFromSupply("Estate").moveTo(discard);
        for (int i = 0; i < 7; i++)
            getCardFromSupply("Copper").moveTo(discard);

        // Mélange la défausse, construit la pioche et pioche 5 cartes en main
        Collections.shuffle(discard);
        while (!discard.isEmpty()) {
            discard.getLast().moveTo(draw);
        }
        for (int i = 0; i < 5; i++) {
            draw.getLast().moveTo(hand);
        }
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
        return money;
    }

    public int getNumberOfActions() {
        return numberOfActions;
    }

    public int getNumberOfBuys() {
        return numberOfBuys;
    }

    public Game getGame() {
        return game;
    }

    /**
     * Renvoie une liste des cartes que le joueur a en main.
     * <p>
     * La liste renvoyée est une copie de la liste {@code hand} du joueur.
     * Elle contient les mêmes cartes mais une modification de la liste renvoyée ne
     * modifie pas la liste originale.
     */
    public List<Card> getCardsInHand() {
        return new ArrayList<>(hand);
    }

    /**
     * Renvoie une liste des cartes que le joueur a dans sa défausse.
     * <p>
     * La liste renvoyée est une copie de la liste {@code discard} du joueur.
     * Elle contient les mêmes cartes mais une modification de la liste renvoyée ne
     * modifie pas la liste originale.
     */
    public List<Card> getCardsInDiscard() {
        return new ArrayList<>(discard);
    }

    /**
     * Renvoie une liste des cartes que le joueur a dans sa pioche.
     * <p>
     * La liste renvoyée est une copie de la liste {@code draw} du joueur.
     * Elle contient les mêmes cartes mais une modification de la liste renvoyée ne
     * modifie pas la liste originale.
     * 
     */
    public List<Card> getCardsInDraw() {
        return new ArrayList<>(draw);
    }

    /**
     * Renvoie une liste des cartes que le joueur a en jeu.
     * <p>
     * La liste renvoyée est une copie de la liste {@code inPlay} du joueur.
     * Elle contient les mêmes cartes mais une modification de la liste renvoyée ne
     * modifie pas la liste originale.
     */
    public List<Card> getCardsInPlay() {
        return new ArrayList<>(inPlay);
    }

    /**
     * Renvoie une liste des cartes que le joueur a mises de côté.
     * <p>
     * La liste renvoyée est une copie de la liste {@code cardsSetAside} du joueur.
     * Elle contient les mêmes cartes mais une modification de la liste renvoyée ne
     * modifie pas la liste originale.
     */
    public List<Card> getCardsSetAside() {
        return new ArrayList<>(cardsSetAside);
    }

    /**
     * Renvoie une liste des cartes que le joueur a sur son plateau île (island
     * mat).
     * <p>
     * La liste renvoyée est une copie de la liste {@code islandMat} du joueur.
     * Elle contient les mêmes cartes mais une modification de la liste renvoyée ne
     * modifie pas la liste originale.
     */
    public List<Card> getCardsOnIslandMat() {
        return new ArrayList<>(islandMat);
    }

    /**
     * Renvoie une liste des cartes que le joueur a sur son plateau Native Village.
     * <p>
     * La liste renvoyée est une copie de la liste {@code nativeVillageMat} du
     * joueur.
     * Elle contient les mêmes cartes mais une modification de la liste renvoyée ne
     * modifie pas la liste originale.
     */
    public List<Card> getCardsOnNativeVillageMat() {
        return new ArrayList<>(nativeVillageMat);
    }

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
        allCards.addAll(hand);
        allCards.addAll(discard);
        allCards.addAll(draw);
        allCards.addAll(inPlay);
        allCards.addAll(cardsSetAside);
        allCards.addAll(islandMat);
        allCards.addAll(nativeVillageMat);
        return allCards;
    }

    /**
     * Renvoie le nombre total de points de victoire du joueur
     * <p>
     * Ce total est calculé en ajoutant les valeurs individuelles de toutes les
     * cartes possédées par le joueur (en utilisant la méthode
     * {@code getVictoryValue()}) des cartes
     */
    public int getVictoryPoints() {
        return getCardsInHand().stream().mapToInt(Card::getVictoryValue).sum();
    }

    /**
     * Incrémente le nombre de pièces du joueur ({@code money})
     *
     * @param n nombre de pièces à ajouter (ce nombre peut être négatif si l'on
     *          souhaite diminuer le nombre de pièces)
     */
    public void incrementMoney(int n) {
        money += n;
    }

    /** Incrémente le nombre d'actions d'achats du joueur ({@link Player#numberOfBuys})
     * @param value nombre d'action d'achat à ajouter
     */
    public void incrementBuyActions(int value){numberOfBuys += value;}

    /**
     * Incrémente le nombre d'action du joueur ({@link Player#numberOfActions})
     * @param value nombre d'action à ajouter
     */
    public void incrementActions(int value){numberOfActions += value;}

    /**
     * Incrémente, ou décrémente le nombre de cartes à piocher au prochain tour ({@link Player#drawBonusNextTurn})
     * @param value - nombre de cartes à piocher au prochain tour ({@link fr.umontpellier.iut.dominion.cards.seaside.Outpost}
     */
    public void updateDrawBonusValue(int value){
        if(drawBonusNextTurn == -2)return;
        drawBonusNextTurn += value;
    }

    /**
     * Incremente le nombre de jeton pièce du joueur ({@link Player#coins})
     * @param value nombre de Jeton à ajouter ({@link fr.umontpellier.iut.dominion.cards.seaside.PirateShip})
     */
    public void incrementCoin(int value){
        coins+=value;
    }

    public int getCoins(){
        return coins;
    }
    //endregion

    //region --METHODES MoveTo--
    /**
     * Déplace une carte dans la main du joueur.
     *
     * @param c la carte à déplacer
     */
    public void moveToHand(Card c) {
        c.moveTo(hand);
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
        if(draw.isEmpty()){
            shuffle();
        }
        return draw.getLast();
    }

    /**
     * Mélange la défausse puis remet les cartes dans la pioche
     */
    public void shuffle(){
        Collections.shuffle(discard);
        getCardsInDiscard().forEach(c -> c.moveTo(draw));
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
    public void discardFromHand(int number){
        if(hand.isEmpty())return;
        number = Math.min(number, hand.size());
        for(int i = 0; i < number; i++){
            Card c = chooseCardFromHand("Défausse " + (number-i) + " cartes", false );
            if(c != null)
                moveTo(c, Destination.DISCARD);
        }
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
        if(c == null)return;
        c.moveTo(inPlay);
        c.play(this);
        triggerEvent(TriggerComponent.OnCardPlayed.class, c);
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
        hand.stream()
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
    }

    /**
     * Le joueur gagne une carte et la place dans un emplacement donné
     * <p>Ajoute la carte dans {@link Player#CardGainedCurrentTurn}  </p>
     * <p>Cette méthode applique l'effet des cartes ayant le composant {@link fr.umontpellier.iut.dominion.cards.component.TriggerComponent.OnPlayerGain}
     * en appelant {@link Player#triggerEvent(Class, Card)}</p>
     *
     *
     * @param card carte gagnée
     * @param dest destination de la carte {@link Destination}
     * @see Player#gainTo(Destination, Card)
     */
    public void gain(Card card, Destination dest){
        CardGainedCurrentTurn.add(card);
        gainTo(dest, card);
        triggerEvent(TriggerComponent.OnPlayerGain.class, card);
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
        gainTo(dest, card);
    }

    /**
     * Place réellement la carte dans l'emplacement donnée
     * @param dest destination {@link Destination}
     * @param card carte gagnée
     * @see Player#gainTo(Card, List)
     */
    public void gainTo(Destination dest, Card card){
        switch(dest){
            case HAND -> gainTo(card, hand);
            case DRAW -> gainTo(card, draw);
            case ASIDE -> gainTo(card, cardsSetAside);
            case ISLAND -> gainTo(card, islandMat);
            case NATIVE ->  gainTo(card, nativeVillageMat);
            default -> gainTo(card, discard);
        }
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
                numberOfActions,
                money, numberOfBuys, draw.size(), discard.size());
        r += String.format("In play: %s\n", inPlay.toString());
        r += String.format("Hand: %s\n", hand.toString());
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
        joiner.add(String.format("\"actions\": %d", numberOfActions));
        joiner.add(String.format("\"money\": %d", money));
        joiner.add(String.format("\"buys\": %d", numberOfBuys));
        joiner.add(String.format("\"draw\": %s", Utils.toJSON(draw)));
        joiner.add(String.format("\"discard\": %s", Utils.toJSON(discard)));
        joiner.add(String.format("\"in_play\": %s", Utils.toJSON(inPlay)));
        joiner.add(String.format("\"hand\": %s", Utils.toJSON(hand)));
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
            game.prompt(instruction, choices, buttons, getIndex());
            String input = game.readLine();
            if (choices.contains(input)) {
                return input;
            }
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
        List<String> choices = hand.stream().filter(filter).map(c -> "HAND:" + c.getName())
                .collect(Collectors.toList());
        String choice = choose(instruction, choices, new ArrayList<>(), canPass);
        if (choice.startsWith("HAND:")) {
            return hand.stream()
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
     * @return
     */
    public Card chooseCardFromHand(String instruction, boolean canPass) {
        return chooseCardFromHand(instruction, c -> true, canPass);
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
        numberOfActions++;
        numberOfBuys++;

        boolean canPlayAction = true;
        boolean canPlayTreasure = true;

        triggerDurationCard();
        triggerStart(TriggerComponent.onStartTurn.class);
        while (true) {
            List<String> choices = new ArrayList<>();
            computeChoices(choices, canPlayAction, canPlayTreasure);

            String instruction = computeInstruction(canPlayAction, canPlayTreasure);

            String playCard = choose(instruction, choices, new ArrayList<>(), true);

            if (playCard.isEmpty()) break;

            if (playCard.startsWith("HAND:")) {
                Card play = hand.stream()
                        .filter(c -> c.hasName(playCard.split(":")[1]))
                        .findFirst()
                        .orElse(null);
                if (play == null) continue;

                if (play.hasType(CardType.ACTION)) {
                    numberOfActions--;

                }
                if (play.hasType(CardType.TREASURE)) {
                    canPlayAction = false;
                }

                playCard(play);

                if (numberOfActions == 0) {
                    canPlayAction = false;
                }
            }
            if (playCard.startsWith("SUPPLY:")) {
                Card play = getCardFromSupply(playCard.split(":")[1]);
                numberOfBuys--;
                canPlayAction = false;
                canPlayTreasure = false;
                money -= play.getCost();
                log(name + " a acheté " + play.getName());
                gain(play, Destination.DISCARD);
                if(getGame().hasToken(play.getName())){
                    gain(getCardFromSupply("Curse"),Destination.DISCARD);
                }
                if (numberOfBuys == 0) {
                    break;
                }
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
        for (Card c : hand) {
            if(canPlayAction && c.hasType(CardType.ACTION) && numberOfActions > 0){
                choices.add("HAND:" + c.getName());
            }
            if(canPlayTreasure && c.hasType(CardType.TREASURE)) {
                choices.add("HAND:" + c.getName());
            }
        }
        for(Card c : game.getAvailableSupplyCards()){
            if(money<c.getCost()) continue;
            choices.add("SUPPLY:" + c.getName());
        }
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

        numberOfBuys = 0;
        numberOfActions = 0;
        money = 0;

        getCardsInHand().forEach(c -> {c.moveTo(discard);});

        getCardsInPlay().stream().filter(c->
                        c.as(DurationComponent.class)
                                .map(DurationComponent::isFinished)
                                .orElse(true))
                .toList()
                .forEach(c -> c.moveTo(discard));


        int numberOfDraw = Math.max(5 + drawBonusNextTurn,0) ;
        draw(numberOfDraw);
        drawBonusNextTurn = 0;

        CardGainedLastTurn = new ArrayList<>(CardGainedCurrentTurn);
        CardGainedCurrentTurn.clear();

    }


    //endregion

    //region --TriggerComponent--
    /**
     *Utilisé dans {@link Player#playTurn()} en début de tour.
     * <p>Cette méthode parcourt le {@link Player#inPlay}, puis lance l'action du composant {@link DurationComponent}, 
     * si l'{@link Optional} est vide la méthode ne fait rien et passe à la prochaine carte
     * </p>
     * @see Card#as(Class)
     */

    private void triggerDurationCard() {
        getCardsInPlay()
                .forEach(c -> c.as(DurationComponent.class).ifPresent(d ->{
                    d.execute(this);
                    d.decrement();
                }));
    }


    /**
     * Lance l'effet (Trigger) de la carte sur tout les joueurs ( qui peut être soit-même)
     *
     * @param type classe du composant (triggerComponent & TriggerEffect
     * @param c la carte qui a été déclenché par une action d'un joueur
     * @param <T> le composant héritant de TriggerComponent et TriggerEffect qui sont des {@link FunctionalInterface}
     */
    public<T extends TriggerComponent & TriggerEffect> void triggerEvent(Class<T> type, Card c){
        getGame().notifyTrigger(type, this, c);
    }

    /**
     * Lance l'effet (Trigger) des cartes en jeu sur soit-même
     *
     * @param type classe du composant (triggerComponent & TriggerEffect
     * @param <T> le composant héritant de TriggerComponent et TriggerEffect qui sont des {@link FunctionalInterface}
     */
    public<T extends TriggerComponent & TriggerEffect> void triggerEvent(Class<T> type){
        for(Card c : getCardsInPlay()){
            c.as(type).ifPresent(d -> d.execute(this, this, c));
        }
    }

    /**
     *Utilisé dans {@link Player#playTurn()} en début de tour.
     * <p>Cette méthode parcourt le {@link Player#inPlay}, puis lance l'action du composant {@link TriggerComponent.onStartTurn},
     * si l'{@link Optional} est vide la méthode ne fait rien et passe à la prochaine carte
     * </p>
     * @param type la classe du composant
     * @see Card#as(Class)
     */
    public<T extends TriggerComponent & Consumer<Player>> void triggerStart(Class<T> type){
        getCardsInPlay().forEach(c-> c.as(type).ifPresent(t -> t.accept(this)));
    }

    /**
     * Lance une vérification sur l'état du joueur et si il est immunisé contre le type {@link CardType#ATTACK}
     *
     * @param type la classe du composant {@link fr.umontpellier.iut.dominion.cards.component.TriggerComponent.Immunity}
     * @return l'état du joueur {@code False} si il n'est pas immunisé {@code True} si il l'est
     * @param <T> le composant
     *           
     * @see Player#getCardsInPlay() 
     * @see Card#hasComponent(Class)
     * @see fr.umontpellier.iut.dominion.cards.seaside.Lighthouse
     */
    public<T extends TriggerComponent.Immunity> boolean immunity(Class<T> type){
        return getCardsInPlay().stream().anyMatch(card -> card.hasComponent(type));
    }

    /**
     * Regarde dans la main en jeu, si la carte comporte le composant {@link ExtraTurnComponent}
     * <p>Dans l'ordre</p>
     * <li> Stream {@link Player#getCardsInPlay()}</li>
     * <li> {@code flatMap()} Vérifie les cartes qui contiennent un {@code ExtraTurnComponent}
     * une par une  et Stream le résultat</li>
     * <li> {@code flatMap()} Vérifie si la carte peut déclencher l'effet {@link ExtraTurnComponent#canUseExtraTurn()} et retoune un nouveau Stream}</li>
     * <li> {@code findFirst()} Récupère le premier qui peut déclencher le résultat en tant que {@code Optional<ExtraTurnComponent>} </li>
     * <li> {@code map()} si L'Optional existe {@code Non Vide}, la map déclenche {@link ExtraTurnComponent#consume()} puis renvoie true}</li>
     * <li> {@code orElse()} retourne false si l'Optional est vide</li>
     *
     * @return si le joueur peut faire un tour en plus {@link Outpost}
     *
     * @see Card#as(Class)
     */
    public boolean triggerAnotherTurn() {
        return getCardsInPlay().stream()
                .flatMap(c -> c.as(ExtraTurnComponent.class).stream())
                .flatMap(comp -> comp.canUseExtraTurn().stream())
                .findFirst()
                .map(e -> {e.consume();
                    return true;
                }).orElse(false);
    }
    //--endregion
}
