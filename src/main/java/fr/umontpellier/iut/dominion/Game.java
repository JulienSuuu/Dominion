package fr.umontpellier.iut.dominion;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import fr.umontpellier.iut.dominion.cards.*;
import fr.umontpellier.iut.dominion.cards.component.CardSelector;
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent;
import fr.umontpellier.iut.dominion.cards.component.TriggerEffect;
import fr.umontpellier.iut.dominion.gui.Utils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Class représentant une partie de Dominion
 */
public class Game {
    /**
     * Tableau contenant les joueurs de la partie
     */
    private final ArrayList<Player> players;

    /**
     * Le joueur dont c'est actuellement le tour
     */
    private Player currentTurnPlayer;
    private final ObjectProperty<Player> currentTurnPlayerProperty = new SimpleObjectProperty<>(null);
    private final Map<String, ObservableList<String>> namedCard = new HashMap<>();
    /**
     * Numéro du tour courant (commence à 1 et est incrémenté à chaque fois que
     * le tour d'un nouveau joueur commence)
     */
    private int turnNumber = 1;
    private int sizeOfcommun = 7;

    /**
     * Messages envoyés dans le log du jeu (pour affichage dans l'interface
     * graphique)
     */
    private ArrayList<String> logLines = new ArrayList<>();

    /**
     * Liste des piles dans la réserve du jeu.
     * <p>
     * On suppose ici que toutes les listes contiennent des copies de la même
     * carte. Ces piles peuvent être vides en cours de partie si toutes les
     * cartes de la pile ont été achetées ou gagnées par les joueurs.
     */
    private final List<SupplyPile> supplyPiles;

    /**
     * Liste des cartes qui ont été écartées (trash)
     */
    private final List<Card> trashedCards;

    /**
     * Scanner permettant de lire les entrées au clavier
     */
    private final Scanner scanner;

    /**
     * Constructeur
     *
     * @param playerNames  liste des noms des joueurs qui participent à la
     *                     partie. Le constructeur doit créer les objets
     *                     correspondant aux joueurs
     * @param kingdomPiles nom des cartes "royaume" à utiliser pour la partie
     */
    public Game(String[] playerNames, String[] kingdomPiles) {
        int nbPlayers = playerNames.length;
        trashedCards = new ArrayList<>();
        scanner = new Scanner(System.in);

        // Création des piles de réserve
        supplyPiles = new ArrayList<>();
        for (String cardName : kingdomPiles) {
            supplyPiles.add(FactorySupplyPile.createSupplyPile(cardName, nbPlayers));
        }
        supplyPiles.sort(new PileComparator());
        // Ajout des piles communes à la réserve
        supplyPiles.add(FactorySupplyPile.createSupplyPile("Copper", nbPlayers));
        supplyPiles.add(FactorySupplyPile.createSupplyPile("Silver", nbPlayers));
        supplyPiles.add(FactorySupplyPile.createSupplyPile("Gold", nbPlayers));
        supplyPiles.add(FactorySupplyPile.createSupplyPile("Estate", nbPlayers));
        supplyPiles.add(FactorySupplyPile.createSupplyPile("Duchy", nbPlayers));
        supplyPiles.add(FactorySupplyPile.createSupplyPile("Province", nbPlayers));
        supplyPiles.add(FactorySupplyPile.createSupplyPile("Curse", nbPlayers));

        List<String> Alchimy = RegistryName.getExtension("Alchemy");
        List<String> Prosperity = RegistryName.getExtension("Prosperity");

        boolean asAlchimy = supplyPiles.stream().anyMatch(p -> Alchimy.contains(p.getName()));
        boolean asProsperity = supplyPiles.stream().anyMatch(p -> Prosperity.contains(p.getName()));

        if(asAlchimy){
            supplyPiles.add(FactorySupplyPile.createSupplyPile("Potion", nbPlayers));
            sizeOfcommun++;
        }
        if(asProsperity){
            supplyPiles.add(FactorySupplyPile.createSupplyPile("Platinum", nbPlayers));
            supplyPiles.add(FactorySupplyPile.createSupplyPile("Colony", nbPlayers));
            sizeOfcommun++;
        }

        if(supplyPiles.stream().anyMatch(s -> s.getName().equals("Trade Route"))) {
            supplyPiles.forEach(s ->{
                if(s.getFirst().hasType(CardType.VICTORY)){
                    s.setToken(1);
                }
            });
        }

        // Création des joueurs
        players = new ArrayList<>(nbPlayers);
        for (String playerName : playerNames)
            players.add(new Player(playerName, this));
        currentTurnPlayer = players.getFirst();
        currentTurnPlayerProperty.set(currentTurnPlayer);

        GameStat.initialize(supplyPiles,currentTurnPlayerProperty);

    }

