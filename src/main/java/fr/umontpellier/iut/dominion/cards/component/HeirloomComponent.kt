package fr.umontpellier.iut.dominion.cards.component

import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.factories.FactorySupplyPile

class HeirloomComponent(val heirloom : String) : CardComponent {

    fun createHeirloom() : Card? {
        return FactorySupplyPile.createNewCard(heirloom)
    }
}

fun Card.createHeirloom() : Card? {return getComponent<HeirloomComponent>()?.createHeirloom()}