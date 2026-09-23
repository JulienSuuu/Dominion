package fr.umontpellier.iut.dominion.cards.factories.Hinterlands

import fr.umontpellier.iut.dominion.Button
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.game.Game
import fr.umontpellier.iut.dominion.Player.Skills.gain
import fr.umontpellier.iut.dominion.Player.Skills.playCard
import fr.umontpellier.iut.dominion.Player.Skills.reveals
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.Events.Event
import fr.umontpellier.iut.dominion.cards.Events.OnGainEvent
import fr.umontpellier.iut.dominion.cards.component.BiEffect
import fr.umontpellier.iut.dominion.cards.component.InteractionRequest
import fr.umontpellier.iut.dominion.cards.component.chooseWhatToDo

class HinterlandsRules(private val game: Game?) {
    suspend fun DuchessPassive(event: OnGainEvent) {
        val player = event.player
        if (event.card?.hasName("Duchy") == true) {
            val duchess = player.getCardFromSupply("Duchess")
            val choice =
                player.chooseWhatToDo("Do you want to gain a Duchess ?", listOfNotNull(duchess), buttons =  Button.yesOrNo, canPass =  true)
            if ("y" == choice) {
                player.gain(duchess, Destination.PlayerZone.Discard )
            }
        }
    }
}


suspend fun runTrail(event : Event, self : Card) {
    val effect = BiEffect.empty<Event, Card>()
        .chooseWhatToDo { event, self -> InteractionRequest(
            instruction = "${event.player}, Do you want to play $self ?",
            cards = listOf(self),
            buttons = Button.yesOrNo
        ) }
        .branch(
            "y" to {event, self, _ ->
                val p = event.player
                p.reveals(self)
                p.playCard(self)
                event.destination = Destination.PlayerZone.InPlay
            }
        )
        .end()

    effect(event, self)
}
