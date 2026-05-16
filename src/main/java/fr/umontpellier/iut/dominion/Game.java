package fr.umontpellier.iut.dominion;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import fr.umontpellier.iut.dominion.Player.Player;
import fr.umontpellier.iut.dominion.Supply.SupplyPile;
import fr.umontpellier.iut.dominion.cards.*;
import fr.umontpellier.iut.dominion.cards.Events.Event;
import fr.umontpellier.iut.dominion.cards.Events.OnGainEvent;
import fr.umontpellier.iut.dominion.cards.component.CardSelector;
import fr.umontpellier.iut.dominion.cards.component.EventLink;
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent;
import fr.umontpellier.iut.dominion.cards.factories.Adventures.AdventuresFactory;
import fr.umontpellier.iut.dominion.cards.factories.Cornucopia_Guilds.CornucopiaRules;
import fr.umontpellier.iut.dominion.cards.factories.FactorySupplyPile;
import fr.umontpellier.iut.dominion.cards.factories.FactoryUtil;
import fr.umontpellier.iut.dominion.cards.factories.Hinterlands.HinterlandsRules;
import fr.umontpellier.iut.dominion.gui.UiStateService;
import fr.umontpellier.iut.dominion.gui.Utils;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Class représentant une partie de Dominion
 */
@Component
public class Game {
    private final ApplicationContext context;
    private final UiStateService uiStateService;
    private Card currentAlly;
    /**
     * Tableau contenant les joueurs de la partie
     */
    private List<Player> players;

    /**
     * Le joueur dont c'est actuellement le tour
     */
    private Player currentTurnPlayer;
    private final ObjectProperty<Player> currentTurnPlayerProperty = new SimpleObjectProperty<>(null);
    private final Map<String, ObservableList<String>> namedCard = new HashMap<>();
    private final Map<Class<? extends Event>, List<EventLink>> eventHandlers = new HashMap<>();
    private final List<String> kingdomsList = new ArrayList<>();
    private final Map<String, Boolean> expansionsAvailable = new HashMap<>();
    private final List<SupplyPile> events = new ArrayList<>();

    private final Map<String, Boolean> hasCards = new HashMap<>();
    private String Banes = "";
    /**
     * Numéro du tour courant (commence à 1 et est incrémenté à chaque fois que
     * le tour d'un nouveau joueur commence)
     */
    private int turnNumber = 1;
    private int sizeOfcommun = 7;
    private final IntegerProperty coffers = new SimpleIntegerProperty(35);

    /**
     * Messages envoyés dans le log du jeu (pour affichage dans l'interface
     * graphique)
     */
    private final ArrayList<String> logLines = new ArrayList<>();

    /**
     * Liste des piles dans la réserve du jeu.
     * <p>
     * On suppose ici que toutes les listes contiennent des copies de la même
     * carte. Ces piles peuvent être vides en cours de partie si toutes les
     * cartes de la pile ont été achetées ou gagnées par les joueurs.
     */
    private Map<String, SupplyPile> supplyPiles;
    private Map<String, List<SupplyPile>> asideSupplyPiles;

    /**
     * Liste des cartes qui ont été écartées (trash)
     */
    private List<Card> trashedCards;

    /**
     * Scanner permettant de lire les entrées au clavier
     */
    private Scanner scanner;

    /**
     * Constructeur
     */
    public Game(ApplicationContext context, UiStateService uiStateService) {
        this.context = context;
        this.uiStateService = uiStateService;
    }

