package fr.umontpellier.iut.dominion;

import java.io.IOException;
import java.util.*;

import fr.umontpellier.iut.dominion.cards.factories.FactorySupplyPile;
import fr.umontpellier.iut.dominion.gui.GameGUI;
import fr.umontpellier.iut.dominion.gui.WebSocketClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import jakarta.websocket.Session;
import jakarta.websocket.DeploymentException;
import org.glassfish.tyrus.server.Server;

import java.util.*;
import java.util.stream.Collectors;

@SpringBootApplication
@EnableAspectJAutoProxy
public class AppDominion implements CommandLineRunner {

    private final static ArrayList<Session> clients = new ArrayList<>();
    private static String gameState;
    private static AppDominion instance;

    private final GameGUI game;

    public AppDominion(GameGUI game) {
        this.game = game;
    }

    public static void main(String[] args) {
        SpringApplication.run(AppDominion.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        instance = this;
        FactorySupplyPile.loadAllCards();

        // On récupère les données formatées
        String availableJson = formatMapToJson(FactorySupplyPile.getCardsByExtension());
        String presetsJson = formatNestedMapToJson(FactorySupplyPile.getPreSets());

        String selectedCardsJson = "[]";

        gameState = "{" +
                "\"view\":\"LOBBY\"," +
                "\"availableCards\":" + availableJson + "," +
                "\"presets\":" + presetsJson + "," +
                "\"selectedCards\":" + selectedCardsJson + "," +
                "\"game\":null" +
                "}";

        Server server = new Server("localhost", 3232, "/", null, Collections.singleton(WebSocketClient.class));

        try {
            server.start();
            System.out.println("--- Serveur démarré : En attente de configuration sur le Hub ---");

            while (true) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.stop();
        }
    }

    private static String[] playerNames = new String[]{"Marco", "Polo"};
    private static List<String> currentSelected = new ArrayList<>();
    private static Map<String, String[]> setupChoices = new HashMap<>();
    private static List<String> setupTasks = new ArrayList<>();
    private static String presetExtraCard = null;

    public static void addInput(String message) {
        if (instance == null) return;

        if (message.startsWith("CONFIRM_FERRYMAN:")) {
            String chosenCard = message.split(":")[1];
            setupChoices.put("Ferryman", new String[]{chosenCard});


            if (chosenCard.equals("Young Witch")) {
                setupTasks.addFirst("YOUNG_WITCH");
            }

            processNextSetupTask();
            return;
        }

        if (message.startsWith("CONFIRM_YOUNG_WITCH:")) {
            setupChoices.put("Banes", new String[]{message.split(":")[1]});
            processNextSetupTask();
            return;
        }

        if (message.equals("CLEAR_CARDS")) {
            currentSelected.clear();
            presetExtraCard = null;
            updateHubState();
        }

        else if (message.startsWith("SET_CARDS:")) {
            String content = message.substring(10);
            String[] cards = content.split(",");

            currentSelected.clear();
            presetExtraCard = null;

            for (int i = 0; i < cards.length; i++) {
                String cardName = cards[i].trim();
                if (i < 10) {
                    currentSelected.add(cardName);
                } else {
                    presetExtraCard = cardName;
                }
            }
            updateHubState();
        }
        else if (message.startsWith("TOGGLE:")) {
            String cardName = message.substring(7);

            if (currentSelected.contains(cardName)) {
                currentSelected.remove(cardName);
            } else if (currentSelected.size() < 10) {
                currentSelected.add(cardName);
            }

            updateHubState();

        } else if (message.equals("START_GAME")) {
            if (currentSelected.size() >= 10) {
                prepareSetupTasks();
            }
        } else {
            if (instance.game != null) {
                instance.game.addInput(message);
            }
        }
    }

    private static void prepareSetupTasks() {
        setupTasks.clear();
        setupChoices.clear();

        if (currentSelected.contains("Ferryman") && presetExtraCard != null) {
            setupChoices.put("Ferryman", new String[]{presetExtraCard});
        } else if (currentSelected.contains("Ferryman")) {
            setupTasks.add("FERRYMAN");
        }

        if (currentSelected.contains("Young Witch") && presetExtraCard != null) {
            setupChoices.put("Banes", new String[]{presetExtraCard});
        } else if (currentSelected.contains("Young Witch")) {
            setupTasks.add("YOUNG_WITCH");
        }

        processNextSetupTask();
    }

    private static void processNextSetupTask() {
        if (setupTasks.isEmpty()) {
            launchGame();
            return;
        }

        String currentTask = setupTasks.removeFirst();

        if (currentTask.equals("FERRYMAN")) {
            sendChoiceRequest("FERRYMAN_CHOICE", FactorySupplyPile.getFerrymanOptions(currentSelected));
        } else if (currentTask.equals("YOUNG_WITCH")) {
            sendChoiceRequest("YOUNG_WITCH_CHOICE", FactorySupplyPile.getYoungWitchOptions(currentSelected));
        }
    }

    private static void sendChoiceRequest(String viewName, Map<String, List<String>> options) {
        String optionsJson = options.entrySet().stream()
                .map(entry -> {
                    String cards = entry.getValue().stream()
                            .map(card -> "\"" + card + "\"")
                            .collect(Collectors.joining(",", "[", "]"));
                    return "\"" + entry.getKey() + "\":" + cards;
                })
                .collect(Collectors.joining(",", "{", "}"));

        String msg = String.format("{\"view\":\"%s\", \"options\":%s}", viewName, optionsJson);
        updateGameState(msg);
    }

    private static void launchGame() {
        System.out.println("Lancement du jeu avec setup spécifique...");
        String[] kingdomCards = currentSelected.stream()
                .limit(10)
                .toArray(String[]::new);
        instance.game.init(playerNames, kingdomCards, new HashMap<>(setupChoices));

        String msg = "{\"view\":\"GAME\"}";
        updateGameState(msg);
        new Thread(instance.game).start();
    }

    private static void updateHubState() {
        Map<String, List<String>> available = FactorySupplyPile.getCardsByExtension();
        Map<String, Map<String, List<String>>> presets = FactorySupplyPile.getPreSets();
        List<String> selected = currentSelected;
        if(presetExtraCard != null) {
            selected.add(presetExtraCard);
        }

        String availableJson = formatMapToJson(available);
        String presetsJson = formatNestedMapToJson(presets);
        String selectedJson = selected.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",", "[", "]"));

