package fr.umontpellier.iut.dominion;

import java.util.*;
import java.util.stream.Collectors;

import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.FactorySupplyPile;
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent;
import fr.umontpellier.iut.dominion.cards.component.TriggerEffect;
import fr.umontpellier.iut.dominion.gui.Utils;

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

    /**
     * Numéro du tour courant (commence à 1 et est incrémenté à chaque fois que
     * le tour d'un nouveau joueur commence)
     */
    private int turnNumber = 1;

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

        // Création des joueurs
        players = new ArrayList<>(nbPlayers);
        for (String playerName : playerNames)
            players.add(new Player(playerName, this));
        currentTurnPlayer = players.getFirst();
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
    void moveCardToTrash(Card c) {
        c.moveTo(trashedCards);
    }
    public void moveToTrash(Card c) {
        log(String.format("Trash %s ", c.getName()));
        moveCardToTrash(c);
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
        StringJoiner kingdomJoiner = new StringJoiner(", ");
        for (SupplyPile pile : supplyPiles) {
            kingdomJoiner.add(
                    "{\"card\": \"%s\", \"number\": %d, \"cost\": %d}"
                            .formatted(pile.getName(), pile.size(), pile.getCost()));
        }
        joiner.add("\"supply\": [" + kingdomJoiner + "]");

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
     * Teste si la partie est terminée
     *
     * @return un booléen indiquant si la partie est terminée, c'est-à-dire si
     *         au moins l'une des deux conditions de fin suivantes est vraie
     *         - 3 piles ou plus de la réserve sont vides
     *         - la pile de Provinces de la réserve est vide
     */
    public boolean isFinished() {
        boolean provincesIsEmpty = supplyPiles.stream()
                .filter(pile -> pile.getName().equals("Province"))
                .findFirst()
                .map(SupplyPile::isEmpty)
                .orElse(false);

        if(provincesIsEmpty) return true;

        long nbQueueEmpty = supplyPiles
                .stream().
                filter(ArrayList::isEmpty)
                .limit(3)
                .count();

        return nbQueueEmpty >= 3;
    }

    /**
     * Passe au joueur suivant et incrémente le numéro du tour si nécessaire.
     * <p>
     * Cette méthode doit mettre à jour l'attribut {@code currentTurnPlayer} pour
     * qu'il référence le joueur dont c'est le tour après l'appel de la
     * méthode.
     */
    public void moveToNextPlayer() {
        boolean next = currentTurnPlayer.triggerAnotherTurn();
        if(next){
            turnNumber++;
            return;
        }

        int currentIndex = getPlayerIndex(currentTurnPlayer);
        int nextIndex = (currentIndex + 1)%players.size();
        turnNumber++;
        currentTurnPlayer = players.get(nextIndex);

    }

    public <T extends TriggerComponent & TriggerEffect> void notifyTrigger(Class<T> triggerType, Player actor, Card card) {
        for (Player p : players) {
            p.getCardsInPlay().forEach(c -> processTrigger(c, triggerType, p, actor, card));

            p.getCardsInHand().stream()
                    .filter(c -> c.hasType(CardType.REACTION))
                    .forEach(c -> processTrigger(c, triggerType, p, actor, card));
        }
    }

    private <T extends TriggerComponent & TriggerEffect> void processTrigger(Card c, Class<T> type, Player owner, Player actor, Card source) {
        if (isImmune(c, actor)) return;
        c.as(type).ifPresent(trigger -> trigger.execute(owner, actor, source));
    }

    public boolean isImmune(Card c, Player actor) {
        return c.hasType(CardType.ATTACK) && actor.immunity(TriggerComponent.Immunity.class);
    }

    public void processGain(Player p, Card c, Destination dest, String nameCard){
        players.stream().filter(victim -> victim != p && !isImmune(c, victim))
                .forEach(victim ->
                    Optional.ofNullable(victim.getCardFromSupply(nameCard)).ifPresent( card ->{
                            victim.gain(card, dest);
                            victim.log(String.format("%s Gain %s : %s.",victim.getName(), nameCard, card.getName()));
                    })
                );
    }

    public void processMoveTo( Player p, Card c, Destination dest){
        players.stream().filter(victim -> victim != p && !isImmune(c, victim) && victim.getCardsInHand().size()>= 4)
                .forEach( victim -> {
                    while(victim.getCardsInHand().size() > 3){
                        Optional.ofNullable(victim.chooseCardFromHand("Mets sur la pioche tant que tu as plus de 3 cartes en main", false))
                                .ifPresent( card -> {
                                    victim.moveTo(card, dest);
                                    p.log(String.format("Attack %s : %s met en pioche %s", c.getName().toUpperCase(), victim.getName(), card.getName().toUpperCase()));
                                    });
                    }
                });
    }

    public List<Card> processTreasureToTrash(Player p, Card c, int numberOfCard){
        return players.stream()
                .filter(victim -> victim != p && !isImmune(c, victim))
                .map(victim -> {

                    if(victim.getCardsInHand().size()<numberOfCard) victim.shuffle();

                    List<Card> revealed = victim.getCardsInDraw().reversed().stream().limit(numberOfCard).toList();
                    p.log(String.format("Attack %s: %s dévoile %s",
                            c.getName().toUpperCase(), victim.getName(), revealed));

                    List<Card> treasure = revealed.stream().filter(card -> card.hasType(CardType.TREASURE)).toList();

                    Card choosen = null;
                    if(!treasure.isEmpty()){
                        choosen = p.chooseCardFromButtons("Choisis un trésor à mettre dans la défausse commune", treasure, false );
                        victim.getGame().moveToTrash(choosen);
                    }

                    Card finalChoosen = choosen;
                    revealed.stream().filter(card -> card != finalChoosen).forEach(card -> victim.moveTo(card, Destination.DISCARD));

                    return Optional.ofNullable(finalChoosen);

                }).flatMap(Optional::stream).toList();
    }

    public void processDiscard(Player p, Card c){
        players.stream().filter(victim -> victim != p && !isImmune(c, victim))
                .forEach(victim ->
                        Optional.ofNullable(victim.getCardFromDeck())
                                .ifPresent(card -> victim.moveTo(card, Destination.DISCARD))
                );
    }

    public void discardOrShowHand(Player actor, String name, Card played){
        players.stream().filter(victim -> victim != actor && !isImmune(played, victim))
                .forEach(victim ->
                    victim.getCardsInHand()
                            .stream()
                            .filter(card -> card.hasName(name))
                            .findFirst()
                            .ifPresentOrElse(card -> {
                                victim.moveTo(card, Destination.DISCARD);
                                victim.log(String.format("TRIGGER %s : %s défausse %s", played.getName().toUpperCase(), victim.getName(), card.getName().toUpperCase() ));
                                }, () -> victim.log(String.format(" Main de %s : %s",victim.getName(), victim.getCardsInHand().toString()))
                            )
                );
    }


    public boolean onTheRight(Player actor, Player victim){
        int rightIndex =  (getPlayerIndex(actor) -1 + players.size()) % players.size();
        int ownerIndex = getPlayerIndex(victim);
        return rightIndex == ownerIndex;
    }

    public Player onTheRight(Player actor){
        for(Player p : players){
            if(p==actor) continue;
            if(onTheRight(actor, p)) return p;
        }
        return null;
    }

    /**
     * Boucle d'exécution d'une partie.
     * <p>
     * Cette méthode exécute les tours des joueurs jusqu'à ce que la partie soit
     * terminée. Lorsque la partie se termine, la méthode affiche le score
     * final et les cartes possédées par chacun des joueurs.
     */
    public void run() {
        while (!isFinished()) {
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
        // Envoie la version pour l'UI
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

    public boolean replaceCardInSupply(Card card, Card revealed){
        if(!card.hasSameNameAs(revealed))return false;
        supplyPiles.stream().filter(s -> s.getName().equals(revealed.getName()) && revealed.hasSameNameAs(card)).findFirst().map(s -> s.add(card));
        card.moveTo(new ArrayList<>());
        return true;
    }


}