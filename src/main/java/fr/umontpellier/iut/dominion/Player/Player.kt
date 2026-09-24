package fr.umontpellier.iut.dominion.Player


import fr.umontpellier.iut.dominion.Annotation.Selection_Mode
import fr.umontpellier.iut.dominion.Button
import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Flags
import fr.umontpellier.iut.dominion.game.Game
import fr.umontpellier.iut.dominion.Interface.Logger
import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.PlayerComponent.Night
import fr.umontpellier.iut.dominion.Player.PlayerComponent.PlayerComponent
import fr.umontpellier.iut.dominion.Player.PlayerComponent.PlayerInteractionState
import fr.umontpellier.iut.dominion.Player.PlayerComponent.PlayerInTurnState
import fr.umontpellier.iut.dominion.Player.PlayerComponent.PlayerState
import fr.umontpellier.iut.dominion.Player.PlayerComponent.PlayerTurnPhase
import fr.umontpellier.iut.dominion.Player.PlayerComponent.PossessionComponent
import fr.umontpellier.iut.dominion.Player.Tokens.Token
import fr.umontpellier.iut.dominion.Properties
import fr.umontpellier.iut.dominion.game.ShadowKey
import fr.umontpellier.iut.dominion.bindComputed
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.Id
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent
import fr.umontpellier.iut.dominion.Player.PlayerComponent.TokenComponent
import fr.umontpellier.iut.dominion.Player.PlayerComponent.TurnHistoryComponent
import fr.umontpellier.iut.dominion.Player.PlayerComponent.isAffectedBy
import fr.umontpellier.iut.dominion.Player.PlayerComponent.updateTokenFlag
import fr.umontpellier.iut.dominion.Player.Skills.choose
import fr.umontpellier.iut.dominion.Player.Skills.chooseOrder
import fr.umontpellier.iut.dominion.Player.Skills.computeChoices
import fr.umontpellier.iut.dominion.Player.Skills.handleActionPhase
import fr.umontpellier.iut.dominion.Player.Skills.handleStartBuyPhase
import fr.umontpellier.iut.dominion.Player.Skills.handleStartTurn
import fr.umontpellier.iut.dominion.Player.Skills.moveCardsFromTemp
import fr.umontpellier.iut.dominion.Player.Skills.privateChooseWhatToDo
import fr.umontpellier.iut.dominion.Player.Skills.privateLog
import fr.umontpellier.iut.dominion.cards.component.PokerHandReactionComponent
import fr.umontpellier.iut.dominion.cards.factories.FactorySupplyPile
import fr.umontpellier.iut.dominion.cards.factories.Futaba.EvaluatedPokerHand
import fr.umontpellier.iut.dominion.cards.factories.Futaba.PokerHandEvaluator
import fr.umontpellier.iut.dominion.cards.factories.Nocturne.underState
import fr.umontpellier.iut.dominion.cards.plusAssign
import fr.umontpellier.iut.dominion.client.Client
import fr.umontpellier.iut.dominion.client.StatKey
import fr.umontpellier.iut.dominion.gui.game.ItemChangeEvent
import fr.umontpellier.iut.dominion.gui.Utils
import fr.umontpellier.iut.dominion.isBound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.util.*
import kotlin.reflect.KClass

@JvmInline
value class PlayerMessage(val message : String)



@Component
@Scope("prototype")
open class Player : Logger {

    companion object {
        private val colors = listOf(
            "#e74c3c", "#3498db", "#2ecc71", "#f1c40f", "#9b59b6", "#e67e22", "#1abc9c"
        )
        private var idCounter = 0
        val tempEnd : PlayerMessage = PlayerMessage("tempEnd")
        val clearShadowZone : PlayerMessage = PlayerMessage("clearShadowZone")
    }

    lateinit var self: Player
        @Autowired
        @Lazy
        set

    override lateinit var name: String
    var id = idCounter++
    lateinit var client : Client

    private lateinit var playerScope : CoroutineScope
    private val cardSet = hashMapOf<Destination.PlayerZone, MutableStateFlow<List<Card>>>()
    private val items = EnumMap<Item, MutableStateFlow<Int>>(Item::class.java)
    val state = MutableStateFlow(PlayerState())

