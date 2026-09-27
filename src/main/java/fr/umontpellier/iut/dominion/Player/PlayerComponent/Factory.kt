package fr.umontpellier.iut.dominion.Player.PlayerComponent

import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Supply.EmptySupply.game
import fr.umontpellier.iut.dominion.cards.factories.FactorySupplyPile
import fr.umontpellier.iut.dominion.game.Game

object Factory {
    private val GAME_COMPONENT_RULES = listOf(
        ComponentRule(
            condition = {true},
            factory = { TurnHistoryComponent(it)}
        ),
        ComponentRule(
            condition = { game -> game.hasType(CardType.NIGHT, 1) },
            factory = { player -> Night(player) }
        ),
        ComponentRule(
            condition = { game -> game.hasCard("Possession") },
            factory = { player -> PossessionComponent(player) }
        ),
        ComponentRule(
            condition = { game -> game.hasExpansion("Adventures", 1) },
            factory = { player -> TokenComponent(player) }
        ),
        ComponentRule(
            condition = { game -> game.hasType(CardType.POKER, 1) },
            factory = { player -> PokerComponent(player) }
        ),
        ComponentRule(
            condition = { game -> game.hasExpansion("Nocturne", 1) },
            factory = {player -> NocturneStateComponent(player) }
        ),
        ComponentRule(
            condition = { game -> game.hasCard("Expedition") },
            factory = { player -> MissionComponent(player) }
        ),
        ComponentRule(
            condition = { game -> game.hasCard("Contraband") },
            factory = { player -> ContrabandComponent(player) }
        )
    )

    private val GAME_CARDSET = listOf(
        CardSet(
            factory = {
                val basePlayerZones = listOf(
                    Destination.PlayerZone.Hand,
                    Destination.PlayerZone.InPlay,
                    Destination.PlayerZone.Discard,
                    Destination.PlayerZone.Draw,
                    Destination.PlayerZone.Aside,
                )

                basePlayerZones.forEach { destination -> it.addSet(destination) }
            }
        ),
        CardSet(
            condition = { it.hasCard("Island") },
            factory = {it.addSet(Destination.OtherZone.Island) }
        ),
        CardSet(
            condition = { it.hasCard("Native Village") },
            factory = {it.addSet(Destination.OtherZone.Native) }
        ),
        CardSet(
            condition = { it.hasType(CardType.RESERVE, 1) },
            factory = {it.addSet(Destination.OtherZone.Tavern) }
        ),
        CardSet(
            condition = { it.hasType(CardType.BOON, 1) },
            factory = {it.addSet(Destination.PlayerZone.NocturneZone.Boons) }
        ),
        CardSet(
            condition = { it.hasType(CardType.HEX, 1) },
            factory = {it.addSet(Destination.PlayerZone.NocturneZone.Hex) }
        ),
    )


    fun createStartingDeck(player: Player, useShelters: Boolean) {
        val discardZone = player.get(Destination.PlayerZone.Discard)
        if (useShelters) {
            listOf("Hovel", "Necropolis", "Overgrown Estate").forEach { cardName ->
                FactorySupplyPile.createNewCard(cardName)?.moveTo(discardZone, Destination.PlayerZone.Discard)
            }
        } else {
            repeat(3) { player.getCardFromSupply("Estate")?.moveTo(discardZone, Destination.PlayerZone.Discard) }
        }

        val heirlooms = player.game.getHeirloomsForGame(player.game)
        heirlooms.forEach { card ->
            card.moveTo(discardZone, Destination.PlayerZone.Discard)
        }

        val nbCopper = 7 - heirlooms.size
        repeat(nbCopper) {
            player.getCardFromSupply("Copper")?.moveTo(discardZone, Destination.PlayerZone.Discard)
        }
    }


    fun initializePlayer(player : Player) {
        initializeCardSetForPlayer(player)
        initializeRulesForPlayer(player)
    }

    fun initializeCardSetForPlayer(player : Player) {
        GAME_CARDSET.forEach { it.initSetForPlayer(player) }
    }

    fun initializeRulesForPlayer(player: Player){
        GAME_COMPONENT_RULES.forEach { rule -> rule.initRuleForPlayer(player) }
    }
}

data class ComponentRule(
    val condition: (Game) -> Boolean,
    val factory: (Player) -> PlayerComponent,
){
    fun initRuleForPlayer(player : Player){
        if(condition(player.game)){
            player.addComponent(factory(player))
        }
    }
}

data class CardSet(
    val condition: (Game) -> Boolean = {true},
    val factory : (Player) -> Unit
){
    fun initSetForPlayer(player: Player){
        if(condition(player.game)){ factory(player) }
    }
}