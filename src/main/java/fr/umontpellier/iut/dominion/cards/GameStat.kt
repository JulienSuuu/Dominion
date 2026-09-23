package fr.umontpellier.iut.dominion.cards

import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.game.Game
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.Tokens.Token
import fr.umontpellier.iut.dominion.Properties
import fr.umontpellier.iut.dominion.Supply.SupplyPile
import fr.umontpellier.iut.dominion.addListener
import fr.umontpellier.iut.dominion.bind
import fr.umontpellier.iut.dominion.bindCombined
import fr.umontpellier.iut.dominion.cards.component.OnPlayComponent
import fr.umontpellier.iut.dominion.Player.PlayerComponent.askReductionToken
import fr.umontpellier.iut.dominion.Player.PlayerComponent.getToken
import fr.umontpellier.iut.dominion.unbind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.collections.toTypedArray
import kotlin.math.max

class GameStat {
    lateinit var  gameStatScope : CoroutineScope
    val charlatanPower = MutableStateFlow(false)
    val reduction = MutableStateFlow(0)
    val emptyPiles = MutableStateFlow(0)
    val isFinished = MutableStateFlow(false)
    val firstProvinceGain = MutableStateFlow(false)


    lateinit var allCardsInSupply: Map<String, SupplyPile>
        private set
    lateinit var players: List<Player>
        private set
    lateinit var currentPlayer: MutableStateFlow<Player?>
    lateinit var Game : Game

    fun initialize(
        game : Game,
        allCards: Map<String, SupplyPile>,
        currentTurnPlayer: MutableStateFlow<Player?>,
        playerList: List<Player>
    ) {
        allCardsInSupply = allCards
        players = playerList
        currentPlayer = currentTurnPlayer
        Game = game
        gameStatScope = game.gameScope

        val supplyFlows: Array<Flow<*>> = allCardsInSupply.values
            .map { pile -> pile.cards }
            .toTypedArray()


        charlatanPower.bindCombined(gameStatScope, *supplyFlows){
            allCardsInSupply.containsKey("Charlatan")
        }

        if(charlatanPower.value){
            allCardsInSupply["Curse"]?.forEach { it.addType(CardType.TREASURE) }
        }

        emptyPiles.bindCombined(gameStatScope, *supplyFlows){
            allCardsInSupply.values.count{it.isEmpty}
        }

        val provinceEmpty = MutableStateFlow(false)
        val colonyEmpty = MutableStateFlow(false)

        provinceEmpty.bindCombined(gameStatScope, emptyPiles){
            allCards.containsKey("Province") && allCards["Province"]?.isEmpty == true
        }

        colonyEmpty.bindCombined(gameStatScope, emptyPiles){
            allCards.containsKey("Colony") && allCards["Colony"]?.isEmpty == true
        }

        isFinished.bind(gameStatScope, provinceEmpty or colonyEmpty or emptyPiles.greaterThanOrEqualTo(3))

        updatePlayer(currentTurnPlayer)
    }

    fun updatePlayer(current: MutableStateFlow<Player?>) {
        allCardsInSupply.values.forEach { pile ->
            pile.priceProperty().unbind()

            pile.priceProperty().bindCombined(gameStatScope, currentPlayer, reduction, pile.cards)
            {
                val player = currentPlayer.value
                val topCard = if (pile.isEmpty) null else pile.popCard() ?: return@bindCombined 0

                val baseCost = topCard?.basicPrice() ?: 0
                val redGlobale = reduction.value

                if (player == null) return@bindCombined max(0, baseCost - redGlobale)

                val pRedPeddler = player.getProperties(Properties.puddlerReduction)
                val pRedQuarry = player.getProperties(Properties.quarryReduction)

                val pRedToken = if (player.askReductionToken(topCard?.name)) 2 else 0
                val quarryRed = if (topCard?.hasType(CardType.ACTION) == true) pRedQuarry.value else 0
                val peddlerRed = if (topCard?.name == "Peddler") pRedPeddler.value else 0

                max(0, baseCost - redGlobale - peddlerRed - quarryRed - pRedToken)
            }

            pile.update(gameStatScope)
        }

        currentPlayer.addListener(gameStatScope) {oldPlayer, player ->
            if (player == null) return@addListener

            val allEstates = mutableListOf<Card>()
                .apply {
                    allCardsInSupply["Estates"]?.let { addAll(it.cards.value) }
                    players.forEach { addAll(it.allOwnedCards.filter { c -> c.hasName("Estate") }) }
                }

            val inheritedName = player.getToken(Token.OnPile.EstateToken)
            val template = if (inheritedName.isNotEmpty()) {
                player.getList(Destination.PlayerZone.Aside).firstOrNull { it.hasName(inheritedName) }
            } else null

            val activePlayersEstate = player.allOwnedCards.filter { c -> c.hasName("Estate") }

            allEstates.forEach {
                it.removeType(CardType.ACTION)
                it.removeType(CardType.COMMAND)
                it.removeComponent<OnPlayComponent>()

                if (template != null && activePlayersEstate.contains(it)) {
                    it.addType(CardType.ACTION)
                    it.addType(CardType.COMMAND)
                    it.setup {
                            onPlay(
                                template.copy().getComponent<OnPlayComponent>()
                                ?: OnPlayComponent { _, _ -> })
                    }
                }
            }
        }

    }
}