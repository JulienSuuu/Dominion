package fr.umontpellier.iut.dominion.Player.PlayerComponent

import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.cards.Card

class NocturneStateComponent(val self : Player) : PlayerComponent {
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
