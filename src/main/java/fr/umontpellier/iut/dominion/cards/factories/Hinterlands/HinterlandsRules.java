package fr.umontpellier.iut.dominion.cards.factories.Hinterlands;

import fr.umontpellier.iut.dominion.Button;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Game;
import fr.umontpellier.iut.dominion.Player.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.Events.OnGainEvent;

import java.util.List;

public class HinterlandsRules {
    private Game game;
    public HinterlandsRules(Game game) {
        this.game = game;
    }

    public void DuchessPassive(OnGainEvent event) {
        Player player = event.getPlayer();
        if(event.getCard().hasName("Duchy")){
            Card duchess = player.getCardFromSupply("Duchess");
            String choice = player.chooseWhatToDo("Do you want to gain a Duchess ?", List.of(duchess), Button.yesOrNo, true);
            if("y".equals(choice)){
                player.gain(duchess, Destination.DISCARD);
            }
        }
    }

}
