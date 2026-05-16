package fr.umontpellier.iut.dominion.cards.component;

import fr.umontpellier.iut.dominion.Interface.Logger;

public record Context<U extends Logger, V, X>(U right, V left, X data) {}