    /**
     * Renvoie l'indice du joueur passé en argument dans le tableau des
     * joueurs, ou -1 si le joueur n'est pas dans le tableau.
     */
    public int getPlayerIndex(Player p) {
        return players.indexOf(p);
    }

    public Player getCurrentTurnPlayer() {
        return currentTurnPlayer;
    }
    public int getTurnNumber() {return turnNumber;}
    /**
     * @return une liste de cartes contenant la carte du dessus (la dernière de la
     *         liste) de chaque pile non-vide de la réserve (cartes royaume et
     *         cartes communes)
     */
    public List<Card> getAvailableSupplyCards() {
        return supplyPiles.stream()
                .filter(pile -> !pile.isEmpty())
                .map(SupplyPile::getLast).toList();
    }

    /**
     * Déplace une carte vers la pile de trash (écartée).
     * 
     * @param c la carte à écarter
     */
    public void moveCardToTrash(Card c) {
        c.moveTo(trashedCards);
    }

    /**
     * Renvoie une représentation de l'état de la partie sous forme d'une chaîne
     * de caractères.
     * <p>
     * Cette représentation comporte
     * — le nom du joueur dont c'est le tour
     * — la liste des piles de la réserve en indiquant pour chacune :
     * — le nom de la carte
     * — le nombre de copies disponibles
     * — le prix de la carte entre parenthèses
     * si la pile n'est pas vide, ou "Empty pile" si la pile est vide.
     * <p>
     * On pourrait par exemple avoir l'affichage suivant :
     * <p>
     * -- Toto's Turn --
     * Ambassador x4(3) [Empty pile] Smugglers x5(3) Blockade x10(4) Navigator
     * x10(4) Sailor x8(4) Treasure Map x10(4) Outpost x10(5) Treasury x10(5) Wharf
     * x10(5) Copper x60(0) Silver x32(3) Gold x20(6) Estate x8(2) Duchy x8(5)
     * Province x2(8) Curse x4(0)
     */
    @Override
    public String toString() {
        String title = String.format("     -- %s's Turn --\n", currentTurnPlayer.getName());
        StringJoiner joiner = new StringJoiner("   ");
        for (List<Card> pile : supplyPiles)
            if (pile.isEmpty())
                joiner.add("[Empty pile]");
            else {
                Card c = pile.getLast();
                joiner.add(String.format("%s x%d(%d)", c.getName(), pile.size(), c.getCost()));
            }
        return title + joiner + "\n";
    }

    /**
     * Méthode utilitaire pour l'interface graphique.
     * À NE PAS MODIFIER.
     */
    public String toJSON() {
        StringJoiner joiner = new StringJoiner(", ");
        joiner.add("\"turn_player\": " + players.indexOf(currentTurnPlayer));
        StringJoiner kingdomJoiner = getStringJoiner();
        joiner.add("\"supply\": [" + kingdomJoiner + "]");
        joiner.add("\"size\": " + sizeOfcommun);
        StringJoiner playersJoiner = new StringJoiner(", ");
        for (Player p : players) {
            playersJoiner.add(p.toJSON());
        }
        joiner.add("\"players\": [" + playersJoiner + "]");
        joiner.add("\"log\": ["
                + String.join(", ", logLines.stream().map(s -> "\"" + s.replace("\"", "\\\"") + "\"").toList())
                + "]");
        return "{" + joiner + "}";
    }

    private StringJoiner getStringJoiner() {
        StringJoiner kingdomJoiner = new StringJoiner(", ");
        for (SupplyPile pile : supplyPiles) {
            kingdomJoiner.add(
                    "{\"card\": \"%s\", \"number\": %d, \"cost\": %d, \"potion\": %d, \"debt\": %d}"
                            .formatted(
                                    pile.getName(),
                                    pile.size(),
                                    pile.getCost(),
                                    pile.getPrice().potion(),
                                    pile.getPrice().debt().get()
                            )
            );
        }
        return kingdomJoiner;
    }

    /**
     * Renvoie une carte de la réserve dont le nom est passé en argument.
     *
     * @param cardName nom de la carte à trouver dans la réserve
     * @return la carte du dessus de la pile de réserve dont le nom est passé en
     *         argument ou {@code null} si aucune carte ne correspond (ou si la pile
     *         de cette carte est vide)
     */
    public Card getCardFromSupply(String cardName) {
        for (SupplyPile pile : supplyPiles)
            if (pile.getName().equals(cardName) && !pile.isEmpty()) {
                return pile.getLast();
            }
        return null;
    }

