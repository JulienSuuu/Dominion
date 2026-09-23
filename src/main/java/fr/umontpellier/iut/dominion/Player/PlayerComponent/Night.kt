package fr.umontpellier.iut.dominion.Player.PlayerComponent

import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.Skills.playCard
import fr.umontpellier.iut.dominion.cards.plusAssign

class Night(private val self: Player) : PlayerComponent {
    suspend fun startNightPhase() {
        self.getFlag("NightPhase") += true
        while (true) {
            val hand = self.getList(Destination.PlayerZone.Hand)

            val nightCardsInHand = hand.filter { c -> c.hasType(CardType.NIGHT) }

            if (nightCardsInHand.isEmpty()) {
                self.log(self.name + " don't have night cards in hand.")
                break
            }

            self.chooseCardFromHand("play Nocturne Phase",  predicate = {it.hasType(CardType.NIGHT)}, canPass = true)
                ?.let { self.playCard(it)  }
                ?: run {
                    self.log(self.name + " skip night phase")
                    break
                }


        }
    }
}

suspend fun Player.startNightPhase() = getComponent<Night>()?.startNightPhase()