    fun currentlyInTurn(check : PlayerTurnPhase) = state.value.turnPhase == check
    fun currentlyInState(check : PlayerInTurnState) = state.value.inTurnState == check
    fun currentlyInInteraction(check : PlayerInteractionState) = state.value.interaction == check

    val clientState get() = client.uiState


    private val flags = hashMapOf<String, MutableStateFlow<Boolean>>()
    private val properties = hashMapOf<String, MutableStateFlow<Int>>()
    private val discardHooks = mutableListOf<TriggerComponent.discardHook>()
    internal val hooks: MutableList<TriggerComponent.discardHook> get() = discardHooks
    private val permanentFlags = hashMapOf<String, MutableStateFlow<Boolean>>()
    private val playerTempZone = hashMapOf<Id, MutableStateFlow<List<Card>>>()

    val playerComponent = mutableMapOf<KClass<out PlayerComponent>, PlayerComponent>()

    lateinit var shadowKey : ShadowKey

    private val persistent: EnumSet<Item> = EnumSet.of(
        Item.DEBT,
        Item.COFFER,
        Item.VICTORY_TOKEN,
        Item.COIN_TOKEN_SHIP
    )

    var drawBonusNextTurn: Int = 0

     override lateinit var game: Game
        

    var isSecondTurn: Boolean = false

    //val activeEventEffect = mutableListOf<EventCard>()


    val nextTurnEffect = mutableListOf<suspend Player.() -> Unit>()
    val endTurnEffect = mutableListOf<suspend Player.() -> Unit>()
    val cleanUpEffect = mutableListOf<suspend Player.() -> Unit>()
    //val gainEffect = mutableListOf<BiEffect<Player, Card>>()

    fun init(game: Game, request: Boolean) {
        this.game = game
        this.playerScope = game.gameScope
        addComponent(TurnHistoryComponent(self))

        shadowKey = ShadowKey.get(Id("${name}-$id"))

        val basePlayerZones = listOf(
            Destination.PlayerZone.Hand,
            Destination.PlayerZone.InPlay,
            Destination.PlayerZone.Discard,
            Destination.PlayerZone.Draw,
            Destination.PlayerZone.Aside,
        )

        basePlayerZones.forEach { destination ->
            cardSet[destination] = MutableStateFlow(emptyList())
        }

        if(game.hasCard("Island")) cardSet[Destination.OtherZone.Island] = MutableStateFlow(emptyList())
        if(game.hasCard("Native Village")) cardSet[Destination.OtherZone.Native] = MutableStateFlow(emptyList())
        if(game.hasType(CardType.RESERVE, 1)) cardSet[Destination.OtherZone.Tavern] = MutableStateFlow(emptyList())

        if(game.hasType(CardType.BOON, 1)) cardSet[Destination.PlayerZone.NocturneZone.Boons] = MutableStateFlow(emptyList())
        if(game.hasType(CardType.HEX, 1)) cardSet[Destination.PlayerZone.NocturneZone.Hex] = MutableStateFlow(emptyList())

        cardSet[Destination.TempZone.Temp] = MutableStateFlow(emptyList())


        Item.entries.forEach { item ->
            items[item] = MutableStateFlow(0)
        }

        if (request) {
            FactorySupplyPile.createCard("Hovel")?.moveTo(get(Destination.PlayerZone.Discard), Destination.PlayerZone.Discard)
            FactorySupplyPile.createCard("Necropolis")?.moveTo(get(Destination.PlayerZone.Discard), Destination.PlayerZone.Discard)
            FactorySupplyPile.createCard("Overgrown Estate")?.moveTo(get(Destination.PlayerZone.Discard), Destination.PlayerZone.Discard)
        } else {
            repeat(3) {
                getCardFromSupply("Estate")?.moveTo(get(Destination.PlayerZone.Discard), Destination.PlayerZone.Discard)
            }
        }


        if(game.hasType(CardType.NIGHT, 1)) {

        }
        else {
            repeat(7) {
                getCardFromSupply("Copper")?.moveTo(get(Destination.PlayerZone.Discard), Destination.PlayerZone.Discard)
            }
        }

        shuffle()

        repeat(5){
            getList(Destination.PlayerZone.Draw).last().moveTo(get(Destination.PlayerZone.Hand), Destination.PlayerZone.Hand)
        }

        if(game.hasType(CardType.NIGHT, 1)) addComponent(Night(self))
        if(game.hasCard("Possession")) addComponent(PossessionComponent(self))
        if(game.hasExpansion("Adventures", 1)) addComponent(TokenComponent(self))

        listener()
    }

