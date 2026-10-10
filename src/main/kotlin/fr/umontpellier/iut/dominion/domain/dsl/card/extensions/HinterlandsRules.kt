package fr.umontpellier.iut.dominion.domain.dsl.card.extensions

import fr.umontpellier.iut.dominion.domain.model.ui.Button
import fr.umontpellier.iut.dominion.domain.model.location.Destination
import fr.umontpellier.iut.dominion.domain.model.player.skills.gain
import fr.umontpellier.iut.dominion.domain.model.player.skills.playCard
import fr.umontpellier.iut.dominion.domain.model.player.skills.reveals
import fr.umontpellier.iut.dominion.domain.model.card.Card
import fr.umontpellier.iut.dominion.domain.model.event.Event
import fr.umontpellier.iut.dominion.domain.model.event.OnGainEvent
import fr.umontpellier.iut.dominion.domain.components.card.effect.BiEffect
import fr.umontpellier.iut.dominion.domain.dsl.card.core.InteractionRequest
import fr.umontpellier.iut.dominion.domain.dsl.card.builders.chooseWhatToDo

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
