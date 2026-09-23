package fr.umontpellier.iut.dominion.cards.factories.Nocturne

import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.game.Game
import fr.umontpellier.iut.dominion.game.rules.GameComponent
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Supply.SupplyPile
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.Id
import fr.umontpellier.iut.dominion.cards.factories.createMixedSupplyPile
import fr.umontpellier.iut.dominion.cards.factories.createSupplyPile
import fr.umontpellier.iut.dominion.game.toJsonString
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Collections
import kotlin.collections.filter

class NocturneRules(val game : Game) : GameComponent {
    val discardMat = mutableMapOf<Destination.NocturneZone, MutableStateFlow<List<Card>>>().apply {
        if(game.hasType(CardType.FATE, 1)) put(Destination.NocturneZone.Boons, MutableStateFlow(emptyList()))
        if(game.hasType(CardType.DOOM, 1)) put(Destination.NocturneZone.Hex, MutableStateFlow(emptyList()))
    }

    val nocturnePile = mutableMapOf<String,SupplyPile>().apply {
        if(game.hasType(CardType.FATE, 1)) put("Boons", game.createMixedSupplyPile("Boons", game.factory.getMixedCards(CardType.BOON)))
        if(game.hasType(CardType.DOOM, 1)) put("Hexes", game.createMixedSupplyPile("Hex", game.factory.getMixedCards(CardType.HEX)))
    }


    val states = mutableMapOf<Id, State>()
    val stateOwners = mutableMapOf<State, Player>()

    val othersPile = mutableMapOf<String, MutableList<SupplyPile>>().apply {}


    val druidBoons = MutableStateFlow(emptyList<Card>())

    fun discardTo(card : Card ? ,dest: Destination.NocturneZone) {
        if(card == null) return
        card.moveTo(discardMat[dest],  dest)
    }

    fun initSupply(){
        nocturnePile.values.forEach {
            it.onCardChange = {event -> game.sendToUI(event.toJson("""
                "location": "nocturne",
                "playerId": "${game.lastPlayerIdChoice}"
            """.trimIndent()))}
        }

        othersPile.values.forEach {
            it.forEach { s -> s.onCardChange = {event -> game.sendToUI(event.toJson("""
                "location": "nocturne",
                "playerId": "${game.lastPlayerIdChoice}"
            """.trimIndent()))} }
        }
    }


    fun putAsideForDruid() {
        for (i in 0 until  3) {
            val c = receiveNextBoon() ?: break
            c.moveTo(druidBoons, Destination.NocturneZone.Druid)
        }
    }

    fun addNocturnePile(key: String, cardName: String) {

        val alreadyExists = othersPile.values.any { pilesList ->
            pilesList.any { pile -> pile.verifyName(cardName) }
        }

        if (alreadyExists) return

        val list = othersPile.getOrPut(key) { mutableListOf() }

        list.add(game.createSupplyPile(cardName))
    }

    fun addState(key: String, number: Int) {
        if (states.values.any { it.matches(key) }) return

        repeat(number) {
            val state = State.states[key]?.invoke()
            if (state != null) {
                states[state.id] = state
            }
        }
    }


    fun getAvailableCard(namePile: String) : List<Card> {
        return othersPile[namePile]?.filter(SupplyPile::isNotEmpty)?.mapNotNull(SupplyPile::popCard)?: Collections.emptyList()
    }

    fun getSpecificAsideCard(namePile: String, key: String) : Card? {
        return getAvailableCard(namePile).find { it.hasName(key) }
    }

    fun receiveNextBoon() : Card? {
        val list = nocturnePile["Boons"] ?: return null
        if(list.isEmpty){ shuffle(Destination.NocturneZone.Boons, "Boons") }
        return list.popCard()
    }

    fun receiveBoons(number : Int) : MutableList<Card> {
        val list = nocturnePile["Boons"] ?: return mutableListOf()
        if(list.size < number){shuffle(Destination.NocturneZone.Boons, "Boons") }
        return list.take(number)

    }

    fun receiveNextHex() : Card? {
        val list = nocturnePile["Hexes"] ?: return null
        if(list.isEmpty){
            shuffle(Destination.NocturneZone.Hex, "Hexes")
        }

        return list.popCard()
    }