    final inline fun <reified T : PlayerComponent> addComponent(component : T) {
        playerComponent[T::class] = component
    }

    final inline fun <reified T : PlayerComponent> getComponent() : T? = playerComponent[T::class] as? T


    fun inPlayListener(){
        val inPlayFlow = get(Destination.PlayerZone.InPlay) ?:return

        getFlag(Flags.COPPER_PLAYED).bindComputed(playerScope, inPlayFlow){
                cards -> cards.any{it.hasName("Copper")}
        }

        getFlag("Active").bindComputed(playerScope, game.currentTurnPlayerProperty){
                _ -> isActive
        }

        if (game.hasCard("Peddler"))
            getProperties(Properties.puddlerReduction).bindComputed(playerScope, inPlayFlow) {
                    cards -> cards.count { it.hasType(CardType.ACTION) } * 2
            }

        if(game.hasCard("Crossroads")){
            getFlag(Flags.playedCrossroads).bindComputed(playerScope, inPlayFlow){
                    cards -> cards.any{it.hasName("Crossroads")}
            }
        }

        if(game.hasCard("Fool's Gold")){
            getFlag(Flags.playedFoolsGold).bindComputed(playerScope, inPlayFlow){
                    cards -> cards.count{it.hasName("Fool's Gold")} >= 2
            }
        }
    }

    fun stateListener(){
        state.map { it.turnPhase }
            .distinctUntilChanged()
            .onEach { phase ->
                when (phase) {
                    PlayerTurnPhase.Waiting -> {}

                    PlayerTurnPhase.StartTurn -> {
                        playerScope.launch {
                            handleStartTurn()
                            handleActionPhase()

                            state.update { it.changeTurnState(PlayerTurnPhase.ActionPhase) }
                        }
                    }

                    PlayerTurnPhase.StartBuyPhase -> {
                        playerScope.launch {
                            handleStartBuyPhase()

                            state.update { it.changeTurnState(PlayerTurnPhase.BuyPhase) }
                        }
                    }

                    else -> {}
                }
            }
            .launchIn(playerScope)

        items.forEach { (item, flow) -> observeItemChanges(item, flow) }
    }

    private data class PokerCheckResult(
        val evaluatedHand: EvaluatedPokerHand?,
        val isActionPhase: Boolean
    )

    fun handListener(){}


    fun listener(){
        handListener()
        inPlayListener()
        stateListener()
    }


    private fun observeItemChanges(itemType: Item, flow: StateFlow<Int>) {
        flow
            .combine(state) { value, phase -> value to phase }
            .scan(Triple(0, 0, PlayerState())) { (_, oldVal, _), (newVal, phase) ->
                Triple(oldVal, newVal, phase)
            }
            .onEach { (oldValue, newValue, state) ->
                if (oldValue != newValue) {
                    val delta = newValue - oldValue
                    val playerId = client.id

                    val event = ItemChangeEvent(
                        playerId = playerId,
                        item = itemType.name,
                        old = oldValue,
                        new = newValue,
                        delta = delta
                    )

                    when {
                        !state.isIgnoringStats -> {
                            if (delta < 0) {
                                client.recordHoverDetail(StatKey.RESOURCES_USED, itemType.name, delta * -1)
                            } else {
                                client.recordHoverDetail(StatKey.RESOURCES_OBTAINED, itemType.name, delta)
                            }
                        }
                    }
                    game.sendToUI(event.toJson())
                }
            }
            .launchIn(playerScope)
    }

    fun returnToActionPhase(){
        resetFlags()
        getFlag("Action") += true
        getFlag("Treasure") += true
    }

//    fun addGainEffect(effect : BiEffect<Player, Card>){
//        if(gainEffect.contains(effect))return
//        gainEffect.add(effect)
//    }


    internal fun clearList(){
        getComponent<TurnHistoryComponent>()?.reset()
        discardHooks.clear()
    }


    internal fun resetFlags(){
        flags.forEach { (_, property) ->
            if(!property.isBound){
                property.value = false
            }
        }
    }

