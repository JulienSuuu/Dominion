package fr.umontpellier.iut.dominion.cards.component;


public interface TriggerEffect<U, T extends TriggerEffect<U, T> & CardComponent> extends TriggerComponent, Effect<U, T> {
}