    /**
     * Passe au joueur suivant et incrémente le numéro du tour si nécessaire.
     * <p>
     * Cette méthode doit mettre à jour l'attribut {@code currentTurnPlayer} pour
     * qu'il référence le joueur dont c'est le tour après l'appel de la
     * méthode.
     */
    public void moveToNextPlayer() {
        namedCard.clear();
        turnNumber++;

        boolean anotherTurn = currentTurnPlayer.triggerAnotherTurn();

        if (!anotherTurn) {
            int currentIndex = getPlayerIndex(currentTurnPlayer);
            int nextIndex = (currentIndex + 1) % players.size();
            currentTurnPlayer = players.get(nextIndex);
            currentTurnPlayerProperty.set(currentTurnPlayer);
        } else {
            currentTurnPlayerProperty.set(null);
            currentTurnPlayerProperty.set(currentTurnPlayer);
        }
    }

    public <T extends TriggerComponent & TriggerEffect> void notifyTrigger(Class<T> triggerType, Player actor, Event event) {
        List<Player> orderedPlayers = getPlayersStartingFrom(actor);

        for (Player p : orderedPlayers) {
            p.getCopyOf(Destination.INPLAY).stream()
                    .filter(c ->( !c.hasType(CardType.REACTION) &&  c.canExecute(event, p) && c.hasComponent(triggerType)))
                    .forEach(c -> processTrigger(c, triggerType, p, actor, event));

            Set<Card> alreadyRevealed = new HashSet<>();

            while (true) {
                List<Card> validReactions = p.getCopyOf(Destination.HAND).stream()
                        .filter(c -> c.hasType(CardType.REACTION)
                                && c.canExecute(event, p)
                                && c.hasComponent(triggerType)
                                && !alreadyRevealed.contains(c))
                        .toList();

                if (validReactions.isEmpty()) break;

                Card chosen = p.chooseCardFromList("Reveal a Reaction?", card -> true, validReactions, true);

                if (chosen == null) break;

                alreadyRevealed.add(chosen);

                processTrigger(chosen, triggerType, p, actor, event);
            }
        }
    }

    private <T extends TriggerComponent & TriggerEffect> void processTrigger(Card c, Class<T> type, Player owner, Player actor, Event event) {
        if (isImmune(c, actor)) return;
        c.as(type).ifPresent(trigger -> trigger.execute(owner, actor, event));
    }

    public boolean isImmune(Card c, Player actor) {
        if(!c.hasType(CardType.ATTACK))return false;

        if(c.get("Players", Set.class) != null){
            return c.get("Players", Set.class).contains(actor);
        }

        return actor.immunity(TriggerComponent.Immunity.class);
    }

    public void processGain(Player p, Card c, Destination dest, String nameCard){
        processAttack(
                p, c, victim -> Optional.ofNullable(victim.getCardFromSupply(nameCard)).ifPresent( card ->{
                    victim.gain(card, dest);
                    victim.log(String.format("%s Gain %s : %s.",victim.getName(), nameCard, card.getName()));
                })
        );
    }

    public void processMoveTo( Player p, Card c, Destination dest, int number, boolean discard){

        Consumer<Player> logic = victim -> {
            while(victim.getCopyOf(Destination.HAND).size() > number){
                CardUtil.executeIfSelected(
                        () -> victim.chooseCardFromHand("Défausse encore " + (victim.getCopyOf(Destination.HAND).size() - number) + " carte(s)", false),
                        card -> {
                            if(discard) victim.discard(card);
                            else victim.moveTo(card, dest);
                            p.log(String.format("Attack %s : %s met en %s %s", c.getName().toUpperCase(), victim.getName(), dest.name().toLowerCase(), card.getName().toUpperCase()));
                        }
                );
            }
        };

        processAttack(p, c, logic);
    }