    internal fun resetProperties(){
        properties.forEach { (_, value) ->
            if(!value.isBound){
                value.value = 0
            }
        }
    }

    internal fun resetItems(){
        game.stat.reduction.value = 0
        Item.entries.forEach { item ->
            if(item !in persistent){
                items[item]?.value = 0
            }
        }
    }


    internal fun setUpTurn(){
        increment(Item.ACTION)
        increment(Item.BUY)
    }

    fun getIndex() : Int = game.getPlayerIndex(self)

    val money: Int get() = items[Item.MONEY]?.value ?: 0

    val numberOfAction: Int
        get() = items[Item.ACTION]?.value ?: 0

    val numberOfBuy: Int
        get() = items[Item.BUY]?.value ?: 0

    val debt: Int
        get() = items[Item.DEBT]?.value ?: 0

    val potion: Int
        get() = items[Item.POTION]?.value ?: 0

    val coffer: Int
        get() = items[Item.COFFER]?.value ?: 0

    val pirateShip: Int
        get() = items[Item.COIN_TOKEN_SHIP]?.value?:0

    val tradeCoin: Int
        get() = items[Item.COIN_TOKEN_ROUTE]?.value?:0

    fun tempZone(card: Card): MutableStateFlow<List<Card>> = playerTempZone.getOrPut(card.id) { MutableStateFlow(emptyList()) }
    fun getTempZone(card: Card) : List<Card> = playerTempZone[card.id]?.value?.toList() ?: emptyList()

    fun increment(item: Item, value: Int = 1) {
        val currentAmount = items[item]?.value ?: 0
        if (value < 0 && currentAmount + value < 0) return

        var finalValue = value

        if (isAffectedBy(Token.OnPlayer.TaxToken) && item == Item.MONEY && value > 0) {
            finalValue--
            updateTokenFlag(Token.OnPlayer.TaxToken, false)
        }

        items[item]?.value += finalValue
    }



    fun incrementByAction(item: Item, action : Player.() -> Int) { increment(item, self.action()) }

    fun decrement(item: Item, value: Int = 1) = items[item]?.value -= value
    fun decrementByAction(item: Item, action : Player.() -> Int) = decrement(item, self.action())

    fun updateDrawBonusValue(value: Int) = if (drawBonusNextTurn > -4) drawBonusNextTurn += value else Unit

    fun getValueOf(key : Item): Int = items[key]?.value ?: 0
    fun getPropertyOf(key : Item) = items[key]

    fun getProperties(key : String) : MutableStateFlow<Int> = properties.getOrPut(key){MutableStateFlow(0)}

    fun getFlag(key : String): MutableStateFlow<Boolean> = flags.getOrPut(key){ MutableStateFlow(false) }
    fun getPersistentFlag(key: String) = permanentFlags.getOrPut(key) { MutableStateFlow(false) }
    fun isFlagSet (key : String): Boolean = flags[key]?.value ?: false
    fun isUsed(key : Card): Boolean = flags[key.name]?.value ?: false

    internal fun get(destination: Destination.PlayerZone) = cardSet[destination]
    internal fun get(destination: Destination?) = cardSet[destination]
    internal fun getList(destination: Destination) = cardSet[destination]?.value ?: emptyList()

    fun sizeOf(destination: Destination.PlayerZone) = getList(destination).size

    fun getCardFromSupply(cardName : String): Card? = game.getCardFromSupply(cardName)
    fun getCardFromAsideSupply(key : String, name : String) = game.getAvailableAsideCard(key, name)




    fun getCardFromDeck(): Card? {
        if (getList(Destination.PlayerZone.Draw).isEmpty()) {
            shuffle()
            if (getList(Destination.PlayerZone.Draw).isEmpty()) return null
        }

        return getList(Destination.PlayerZone.Draw).lastOrNull()
    }

    fun getCopyOf(zone : Destination.PlayerZone): MutableList<Card>? = cardSet[zone]?.value?.toMutableList()
    val allOwnedCards: List<Card> get() = cardSet.values.flatMap { it.value }

    fun getCardFromIndex(cards: List<Card>, choice: String) : Card {
        return cards[choice.split(":")[1].toInt()]
    }

