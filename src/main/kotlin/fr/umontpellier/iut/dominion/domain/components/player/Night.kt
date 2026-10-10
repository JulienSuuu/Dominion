package fr.umontpellier.iut.dominion.domain.components.player

import fr.umontpellier.iut.dominion.domain.model.card.CardType
import fr.umontpellier.iut.dominion.domain.model.location.Destination
import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.player.skills.playCard
import fr.umontpellier.iut.dominion.domain.utils.plusAssign

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

