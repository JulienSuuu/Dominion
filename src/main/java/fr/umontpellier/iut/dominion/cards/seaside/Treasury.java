package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.Button;
import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Carte Trésorerie (Treasury)
 * <p>
 * +1 Carte
 * +1 Action
 * +1 Pièce
 * À la fin de votre phase Achat, si vous n'avez pas reçu de carte Victoire
 * durant celle-ci, vous pouvez placer cette carte sur votre pioche.
 */
public class Treasury extends Card {
    public Treasury() {

        super("Treasury", 5, CardType.ACTION);
        addComponent(TriggerComponent.onEndBuy.class, (owner, victim, unused) -> {
            if (!owner.getCardsInPlay().contains(this)) return;

            boolean victory = owner.getCardGainedCurrentTurn()
                    .stream()
                    .anyMatch(card -> card.hasType(CardType.VICTORY));

            if(!victory){
                List<Button> buttons = new ArrayList<>();
                buttons.add(new Button("deck" , "y"));
                buttons.add(new Button("discard" , "n"));
                String choice = owner.chooseStringFromButtons("Voulez vous remettre la trésorerie sur la pioche ? ", buttons, true);

                if(choice.equals("y")){
                    owner.moveTo(this, Destination.DRAW);
                }
            }
        });
    }


    @Override
    public void play(Player p) {
        CardUtil.TriggerEffect(p, 1,1,1,0,"Effect", this);

    }
}
