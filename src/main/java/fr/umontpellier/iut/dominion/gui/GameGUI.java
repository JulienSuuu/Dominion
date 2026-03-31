package fr.umontpellier.iut.dominion.gui;

import fr.umontpellier.iut.dominion.AppDominion;
import fr.umontpellier.iut.dominion.Game;

import java.util.concurrent.LinkedBlockingQueue;

public class GameGUI extends Game implements Runnable {

    private final LinkedBlockingQueue<String> inputQueue;

    public GameGUI(String[] playerNames, String[] kingdomPiles) {
        super(playerNames, kingdomPiles);
        this.inputQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public void sendToUI(String message) {
        AppDominion.updateGameState(message);
    }

    public void addInput(String message) {
        inputQueue.add(message);
    }

    @Override
    public String readLine() {
        try {
            return inputQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
