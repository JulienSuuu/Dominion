package fr.umontpellier.iut.dominion.cards.component

import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.minusAssign
import kotlinx.coroutines.flow.MutableStateFlow

class DurationComponent(
    /** Effet du prochain tour. */
    private var nextTurnEffect: Duration? = null,
    val scope : Card
) : CardComponent {

    private var trigger: (Player, Card) -> Boolean = {_, _ -> true }
    private var thingToDo: (Card) -> Boolean = { false }
    private var shouldBeDiscarded: (Player, Card) -> Boolean = { p, c -> checkDuration()(c) || !c.hasForLocation(Destination.PlayerZone.InPlay)}

    private val duration: Int get() = durationFlow.value

    private var numberOfTurns = 1
    private var isInfinite = false
    private val durationFlow = MutableStateFlow(1)

    fun interface Duration : BiEffect<Player, Card>, CardComponent


    fun setTrigger(trigger: (Player, Card) -> Boolean): DurationComponent {
        this.trigger = trigger
        return this
    }

    fun setInfinite(infinite: Boolean): DurationComponent {
        this.isInfinite = infinite
        return this
    }

    fun setEffect(effect : Duration){ this.nextTurnEffect = effect }

    fun stayInPlayCondition(shouldBeDiscarded: (Player, Card) -> Boolean): DurationComponent {
        this.shouldBeDiscarded = shouldBeDiscarded
        return this
    }

    infix fun <X, Y> ((X, Y) -> Boolean).and(other : (X, Y) -> Boolean) : (X, Y) -> Boolean {
        return { x, y -> this(x,y) && other(x,y) }
    }

    fun thingToDo(thingToDo: (Card) -> Boolean): DurationComponent {
        this.thingToDo = thingToDo
        return this
    }

    fun setNumberOfTurns(numberOfTurns: Int): DurationComponent {
        this.numberOfTurns = numberOfTurns
        return this
    }

    // --- Logique métier ---

    /**
     * Lance l'effet du composant.
     * @param p Le joueur (le lanceur ou le receveur).
     */
    suspend fun execute(p: Player, c: Card) {
        nextTurnEffect?.invoke(p, c)
    }

    /**
     * Décrémente la durée.
     */
    fun consume() {
        if (isInfinite) return
        durationFlow -= 1
    }

    /**
     * @return si le joueur doit défausser la carte.
     */
    fun isFinished(p : Player): Boolean {
        if (isInfinite) return false
        if (thingToDo(scope)) return false
        return shouldBeDiscarded(p, scope)
    }

    fun activeDuration(player : Player, c: Card) {
        if (!trigger(player, c) || duration == numberOfTurns) return
        durationFlow.value = numberOfTurns
    }

    fun checkDuration(): (Card) -> Boolean {
        return { _ -> duration <= 0 }
    }
}