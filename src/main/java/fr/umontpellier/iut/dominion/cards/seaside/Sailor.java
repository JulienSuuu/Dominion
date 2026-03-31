package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.Button;
import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Carte Navigatrice (Sailor)
 * <p>
 * +1 Action
 * Une fois durant ce tour, quand vous recevez une carte Durée (Duration),
 * vous pouvez la jouer.
 * Au début de votre prochain tour, +2 Pièces et vous pouvez écarter une carte
 * de votre main.
 */
public class Sailor extends Card {
    public Sailor() {

        super("Sailor", 4, CardType.ACTION, CardType.DURATION);
        addComponent(new DurationComponent(0, p -> {
            CardUtil.TriggerEffect(p, 2,0,0,0, "Duration", this);
            Card choice = p.chooseCardFromHand("Choisie une carte à écarter", true);
            if(choice!= null){
                p.getGame().moveToTrash(choice);
            }
        }, this));
    }

    @Override
    public void play(Player p) {
        super.play(p);

        CardUtil.TriggerEffect(p, 0,1,0,0,"Effect", this);
        AtomicBoolean alreadyUsed = new AtomicBoolean(false);

        addComponent(TriggerComponent.OnPlayerGain.class, (owner, victims, c) -> {
            if(c.hasType(CardType.DURATION) && owner == victims && !alreadyUsed.get()){
                List<Button> buttons = new ArrayList<>();
                buttons.add(new Button("play", "y"));
                buttons.add(new Button("skip", "n"));

                String choice = owner.chooseStringFromButtons("Veux-tu jouer ta carte " + c.getName(), buttons, true);
                if(choice.equals("y")){
                    p.playCard(c);
                    alreadyUsed.set(true);
                }

            }
        });

    }
}
