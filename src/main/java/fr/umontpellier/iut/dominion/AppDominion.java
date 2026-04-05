package fr.umontpellier.iut.dominion;

import java.io.IOException;
import java.util.*;

import fr.umontpellier.iut.dominion.cards.RegistryName;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Session;

import org.glassfish.tyrus.server.Server;

import fr.umontpellier.iut.dominion.gui.GameGUI;
import fr.umontpellier.iut.dominion.gui.WebSocketClient;

public class AppDominion {
    private final static ArrayList<Session> clients = new ArrayList<>();
    private static String gameState;
    private static GameGUI game;

    public static void main(String[] args) {
        // Liste de toutes les cartes disponibles
        List<String> allKingdomCards = RegistryName.getExtension("Seaside");
        Collections.shuffle(allKingdomCards);

        // Noms des joueurs
        String[] playerNames = new String[]{"Marco", "Polo"};
        // Cartes royaume à utiliser
        // Option 1. Aucune carte royaume (uniquement les cartes communes)
        //String[] kingdomCards = new String[]{"Astrolabe", "Bazaar", "Blockade"};

        // Option 2. Liste explicite de cartes royaume à utiliser (le nombre de cartes peut être quelconque)
        // String[] kingdomCards = new String[]{"Ambassador", "Blockade", "Pirate", "Sailor"};

        // Option 3. Choix aléatoire de 10 cartes parmi la liste complète allKingdomCards définie précédemment
        String[] kingdomCards = allKingdomCards.toArray(new String[0]);

        // Lancement de la partie
        game = new GameGUI(playerNames, kingdomCards);

        // Prépare le serveur websocket
        Server server = new Server(
                "localhost",
                3232,
                "/",
                null,
                Collections.singleton(WebSocketClient.class)
        );
        try (Scanner scanner = new Scanner(System.in)) {
            server.start(); // lance le serveur
            new Thread(game).start();   // démarre le jeu
            while (true) {
                game.addInput(scanner.nextLine());
            }
        } catch (DeploymentException e) {
            throw new RuntimeException(e);
        } finally {
            server.stop();
        }
    }

    public static void addInput(String message) {
        game.addInput(message);
    }

    public static void updateGameState(String message) {
        gameState = message;
        // Envoie l'état de la partie à tous les clients
        try {
            for (Session session : clients) {
                session.getBasicRemote().sendText(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addClient(Session session) {
        AppDominion.clients.add(session);
        // Envoie l'état actuel de la partie au nouveau client
        try {
            session.getBasicRemote().sendText(gameState);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void removeClient(Session session) {
        AppDominion.clients.remove(session);
    }
}