    public List<Card> processAttackWithReveal(Player attacker, Card attackCard, int count, Predicate<Card> filter, CardSelector selector) {
        return getPlayersStartingFrom(attacker).stream()
                .filter(v -> v != attacker && !isImmune(attackCard, v))
                .map(victim -> {
                    List<Card> revealed = CardUtil.getTopCards(victim, count);
                    attacker.log(String.format("Attack %s: %s dévoile %s",
                            attackCard.getName().toUpperCase(), victim.getName(), revealed));

                    List<Card> targets = revealed.stream().filter(filter).toList();

                    Card chosen = null;
                    if (!targets.isEmpty()) {
                        chosen = selector.select(attacker, victim, targets);
                        victim.moveToTrash(chosen);
                    }

                    Card finalChosen = chosen;
                    revealed.stream()
                            .filter(card -> card != finalChosen)
                            .forEach(card -> victim.moveTo(card, Destination.DISCARD));

                    return Optional.ofNullable(finalChosen);
                })
                .flatMap(Optional::stream)
                .toList();
    }


    public Card chooseACard(Player p, List<Card> treasure){
        return p.chooseCardFromList("Move a card from a list", c -> true, treasure, false);

    }

    public void processDiscard(Player p, Card c){
        processAttack(
                p,
                c,
                player -> Optional.ofNullable(player.getCardFromDeck())
                        .ifPresent(card -> player.moveTo(card, Destination.DISCARD))
        );
    }

    public void checkHandOrShow(Player attacker ,Card attackCard, Predicate<Card> targetFilter, BiFunction<Player, List<Card>, Optional<Card>> decisionLogic, Destination destination) {
        processAttack(attacker, attackCard, victim -> {
            List<Card> validCards = victim.getCopyOf(Destination.HAND).stream()
                    .filter(targetFilter).toList();

            decisionLogic.apply(victim, validCards).ifPresentOrElse(
                    chosen -> {
                        victim.discard(chosen);
                        attacker.log(String.format("Attack %s : %s déplace %s",
                                attackCard.getName().toUpperCase(), victim.getName(), chosen.getName().toUpperCase()));
                    },
                    () -> attacker.log(String.format("Main de %s : %s",
                            victim.getName(), victim.getCopyOf(Destination.HAND)))
            );
        });
    }


    public void processAttack(Player attacker, Card attackCard, Consumer<Player> attackLogic) {
        this.getPlayersStartingFrom(attacker).stream()
                .filter(victim -> victim != attacker && !isImmune(attackCard, victim))
                .forEach(attackLogic);
    }

    public void processBenefit(Player attacker, Consumer<Player> benefitLogic) {
        this.getPlayersStartingFrom(attacker).stream()
                .filter(victim -> victim != attacker)
                .forEach(benefitLogic);

    }

    public void processGlobalEffect(Player actor, Consumer<Player> effectLogic) {
        this.getPlayersStartingFrom(actor).forEach(effectLogic);
    }


    public boolean onTheRight(Player actor, Player victim){
        int rightIndex =  (getPlayerIndex(actor) -1 + players.size()) % players.size();
        int ownerIndex = getPlayerIndex(victim);
        return rightIndex == ownerIndex;
    }

    public Player onTheRight(Player actor){
        var index = getPlayerIndex(actor);
        return index == -1 ? null : players.get((players.size() + index - 1) % players.size());
    }

    private List<Player> getPlayersStartingFrom(Player actor) {
        List<Player> ordered = new ArrayList<>();
        int startIndex = players.indexOf(actor);
        for (int i = 0; i < players.size(); i++) {
            ordered.add(players.get((startIndex + i) % players.size()));
        }
        return ordered;
    }

    public Player onTheLeft(Player victim) {
        int index = getPlayerIndex(victim);
        int nextIndex = (index + 1) % players.size();
        return players.get(nextIndex);
    }

    /**
     * Boucle d'exécution d'une partie.
     * <p>
     * Cette méthode exécute les tours des joueurs jusqu'à ce que la partie soit
     * terminée. Lorsque la partie se termine, la méthode affiche le score
     * final et les cartes possédées par chacun des joueurs.
     */
    public void run() {
        while (!GameStat.isFinished.get()) {
            // joue le tour du joueur courant
            log("<div class=\"turn-title\">%s (turn %d)</div>".formatted(currentTurnPlayer.toLog(), turnNumber));
            currentTurnPlayer.playTurn();
            currentTurnPlayer.cleanup();
            moveToNextPlayer();
        }

        // affiche le score de chaque joueur dans le log et regroupe toutes les
        // cartes des joueurs dans leur main
        log("<div class=\"turn-title\">Game over</div>");
        for (Player p : players) {
            for (Card c : p.getAllOwnedCards()) {
                p.moveTo(c, Destination.HAND);
            }
            log("%s: %d Points".formatted(
                    p.toLog(),
                    p.getVictoryPoints()));
            log(Utils.toLog(p.getAllOwnedCards()));
        }
        // force un rafraîchissement de l'interface graphique
        prompt("Game over", new ArrayList<>(), new ArrayList<>(), 0);
    }

