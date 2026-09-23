package fr.umontpellier.iut.dominion.cards.builders

import fr.umontpellier.iut.dominion.Interface.Logger

class ChooseBranchBuilder<U : Logger, V, X : Any, R : Any, D : Any>(
    val context: ChooseDecisionContext<U, V, X, R, D>
) {
    val cases = mutableListOf<Pair<ChooseDecisionContext<U, V, X, R, D>.() -> Boolean, suspend R.(U, V, X) -> Unit>>()

    infix fun (ChooseDecisionContext<U, V, X, R, D>.() -> Boolean).then(action: suspend R.(U, V, X) -> Unit) {
        cases.add(this to action)
    }


    infix fun otherwise(action: suspend R.(U, V, X) -> Unit){
        cases.add(on {true} to action)
    }

    infix fun on(condition: ChooseDecisionContext<U, V, X, R, D>.() -> Boolean) = condition
}