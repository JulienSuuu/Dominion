package fr.umontpellier.iut.dominion.cards.factories.Cornucopia_Guilds;

import fr.umontpellier.iut.dominion.Game;
import fr.umontpellier.iut.dominion.cards.Events.Event;
import fr.umontpellier.iut.dominion.cards.Events.OnGainEvent;

public class CornucopiaRules {
    private Game game;

    public CornucopiaRules(Game game) {
        this.game = game;
    }

    public void footpadPassive(OnGainEvent event) {
        if (game.isActionPhase()) {
            event.getPlayer().draw(1);
            game.log("Règle Footpad : +1 Carte piochée.");
        }
    }
}
