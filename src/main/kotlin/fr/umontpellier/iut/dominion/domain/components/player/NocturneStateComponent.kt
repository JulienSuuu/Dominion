package fr.umontpellier.iut.dominion.domain.components.player

import fr.umontpellier.iut.dominion.domain.model.card.CardType
import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.card.Card

class NocturneStateComponent(val self : Player) : BuyValidator, PlayerLifecycleHook {
    var isDeluded = false
        private set

    var isEnvious = false
     private set


    override fun canBuy(card: Card): Boolean {
        return !(isDeluded && card.hasType(CardType.ACTION))
    }

    fun update(nameState : String, bool : Boolean) {
        if(nameState == "Deluded"){
            isDeluded = bool
        }
        if(nameState == "Envious"){
            isEnvious = bool
        }
    }

    override fun onCleanUp() {
        isDeluded = false
        isEnvious = false
    }
}

fun Player.update(state : String, bool : Boolean) { getComponent<NocturneStateComponent>()?.update(state,bool) }
val Player.isEnvious get() = getComponent<NocturneStateComponent>()?.isEnvious ?: false
