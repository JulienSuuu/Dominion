package fr.umontpellier.iut.dominion.Player;

import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Flags;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.Events.Event;
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TrashSkills implements PlayerComponent {
    private final Player self;
    public TrashSkills(Player self) {
        this.self = self;
    }

    public boolean trash(Card c) {
        if ((self.getFlag(Flags.resolveBandOfMisfit).get() && c.getFlag("cant")) || c.getFlag("unable")) {
            self.log(String.format("%s stay in " + c.getLocation().name().toLowerCase() + "(impossible movement).", c.getName()));
            return false;
        }
        Event trash = new Event(c, Destination.TRASH, self);

        self.getGame().moveCardToTrash(c);
        self.log(String.format("Trash %s ", c.getName()));

        triggerTrashEvent(trash);

        if(!self.getController().equals(self)){
            c.moveTo(self.get(Destination.ASIDE), Destination.ASIDE);
            self.mustBeDiscarded().add(trash.getCard());
            return false;
        }

        if(!trash.getCard().hasForLocation(trash.getDest())){
            self.moveTo(trash.getCard(), trash.getDest());
        }

        if(c.hasForLocation(Destination.TRASH)) c.clear();

        return true;
    }

    public void trashAll(Destination sourceZone) {
        List<Card> cardsToTrash = self.getCopyOf(sourceZone);
        if (cardsToTrash.isEmpty()) return;

        for (Card card : cardsToTrash) {
            self.getGame().moveCardToTrash(card);
        }

        List<Card> triggersPending = cardsToTrash.stream()
                .filter(c -> c.hasComponent(TriggerComponent.checkItselfTrashed.class))
                .collect(Collectors.toCollection(ArrayList::new));

        while (!triggersPending.isEmpty()) {
            Card chosen = null;
            if (triggersPending.size() == 1) {
                chosen = triggersPending.getFirst();
            } else {
                Optional<Card> card = self.chooseCardFromList(
                        "Choose the order of trashing",c -> true,
                        triggersPending, false
                );

                if(card.isPresent()) {
                    chosen = card.get();
                }
            }
            if(chosen == null)continue;

            Event trashEvent = new Event(chosen, Destination.TRASH, self);
            triggerTrashEvent(trashEvent);
            triggersPending.remove(trashEvent.getCard());
        }
    }

    public boolean trash(int number){
        for(int i = 0; i < number; i++){
            self.chooseCardFromHand("Trash " + (number-i) + "card(s) from your hand", true)
                    .ifPresent(self::trash);
        }
        return true;
    }

    public void trashWithCondition(int number, Predicate<Card> filter, Destination from){
        List<Card> list = self.getCopyOf(from);
        for(int i = 0; i < number; i++){
            self.chooseCardFromList("Trash " + (number-i) + "card(s) from your hand",filter, list, true)
                    .ifPresent(self::trash);
        }
    }

    public Card trash(){
        Optional<Card> toTrash = self.chooseCardFromHand("Trash a card from your hand", true);
        if(toTrash.isPresent()){
            Card c =  toTrash.get();
            self.trash(c);
            return c;
        }
        return null;
    }

    private void triggerTrashEvent(Event event) {
        self.triggerOneCard(TriggerComponent.checkItselfTrashed.class, event.getCard(), event);
        self.triggerOthersEvent(TriggerComponent.onCardTrashed.class, event);
    }
}
