package fr.umontpellier.iut.dominion.cards;

import fr.umontpellier.iut.dominion.Button;
import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;
import fr.umontpellier.iut.dominion.cards.component.OnPlayComponent;
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class SeaSideFactory {

    public static Card Ambassador(){
        Card c = new Card("Ambassador", 3, CardType.ACTION, CardType.ATTACK);
        c.addComponent(OnPlayComponent.class, p -> {
            Card choice = p.chooseCardFromHand("Dévoile une carte de ta main", false);

            if(choice != null){
                p.log(p.getName() + " a dévoilé " + choice.getName());
                    for(int i = 0; i < 2; i++){
                        Card choosed = p.chooseCardFromHand("Choisi la même carte à remetttre dans la réserve", choice::hasSameNameAs, true);
                        if(choosed == null) break;
                        if(!p.getGame().replaceCardInSupply(choosed, choice)){
                            i--;
                        }
                    }
                p.getGame().processGain(p, c, Destination.DISCARD, choice.getName());
            }
        });
        return c;
    }

    public static Card Astrolabe(){
        Card c = new Card("Astrolabe", 3, CardType.TREASURE, CardType.DURATION);
        c.addComponent(OnPlayComponent.class, p -> {
            CardUtil.TriggerEffect(p, 1, 0, 0, 1, "Effect", c);
        });

        c.addComponent(new DurationComponent(0,
                player -> CardUtil.TriggerEffect(player, 1, 0, 0, 1, "Duration", c)
                , c));

        return c;
    }

    public static Card Bazaar(){
        Card c = new Card("Bazaar", 5, CardType.ACTION);
        c.addComponent(OnPlayComponent.class, p -> CardUtil.TriggerEffect(p, 1, 2, 1, 0 ,"Effect", c));
        return c;
    }

    public static Card Blockade(){
        Card c = new Card("Blockade", 4, CardType.ACTION, CardType.DURATION, CardType.ATTACK);
        final AtomicReference<Card> blocked = new AtomicReference<>();

        c.addComponent(OnPlayComponent.class, p -> {
            Card choice = p.chooseCardFromSupply("Choississez une carte qui coûte au maximum 4 de pièces", card -> card.getCost() > 0 && card.getCost()<=4, false );
            if(choice != null){
                blocked.set(choice);
                p.gainSilent(choice, Destination.ASIDE, true);
                p.log(String.format("%s bloquée", choice.getName().toUpperCase()));

            }
        });

        c.addComponent(new DurationComponent(0, player -> {
            Optional.ofNullable(blocked.get()).ifPresent( card -> player.moveTo(card, Destination.HAND));
            blocked.set(null);
        } , c));

        c.addComponent(TriggerComponent.OnPlayerGain.class, (owner, victim, c1) -> {
            if(blocked.get() != null && c1.hasSameNameAs(blocked.get()) && victim!= owner){
                victim.gain(victim.getCardFromSupply("Curse"), Destination.DISCARD);
                owner.log(String.format("TRIGGER %s : %s gagne une malédiction ", c.getName().toUpperCase(), victim.getName()));
            }
        });

        return c;
    }

    public static Card Caravan(){
        Card c = new Card("Caravan", 4, CardType.ACTION, CardType.DURATION);
        c.addComponent(OnPlayComponent.class, p -> {
            CardUtil.TriggerEffect(p, 0,1,1,0,"Effect", c);
        });

        c.addComponent(new DurationComponent(0, p ->{
            CardUtil.TriggerEffect(p, 0,0,1,0,"Duration", c);
        },c));

        return c;
    }

    public static Card Corsair(){
        Card c = new Card("Corsair", 5, CardType.ACTION, CardType.DURATION, CardType.ATTACK);

        final Map<Player, Integer> lastAttackTour = new HashMap<>();

        c.addComponent(OnPlayComponent.class, p -> {
            CardUtil.TriggerEffect(p, 2,0,0,0,"Effect", c);
        });

        c.addComponent(new DurationComponent(0, player -> CardUtil.TriggerEffect(player, 0,0,1,0,"Duration", c), c));
        c.addComponent(TriggerComponent.OnCardPlayed.class, (owner, actor, card) -> {
            if (owner == actor) return;

            if (card.hasName("Silver") || card.hasName("Gold")) {
                int currentTurn = owner.getGame().getTurnNumber();

                if (lastAttackTour.getOrDefault(actor, -1) != currentTurn) {
                    if(owner.getGame().isImmune(c, actor))return;
                    actor.getGame().moveToTrash(card);
                    lastAttackTour.put(actor, currentTurn);
                    owner.log(String.format("TRIGGER %s : %s écarte %s ", c.getName().toUpperCase(), actor.getName(), card.getName().toUpperCase()));
                }
            }
        });
        return c;
    }

    public static Card Cutpurse(){
        Card c = new Card("Cutpurse", 4, CardType.ACTION, CardType.ATTACK);
        c.addComponent(OnPlayComponent.class, p -> {
            CardUtil.TriggerEffect(p, 2,0,0,0,"Effect", c);
            p.getGame().discardOrShowHand(p, "Copper", c);

        });
        return c;
    }
}
