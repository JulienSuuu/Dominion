package fr.umontpellier.iut.dominion.game

import com.fasterxml.jackson.databind.JsonNode
import fr.umontpellier.iut.dominion.Button
import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.PileComparator
import fr.umontpellier.iut.dominion.Player.*
import fr.umontpellier.iut.dominion.Player.PlayerComponent.PlayerTurnPhase
import fr.umontpellier.iut.dominion.Player.PlayerComponent.startNightPhase
import fr.umontpellier.iut.dominion.Player.Skills.chooseOrder
import fr.umontpellier.iut.dominion.Player.Skills.cleanup
import fr.umontpellier.iut.dominion.Player.Skills.discard
import fr.umontpellier.iut.dominion.Player.Skills.discardFromDeck
import fr.umontpellier.iut.dominion.Player.Skills.discardTo
import fr.umontpellier.iut.dominion.Player.Skills.generalCleanUp
import fr.umontpellier.iut.dominion.Player.Skills.getValidReaction
import fr.umontpellier.iut.dominion.Player.Skills.immunity
import fr.umontpellier.iut.dominion.Player.Skills.moveTo
import fr.umontpellier.iut.dominion.Player.Skills.playTurn
import fr.umontpellier.iut.dominion.Player.Skills.reveals
import fr.umontpellier.iut.dominion.Player.Skills.trash
import fr.umontpellier.iut.dominion.Player.Skills.triggerAnotherTurn
import fr.umontpellier.iut.dominion.Supply.SupplyPile
import fr.umontpellier.iut.dominion.bindCombined
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.Description.InstructionDescription
import fr.umontpellier.iut.dominion.cards.Description.InteractionDescription
import fr.umontpellier.iut.dominion.cards.Description.SecondaryEffect
import fr.umontpellier.iut.dominion.cards.Events.Event
import fr.umontpellier.iut.dominion.cards.Events.TriggerEvent
import fr.umontpellier.iut.dominion.cards.GameStat
import fr.umontpellier.iut.dominion.cards.Id
import fr.umontpellier.iut.dominion.cards.component.CardSelector
import fr.umontpellier.iut.dominion.cards.component.EventLink
import fr.umontpellier.iut.dominion.cards.component.OnSetup
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent
import fr.umontpellier.iut.dominion.cards.factories.Cornucopia_Guilds.CornucopiaRules
import fr.umontpellier.iut.dominion.cards.factories.DA
import fr.umontpellier.iut.dominion.cards.factories.Empires.EmpiresRules
import fr.umontpellier.iut.dominion.cards.factories.FactorySupplyPile
import fr.umontpellier.iut.dominion.cards.factories.Hinterlands.HinterlandsRules
import fr.umontpellier.iut.dominion.cards.factories.Nocturne.NocturneRules
import fr.umontpellier.iut.dominion.cards.factories.countMax
import fr.umontpellier.iut.dominion.cards.factories.createMixedSupplyPile
import fr.umontpellier.iut.dominion.cards.factories.createSupplyPile
import fr.umontpellier.iut.dominion.cards.forEachSuspend
import fr.umontpellier.iut.dominion.cards.getTopCards
import fr.umontpellier.iut.dominion.client.Client
import fr.umontpellier.iut.dominion.game.rules.GameComponent
import fr.umontpellier.iut.dominion.game.rules.GameTurn
import fr.umontpellier.iut.dominion.game.rules.cleanupTurnCard
import fr.umontpellier.iut.dominion.game.rules.resetTurnTracker
import fr.umontpellier.iut.dominion.gui.game.UiStateService
import fr.umontpellier.iut.dominion.gui.UserService
import fr.umontpellier.iut.dominion.gui.Utils
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import java.util.*
import java.util.Collections.emptyList
import kotlin.properties.Delegates
import kotlin.reflect.KClass


import java.util.WeakHashMap
import java.util.Collections