        String newGameState = "{" +
                "\"view\":\"LOBBY\"," +
                "\"availableCards\":" + availableJson + "," +
                "\"presets\":" + presetsJson + "," +
                "\"selectedCards\":" + selectedJson + "," +
                "\"game\":null" +
                "}";

        updateGameState(newGameState);
    }

    private static String formatMapToJson(Map<String, List<String>> map) {
        return map.entrySet().stream()
                .map(e -> "\"" + e.getKey() + "\":[" +
                        e.getValue().stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",")) + "]")
                .collect(Collectors.joining(",", "{", "}"));
    }

    private static String formatNestedMapToJson(Map<String, Map<String, List<String>>> map) {
        return map.entrySet().stream()
                .map(expansionEntry -> {
                    String expansionName = expansionEntry.getKey();
                    Map<String, List<String>> sets = expansionEntry.getValue();

                    String setsJson = sets.entrySet().stream()
                            .map(setEntry -> "\"" + setEntry.getKey() + "\":[" +
                                    setEntry.getValue().stream()
                                            .map(card -> "\"" + card + "\"")
                                            .collect(Collectors.joining(",")) + "]")
                            .collect(Collectors.joining(",", "{", "}"));

                    return "\"" + expansionName + "\":" + setsJson;
                })
                .collect(Collectors.joining(",", "{", "}"));
    }

    public static void updateGameState(String message) {
        gameState = message;
        synchronized (clients) {
            Iterator<Session> it = clients.iterator();
            while (it.hasNext()) {
                Session session = it.next();
                try {
                    if (session.isOpen()) {
                        session.getBasicRemote().sendText(message);
                    } else {
                        it.remove();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void addClient(Session session) {
        synchronized (clients) {
            clients.add(session);
        }
        try {
            if (gameState != null) {
                session.getBasicRemote().sendText(gameState);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void removeClient(Session session) {
        synchronized (clients) {
            clients.remove(session);
        }
    }

    public static Game getGame() {
        return instance.game;
    }
}