    /**
     * Envoie une chaîne de caractères à l'interface graphique
     * <p>
     * Cette méthode ne fait rien mais elle est utilisée par une sous-classe de
     * Game ({@code GameGUI}) qui communique avec l'interface graphique. Vous
     * ne devez pas l'utiliser ni la modifier.
     *
     * @param message chaîne de caractères à envoyer
     */
    public void sendToUI(String message) {
    }

    /**
     * Lit une ligne de l'entrée standard
     * <p>
     * C'est cette méthode qui doit être appelée à chaque fois qu'on veut lire
     * l'entrée clavier de l'utilisateur (par exemple dans Player.choose), ce
     * qui permet de n'avoir qu'un seul Scanner pour tout le programme.
     *
     * @return une chaîne de caractères correspondant à la ligne suivante de
     *         l'entrée standard (sans le retour à la ligne finale)
     */
    public String readLine() {
        return scanner.nextLine();
    }

    /**
     * Envoie l'état de la partie pour affichage aux joueurs et à l'UI avant de
     * faire un choix
     *
     * @param instruction l'instruction qui est donnée au joueur
     * @param choices     la liste des choix possibles à afficher à l'utilisateur
     * @param buttons     la liste des boutons à afficher à l'utilisateur
     */
    public void prompt(String instruction, List<String> choices, List<Button> buttons, int activePlayerIndex) {
        // Prépare la version affichée à l'utilisateur
        System.out.println("");
        System.out.println(toString());
        System.out.println(currentTurnPlayer.toString());
        String ligneInstruction = ">>> " + instruction + "<<<";
        System.out.println(ligneInstruction);

        // Prépare la représentation envoyée à l'UI
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        joiner.add("\"game\": " + toJSON());
        joiner.add("\"active_player\": " + activePlayerIndex);
        joiner.add("\"instruction\": \"" + instruction + "\"");
        joiner.add("\"choices\": "
                + choices.stream().map(c -> "\"" + c + "\"").collect(Collectors.joining(", ", "[", "]")));
        joiner.add("\"buttons\": " + buttons.stream()
                .map(b -> String.format("{\"label\": \"%s\", \"value\": \"%s\"}", b.label(), b.value()))
                .toList());
        String selectionCardsJson = choices.stream()
                .filter(c -> c.startsWith("SELECT_CARD:"))
                .map(c -> "\"" + c + "\"")
                .collect(Collectors.joining(", ", "[", "]"));

        joiner.add("\"selection_cards\": " + selectionCardsJson);
        sendToUI(joiner.toString());
    }

    /**
     * Ajoute un message dans le log du jeu qui est affiché dans l'interface
     * graphique. Le message peut contenir du HTML pour le formatage.
     * 
     * @param message
     */
    public void log(String message) {
        logLines.add(message);
    }

    public void setToken(String name){
        supplyPiles.stream().filter(s -> s.getName().equals(name)).findFirst().ifPresent(s -> {
            s.setCursed(1);
        });
    }
    public boolean hasToken(String name){
        return supplyPiles.stream().filter(s -> s.getName().equals(name)).findFirst().map(SupplyPile::isCursed).orElse(false);

    }

    public int getToken(String name){
        return supplyPiles.stream().filter(s -> s.getName().equals(name)).findFirst().map(SupplyPile::getCursed).orElse(0);
    }

    public boolean replaceCardInSupply(Card card, Card revealed){
        if(!card.hasSameNameAs(revealed)) return false;
        supplyPiles.stream().filter(s -> s.getName().equals(revealed.getName()) && revealed.hasSameNameAs(card)).findFirst().ifPresent(s -> s.setCard(card));
        return true;
    }

    public List<Card> getTrashCards(){
        return new ArrayList<>(trashedCards);
    }

    public Set<Player> scanImmunity(Player p) {
        return getPlayersStartingFrom(p).stream().filter(s -> s != p && s.immunity(TriggerComponent.Immunity.class)).collect(Collectors.toSet());
    }

    public ObservableList<String> getNamedCardsThisTurn(String key) {
        return namedCard.computeIfAbsent(key, k -> FXCollections.observableArrayList());
    }

    public int tradeRoute(Card c){
        if(!c.hasType(CardType.VICTORY))return 0;


        return supplyPiles.stream().filter(s -> s.getName().equals(c.getName()) && s.hasToken()).findFirst().map(s ->{
            int i = s.getToken();
            s.setToken(0);
            return i;
        }).orElse(0);
    }
}