package fr.umontpellier.iut.dominion.cards.builders

class ChooseDecisionContext<U, V, X, R, D>(
    val right: U,
    val left: V,
    val source: X,
    val choice: R,
    val extraData : D
)