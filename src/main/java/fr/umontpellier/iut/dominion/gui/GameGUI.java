package fr.umontpellier.iut.dominion.gui;

import fr.umontpellier.iut.dominion.AppDominion;
import fr.umontpellier.iut.dominion.Game;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
@Component
public class GameGUI extends Game implements Runnable {

    private LinkedBlockingQueue<String> inputQueue;

    private final UiStateService uiStateService;
    private final ApplicationContext context;

    public GameGUI(UiStateService uiStateService, ApplicationContext context) {
        super(context,  uiStateService);
        this.uiStateService = uiStateService;
        this.context = context;

    }

    public void init(String[] playerNames, String[] kingdomPiles, Map<String, String[] > otherPiles) {
        super.init(playerNames, kingdomPiles, otherPiles);
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
