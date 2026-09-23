package fr.umontpellier.iut.dominion

import fr.umontpellier.iut.dominion.Supply.SupplyPile

internal class PileComparator : Comparator<SupplyPile> {
    override fun compare(o1: SupplyPile, o2: SupplyPile): Int {
        return compareValuesBy(o1, o2,
            { it.cost.coins },
            { it.cost.potions },
            { it.cost.debt },
            { it.supplyName }
        )
    }
}
