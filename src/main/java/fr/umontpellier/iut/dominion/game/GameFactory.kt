package fr.umontpellier.iut.dominion.game

import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.PileComparator
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Supply.SupplyPile
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.component.specification.CofferCard
import fr.umontpellier.iut.dominion.cards.component.OnSetup
import fr.umontpellier.iut.dominion.cards.component.specification.BaneCard
import fr.umontpellier.iut.dominion.cards.factories.CG
import fr.umontpellier.iut.dominion.cards.factories.DA
import fr.umontpellier.iut.dominion.cards.factories.Empires.EmpiresRules
import fr.umontpellier.iut.dominion.cards.factories.FactorySupplyPile
import fr.umontpellier.iut.dominion.cards.factories.Nocturne.NocturneRules
import fr.umontpellier.iut.dominion.cards.factories.createMixedSupplyPile
import fr.umontpellier.iut.dominion.cards.factories.createSupplyPile
import fr.umontpellier.iut.dominion.game.rules.CoffersRule
import fr.umontpellier.iut.dominion.game.rules.GameTurn
import fr.umontpellier.iut.dominion.game.rules.TaxTokenRule

object GameFactory {
    private val factory = FactorySupplyPile
    val baseCardNames = listOf("Copper", "Silver", "Gold", "Estate", "Duchy", "Province", "Curse")

    private val GAME_COMPONENT = listOf(
        GameComponent(
            condition = {true},
            action = {it.addComponent(GameTurn(it))}
        ),
        GameComponent(
            condition = {it.hasExpansion("Nocturne", 1)},
            action = {it.addComponent(NocturneRules(it))}
        ),
        GameComponent(
            condition = {it.hasExpansion("Empires", 1)},
            action = {it.addComponent(EmpiresRules(it))}
        ),
        GameComponent(
            condition = {it.hasExpansion("Adventures", 1)},
            action = {it.addComponent(TaxTokenRule(it))}
        ),
        GameComponent(
            condition = {
                if(it.hasExpansion(CG, 1)) true
                else it.allSupply.any{pile -> pile.peekCard()?.hasComponent<CofferCard>() == true}
            },
            action = {it.addComponent(CoffersRule(it))}
        )
    )
    private val GAME_RULES = listOf(
        GameRule(
            condition = { game, piles -> factory.isExpansionRequired(piles, "Prosperity") },
            action = { game, piles ->
                piles.add(game.createSupplyPile("Platinum"))
                piles.add(game.createSupplyPile("Colony"))
                game.sizeOfCommon+=2
            }
        ),
        GameRule(
            condition = { game, piles -> factory.isExpansionRequired(piles, "Alchemy") },
            action = { game, piles -> piles.add(game.createSupplyPile("Potion")); game.sizeOfCommon++ }
        ),
        GameRule(
            condition = { game, piles -> factory.isExpansionRequired(piles, DA) },
            action = { game, piles -> piles.add(game.createMixedSupplyPile("Ruins", factory.getMixedCards(CardType.RUINS)).apply { shuffle() }); game.sizeOfCommon++ }
        )
    )

    private fun applyPileRules(game: Game, piles : MutableList<SupplyPile>) {
        val names = piles.map { it.name }
        GAME_RULES.forEach { rule ->
            if (rule.condition(game, names)) {
                rule.action(game, piles)
            }
        }
    }

    fun setUpBasePiles(game : Game, allSupplyPile : MutableList<SupplyPile>) : MutableList<SupplyPile> {
        baseCardNames.forEach { name ->
            allSupplyPile.add(game.createSupplyPile(name))
        }

        applyPileRules(game, allSupplyPile)

        return allSupplyPile
    }

    fun setUpAsidePiles(game : Game, extras : Map<String, Array<String>>?, action : Game.() -> Unit ) {
        extras?.filterKeys { it != "Banes" }?.forEach { (key, cardNames) ->
            cardNames.forEach { name -> game.addAsideSupplyPile(key, name) }
        }

        game.action()
    }

    fun setUpRules(game : Game){
        GAME_COMPONENT.forEach { rule ->
            if(rule.condition(game)){
                rule.action(game)
            }
        }
    }

    fun setUpKingdomAndEvent(
        game: Game,
        kingdomPiles: List<Card>,
        events: List<Card>,
        extras: Map<String, Array<String>>?
    ): MutableList<SupplyPile> {

        events.forEach { event -> game.addEvents(event.name) }
        val asideKeys = setOf("Ferryman", "BlackMarket")

        extras?.filterKeys { it in asideKeys }?.forEach { (key, cardNames) ->
            cardNames.forEach { cardName -> game.addAsideSupplyPile(key, cardName) }
        }

        val kingdomExtraCards = extras?.filterKeys { it !in asideKeys }
            ?.flatMap { (_, cardNames) -> cardNames.mapNotNull { FactorySupplyPile.createCard(it) } }
            .orEmpty()

        val _kingdomsListCard = kingdomPiles + kingdomExtraCards + game.events.mapNotNull { it.peekCard() }

        val allSupplyPile = mutableListOf<SupplyPile>()

        _kingdomsListCard
            .filter { c ->
                !c.hasType(CardType.TEMPLATE) &&
                        !c.hasType(CardType.EVENT) &&
                        !c.hasType(CardType.LANDMARK)
            }
            .forEach { c -> allSupplyPile.add(game.createSupplyPile(c.name)) }

        extras?.get("Banes")?.firstOrNull()?.let { baneName ->
            allSupplyPile.find { it.peekCard()?.name == baneName }
                ?.forEach { it.register(BaneCard) }
        }

        _kingdomsListCard
            .mapNotNull { it.getComponent<OnSetup>() }
            .filter { !it.neededPlayer }
            .forEach { onSetup -> onSetup.execute(game, allSupplyPile) }

        allSupplyPile.sortWith(PileComparator())

        return allSupplyPile
    }

    fun initializePlayers(game : Game, players : List<Player>, _players : MutableList<Player>) {
        players.forEach { player -> player.init(game, game.hasExpansion(DA, 5)) }
        _players.addAll(players)

        val firstPlayer = _players.first()
        game.currentTurnPlayer = firstPlayer
        game.currentTurnPlayerProperty.value = firstPlayer
    }
}

data class GameRule(
    val condition: (Game, List<String>) -> Boolean,
    val action: (Game, MutableList<SupplyPile>) -> Unit
)

data class GameComponent(
    val condition : (Game) -> Boolean,
    val action : (Game) -> Unit
)