class ShadowKey private constructor(
    val shadowId: Id,
    val activationIndex: Int = 0
) {
    companion object {
        private val cache = Collections.synchronizedMap(WeakHashMap<String, ShadowKey>())


        fun get(shadowId: Id, activationIndex: Int = 0): ShadowKey {
            val lookupKey = "${shadowId.id}_$activationIndex"
            return cache.computeIfAbsent(lookupKey) {
                ShadowKey(shadowId, activationIndex)
            }
        }
    }

    override fun toString(): String = "$shadowId, $activationIndex"

    operator fun plus(other: ShadowKey): ShadowKey {
        return get(Id("${this.shadowId.id}-${other.shadowId.id}"), other.activationIndex)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ShadowKey
        return shadowId.id == other.shadowId.id && activationIndex == other.activationIndex
    }

    fun contains(other: Id) = shadowId.contains(other)

    override fun hashCode(): Int {
        var result = shadowId.id.hashCode()
        result = 31 * result + activationIndex
        return result
    }
}

@Component
open class Game(
    open val context: ApplicationContext,
    open val uiStateService: UiStateService
) {

    data class TypedHandler<T : Event>(val action : suspend (T) -> Unit) : EventLink<T> {
        override suspend fun invoke(event: T) {
            action(event)
        }

    }

    var lastPlayerIdChoice : Client? = null

    lateinit var instance: GameInstance

    val components = mutableMapOf<KClass<out GameComponent>, GameComponent>()

    val factory = FactorySupplyPile

    var currentAlly: Card? = null
    val stat : GameStat = GameStat()
    var banes: String = ""

    lateinit var gameScope : CoroutineScope

    private val _players = mutableListOf<Player>()
    val players: List<Player> get() = _players

    lateinit var currentTurnPlayer: Player
    var currentTurnPlayerProperty: MutableStateFlow<Player?> = MutableStateFlow(null)

    private val namedCards: MutableMap<String, ObservableList<String>> = mutableMapOf()

    private val shadowZone = mutableMapOf<ShadowKey, MutableStateFlow<List<Card>>>()

    private val _kingdomsList = mutableListOf<String>()
    private val expansionAvailable = mutableSetOf<String>()
    private val hasCards = mutableSetOf<String>()

    val eventHandlers: MutableMap<KClass<out Event>, MutableList<EventLink<*>>> = mutableMapOf()

    private val _events = mutableListOf<SupplyPile>()
    val events: List<SupplyPile> get() = _events

    var turnNumber: Int = 1
        

    private var sizeOfCommon: Int = 7

    val coffers = MutableStateFlow(100)

    lateinit var supplyPiles : MutableMap<String, SupplyPile>
    lateinit var asideSupplyPiles : MutableMap<String, MutableList<SupplyPile>>
    private val _trashedCards = MutableStateFlow<List<Card>>(emptyList())
    val trashedCards: List<Card> get() = _trashedCards.value
    private val scanner = Scanner(System.`in`)
    var nbPlayers by Delegates.notNull<Int>()
    var allPilesForSupply = mutableListOf<SupplyPile>()


    open fun init(playerNames: List<Player>, kingdomPiles: List<Card>, events : List<Card>, extras: Map<String, Array<String>>?, instance : GameInstance) {
        this.instance = instance
        gameScope = CoroutineScope(Job(instance.scope.coroutineContext[Job]) + Dispatchers.Default)
        val cornucopia = CornucopiaRules(this)
        val hinterland = HinterlandsRules(this)
        addComponent(GameTurn(this))
        nbPlayers = playerNames.size
        this.asideSupplyPiles = hashMapOf()
        banes = extras?.get("Banes")?.first() ?: ""
        
        events.forEach { _events.add(createSupplyPile(it.name)) }

        val _kingdomsListCard = kingdomPiles +
                (extras?.flatMap { (_, cardNames) -> cardNames.mapNotNull { factory.createCard(it) } }
                    ?: emptyList()) + _events.mapNotNull { it.popCard() }


        val ferrymanCards = extras?.get("Ferryman").orEmpty()

        _kingdomsListCard
            .filter { c ->
                c.name !in ferrymanCards &&
                        !c.hasType(CardType.TEMPLATE) &&
                        !c.hasType(CardType.EVENT) &&
                        !c.hasType(CardType.LANDMARK)
            }
            .forEach { c ->
                allPilesForSupply.add(createSupplyPile(c.name))
            }


        _kingdomsList.addAll(_kingdomsListCard.map { it.name })


        if (factory.isExpansionRequired(_kingdomsList, DA)) {
            val ruins = createMixedSupplyPile("Ruins", factory.getMixedCards(CardType.RUINS)).apply { shuffle() }
            allPilesForSupply.add(ruins)
        }

        _kingdomsListCard.mapNotNull { it.getComponent<OnSetup>() }.filter { !it.neededPlayer }.forEach { onSetup ->
            onSetup.execute(this)
        }

        allPilesForSupply.sortWith(PileComparator())


        val baseCardNames = listOf("Copper", "Silver", "Gold", "Estate", "Duchy", "Province", "Curse")
        baseCardNames.forEach { name ->
            allPilesForSupply.add(createSupplyPile(name))
        }

        if (factory.isExpansionRequired(_kingdomsList, "Alchemy")) {
            allPilesForSupply.add(createSupplyPile("Potion"))
            sizeOfCommon++
        }

        if (factory.isExpansionRequired(_kingdomsList, "Prosperity")) {
            allPilesForSupply.add(createSupplyPile("Platinum"))
            allPilesForSupply.add(createSupplyPile("Colony"))
            sizeOfCommon+=2
        }

        this.supplyPiles = allPilesForSupply.associateByTo(LinkedHashMap()) { it.supplyName }

        extras?.filterKeys { it != "Banes" }?.forEach { (key, cardNames) ->
            val list = asideSupplyPiles.getOrPut(key) { mutableListOf() }
            cardNames.forEach { name ->
                list.add(createSupplyPile(name))
            }
        }

        val required = factory.shouldEnableSpecialty(_kingdomsList, DA, 5)

        allPilesForSupply.forEach { hasCards.add(it.supplyName) }
        asideSupplyPiles.values.forEach { piles -> piles.forEach { hasCards.add(it.supplyName) } }

        if (hasCard("Bandit Camp") || hasCard("Marauder") || hasCard("Pillage")) {
            asideSupplyPiles.getOrPut("Dark Ages") { mutableListOf() }.add(createSupplyPile("Spoils"))
        }
        if (hasCard("Urchin")) {
            asideSupplyPiles.getOrPut("Dark Ages") { mutableListOf() }.add(createSupplyPile("Mercenary"))
        }
        if (hasCard("Hermit")) {
            asideSupplyPiles.getOrPut("Dark Ages") { mutableListOf() }.add(createSupplyPile("Madman"))
        }

        if (hasCard("Footpad")) addListener( cornucopia::footpadPassive)
        if (hasCard("Duchess")) addListener( hinterland::DuchessPassive)

        if(hasExpansion("Nocturne", 1)) addComponent<NocturneRules>(NocturneRules(this))
        if(hasExpansion("Empires", 1)) addComponent<EmpiresRules>(EmpiresRules(this))

        playerNames.forEach { it.init(this, required) }
        _players.addAll(playerNames)

        val firstPlayer = _players.first()
        currentTurnPlayer = firstPlayer
        currentTurnPlayerProperty.value = firstPlayer

        stat.initialize(this, supplyPiles, currentTurnPlayerProperty, players)
        listener()
        specialEffectFromCard(_kingdomsListCard)
        allPilesForSupply = mutableListOf()
    }

    final inline fun < reified C : GameComponent> addComponent(component: C) {
        components[C::class] = component
    }
    final inline fun <reified C : GameComponent> getRule(): C? {
        return components[C::class] as? C
    }

    private fun listener() {
        val cofferFlows: Array<Flow<Int>> = players.map {
            it.getPropertyOf(Item.COFFER) ?: MutableStateFlow(0)
        }.toTypedArray()

        coffers.bindCombined(gameScope, *cofferFlows) {
            250 - players.sumOf { it.getValueOf(Item.COFFER) }
        }

        supplyPiles.values.forEach {
            it.onCardChange = {event ->
                sendToUI(event.toJson("""
                "location": "kingdom"
                """.trimIndent()), lastPlayerIdChoice)}
        }

        asideSupplyPiles.values.forEach {
            it.forEach { supplyPile ->
                supplyPile.onCardChange = {event -> sendToUI(event.toJson("""
                    "location": "aside"
                """.trimIndent()), lastPlayerIdChoice )}
            }
        }

        getRule<NocturneRules>()?.initSupply()
    }

    private fun specialEffectFromCard(list : List<Card>) {
        list.mapNotNull { it.getComponent<OnSetup>() }.filter { it.neededPlayer }.forEach { onSetup ->
            onSetup.execute(this)
        }
    }

    final inline fun <reified T : Event> addListener( noinline action : suspend (T) -> Unit) {
        eventHandlers.computeIfAbsent(T::class) { mutableListOf() }.add(TypedHandler(action))
    }


    suspend fun Player.startHisTurn() {
        state.update { it.changeTurnState(PlayerTurnPhase.StartTurn) }

        state.first{it.turnPhase == PlayerTurnPhase.ActionPhase}

        playTurn()

        state.update { it.changeTurnState(PlayerTurnPhase.StartNightPhase) }
        startNightPhase()

        state.update { it.changeTurnState(PlayerTurnPhase.CleanupPhase) }
        cleanup()

        this@Game.generalCleanUp()

        state.update { it.reset() }
    }

    suspend fun run(){
        while(!stat.isFinished.value){
            log("${currentTurnPlayer.toLog} (turn $turnNumber)", "TURN-TITLE")
            currentTurnPlayer.startHisTurn()
            getRule<EmpiresRules>()?.resetTurnTracker()
            cleanupTurnCard()
            moveToNextPlayer()
        }

        log("Game over", "TURN-TITLE")

        val points = players
            .map { it to it.victoryPoint }
            .sortedByDescending { it.second }

        gameOver(points)

    }

    @Suppress("UNCHECKED_CAST")
    final suspend inline fun <reified T : Event> fireEvent(event : T){
        val links = eventHandlers[T::class] as? List<EventLink<T>>
        links.orEmpty().forEachSuspend {it(event)}
    }

    val availableSupplyCard : List<Card>
        get() = supplyPiles.values.filter(SupplyPile::isNotEmpty).mapNotNull(SupplyPile::popCard)

    val actionSupplyCard : List<Card>
        get() = supplyPiles.values.filter { it.hasType(CardType.ACTION) &&  it.isNotEmpty }.mapNotNull(SupplyPile::popCard)

    fun getCardFromSupply(cardName : String) : Card? {
        var pile = supplyPiles[cardName]
        if(pile == null) {
            pile = supplyPiles.values.firstOrNull { it.verifyName(cardName) }
        }

        return pile?.popCard()
    }

    val eventCards : List<Card>
    get() = events.filter { it.hasType(CardType.EVENT) }.filter(SupplyPile::isNotEmpty).mapNotNull(SupplyPile::popCard)

    fun getEvent(nameEvent : String) : Card? = eventCards.firstOrNull{ it.name == nameEvent }
    val landMarks get() = events.filter { it.hasType(CardType.LANDMARK) }.filter { it.isNotEmpty }.mapNotNull { it.popCard() }

    fun getAvailableAsidePilesCard(nameCard: String) : List<Card> {
        return asideSupplyPiles[nameCard]?.filter(SupplyPile::isNotEmpty)?.mapNotNull(SupplyPile::popCard)?: emptyList()
    }

    fun compareCardToOther(player : Player, name : String, number : Int) : Boolean {
        return getPlayersStartingFrom(player).all { number >= it.allOwnedCards.count{c -> c.hasName(name) }}
    }

    fun getAvailableAsideCard(nameCard: String, key : String) : Card? = getAvailableAsidePilesCard(key).find { it.hasName(nameCard) }


    fun moveCardToTrash(c : Card) {
        c.moveTo(_trashedCards, Destination.Trash)
    }

    fun getPlayerIndex(p : Player) : Int {
        return players.indexOf(p)
    }

    fun getPlayersStartingFrom(player: Player) : List<Player> {
        val ordered = mutableListOf<Player>()
        val start = getPlayerIndex(player)
        for(i in players.indices) {
            ordered.add(players[(start + i) % players.size])
        }
        return ordered
    }


    suspend fun scanImmunity(p : Player, card: Card) : Set<Player> = getPlayersStartingFrom(p).filter { it != p && it.immunity<TriggerComponent.Immunity>(card) }.toSet()

    fun onTheRight(actor : Player, victim : Player) : Boolean {
        val rightIndex: Int = (getPlayerIndex(actor) - 1 + players.size) % players.size
        val ownerIndex: Int = getPlayerIndex(victim)
        return rightIndex == ownerIndex
    }

    fun onTheLeft(actor : Player, victim : Player) : Boolean {
        val leftIndex: Int = (getPlayerIndex(actor) - 1 - players.size) % players.size
        val ownerIndex: Int = getPlayerIndex(victim)
        return leftIndex == ownerIndex
    }

    fun onTheLeft(actor: Player) : Player {
        val index = getPlayerIndex(actor)
        return (if(index == -1) null else players[(players.size + index -1 ) % players.size])!!
    }

    fun onTheRight(actor : Player) : Player {
        val index = getPlayerIndex(actor)
        return (if (index == -1) null else players[(players.size + index - 1) % players.size])!!
    }

    fun moveToNextPlayer(){
        namedCards.clear()
        turnNumber++

        val anotherTurn = currentTurnPlayer.triggerAnotherTurn()
        if(!anotherTurn){
            val currentIndex = getPlayerIndex(currentTurnPlayer)
            val nextIndex = (currentIndex + 1)% players.size
            currentTurnPlayer = players[nextIndex]
            currentTurnPlayerProperty.value = currentTurnPlayer
        }else {
            currentTurnPlayerProperty.value = null
            currentTurnPlayerProperty.value = currentTurnPlayer
        }
    }

    suspend fun generalCleanUp() = coroutineScope {
        getPlayersStartingFrom(currentTurnPlayer)
            .filter { it != currentTurnPlayer }
            .map { player ->
                async {
                    player.state.update { old -> old.changeTurnState(PlayerTurnPhase.CleanupGeneral) }
                    player.generalCleanUp()
                    player.state.update { old -> old.reset() }
                }
            }
            .awaitAll()
    }

    final suspend inline fun <reified T> notifyTrigger(actor : Player, event : Event) where T : TriggerComponent, T : suspend (Player, TriggerEvent) -> Unit {
        val ordered = getPlayersStartingFrom(actor)
        val triggerEvent = TriggerEvent(event)
        for(p in ordered){
            p.getCopyOf(Destination.PlayerZone.InPlay)
                ?.filter { !it.hasType(CardType.REACTION) && it.canExecute<T>(event, p) }
                ?.forEach {
                    processTrigger<T>(it, p, actor, triggerEvent)
                }


            p.chooseOrder<T>(
                "Reveal a Reaction ?",
                {p.getValidReaction()},
                true,
                event,
            ){it.getComponent<T>()?.let { d ->
                triggerEvent.updateScope(it)
                d(p, triggerEvent)
            }}
        }
    }

    final suspend inline fun <reified T> processTrigger(c : Card, owner : Player, actor : Player, event : TriggerEvent) where T : TriggerComponent, T : suspend (Player, TriggerEvent) -> Unit {
        if(isImmune(c, actor)) return
        if(event.destination == null || event.card == null) return
        c.getComponent<T>()?.let {
            event.updateScope(c)
            it(owner, event)
        }
    }

    suspend fun processHandDown(p : Player, c : Card, dest : Destination.PlayerZone = Destination.PlayerZone.Discard, toReach : Int, mayDiscard : Boolean = false, extraAction : suspend (Player) -> Unit = {}) =
        processAttack(p, c) { victim ->
            victim.discardTo(toReach) { card ->
                if(mayDiscard) victim.discard(card)
                else victim.moveTo(card, dest)
                true
            }
            extraAction(victim)
        }

    suspend fun processHandDown(
        attacker : Player,
        attackCard: Card,
        interaction: InteractionDescription? = attackCard.getDescription< InstructionDescription>()?.getTop<InteractionDescription>(),
        extraAction: suspend (Player) -> Unit = {}
        )
    = processAttack(attacker, attackCard){ victim ->
        val toReach = when(val effect = interaction?.secondaryEffect) {
            is SecondaryEffect.DiscardDownTo -> effect.targetHandSize
            else -> victim.sizeOf(Destination.PlayerZone.Hand)
        }
        val to = interaction?.primaryDestination ?: Destination.PlayerZone.Discard
        victim.discardTo(toReach){
            if(to == Destination.PlayerZone.Discard) victim.discard(it)
            else victim.moveTo(it, to)
            true
        }
        extraAction(victim)
    }



    suspend fun processAttackWithReveals(
        attacker : Player,
        attackCard :Card,
        count : Int,
        filter : (Card) -> Boolean,
        selector: CardSelector)
    : List<Card> = getPlayersStartingFrom(attacker).filter { it != attacker && !isImmune(attackCard, it) }
        .mapNotNull {
            val revealed = it.getTopCards(count)
            it.reveals(revealed)
            val targets = revealed.filter(filter).toMutableList()

            var chosen : Card? = null
            if(targets.isNotEmpty()){
                chosen = selector(attacker, it, targets)
                it.trash(chosen)
            }

            revealed.filter { card -> card != chosen }
                .forEach { card -> it.moveTo(card, Destination.PlayerZone.Discard) }

            chosen
        }


    suspend fun processAttackWithReveals(
        attacker: Player,
        attackCard: Card,
        interaction: InteractionDescription? = attackCard.getDescription<InstructionDescription>()?.getTop<InteractionDescription>(),
        selector: CardSelector
    ): List<Card> {
        val count = interaction?.amount ?: 1
        val filter = interaction?.applyFilter() ?: { true }

        return getPlayersStartingFrom(attacker)
            .filter { it != attacker && !isImmune(attackCard, it) }
            .mapNotNull { victim ->
                val revealed = victim.getTopCards(count)
                victim.reveals(revealed)

                val targets = revealed.filter(filter).toMutableList()
                var chosen: Card? = null

                if (targets.isNotEmpty()) {
                    chosen = selector(attacker, victim, targets)
                    victim.trash(chosen)
                }

                val remainingDestination = when (interaction?.secondaryEffect) {
                    is SecondaryEffect.DiscardRemaining -> Destination.PlayerZone.Discard
                    else -> Destination.PlayerZone.Discard
                }

                revealed.filter { card -> card != chosen }
                    .forEach { card -> victim.moveTo(card, remainingDestination) }

                chosen
            }
    }

    suspend fun processDiscard(p : Player, c : Card) = processAttack(p, c, Player::discardFromDeck)

    suspend fun checkHandOrShow(attacker: Player, attackCard: Card, filter: (Card) -> Boolean, destination: Destination = Destination.PlayerZone.Discard, decision : suspend (Player, List<Card>) -> Card?){
        processAttack(attacker, attackCard){
            val validCards = it.getCopyOf(Destination.PlayerZone.Hand)?.filter(filter) ?: emptyList()
            decision(attacker, validCards)?.let {card ->
                it.moveTo(card, destination)
            } ?:run { it.reveals(validCards) }
        }
    }

    suspend fun checkHandOrShow(
        attacker : Player,
        attackCard : Card,
        interaction: InteractionDescription? = attackCard.getDescription<InstructionDescription>()?.getTop<InteractionDescription>(),
        decision : suspend (Player, List<Card>) -> Card?
    ) = processAttack(attacker, attackCard){
        val filter = interaction?.applyFilter() ?: { true }
        val from = interaction?.fromZone ?: Destination.PlayerZone.Hand
        val validCards = it.getList(from).filter(filter)
        decision(it, validCards)?.let {card ->
            val destination = interaction?.primaryDestination ?: Destination.PlayerZone.Discard
            it.moveTo(card, destination)
        } ?: run {
            when(interaction?.secondaryEffect) {
                is SecondaryEffect.RevealHandIfNoMatch -> it.reveals(it.getList(Destination.PlayerZone.Hand))
                else -> it.reveals(validCards)
            }
        }
    }

    suspend fun chooseCard(p : Player, treasure : List<Card>) : Card? = p.chooseCardFromList("Move a card from this list", cards = treasure, canPass = true )

    suspend fun isImmune(c : Card, player : Player) : Boolean {
        if(!c.hasType(CardType.ATTACK)) return false
        if(c.getCollection<Player>("Players").contains(player)) return false
        return player.immunity<TriggerComponent.Immunity>(c)
    }

    suspend fun processAttack(attacker : Player, attackCard : Card, playerLogic : suspend (Player) -> Unit){
        process(attacker, playerLogic) { it != attacker && !isImmune(attackCard, it) }
    }

    suspend fun processBenefit(attacker: Player, playerLogic : suspend (Player) -> Unit){
        process(attacker, playerLogic) { it != attacker }
    }

    suspend fun processGlobalEffect(actor : Player, playerLogic : suspend (Player) -> Unit){
        process(actor, playerLogic)
    }


    private suspend fun process(attacker: Player, playerLogic : suspend (Player) -> Unit, filter : suspend (Player) -> Boolean = {true}){
        getPlayersStartingFrom(attacker)
            .filter{p -> filter(p)}
            .forEachSuspend(playerLogic)
    }

    fun setToken(name : String) = supplyPiles[name]?.let { it.cursed++ }
    fun hasToken(name: String): Boolean = (supplyPiles[name]?.cursed ?: 0) > 0
    fun getToken(name: String): Int = supplyPiles[name]?.cursed?: 0
    fun tradeRoute(c: Card?): Int {
        if (c?.hasType(CardType.VICTORY) == false) return 0
        val pile = supplyPiles[c?.name?:""] ?: return 0
        val tokenCount = pile.token
        pile.token = 0
        return tokenCount
    }

    val isActionPhase : Boolean get() = currentTurnPlayer.currentlyInTurn(PlayerTurnPhase.ActionPhase)

    fun hasCard(nameCard : String) : Boolean {
        if (!hasCards.contains(nameCard)){
            if(allSupply.any { it.verifyName(nameCard) }){
                hasCards.add(nameCard)
            }
        }
        return hasCards.contains(nameCard)
    }

    fun hasExpansion(expansion : String, threshold : Int) : Boolean {
        if(!expansionAvailable.contains(expansion)) {
            val shouldEnable = factory.shouldEnableSpecialty(allSupply.map { it.name }, expansion, threshold)
            if(shouldEnable){ expansionAvailable.add(expansion) }
        }

        return expansionAvailable.contains(expansion)
    }

    fun hasType(type : CardType, threshold: Int) : Boolean = allSupply.countMax(threshold){it.hasType(type)}

    fun verifyPileToken(toCheck : String, cardName : String) : Boolean {
        val pile = supplyPiles[toCheck] ?: supplyPiles.values.firstOrNull { it.verifyName(toCheck) }
        return pile?.verifyName(cardName) ?: false
    }

    val allSupply get() = supplyPiles.values + asideSupplyPiles.values.flatten() + events


    fun getNamedCardsThisTurn(key : String) : ObservableList<String> = namedCards.computeIfAbsent(key, { FXCollections.observableArrayList() })

    fun shadowZone(key : ShadowKey) = shadowZone.getOrPut(key){ MutableStateFlow(emptyList()) }
    fun shadowList(key : ShadowKey): List<Card> = shadowZone[key]?.value ?: emptyList()
    fun clearShadowZone(key : ShadowKey) {
        shadowZone(key).update { emptyList() }
        shadowZone.remove(key)
    }

    fun addListToShadow(key : ShadowKey, cards : List<Card>) {
        shadowZone(key).update { it + cards } }

    fun updateShadowZone(key : ShadowKey, newList : List<Card>){
        when{
            newList.isEmpty() -> shadowZone.remove(key)
            else -> shadowZone(key).update { newList }
        }
    }
    fun allListFromShadowZoneFor(key : ShadowKey) : List<Card> {
        return shadowZone.entries
            .filter{(register, _) -> key.shadowId == register.shadowId}
            .flatMap { (_, list) -> list.value }
    }

    fun allKeysFor(key : ShadowKey) = shadowZone.keys.filter { it.shadowId == key.shadowId }.sortedBy { it.activationIndex }

    fun shadowZoneToJson(key: Id): String {
        val playerShadowZone = shadowZone.filter { (k, _) -> k.contains(key) }

        val entriesJson = playerShadowZone.entries.joinToString(separator = ",\n        ") { (k, flow) ->
            val jsonList = flow.value.joinToString(prefix = "[", postfix = "]") { it.toShadowZoneJson() }
            "\"${k.shadowId.cardId()}\": $jsonList"
        }

        return """
            "shadowZone": {
                $entriesJson
            }
    """.trimIndent()
    }

    open fun sendToUI(message: String) = Unit
    open fun sendToUI(message: String, session : Client?) = Unit
    open suspend fun readLine() : JsonNode? = null

    fun log(message: String, type: String = "INFO") {
        val timestamp = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val escapedMessage = message
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")

        sendToUI(
            """
        {
          "logChange": {
            "id": "$id",
            "type": "$type",
            "text": "$escapedMessage",
            "timestamp": "$timestamp"
          }
        }
        """.trimIndent()
        )
    }

    fun prompt(instruction : String, choices: List<String>, allCards : List<String>, buttons : List<Button>, activePlayerIndex: Int){
        val choiceField = listOf(
            "\"instruction\": \"$instruction\"",
            "\"choices\": [${choices.joinToString(", ") { "\"$it\"" }}]",
            "\"buttons\": [${buttons.joinToString(", ") { "{\"label\": \"${it.label()}\", \"value\": \"${it.value()}\"}" }}]",
            "\"selection_cards\": [${allCards.filter { it.startsWith("SELECT_CARD:") }.joinToString(", ") { "\"$it\"" }}]",
            "\"mode\": ${uiStateService.isPromptActive}"
        )

        for(p in players){
            val fields = mutableListOf(
                "\"game\": ${toJSON(p)}",
                "\"active_player\": ${players.indexOf(p) == activePlayerIndex}",
            )

            if(players.indexOf(p) == activePlayerIndex) {
                fields += choiceField
            }

            val jsonResult = fields.joinToString(", ", prefix = "{", postfix = "}")
            sendToUI(jsonResult, p.client)
        }
    }

    fun gameOver(classement : List<Pair<Player, Int>>){

        val userService = context.getBean<UserService>()

        classement.forEach { (player, victoryPoints) ->
            log("${player.toLog}: $victoryPoints Points ")
            log(Utils.toLog(player.allOwnedCards))
        }

        if (classement.isNotEmpty()) {
            val winner = classement.first().first

            classement.forEach { (player, score) ->
                val client = player.client
                val isWinner = (player == winner)

                userService.updateStatsAfterGame(client, isWinner = isWinner, score = score, instance.gameId)
            }
        }

        val classementJson = classement.joinToString(prefix = "[", postfix = "]") {
            """{ "client": ${it.first.toGameOverJson()}, "points": ${it.second} }"""
        }

        val json = """
        {
        "gameId": "${instance.gameId}",
        "view" : "GAME_OVER",
        "classement": $classementJson
        }
        """.trimIndent()

        endGame(json)
    }

    open fun endGame(message : String) {}

    override fun toString(): String {
        val title = "     -- ${currentTurnPlayer.name}'s Turn --\n"

        val piles = supplyPiles.values.joinToString(separator = "   ") { pile ->
            if (pile.isEmpty) {
                "[Empty pile]"
            } else {
                val c = pile.popCard()
                "${c?.name} x${pile.size}(${c?.costValue})"
            }
        }

        return "$title$piles\n"
    }

    fun toJSON(player: Player): String {
        val turnPlayerId = currentTurnPlayer.client.id

        val fields = mutableListOf(
            "\"turn_player\": \"$turnPlayerId\"",
            "\"client\": ${player.toJSON()}",
            "\"supply\": [${getSupplyPilesJson(supplyPiles.values)}]",
            "\"aside\": ${getCategorizedAsideJson(asideSupplyPiles)}",
            "\"events\": [${getSupplyPilesJson(events)}]",
            "\"size\": $sizeOfCommon",
            "\"players\": [${players.filter { it != player }.joinToString(", ") { it.toSideJson() }}]",
        )

        getRule<NocturneRules>()?.let { fields.add("\"nocturne\": ${it.toJson()}") }
        getRule<EmpiresRules>()?.let { fields.add("\"empire\": ${it.toJson()}") }

        return fields.joinToString(", ", prefix = "{", postfix = "}")
    }

    private fun getCategorizedAsideJson(asideMap: Map<String, List<SupplyPile>>?): String {
        if (asideMap.isNullOrEmpty()) return "{}"

        return asideMap.entries.joinToString(", ", prefix = "{", postfix = "}") { (categoryName, piles) ->
            "\"$categoryName\": [${getSupplyPilesJson(piles)}]"
        }
    }

    fun getSupplyPilesJson(piles: Collection<SupplyPile>): String =
        piles.joinToString(", ") { it.toJsonString() }

}

fun SupplyPile.toJsonString(): String {
    val lastJson = topCard?.let { ",\n \"card\": ${it.toJson()}" } ?: ""

    return """
        {
            "name": "$name",
            "number": $size $lastJson
        }
    """.trimIndent()
}