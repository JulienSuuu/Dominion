package fr.umontpellier.iut.dominion;

import java.util.Comparator;

class PileComparator implements Comparator<SupplyPile> {
    @Override
    public int compare(SupplyPile o1, SupplyPile o2) {
        if (o1.getCost() != o2.getCost()) {
            return o1.getCost() - o2.getCost();
        } else {
            return o1.getName().compareTo(o2.getName());
        }
    }
}
