package fr.umontpellier.iut.dominion.cards.seaside;

import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.CardUtil;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Carte Corsaire (Corsair)
 * <p>
 * +2 Pièces
 * Au début de votre prochain tour, +1 Carte. D'ici là, chacun de vos
 * adversaires écarte le premier Argent ou Or qu'il joue à chaque tour.
 */
public class Corsair extends Card {
    public Corsair() {
        super("Corsair", 5, CardType.ACTION, CardType.DURATION, CardType.ATTACK);
        addComponent(new DurationComponent(0, p ->{
            CardUtil.TriggerEffect(p, 0,0,1,0,"Duration", this);
        }, this));

    }

    private final Map<Player, Integer> lastAttackTour = new HashMap<>();

    @Override
    public void play(Player p) {
        super.play(p);

        CardUtil.TriggerEffect(p, 2,0,0,0,"Effect", this);

        addComponent(TriggerComponent.OnCardPlayed.class, (owner, actor, card) -> {
            if (owner == actor) return;

            if (card.hasName("Silver") || card.hasName("Gold")) {
                int currentTurn = owner.getGame().getTurnNumber();

                if (lastAttackTour.getOrDefault(actor, -1) != currentTurn) {
                    if(owner.getGame().isImmune(this, actor))return;
                    actor.getGame().moveToTrash(card);
                    lastAttackTour.put(actor, currentTurn);
                    owner.log(String.format("TRIGGER %s : %s écarte %s ", getName().toUpperCase(), actor.getName(), card.getName().toUpperCase()));
                }
            }
        });
    }
}