package fr.umontpellier.iut.dominion.domain.model.card

import fr.umontpellier.iut.dominion.domain.model.location.Destination
import fr.umontpellier.iut.dominion.domain.model.core.Item
import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.player.skills.draw
import fr.umontpellier.iut.dominion.domain.model.supply.SupplyPile
import fr.umontpellier.iut.dominion.domain.model.supply.SupplyType
import fr.umontpellier.iut.dominion.domain.setup.card.CardConfigurator
import fr.umontpellier.iut.dominion.domain.setup.card.factories.empires.EmpiresRules
import fr.umontpellier.iut.dominion.domain.components.card.CardComponent
import fr.umontpellier.iut.dominion.domain.components.card.ComponentRegistry
import fr.umontpellier.iut.dominion.domain.components.card.description.BonusDescription
import fr.umontpellier.iut.dominion.domain.components.card.description.CardCatalog
import fr.umontpellier.iut.dominion.domain.components.card.description.DominionDescription
import fr.umontpellier.iut.dominion.domain.components.card.description.InstructionDescription
import fr.umontpellier.iut.dominion.domain.components.card.description.ReactionDescription
import fr.umontpellier.iut.dominion.domain.components.card.description.Reward
import fr.umontpellier.iut.dominion.domain.components.card.duration.DurationComponent
import fr.umontpellier.iut.dominion.domain.components.card.duration.Follower
import fr.umontpellier.iut.dominion.domain.components.card.score.ScoreComponent
import fr.umontpellier.iut.dominion.domain.components.card.trigger.OnPlayComponent
import fr.umontpellier.iut.dominion.domain.components.game.markPlayerAsAttacked
import fr.umontpellier.iut.dominion.domain.components.game.shouldEnchant
import fr.umontpellier.iut.dominion.domain.model.card.price.Price
import fr.umontpellier.iut.dominion.domain.model.core.Id
import fr.umontpellier.iut.dominion.domain.model.event.Event
import fr.umontpellier.iut.dominion.domain.model.core.ShadowKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update



