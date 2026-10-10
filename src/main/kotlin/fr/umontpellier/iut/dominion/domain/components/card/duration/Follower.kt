package fr.umontpellier.iut.dominion.domain.components.card.duration

import fr.umontpellier.iut.dominion.domain.model.location.Destination
import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.card.Card
import fr.umontpellier.iut.dominion.domain.setup.card.factories.activeFollow
import fr.umontpellier.iut.dominion.domain.model.card.isNotIn
import fr.umontpellier.iut.dominion.domain.components.card.CardComponent

class Follower(val scope: Card, val multiplier : Int) : CardComponent {
    val effect = mutableListOf<DurationComponent>()
    val inactive: Boolean
        get() = effect.isEmpty() && followedActions.all {
            it.getComponent<Follower>()?.inactive ?: true
        }
    val followedActions = mutableListOf<Card>()

    fun contains(duration: DurationComponent): Boolean {
        if (effect.contains(duration)) return true
        return followedActions
            .mapNotNull { it.getComponent<Follower>() }
            .filter { !it.inactive && it.scope.isNotIn(Destination.PlayerZone.InPlay) }
            .any { it.contains(duration) }
    }

    fun removeDuration(duration: DurationComponent): Boolean {
        if (effect.remove(duration)) return true

        return followedActions
            .mapNotNull { it.getComponent<Follower>() }
            .filter { it.scope.isNotIn(Destination.PlayerZone.InPlay) }
            .any { it.removeDuration(duration) }
    }


    fun addEffect(duration: DurationComponent?) {
        if (duration != null) {
            effect.add(duration)
        }
    }

    fun clear(){
        effect.clear()
        followedActions.clear()
    }

    fun addFollowedAction(card: Card) {
        followedActions.add(card)
    }


    suspend fun execute(player: Player) {
        effect.forEach {
            executeOneDuration(player, it.scope, it)
        }
    }

    suspend fun executeOneDuration(player: Player, card: Card, d : DurationComponent) {
        if(effect.contains(d)){
            repeat(multiplier) {
                d.execute(player, card)
            }
        }

        val followedOtherThanPlay = followedActions
            .filter { it.isNotIn(Destination.PlayerZone.InPlay) && activeFollow(it) }

        followedOtherThanPlay.forEach {
            val follower = it.getComponent<Follower>()
            follower?.executeOneDuration(player, card, d)
        }
    }

}