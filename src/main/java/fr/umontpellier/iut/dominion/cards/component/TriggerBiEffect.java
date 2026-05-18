package fr.umontpellier.iut.dominion.cards.component;

import fr.umontpellier.iut.dominion.Interface.Logger;

import java.util.function.BiConsumer;


public interface TriggerBiEffect<U extends Logger, V, T extends TriggerBiEffect<U, V, T> & CardComponent> extends BiEffect<U, V, T>, TriggerComponent {

}
