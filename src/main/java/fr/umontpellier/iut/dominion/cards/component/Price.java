package fr.umontpellier.iut.dominion.cards.component;

import javafx.beans.property.IntegerProperty;

/**
 * Prix d'une carte
 * @param price le nombre de pièce qu'elle coute
 * @param potion le nombre potion qu'elle coute (Alchemy)
 * @param debt le nombre de dettes qu'elle coute (Empire)
 */
public record Price (IntegerProperty price, int potion, IntegerProperty debt ) { }
