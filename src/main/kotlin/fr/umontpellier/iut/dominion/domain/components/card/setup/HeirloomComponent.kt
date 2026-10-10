package fr.umontpellier.iut.dominion.domain.components.card.setup

import fr.umontpellier.iut.dominion.domain.model.card.Card
import fr.umontpellier.iut.dominion.domain.setup.card.factories.FactorySupplyPile
import fr.umontpellier.iut.dominion.domain.components.card.CardComponent

class HeirloomComponent(val heirloom : String) : CardComponent {

    fun createHeirloom() : Card? {
        return FactorySupplyPile.createNewCard(heirloom)
    }
}

fun Card.createHeirloom() : Card? {return getComponent<HeirloomComponent>()?.createHeirloom()}