    public void init(String [] playerNames, String[] kingdomPiles, Map<String, String[]> extras) {
        CornucopiaRules cornucopia = new CornucopiaRules(this);
        HinterlandsRules hinterland = new HinterlandsRules(this);
        trashedCards = new ArrayList<>();
        scanner = new Scanner(System.in);
        int nbPlayers = playerNames.length;

        List<SupplyPile> allPilesForSupply = new ArrayList<>();
        List<String> kingdomList = new ArrayList<>(Arrays.asList(kingdomPiles));
        if (extras != null && extras.containsKey("Banes")) {
            kingdomList.add(extras.get("Banes")[0]);
            Banes = extras.get("Banes")[0];
        }

        this.kingdomsList.addAll(Arrays.stream(kingdomPiles).toList());

        for(String name : kingdomList) {
            if(name.equals("Knights"))continue;
            allPilesForSupply.add(FactorySupplyPile.createSupplyPile(name, nbPlayers));
        }

        if(FactorySupplyPile.isExpansionRequired(kingdomList, FactoryUtil.DA)){
            SupplyPile ruins = FactorySupplyPile.createMixedSupplyPile(FactorySupplyPile.getMixedCards(CardType.RUINS));
            Collections.shuffle(ruins);
            allPilesForSupply.add(ruins);
        }

        if(kingdomList.contains("Knights")){
            SupplyPile knight = FactorySupplyPile.createMixedSupplyPile(FactorySupplyPile.getMixedCards(CardType.KNIGHT));
            Collections.shuffle(knight);
            allPilesForSupply.add(knight);
        }



        allPilesForSupply.sort(new PileComparator());

        allPilesForSupply.add(FactorySupplyPile.createSupplyPile("Copper", nbPlayers));
        allPilesForSupply.add(FactorySupplyPile.createSupplyPile("Silver", nbPlayers));
        allPilesForSupply.add(FactorySupplyPile.createSupplyPile("Gold", nbPlayers));
        allPilesForSupply.add(FactorySupplyPile.createSupplyPile("Estate", nbPlayers));
        allPilesForSupply.add(FactorySupplyPile.createSupplyPile("Duchy", nbPlayers));
        allPilesForSupply.add(FactorySupplyPile.createSupplyPile("Province", nbPlayers));
        allPilesForSupply.add(FactorySupplyPile.createSupplyPile("Curse", nbPlayers));

        if(FactorySupplyPile.isExpansionRequired(kingdomList, "Alchemy")){
            allPilesForSupply.add(FactorySupplyPile.createSupplyPile("Potion", nbPlayers));
            sizeOfcommun++;
        }
        if(FactorySupplyPile.isExpansionRequired(kingdomList, "Prosperity")){
            allPilesForSupply.add(FactorySupplyPile.createSupplyPile("Platinum", nbPlayers));
            allPilesForSupply.add(FactorySupplyPile.createSupplyPile("Colony", nbPlayers));
            sizeOfcommun+=2;
        }

        this.supplyPiles = new LinkedHashMap<>();
        for(SupplyPile pile : allPilesForSupply) {
            this.supplyPiles.put(pile.getName(), pile);
        }

        this.asideSupplyPiles = new HashMap<>();
        if (extras != null) {
            extras.forEach((key, value) -> {
                if(!key.equals("Banes")) {
                    for(String cardName : value) {
                        SupplyPile p = FactorySupplyPile.createSupplyPile(cardName, nbPlayers);
                        this.asideSupplyPiles.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
                    }
                }
            });
        }

        boolean required = FactorySupplyPile.shouldEnableSpecialty(kingdomList, FactoryUtil.DA, 5);



        List<SupplyPile> all = new ArrayList<>(allPilesForSupply);
        asideSupplyPiles.values().forEach(all::addAll);
        all.forEach((supplyPile) -> {
            hasCards.put(supplyPile.getName(), true);
        });

        if(hasCard("Peasant")){
            AdventuresFactory.getTraveller("Peasant").forEach(
                    t -> asideSupplyPiles.computeIfAbsent("Peasant upgrade", k -> new ArrayList<>()).add(FactorySupplyPile.createSupplyPile(t, nbPlayers))
            );
        }

        if(hasCard("Page")){
            AdventuresFactory.getTraveller("Page").forEach(
                    t -> asideSupplyPiles.computeIfAbsent("Page upgrade", k -> new ArrayList<>()).add(FactorySupplyPile.createSupplyPile(t, nbPlayers))
            );
        }


        if(hasCard("Joust")) {
            FactorySupplyPile.getMixedCards(CardType.REWARDS).forEach(reward ->
                    asideSupplyPiles.computeIfAbsent("Rewards", k -> new ArrayList<>()).add(FactorySupplyPile.createSupplyPile(reward, nbPlayers)));
        }

        if(hasCard("Bandit Camp") || hasCard("Marauder") || hasCard("Pillage"))asideSupplyPiles.computeIfAbsent("Dark Ages", k -> new ArrayList<>()).add(FactorySupplyPile.createSupplyPile("Spoils", nbPlayers));
        if(hasCard("Urchin"))asideSupplyPiles.computeIfAbsent("Dark Ages", k -> new ArrayList<>()).add(FactorySupplyPile.createSupplyPile("Mercenary", nbPlayers));
        if(hasCard("Hermit"))asideSupplyPiles.computeIfAbsent("Dark Ages", k -> new ArrayList<>()).add(FactorySupplyPile.createSupplyPile("Madman", nbPlayers));

        if(hasCard("Footpad")) {
          addListener(OnGainEvent.class, cornucopia::footpadPassive);
        }

        if(hasCard("Duchess")){
          addListener(OnGainEvent.class, hinterland::DuchessPassive);
        }

//        List<String> event = FactorySupplyPile.getMixedCards(CardType.EVENT);
//        Collections.shuffle(event);
//        event.subList(0,1).forEach(c -> events.add(FactorySupplyPile.createSupplyPile(c, nbPlayers)));

        events.add(FactorySupplyPile.createSupplyPile("Ferry", nbPlayers));

        players = FXCollections.observableArrayList();
        for (String playerName : playerNames) {
            Player p = context.getBean(Player.class);
            p.setSelf(p);
            p.init(playerName, this, required);
            players.add(p);
        }

        Player firstPlayer = players.getFirst();

        this.currentTurnPlayer = firstPlayer;
        this.currentTurnPlayerProperty.set(firstPlayer);

        GameStat.initialize(supplyPiles,currentTurnPlayerProperty, players);
        listener();
        specialEffectFromCard();
    }

