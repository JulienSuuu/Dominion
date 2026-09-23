package fr.umontpellier.iut.dominion.cards.builders

class BranchBuilder<U, V, X>(val context: DecisionContext<U, V, X>) {
    internal val cases = mutableListOf<Pair<DecisionContext<U, V, X>.() -> Boolean, suspend (U, V, X) -> Unit>>()

    infix fun (DecisionContext<U, V, X>.() -> Boolean).then(action: suspend (U, V, X) -> Unit){
        cases.add(this to action)
    }

    val data get() =  context.data


    infix fun otherwise(action: suspend (U, V, X) -> Unit){
        cases.add(on {true} to action)
    }

    infix fun on(condition: DecisionContext<U, V, X>.() -> Boolean) = condition

}

