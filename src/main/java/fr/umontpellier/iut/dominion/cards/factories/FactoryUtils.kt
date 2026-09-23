package fr.umontpellier.iut.dominion.cards.factories
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.Events.Event
import fr.umontpellier.iut.dominion.cards.component.DurationComponent
import fr.umontpellier.iut.dominion.cards.component.Follower

const val EFFECT = "Effect"
const val ACTION = "Action"
const val DURATION = "Duration"
const val TRASHED_ACTION = "Trashed Action"
const val GAIN_ACTION = "Gain Action"
const val CG = "Cornucopia_Guilds"
const val DA = "Dark_Ages"

fun Card.reserveCondition(condition: (Event, Player) -> Boolean = {event, player -> true}): (Event, Player) -> Boolean = {
    event, player ->
    val isAtTavernMat = this.hasForLocation(Destination.OtherZone.Tavern)
    isAtTavernMat && condition(event, player) && event.player == player
}

fun Card.follow(player : Player, toLinked : Card) : Card{
    if(activate(player, toLinked)) getComponent<Follower>()?.addEffect(toLinked.getComponent())
    if(activeFollow(toLinked)) getComponent<Follower>()?.addFollowedAction(toLinked)
    return this
}

val activate : (Player, Card) -> Boolean = {player, c -> c.getComponent<DurationComponent>()?.let {
        d ->
    !d.isFinished(player) }
    ?: false }

val activeFollow : (Card) -> Boolean = {c -> c.getComponent<Follower>()?.inactive == false}

@JvmField
val checkDuration: (Card) -> Boolean = { card ->
    card.getComponent<DurationComponent>()?.checkDuration()?.let { it(card) } ?: false
}

fun Card.checkLink() : (Card) -> Boolean = { getComponent<Follower>()?.inactive == true}