    val victoryPoint: Int get() = allOwnedCards.sumOf { card -> card.getVictoryValueCard(self) } + (items[Item.VICTORY_TOKEN]?.value?: 0) + game.landMarks.sumOf { card -> card.getVictoryValueCard(self) }

    fun shuffle() {
        val discardFlow = cardSet[Destination.PlayerZone.Discard] ?: return
        val drawFlow = cardSet[Destination.PlayerZone.Draw] ?: return
        val shuffledCards = discardFlow.value.shuffled()
        shuffledCards.forEach {it.moveTo(drawFlow, Destination.PlayerZone.Draw)}

        shuffling(Destination.PlayerZone.Draw)
    }

    fun shuffling(destination: Destination.PlayerZone) {
        val flow = cardSet[destination] ?: return
        val shuffledCards = flow.value.shuffled()
        flow.update { shuffledCards }
    }

//    fun addEventEffect(e : EventCard?){
//        if(e == null) return
//        if(activeEventEffect.contains(e)) return
//        activeEventEffect.add(e)
//    }

    fun addNextTurnEffect(effect : suspend Player.() -> Unit){
        if(nextTurnEffect.contains(effect)) return
        nextTurnEffect.add(effect)
    }

    fun addCleanUpEffect(effect : suspend Player.() -> Unit){
        if(cleanUpEffect.contains(effect)) return
        cleanUpEffect.add(effect)
    }

    fun addEndTurnEffect(effect : suspend Player.() -> Unit){
        if(endTurnEffect.contains(effect)) return
        endTurnEffect.add(effect)
    }

    fun addDiscardHook(hook: TriggerComponent.discardHook){ discardHooks.add(hook) }

    suspend fun addCardToShadowZone(scope : Card, c : Card?, extraAction: suspend Player.(Card) -> Unit = {}){
        if(c == null) return
        shadowZone(scope).update { it + c }
        self.extraAction(c)
    }

    suspend fun addListToShadowZone(scope : Card, c : List<Card>, extraAction: suspend Player.(List<Card>) -> Unit = {}){
        if(c.isEmpty()) return
        shadowZone(scope).update { it + c }
        self.extraAction(c)
    }

    fun shadowZone(scope: Card) = game.shadowZone(shadowKey + scope.shadowKey)
    fun getShadowList(scope : Card) : List<Card> = game.shadowList(shadowKey + scope.shadowKey)

    fun removeCardFromShadowZone(scope : Card, c : Card?, extraAction: Player.(Card) -> Unit = {}){
        if(c == null) return
        val list = shadowZone(scope).value
        updateShadowZone(scope, list - c)
        self.extraAction(c)
    }
    fun clearShadowZone(scope: Card) = game.clearShadowZone(shadowKey + scope.shadowKey)

    fun shadowZone(scopeId: ShadowKey) = game.shadowZone(shadowKey + scopeId)
    fun getShadowList(scopeId: ShadowKey) = game.shadowList(shadowKey + scopeId)

    fun getAllKeysFor(scope: Card) = game.allKeysFor(shadowKey + scope.shadowKey)

    fun addListToShadowZone(scopeId: ShadowKey, c : List<Card>) {
        if(c.isEmpty()) return
        game.addListToShadow(shadowKey + scopeId, c)
    }

    fun updateShadowZone(scopeId: ShadowKey, newList: List<Card>) = game.updateShadowZone(shadowKey + scopeId, newList)
    fun updateShadowZone(scope : Card, newList: List<Card>) = game.updateShadowZone(shadowKey + scope.shadowKey, newList)

    fun getAllListFromShadowZoneOf(scope: Card) = game.allListFromShadowZoneFor(shadowKey + scope.shadowKey)


    fun getDistinctTrashCards(): List<Card> = game.trashedCards.distinctBy { it.name }
    fun getDistinctCards(dest: Destination.PlayerZone): List<Card> = getCopyOf(dest)?.distinctBy { it.name }?:emptyList()


    override fun toString(): String = name ?: ""

    val toLog : String get() = name

