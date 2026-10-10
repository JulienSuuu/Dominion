package fr.umontpellier.iut.dominion.domain.components.player

import fr.umontpellier.iut.dominion.domain.model.card.CardType
import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.card.Card

class MissionComponent(val self: Player) : BuyValidator, PlayerLifecycleHook {

    enum class State { INACTIVE, PREPARED, IN_MISSION }

    var state = State.INACTIVE
        private set

    override fun canBuy(card: Card): Boolean {
        if (state != State.IN_MISSION) return true
        return card.hasType(CardType.EVENT)
    }

    fun goToMission() {
        if (state == State.PREPARED) {
            state = State.IN_MISSION
        }
    }

    fun quitMission() {
        state = State.INACTIVE
    }

    fun update(prepare: Boolean) {
        if (prepare) {
            state = State.PREPARED
        } else if (state == State.PREPARED) {
            state = State.INACTIVE
        }
    }

    override fun onCleanUp() {
        if (state == State.IN_MISSION) {
            quitMission()
        }
    }
}


val Player.shouldPrepareMission: Boolean
    get() = getComponent<MissionComponent>()?.state == MissionComponent.State.PREPARED

fun Player.prepareMission() = updateMissionRestriction(true)

fun Player.quitMission() = getComponent<MissionComponent>()?.quitMission()

fun Player.goInMission() = getComponent<MissionComponent>()?.goToMission()

fun Player.updateMissionRestriction(bool: Boolean) =
    getComponent<MissionComponent>()?.update(bool)