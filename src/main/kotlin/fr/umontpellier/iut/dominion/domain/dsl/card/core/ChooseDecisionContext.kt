package fr.umontpellier.iut.dominion.domain.dsl.card.core

class ChooseDecisionContext<U, V, X, R, D>(
    val right: U,
    val left: V,
    val source: X,
    val choice: R,
    val extraData : D
)