class Card(
    override val name: String,
    override val price: Price,
    vararg initialTypes: CardType,
) : ReadableCard {

    override var supply : SupplyPile? = null
    override val id: Id = Id("${name}-${numberOfId++}")
    override val shadowKey: ShadowKey = ShadowKey.get(id)
    override val faceDown = FaceDown(false)

    override val types: MutableSet<CardType> = HashSet(initialTypes.toList())

    override var loc: MutableStateFlow<Destination?> = MutableStateFlow(null)
        private set

    private var location: MutableStateFlow<List<Card>>? = null

    var available  : (Player) -> Boolean = { true }

    private val internalPrice = MutableStateFlow(0)
    private val internalDebt = MutableStateFlow(0)


    @PublishedApi
    internal var components = ComponentRegistry()


    val properties = mutableMapOf<String, Any>()

    init {
        internalPrice.value = price.coins
        internalDebt.value = price.debt
    }

    /**
     * Enregistre un composant.
     */
    inline fun <reified T : CardComponent> register(
        component: T,
        noinline condition: (Event, Player) -> Boolean = { _, _ -> true }
    ) {
        components.register(T::class, component, condition)
    }

    /**
     * Filtre et retourne uniquement les composants dont la condition est valide pour l'événement courant.
     */
    inline fun <reified T : CardComponent> getExecutableComponents(
        event: Event,
        player: Player
    ): List<T> {
        return components.getExecutableComponents(T::class, event, player)
    }

    inline fun <reified T : CardComponent> hasComponent(): Boolean {
        return components.isNotEmpty<T>()
    }

    inline fun <reified T : CardComponent> removeComponent() {
        components.remove(T::class)
    }

    inline fun <reified T : CardComponent> getComponent() : T? {
        return components.getComponent<T>()
    }

    fun copy(): Card = Card(name, price, *types.toTypedArray()).apply {
        components = this@Card.components.copy()
        set("unable", true)
        location = MutableStateFlow(emptyList())
        loc.update { null }
        available = this@Card.available
    }

    fun basicPrice(): Int = internalPrice.value
    fun basicDebt(): Int = internalDebt.value

    fun <T> set(property: String, value: T): T {
        properties[property] = value as Any
        return value
    }

    fun getValue(property: String): Number = properties[property] as? Number ?: 0
    fun getString(property: String): String = properties[property] as? String ?: "None"

    @Suppress("UNCHECKED_CAST")
    fun <K, V> getMap(key: String): Map<K, V> = properties[key] as? Map<K, V> ?: emptyMap<K, V>()

    @Suppress("UNCHECKED_CAST")
    fun <T> getCollection(key: String): Collection<T> {
        return properties.computeIfAbsent(key) { ArrayList<T>() } as Collection<T>
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getMutableCollection(key: String): MutableCollection<T> {
        return properties.computeIfAbsent(key) { ArrayList<T>() } as MutableCollection<T>
    }

    fun getFlag(key: String): Boolean = properties[key] as? Boolean ?: false

    fun clear() {
        properties.clear()
        getComponent<Follower>()?.clear()
    }

    infix fun setup(settings: CardConfigurator.() -> Unit): Card {
        CardConfigurator(this).settings()
        return this
    }


    fun setPrice(money: Int): Card {
        internalPrice.value = money
        return this
    }



    inline fun <reified T : CardComponent> canExecute(event: Event, player : Player = event.player): Boolean {
        return components.getEntry<T>()?.canExecute(event, player) == true
    }

    inline fun <reified T : DominionDescription> getDescription() = CardCatalog.getComponent<T>(name)

    fun generatesCoins(): Boolean {
        val hasDirectCoins = getDescription<BonusDescription>()?.getAttribute("money") ?: false
        if (hasDirectCoins) return true
        val instruction = getDescription<InstructionDescription>()
        val hasReactionCoins = getDescription<InstructionDescription>()?.getTop<ReactionDescription>()
            ?.let { it.reward is Reward.BonusCoins } ?: false

        if (hasReactionCoins) return true
        val coinGainRegex = Regex("""\+\s*\d*\s*🟡""")
        return instruction?.text?.let { text -> coinGainRegex.containsMatchIn(text) } ?: false
    }

    fun toText() : String = CardCatalog.toText(name)

    val costValue: Int get() = 0.coerceAtLeast(price.coins)
    val potion: Int get() = price.potions
    val debt: Int get() = price.debt

    fun hasName(name: String): Boolean = this.name == name
    fun hasSameNameAs(c: Card): Boolean = this.name == c.name

    fun addType(type: CardType): Card = apply { types.add(type) }
    fun addType(first: CardType, second : CardType) : Card = apply {types.add(first); types.add(second) }
    fun addType(vararg types: CardType) : Card = apply {types.forEach { addType(it) } }
    fun hasType(type: CardType): Boolean = types.contains(type)
    fun removeType(type: CardType) { types.remove(type) }
    fun numberType(): Int = types.size

    fun moveTo(newLocation: MutableStateFlow<List<Card>>?, loc: Destination?) {
        if(getFlag("unable")) return

        this.loc.update { loc }

        location?.let { ancienneZone ->
            ancienneZone.update { it - this }
        }

        location = newLocation

        newLocation?.update { it + this }
    }

    fun moveToTemp(newLocation: MutableStateFlow<List<Card>>?) {
        if(newLocation == null)return
        if(getFlag("unable")) return

        location?.value -= this
        location = newLocation
        newLocation.value += this
    }

    fun moveToBottom(newLocation: MutableStateFlow<List<Card>>?, loc: Destination?) {
        if (newLocation == null) return
        if(getFlag("unable")) return

        this.loc.update { loc }
        location?.let { ancienneZone ->
            ancienneZone.value -= this
        }
        location = newLocation
        newLocation.value = listOf(this) + newLocation.value
    }

    fun hasForLocation(dest: Destination?): Boolean = loc.value == dest

    override fun toString(): String = name
    fun toLog(): String = name

    suspend fun play(p: Player) {
        val empiresRules = p.game.getRule<EmpiresRules>()
        when{
            empiresRules?.shouldEnchant(p, this) == true -> {
                empiresRules.markPlayerAsAttacked(p)
                p.log("is Enchanted by Enchantress, +1 Card, +1 Action instead.")
                p.draw(); p.increment(Item.ACTION)
            }


            else -> {
                executeOnPlayEffect(p)
                getComponent<DurationComponent>()?.activeDuration(p, this)
            }

        }
    }

    suspend fun playNocturne(p: Player, number: Int = 1) {
        repeat(number) {
            executeOnPlayEffect(p)
        }
    }

    private suspend fun executeOnPlayEffect(p: Player) {
        getComponent<OnPlayComponent>()?.let { it(p, this) }
    }

    suspend fun play(p : Player, number : Int){
        repeat(number){ play(p) }
    }

    fun buyCondition(potionValue: Int, debtValue: Int): Boolean = potionValue <= price.potions && debtValue <= price.debt

    fun Player.getVictoryValue(): Int = getComponent<ScoreComponent>()?.let { it(self) } ?: 0
    fun getVictoryValueCard(p : Player): Int = p.getVictoryValue()

    fun isAtMost(cost: Int, potion : Int, debt: Int): Boolean = costValue <= cost && buyCondition(potion, debt)
    infix fun isAtMost(cost: Int): Boolean = isAtMost(cost, 0, 0)

    fun isLessThan(cost: Int, potion: Int, debt: Int): Boolean = costValue < cost && this.potion < potion && this.debt < debt
    infix fun isLessThan(cost: Int): Boolean = isLessThan(cost, 0, 0)
    fun isLessThanWithBonus(trashed: Card, bonusMoney: Int = 0) : Boolean = costValue < (trashed.costValue + bonusMoney) && potion <= trashed.potion && debt <= trashed.debt

    fun isEqual(cost: Int, potion: Int, debt: Int): Boolean = costValue == cost && this.potion == potion && this.debt == debt
    fun isEqual(cost: Int): Boolean = isEqual(cost, 0, 0)

    fun isEqualWithBonus(trashed: Card, bonusMoney: Int = 0): Boolean =
        costValue == (trashed.costValue + bonusMoney) && potion == trashed.potion && debt == trashed.debt

    fun isAtLeast(cost :Int, potion : Int = 0, debt: Int = 0): Boolean = costValue >= cost && this.potion >= potion && this.debt >= debt


    fun isAtLeastWithBonus(trashed: Card, bonusMoney: Int) : Boolean =
        costValue >= (trashed.costValue + bonusMoney) && potion >= trashed.potion && debt >= trashed.debt

    fun isAtMostWithBonus(trashed: Card, bonusMoney: Int = 0): Boolean =
         costValue <= (trashed.costValue + bonusMoney) && potion <= trashed.potion && debt <= trashed.debt


    fun isBetween(lower: Int, upper: Int): Boolean = costValue in lower..upper && buyCondition(0, 0)

    fun getSpecialType(): CardType? {
        val specialTypes = CardType.entries.filter { it.isSpecial }.toSet()
        return types.firstOrNull { it in specialTypes }
    }

    fun costInstruction(bonusMoney: Int = 0): String {
        val totalMoney = costValue + bonusMoney
        val potionText = if (potion > 0) ", $potion ⚗\uFE0F" else ""
        val debtText = if (debt > 0) ", $debt ⬡" else ""

        return "$totalMoney \uD83D\uDFE1$potionText$debtText"
    }

    companion object {
        @JvmStatic fun treasure(name: String, price: Price) = Card(name, price, CardType.TREASURE)
        @JvmStatic fun action(name: String, price: Price) = Card(name, price, CardType.ACTION)
        @JvmStatic fun victory(name: String, price: Price) = Card(name, price, CardType.VICTORY)
        @JvmStatic fun duration(name: String, price: Price) = Card(name, price, CardType.DURATION)
        fun attack(name: String, price: Price) = Card(name, price, CardType.ACTION, CardType.ATTACK)
        @JvmStatic fun event(name: String, price: Price) = Card(name, price, CardType.EVENT)
        fun landmark(name : String) = Card(name, Price.classic(0), CardType.LANDMARK).setup {
            available { false }
        }
        fun night(name : String, price: Price) = Card(name, price, CardType.NIGHT)

        private var numberOfId = 0
    }


    fun pickResource(nameFlow : String) = supply?.getResource(nameFlow)?.value ?: 0
    fun flowOfSupply(nameFlow : String) = supply?.getResource(nameFlow)

    fun useResource(resource: String, take: Int) {supply?.useResource(resource, take) }
    fun updateResource(resource: String, value: Int) {supply?.updateResource(resource, value) }
    fun clearResource(resource: String){supply?.clearResource(resource)}

    fun takePointFrom(name : String, max : Int = pickResource(name)) : Int {
        val flow = flowOfSupply(name)
        if(flow?.value == 0) return 0

        val toUsed = flow?.check(max) ?: return 0
        supply?.useResource(name, toUsed)
        return toUsed
    }

    fun addPointToFrom(name : String, max : Int, card : Card) {
        val toTake = card.takePointFrom(name, max)
        addPointTo(name, toTake)
    }

    fun addPointTo(name : String, incrementation : Int) {
        supply?.updateResource(name, incrementation)
    }



    private fun StateFlow<Int>.check(max : Int) : Int = if(this.value < max) this.value else max


    fun replaceInSupply(revealed: Card): Boolean = this.hasSameNameAs(revealed) && replaceInSupply()
    fun replaceInSupply(): Boolean {
        if(getFlag("unable")) {
            return false
        }
        val pile = supply ?: return false

        if(pile.supplyType == SupplyType.EMPTY) return false
        if(!pile.isFlagSet("inGame"))  return false
        if (pile.contains(this))  return false

        pile.replace(this)
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Card) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return name.hashCode() * 31 + id.hashCode()
    }


    fun toJsonPlayer() : String = """
        {
        "id": "$id",
        "name": "$name",
        ${faceDown.toJson()},
        "type": "SUMMARY"
        }
    """.trimIndent()

    fun toShadowZoneJson(): String {
        return if(faceDown.value)
            """{
                ${faceDown.toJson()},
                "id": "hidden",
                "name": "hidden",
                "type": "SHADOW"
            }""".trimIndent()


        else toJsonPlayer()
    }

    fun toJson(): String = """
    {
    "type": "FULL",
    ${faceDown.toJson()},
    "id": "$id",
    "name": "$name",
    "price": ${price.toJson()},
    "types": [${types.joinToString { "\"${it.name}\"" }}],
    "description": "${toText().escapeJson()}",
    "location": ${loc.value?.let { "\"${it.name}\"" } ?: "null"}
    }
""".trimIndent()

    fun hide(){ faceDown.hide() }
    fun reveal(){ faceDown.reveal() }
}


fun String.escapeJson(): String {
    return this.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "")
        .replace("\t", "\\t")
}