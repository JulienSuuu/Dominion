package fr.umontpellier.iut.dominion.cards

import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Interface.IDominionObject
import fr.umontpellier.iut.dominion.Supply.ReadableSupplyPile
import fr.umontpellier.iut.dominion.cards.component.CardComponent
import fr.umontpellier.iut.dominion.cards.component.ReadablePrice
import fr.umontpellier.iut.dominion.game.ShadowKey
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