package fr.umontpellier.iut.dominion.cards.Events;

/**
 * évenement classique de gain
 */
public class OnGainEvent extends Event {
    private final Event event;
    public OnGainEvent(Event event) {
        super(event.getCard(), event.getDest(), event.getPlayer());
        this.event = event;
    }

    @Override
    public boolean hasMoved() {
        return event.hasMoved();
    }

    @Override
    public boolean notMoved() {
        return event.notMoved();
    }

    @Override
    public boolean isSameCard() {
        return event.isSameCard();
    }
}
