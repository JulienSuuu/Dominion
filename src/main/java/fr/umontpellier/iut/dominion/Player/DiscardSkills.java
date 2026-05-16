package fr.umontpellier.iut.dominion.Player;

import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Flags;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.Events.Discard_Type;
import fr.umontpellier.iut.dominion.cards.Events.Event;
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class DiscardSkills implements PlayerComponent{
    private final Player self;

    public DiscardSkills(Player self) {
        this.self = self;
    }

    public boolean discard(Card c, Discard_Type... discard) {
        if (c == null) return false;

        if((self.getFlag(Flags.resolveBandOfMisfit).get() && c.getFlag("cant") ) || c.getFlag("unable") )return false;

        Discard_Type type = (discard.length > 0) ? discard[0] : Discard_Type.ACTION;
        Event event = new Event(c, Destination.DISCARD, self, type);


        if (!c.hasForLocation(Destination.DISCARD)) {
            self.moveTo(c, Destination.DISCARD);
        }
        System.out.println("DEBUG DISCARD - Carte: " + event.getCard().getName());
        System.out.println("DEBUG DISCARD - Location actuelle de la carte: " + event.getCard().getLocation());
        System.out.println("DEBUG DISCARD - event.getCardOrigin(): " + event.cameFrom(Destination.INPLAY));
        System.out.println("DEBUG DISCARD - event.initialOrigin: " + event.initialCameFrom(Destination.INPLAY));
        self.triggerOneCard(TriggerComponent.checkItselfDiscarded.class, event.getCard(), event);
        self.getDiscardHooks().forEach(hook -> hook.accept(event));

        if(!event.getCard().hasForLocation(event.getDest())) {
            self.moveTo(event.getCard(), event.getDest());
        }

        return true;
    }

    public void discardFromHand(int number) {
        if (self.get(Destination.HAND).isEmpty()) return;

        int target = Math.min(number, self.get(Destination.HAND).size());
        int discardedCount = 0;

        while (discardedCount < target) {
            Optional<Card> c = self.chooseCardFromHand("Défausse encore " + (target - discardedCount) + " carte(s)", false);
            if(c.isPresent()) {
                Card card = c.get();
                discardedCount++;
                self.discard(card);
            }else{
                break;
            }
        }
    }

    public void discardTo(int number) {
        int currentHandSize = self.get(Destination.HAND).size();
        if (currentHandSize <= number) return;
        int amountToDiscard = currentHandSize - number;
        discardFromHand(amountToDiscard);
    }

    public void discardUntilYouStop(Destination from, Consumer<Integer> playerAction ) {
        List<Card> list =  self.getCopyOf(from);
        int count = 0;
        while(!list.isEmpty()){
            Optional<Card> card = self.chooseCardFromList("You may discard any card you want ", c -> true, list, true);
            if(card.isPresent()){
                self.discard(card.get());
                list.remove(card.get());
                count++;
            }else break;
        }
        playerAction.accept(count);
    }

    public Card discard(){
        Optional<Card> c = self.chooseCardFromHand("Défausse une carte ", true );
        if(c.isPresent()) {
            Card card = c.get();
            self.discard(card);
            return card;
        }
        return null;
    }

    public void discardAll(Destination dest){
        List<Card> triggerDiscard = self.getCopyOf(dest).stream().filter(c -> c.hasComponent(TriggerComponent.checkItselfDiscarded.class)).toList();

        self.getCopyOf(dest).forEach(card -> self.moveTo(card, Destination.DISCARD));

        new ArrayList<>(triggerDiscard).forEach(self::discard);

    }


    public void discardUntil(Predicate<Card> check, Consumer<Card> action){
        Card c;
        List<Card> toDiscard =  new ArrayList<>();
        while(true){
            c = self.getCardFromDeck();
            if(c==null) break;
            if(check.test(c))break;
            c.moveTo(toDiscard, null);
        }

        if(c != null) action.accept(c);
        self.reveals(toDiscard);
        new ArrayList<>(toDiscard).forEach(self::discard);

    }

    public List<Card> discardAList(List<Card> toDiscard, int numberToDiscard){
        if(toDiscard.isEmpty()) return new ArrayList<>();
        for(int i = 0; i < numberToDiscard; i++){
            if(toDiscard.isEmpty()) break;
            self.chooseCardFromList("Discard a card from this list", card -> true, toDiscard, false)
                   .ifPresent(card -> {
                       self.discard(card);
                       toDiscard.remove(card);
                   });
        }
        return  toDiscard;
    }
}
