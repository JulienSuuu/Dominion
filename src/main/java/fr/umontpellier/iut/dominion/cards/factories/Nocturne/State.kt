package fr.umontpellier.iut.dominion.cards.factories.Nocturne

import fr.umontpellier.iut.dominion.Interface.IDominionObject
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.Skills.discard
import fr.umontpellier.iut.dominion.cards.Id
import fr.umontpellier.iut.dominion.cards.component.BiEffect
import fr.umontpellier.iut.dominion.cards.component.InteractionRequest
import fr.umontpellier.iut.dominion.cards.component.ScoreComponent
import fr.umontpellier.iut.dominion.cards.component.chooseCardFromHand
import fr.umontpellier.iut.dominion.cards.plusAssign
import kotlin.reflect.KClass

class State(override val name: String) : IDominionObject {

    val id = Id("${name}_${counter++}")

    var currentName: String = name
    private var versoName: String = name

    val components = mutableMapOf<KClass<out StateComponent>, StateComponent>()

    fun setup(block: State.() -> Unit): State {
        this.block()
        return this
    }

    fun matches(stateName: String): Boolean = currentName == stateName || versoName == stateName

    /**
     * Définit la face verso de la carte.
     */
    fun flipsTo(otherStateName: String) {
        this.versoName = otherStateName
    }

    /**
     * Effectue le basculement recto/verso de la carte physique
     * et retourne le nouveau nom actif.
     */
    fun flip(): String {
        val temp = currentName
        currentName = versoName
        versoName = temp
        return currentName
    }

    inline fun <reified T : StateComponent> addComponent(component: T) {
        components[T::class] = component
    }

    inline fun <reified T : StateComponent> getComponent(): T? {
        return components[T::class] as? T
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is State) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    companion object {
        var counter: Int = 0

        val states = mutableMapOf<String, () -> State>().apply {

            this["Miserable"] = {
                State("Miserable").setup {
                    flipsTo("Twice Miserable")
                    addComponent(ScoreComponent { player ->
                        if (currentName == "Twice Miserable") -4 else -2
                    })
                }
            }

            this["Twice Miserable"] = this["Miserable"]!!


            // 2. Carte Deluded / Envious
            this["Deluded"] = {
                State("Deluded").setup {
                    flipsTo("Envious")

                    addComponent(StateComponent.OnStartBuyPhase { player, state ->
                        val activeName = state.currentName
                        player.removeState(state)
                        player.getFlag(activeName) += true
                    })
                }
            }

            this["Envious"] = this["Deluded"]!!


            this["Lost In The Woods"] = {
                State("Lost In The Woods").setup {
                    addComponent(StateComponent.OnStartTurnPhase { player, _ ->
                        val effect = BiEffect.empty<Player, Unit>()
                            .chooseCardFromHand { p, _ -> InteractionRequest(
                                instruction = "${p.name}, you may discard a card to receive a boon",
                                canPass = true
                            ) }
                            .thenWith { p, chosenCard ->
                                if (p.discard(chosenCard)) {
                                    p.game.receiveNextBoon()?.playNocturne(p)
                                }
                            }
                            .end()

                        effect(player, Unit)
                    })
                }
            }
        }
    }
}


