package fr.umontpellier.iut.dominion.domain.model.card

import fr.umontpellier.iut.dominion.domain.model.location.Destination
import fr.umontpellier.iut.dominion.domain.common.IDominionObject
import fr.umontpellier.iut.dominion.domain.model.supply.ReadableSupplyPile
import fr.umontpellier.iut.dominion.domain.model.card.price.ReadablePrice
import fr.umontpellier.iut.dominion.domain.model.core.Id
import fr.umontpellier.iut.dominion.domain.model.core.ShadowKey
import kotlinx.coroutines.flow.StateFlow

interface ReadableCard : IDominionObject {
    val supply: ReadableSupplyPile?
    val id : Id
    val shadowKey : ShadowKey

    val faceDown : FaceDown

    val types : Set<CardType>
    val loc : StateFlow<Destination?>

    val price : ReadablePrice

}