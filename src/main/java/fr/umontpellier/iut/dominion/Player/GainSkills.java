package fr.umontpellier.iut.dominion.Player;

import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Flags;
import fr.umontpellier.iut.dominion.Item;
import fr.umontpellier.iut.dominion.Properties;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.Events.Event;
import fr.umontpellier.iut.dominion.cards.Events.OnGainEvent;
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent;
import javafx.beans.property.IntegerProperty;

import java.util.List;

public class GainSkills implements PlayerComponent {

    private final Player self;

    public GainSkills(Player self) {
        this.self = self;
    }

    public void gainTo(Card gainedCard, List<Card> location, Destination destination) {
        if(gainedCard == null)return;
        int i = self.getGame().tradeRoute(gainedCard);
        self.increment(Item.COIN_TOKEN_ROUTE, i);

        gainedCard.moveTo(location, destination);
    }

    public void gain(Card card, Destination dest, boolean isBuy) {
        if (card == null) return;
        Event event = new Event(card, dest, self, isBuy);

        self.checkGainToken(event);
        self.triggerOneCard(TriggerComponent.checkItselfGain.class, event.getCard(), event);
        self.triggerPlayerTavern(TriggerComponent.DuringPlayerGain.class, event);
        self.triggerEvent(TriggerComponent.DuringPlayerGain.class, event);
        self.triggerActiveEffect(TriggerComponent.DuringPlayerGain.class, event);

        if (self.getController() != self) {
            self.getController().gainTo(event.getDest(), event.getCard());
            return;
        } else {
            gainTo(event.getDest(), event.getCard());
        }

        self.getCardGainedCurrentTurn().add(event.getCard());

        if (!event.getCard().hasForLocation(event.getDest())) {
            self.moveTo(event.getCard(), event.getDest());
        }

        self.triggerEvent(TriggerComponent.AfterPlayerGain.class, event);
        self.getGame().fireEvent(OnGainEvent.class, new OnGainEvent(event));

        updateNumberOfBought(event);
    }


    public void gainSilent(Card card, Destination dest, boolean gained) {
        if(card == null)return;
        if(gained) self.getCardGainedCurrentTurn().add(card);
        if (!self.getController().equals(self)) {
            self.getController().gainTo(dest, card);
            return;
        }
        Event event = new Event(card, dest, self);
        self.checkGainToken(event);

        if(gained) self.triggerOneCard(TriggerComponent.checkItselfGain.class, card, event);
        self.getGame().fireEvent(OnGainEvent.class, new OnGainEvent(event));
        if(gained)gainTo(event.getDest(), card);

        if(event.getDest()==null)return;
        card.moveTo(self.get(dest), event.getDest());
    }

    public void gainTo(Destination dest, Card card){
        if(dest == null)return;
        gainTo(card, self.get(dest), dest);
    }

    public void updateNumberOfBought(Event event){
        if(!event.isBuy()) return;
        IntegerProperty prop = self.getProperties(Properties.Cards_Bought);
        prop.setValue(prop.getValue() + 1);
    }

}