    private void listener() {
        coffers.bind(
                Bindings.createIntegerBinding(
                        () -> {
                            int sum = players.stream()
                                    .mapToInt(p -> p.getPropertyOf(Item.COFFER).get())
                                    .sum();
                            return 100 - sum;
                        },
                        players.stream()
                                .map(p -> p.getPropertyOf(Item.COFFER))
                                .toArray(Observable[]::new)
                )
        );

    }

    private void specialEffectFromCard(){
        boolean containBaker = hasCard("Baker");
        if(containBaker){
            players.forEach(player -> {
                player.increment(Item.COFFER, 1);});
        }
        boolean containTradeRoute = hasCard("Trade Route");
        if(containTradeRoute){
            supplyPiles.values().forEach(s -> {
                if(s.getLast().hasType(CardType.VICTORY) && !s.getLast().hasType(CardType.KNIGHT)){
                    s.setToken(1);
                }
            });
        }
    }

    /**
     * Renvoie l'indice du joueur passé en argument dans le tableau des
     * joueurs, ou -1 si le joueur n'est pas dans le tableau.
     */
    public int getPlayerIndex(Player p) {
        if (p == null) return -1;

        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getName().equals(p.getName())) {
                return i;
            }
        }
        return -1;
    }

    public void generalCleanUp(){
        getPlayersStartingFrom(currentTurnPlayer)
                .stream().filter(player -> currentTurnPlayer != player)
                .forEach(Player::generalCleanUp);
    }

    public Card getCurrentAlly() {
        return null;
    }


    private record TypedHandler<T extends Event>(Class<T> type, Consumer<? super T> action) implements EventLink {
        @Override
        public void execute(Event e) {
            action.accept(type.cast(e));
        }
    }

    public <T extends Event> void addListener(Class<T> eventType, Consumer<? super T> action) {
        eventHandlers.computeIfAbsent(eventType, k -> new ArrayList<>())
                .add(new TypedHandler<>(eventType, action));
    }

    public <T extends Event> void fireEvent(Class<T> eventType, T event) {
        List<EventLink> links = eventHandlers.get(eventType);
        System.out.println(links);
        if (links != null) {
            for (EventLink link : links) {
                link.execute(event);
            }
        }
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
        return supplyPiles.values().stream()
                .filter(pile -> !pile.isEmpty())
                .map(SupplyPile::getLast).toList();
    }

    public List<Card> getEventCard(){
        return events.stream().filter(s ->!s.isEmpty()).map(SupplyPile::getLast).toList();
    }

    public List<Card> getAvailableAsidePilesCard(String name){
        if(!asideSupplyPiles.containsKey(name))return new ArrayList<>();
        return asideSupplyPiles.get(name).stream().filter(s -> !s.isEmpty()).map(SupplyPile::getLast).toList();
    }

    public Card getAvailableAsideCard(String nameCard, String key){
        for(Card card : getAvailableAsidePilesCard(key)){
            if(card.getName().equals(nameCard)){
                return card;
            }
        }
        return null;
    }

    public Card getSpecificAsideCard(String nameCard, String key){
        if(!asideSupplyPiles.containsKey(key))return null;
        for(SupplyPile pile : asideSupplyPiles.get(key)){
            for (Card card : pile){
                if(card.hasName(nameCard))return card;
            }
        }
        return null;
    }

    /**
     * Déplace une carte vers la pile de trash (écartée).
     * 
     * @param c la carte à écarter
     */
    public void moveCardToTrash(Card c) {
        c.moveTo(trashedCards, Destination.TRASH);
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
        for (List<Card> pile : supplyPiles.values())
            if (pile.isEmpty())
                joiner.add("[Empty pile]");
            else {
                Card c = pile.getLast();
                joiner.add(String.format("%s x%d(%d)", c.getName(), pile.size(), c.getCost()));
            }
        return title + joiner + "\n";
    }


    private List<Player> getPlayers() {
        return players;
    }
    private Player getCurrentPlayer() {
        return currentTurnPlayer;
    }
    private int getSizeOfcommun() {
        return sizeOfcommun;
    }
    private List<SupplyPile> getEventsPiles() {return events;}
    private List<String> logLine(){
        return logLines;
    }

    private List<SupplyPile> getSupplyPiles() {
        return supplyPiles.values().stream().toList();
    }

    private Map<String, List<SupplyPile>> getAsideSupplyPiles() {
        return  asideSupplyPiles;
    }

    /**
     * Méthode utilitaire pour l'interface graphique.
     * À NE PAS MODIFIER.
     */
    public String toJSON() {
        StringJoiner joiner = new StringJoiner(", ");
        joiner.add("\"turn_player\": " + getPlayers().indexOf(getCurrentPlayer()));
        StringJoiner kingdomJoiner = getStringJoiner(getSupplyPiles());
        String asideJoiner = getCategorizedAsideJson(getAsideSupplyPiles());
        joiner.add("\"supply\": [" + kingdomJoiner + "]");
        joiner.add("\"aside\": " + asideJoiner);
        StringJoiner eventJoiner = getStringJoiner(getEventsPiles());
        joiner.add("\"events\": [ " + eventJoiner + "]");
        joiner.add("\"size\": " + getSizeOfcommun());
        StringJoiner playersJoiner = new StringJoiner(", ");
        for (Player p : getPlayers()) {
            playersJoiner.add(p.toJSON());
        }
        joiner.add("\"players\": [" + playersJoiner + "]");
        joiner.add("\"log\": ["
                + String.join(", ", logLine().stream().map(s -> "\"" + s.replace("\"", "\\\"") + "\"").toList())
                + "]");
        return "{" + joiner + "}";
    }


    private String getCategorizedAsideJson(Map<String, List<SupplyPile>> asideMap) {
        if (asideMap == null || asideMap.isEmpty()) return "{}";

        StringJoiner categoriesJoiner = new StringJoiner(", ");

        for (Map.Entry<String, List<SupplyPile>> entry : asideMap.entrySet()) {
            String categoryName = entry.getKey();
            List<SupplyPile> piles = entry.getValue();

            StringJoiner cardsInCat = new StringJoiner(", ");
            for (SupplyPile p : piles) {
                cardsInCat.add(
                        "{\"card\": \"%s\", \"number\": %d, \"cost\": %d, \"potion\": %d, \"debt\": %d}"
                                .formatted(
                                        p.getName(),
                                        p.size(),
                                        p.getCost(),
                                        p.getPrice().potion(),
                                        p.getPrice().debt().get()
                                )
                );
            }

            // On ajoute "NomCategorie": [cartes...]
            categoriesJoiner.add("\"%s\": [%s]".formatted(categoryName, cardsInCat.toString()));
        }

        return "{" + categoriesJoiner.toString() + "}";
    }

    private StringJoiner getStringJoiner(List<SupplyPile> list) {
        StringJoiner kingdomJoiner = new StringJoiner(", ");
        for (SupplyPile pile : list) {
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
        SupplyPile pile = supplyPiles.get(cardName);

        if (pile == null) {
            pile = supplyPiles.values().stream()
                    .filter(p -> p.getName().equals(cardName))
                    .findFirst()
                    .orElse(null);
        }

        if (pile == null || pile.isEmpty()) return null;
        return pile.getLast();
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

    public <T extends TriggerComponent & BiConsumer<Event, Player>> void notifyTrigger(Class<T> triggerType, Player actor, Event event) {
        List<Player> orderedPlayers = getPlayersStartingFrom(actor);

        for (Player p : orderedPlayers) {
            p.getCopyOf(Destination.INPLAY).stream()
                    .filter(c ->( !c.hasType(CardType.REACTION) &&  c.canExecute(event, p, triggerType) && c.hasComponent(triggerType)))
                    .forEach(c ->processTrigger(c, triggerType, p, actor, event));

            Set<Card> alreadyRevealed = new HashSet<>();

            while (true) {
                List<Card> validReactions = p.getValidReactions(triggerType, event, alreadyRevealed);

                if (validReactions.isEmpty()) break;

                Optional<Card> chosen = p.chooseCardFromList("Reveal a Reaction?", card -> true, validReactions, true);

                if (chosen.isEmpty()) break;

                alreadyRevealed.add(chosen.get());

                processTrigger(chosen.get(), triggerType, p, actor, event);
            }
        }
    }

    private <T extends TriggerComponent & BiConsumer<Event, Player>> void processTrigger(Card c, Class<T> type, Player owner, Player actor, Event event) {
        if (isImmune(c, actor)) return;
        if(event.getDest() == null || event.getCard() == null)return;
        c.getComponent(type).ifPresent(trigger -> trigger.accept(event, owner));
    }

    public boolean isImmune(Card c, Player actor) {
        if(!c.hasType(CardType.ATTACK))return false;

        if(c.get("Players", Set.class).isPresent()){
            return c.get("Players", Set.class).get().contains(actor);
        }

        return actor.immunity(TriggerComponent.Immunity.class, c);
    }

    public void processGain(Player p, Card c, Destination dest, String nameCard){
        processAttack(
                p, c, victim -> Optional.ofNullable(victim.getCardFromSupply(nameCard)).ifPresent( card ->{
                    victim.gain(card, dest);
                })
        );
    }

    public void processHandDown(Player p, Card c, Destination dest, int toReach, boolean mayDiscard){

        Consumer<Player> logic = victim -> {
            while(victim.getCopyOf(Destination.HAND).size() > toReach){
                victim.chooseCardFromHand("Défausse encore " + (victim.getCopyOf(Destination.HAND).size() - toReach) + " carte(s)", false)
                        .ifPresent( card -> {
                            if(mayDiscard) victim.discard(card);
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
                        victim.trash(chosen);
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
        return p.chooseCardFromList("Move a card from a list", c -> true, treasure, false).orElse(null);

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
        int startIndex = getPlayerIndex(actor);
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
            generalCleanUp();
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
        prompt("Game over", new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0);
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
    public void prompt(String instruction, List<String> choices, List<String> allCards, List<Button> buttons, int activePlayerIndex) {
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
        String selectionCardsJson = allCards.stream()
                .filter(c -> c.startsWith("SELECT_CARD:"))
                .map(c -> "\"" + c + "\"")
                .collect(Collectors.joining(", ", "[", "]"));

        joiner.add("\"selection_cards\": " + selectionCardsJson);
        joiner.add("\"mode\":" + getUiStateService().isPromptActive());
        System.out.println("MODE ENVOYÉ : " + getUiStateService().isPromptActive());
        sendToUI(joiner.toString());
    }

    public void updateUI() {
        this.prompt("Mise à jour...", List.of(), List.of(), List.of(), players.indexOf(currentTurnPlayer));
    }

    private UiStateService getUiStateService() {
        return uiStateService;
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
        SupplyPile pile = supplyPiles.get(name);
        if(pile == null)return;
        pile.setCursed(1);

    }
    public boolean hasToken(String name){
        SupplyPile pile = supplyPiles.get(name);
        if(pile == null)return false;
        return pile.isCursed();
    }

    public int getToken(String name){
        SupplyPile pile = supplyPiles.get(name);
        if(pile == null)return 0;
        return pile.getCursed();
    }

    public boolean replaceCardInSupply(Card card, Card revealed){
        if(!card.hasSameNameAs(revealed)) return false;
        return replaceCardInSupply(card);
    }

    public boolean replaceCardInAsideSupply(Card card, String nameSupply){
        SupplyPile pile = asideSupplyPiles.get(nameSupply).stream().filter(p -> p.verifyName(card.getName())).findFirst().orElse(null);
        if(pile == null || pile.contains(card)) return false;
        pile.setCard(card);
        return true;
    }

    public boolean replaceCardInSupply(Card card){
        SupplyPile pile = supplyPiles.get(card.getName());
        if(pile == null || pile.contains(card)) return false;
        pile.setCard(card);
        return true;
    }

    public List<Card> getTrashCards(){
        return new ArrayList<>(trashedCards);
    }

    public Set<Player> scanImmunity(Player p) {
        return getPlayersStartingFrom(p).stream().filter(s -> s != p && s.immunity(TriggerComponent.Immunity.class, null)).collect(Collectors.toSet());
    }

    public ObservableList<String> getNamedCardsThisTurn(String key) {
        return namedCard.computeIfAbsent(key, k -> FXCollections.observableArrayList());
    }

    public int tradeRoute(Card c){
        if(!c.hasType(CardType.VICTORY))return 0;
        SupplyPile pile = supplyPiles.get(c.getName());
        if(pile == null) return 0;
        int i = pile.getToken();
        pile.setToken(0);
        return i;
    }


    public int getCoffers(){
        return coffers.get();
    }

    public boolean isActionPhase(){
        return currentTurnPlayer.getFlag("Action").get();
    }
    public String getBanes(){return Banes;}

    public boolean hasCard(String cardName){
        return hasCards.getOrDefault(cardName, false);
    }
    public boolean hasExpansion(String expansionName, int threshold) {
        if (!expansionsAvailable.getOrDefault(expansionName, false)) {
            boolean shouldEnable = FactorySupplyPile.shouldEnableSpecialty(kingdomsList, expansionName, threshold);
            if (shouldEnable) {
                expansionsAvailable.put(expansionName, true);
            }
        }

        return expansionsAvailable.getOrDefault(expansionName, false);
    }
    public boolean hasType(CardType cardType, int threshold){
        return supplyPiles.values().stream().filter(p -> p.hasType(cardType)).limit(threshold).count() == threshold;
    }


    public boolean verifyPileToken(String toCheck, String CardName){
        SupplyPile pile = supplyPiles.get(toCheck);
        if(pile == null){
            pile = supplyPiles.values()
                    .stream()
                    .filter(p -> p.verifyName(toCheck))
                    .findFirst()
                    .orElse(null);

        }
        if(pile == null) return false;
        return pile.verifyName(CardName);
    }

    public List<Card> getActionSupplyCards(){
        return supplyPiles
                .values().stream().filter(s -> s.hasType(CardType.ACTION) && !s.isEmpty())
                .map(List::getLast).toList();

    }

    public Card getEvent(String name){
        return events.stream().filter(s -> s.verifyName(name)).map(SupplyPile::getLast).findFirst().orElse(null);
    }
}