package fr.umontpellier.iut.dominion.cards.component

import fr.umontpellier.iut.dominion.Button
import fr.umontpellier.iut.dominion.cards.Card

/**
 * Représente une requête d'interaction envoyée à l'interface d'un joueur.
 */
data class InteractionRequest<out X>(
    val instruction: String,
    val data: X? = null,
    val filter: (Card) -> Boolean = { true },
    val buttons: List<Button> = emptyList(),
    val cards: List<Card> = emptyList(),
    val canPass: Boolean = false,
    val chooseFilter : (Card) -> Boolean = { false }
)