    fun assignStateToPlayer(stateName: String, newOwner: Player) {
        val hasTargetState = underState(newOwner, stateName)

        if (stateName == "Miserable" && hasTargetState) {
            val currentState = stateOwners.entries.find { it.value == newOwner && it.key.currentName == "Miserable" }?.key
            currentState?.flip()
            return
        }

        if (hasTargetState || (stateName in listOf("Deluded", "Envious") && (underState(newOwner, "Deluded") || underState(newOwner, "Envious")))) {
            return
        }

        val availableState = states.values.find { state ->
            state !in stateOwners.keys && state.matches(stateName)
        } ?: return

        if (availableState.currentName != stateName) {
            availableState.flip()
        }

        stateOwners[availableState] = newOwner
    }

    fun removeState(state: State) {
        stateOwners.remove(state)
    }

    fun removeState(player: Player, stateName: String) {
        val entry = stateOwners.entries.find { (state, owner) -> owner == player && state.currentName == stateName }
        if (entry != null) {
            stateOwners.remove(entry.key)
        }
    }

    /** Récupère la liste de tous les États physiques possédés par un joueur */
    fun underStates(player: Player): List<State> {
        return stateOwners.filterValues { it == player }.keys.toList()
    }

    /** Vérifie si le joueur possède un État actif sous un nom précis */
    fun underState(player: Player, name: String): Boolean {
        return stateOwners.entries.any { (state, owner) -> owner == player && state.currentName == name }
    }

    fun shuffle(destination: Destination.NocturneZone, to : String){
        val list = nocturnePile[to] ?: return
        val discard = discardMat[destination] ?: return
        val newDiscard = discard.value.shuffled()
        newDiscard.forEach {list.replace(it)}
    }

    fun toJson(): String {
        val discardMatJson = discardMat.entries.joinToString(", ", prefix = "{", postfix = "}") { (zone, flow) ->
            "\"${zone.name}\": ${flow.value.size}"
        }

        val statesJson = stateOwners.entries
            .joinToString(", ", prefix = "{", postfix = "}") { (stateName, player) ->
                val playerIndex = player.id
                val cardJson = states[stateName.id]?.name ?: ""

                "\"$stateName\": { \"owner\": $playerIndex, \"card\": \"$cardJson\" }"
            }

        val allPilesEntries = mutableListOf<String>()

        nocturnePile.forEach { (pileName, supplyPile) ->
            allPilesEntries.add("\"$pileName\": [${supplyPile.toJsonString()}]")
        }

        othersPile.forEach { (categoryName, pilesList) ->
            allPilesEntries.add("\"$categoryName\": [${game.getSupplyPilesJson(pilesList)}]")
        }

        val nocturnePileJson = allPilesEntries.joinToString(", ", prefix = "{", postfix = "}")

        val druidBoonsJson = druidBoons.value.joinToString(", ", prefix = "[", postfix = "]") { "\"${it.id}\"" }


        return """
    {
        "discardMat": $discardMatJson,
        "nocturnePile": $nocturnePileJson,
        "druidBoons": $druidBoonsJson,
        "states": $statesJson
    }
    """.trimIndent()
    }

}

fun Game.receiveNextBoon() : Card? = getRule<NocturneRules>()?.receiveNextBoon()
fun Game.receiveNextHex() : Card? = getRule<NocturneRules>()?.receiveNextHex()
fun Game.discardTo(destination: Destination.NocturneZone, card : Card) = getRule<NocturneRules>()?.discardTo(card, destination)
fun Game.chooseDruidBoon() : List<Card> = getRule<NocturneRules>()?.druidBoons?.value ?: Collections.emptyList<Card>()
fun Game.getAvailableNocturnePile(pileName : String) : List<Card> = getRule<NocturneRules>()?.getAvailableCard(pileName) ?: Collections.emptyList()
fun Game.receiveState(player : Player, nameState : String) = getRule<NocturneRules>()?.assignStateToPlayer(nameState, player)
fun Game.underState(player : Player, nameState: String) = getRule<NocturneRules>()?.underState(player, nameState) ?: false
fun Game.receiveBoons(number: Int) : MutableList<Card> = getRule<NocturneRules>()?.receiveBoons(number) ?: mutableListOf()
fun Game.getSpecificNightCard(namePile: String, key: String) = getRule<NocturneRules>()?.getSpecificAsideCard(namePile, key)
fun Game.addNocturnePile(key : String, cardName: String) { getRule<NocturneRules>()?.addNocturnePile(key, cardName)}
fun Game.removeState(stateName : State) { getRule<NocturneRules>()?.removeState(stateName) }
fun Player.removeState(state : State) { if(underState(state.name)) game.removeState(state) }