    fun toJSON(): String = buildString {
        val playerId = client.id

        val fields = mutableListOf(
            "\"id\": $id",
            "\"client\": \"$playerId\"",
            "\"name\": \"$name\"",
            "\"vt\": $victoryPoint",
            "\"draw\": ${getList(Destination.PlayerZone.Draw).size}",
            "\"discard\": ${getList(Destination.PlayerZone.Discard).size}",
            "\"in_play\": ${Utils.toJSON(getList(Destination.PlayerZone.InPlay))}",
            "\"hand\": ${Utils.toJSON(getList(Destination.PlayerZone.Hand))}",
            "\"color\": \"${colors[id%colors.size]}\"",
            game.shadowZoneToJson(shadowKey.shadowId)
        )

        if (game.hasType(CardType.RESERVE, 1)) {
            fields.add("\"tavern\": ${Utils.toJSON(getList(Destination.OtherZone.Tavern))}")
        }

        val adventure = getComponent<TokenComponent>()?.toJson()
        if(adventure != null) {
            fields.add(adventure)
        }

        append(fields.joinToString(separator = ", ", prefix = "{", postfix = "}"))
    }

    fun toGameOverJson() = """
        {
        "id": "${client.id}",
        "name": "$name",
        "vt": $victoryPoint,
        "cards": ${Utils.toJSON(allOwnedCards)}
        }
    """.trimIndent()


    fun toSideJson() : String = buildString {
        val playerId = client.id

        val fields = mutableListOf("""
        "id": $id,
        "playerId": "$playerId",
        "name": "$name",
        "vt": $victoryPoint,
        "draw": ${getList(Destination.PlayerZone.Draw).size},
        "hand": ${getList(Destination.PlayerZone.Hand).size},
        "discard": ${getList(Destination.PlayerZone.Discard).size},
        "in_play": ${Utils.toJSON(getList(Destination.PlayerZone.InPlay))},
        "color": "${colors[id % colors.size]}"
    """.trimIndent())

        val adventure = getComponent<TokenComponent>()?.toJson()
        if(adventure != null) { fields.add(adventure) }

        append(fields.joinToString(separator = ", ", prefix = "{", postfix = "}"))
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Player) return false
        return id == other.id
    }

    override fun hashCode(): Int { return id * 31 + name.hashCode() }

    override fun log(message : String?) {
        if(message != null) privateLog(message)
    }

    val isActive : Boolean  get() = this == game.currentTurnPlayerProperty.value

    @Selection_Mode
    override suspend fun chooseWhatToDo(instruction: String, list: List<Card>, filter: (Card) -> Boolean, buttons: List<Button>, canPass: Boolean): String =
        self.privateChooseWhatToDo(instruction, list = list, filter = filter, buttons =  buttons, canPass =  canPass)

    @Selection_Mode
    override suspend fun chooseCardFromList(instruction: String, cards: List<Card>, canPass: Boolean, predicate: (Card) -> Boolean): Card? {
        val choices = computeChoices(predicate, cards)
        val choice = if (choices.isEmpty()) "" else choose(instruction, choices, choices, canPass = canPass)
        return if(choice.startsWith("SELECT_CARD:")) getCardFromIndex(cards, choice) else null
    }

    override suspend fun chooseCardFromHand(instruction: String, canPass: Boolean, predicate: (Card) -> Boolean): Card? {
        val choices = getList(Destination.PlayerZone.Hand).filter(predicate).map { "HAND:${it.name}" }.toMutableList()
        val choice = choose(instruction, choices, canPass = canPass)
        if(choice.startsWith("HAND")){
            return getList(Destination.PlayerZone.Hand).first { it.hasName(choice.removePrefix("HAND:")) }
        }
        return null
    }

    override suspend fun chooseCardFromSupply(instruction: String, filter: (Card) -> Boolean, canPass: Boolean): Card? {
        val choices = game.availableSupplyCard.filter(filter).map { "SUPPLY:${it.name}" }.toMutableList()
        val choice = choose(instruction, choices, canPass = canPass)
        return getCardFromSupply(choice.removePrefix("SUPPLY:"))
    }

    override fun sendMessage(message: PlayerMessage, card: Card?) {
        if(card == null) return
        when(message) {
            tempEnd -> moveCardsFromTemp(card)
            clearShadowZone -> clearShadowZone(card)
        }
    }

    override fun toPlayer(): Player {
        return self
    }

    val isInBuyPhase get() = state.value.inBuyPhase


    fun onTheLeft() = game.onTheLeft(self)
    fun onTheRight() = game.onTheRight(self)

    fun underState(nameState : String) = game.underState(self, nameState)
}