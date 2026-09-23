package fr.umontpellier.iut.dominion.cards.factories
import fr.umontpellier.iut.dominion.Annotation.Description
import fr.umontpellier.iut.dominion.Annotation.Dominion_Card
import fr.umontpellier.iut.dominion.Annotation.ExtraSet
import fr.umontpellier.iut.dominion.Annotation.InSet
import fr.umontpellier.iut.dominion.Annotation.PileType
import fr.umontpellier.iut.dominion.Annotation.Type
import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Supply.MixedSupplyPile
import fr.umontpellier.iut.dominion.Supply.StandardSupplyPile
import fr.umontpellier.iut.dominion.Supply.SupplyPile
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.Description.CardCatalog
import fr.umontpellier.iut.dominion.cards.component.ChangingName
import fr.umontpellier.iut.dominion.cards.component.OnSetup
import fr.umontpellier.iut.dominion.cards.factories.Nocturne.State
import fr.umontpellier.iut.dominion.cards.testTypes
import fr.umontpellier.iut.dominion.game.Game
import kotlinx.coroutines.CoroutineScope
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import java.util.*
import java.util.function.Supplier




private enum class Order {
    Dominion, Intrigue, Seaside, Alchemy, Prosperity,
    Cornucopia_Guilds, Hinterlands, Dark_Ages, Adventures, Empires, Nocturne;

    companion object {
        fun getOrdinalOrMax(name: String): Int =
            entries.find { it.name.equals(name, ignoreCase = true) }?.ordinal ?: 99
    }
}




private data class PileConfig(
    val cardSupplier: () -> Card,
    val countFunction: (Int) -> Int
) {
    companion object {
        fun kingdom(s: () -> Card) = PileConfig(s) { 10 }
        fun victory(s: () -> Card) = PileConfig(s) { n -> if (n <= 2) 8 else 12 }
        fun copper(s: () -> Card) = PileConfig(s) { n -> 60 + 7 * n }
        fun silver(s: () -> Card) = PileConfig(s) { 40 }
        fun gold(s: () -> Card) = PileConfig(s) { 30 }
        fun estate(s: () -> Card) = PileConfig(s) { n -> if (n <= 2) 8 + 3 * n else 12 + 3 * n }
        fun curse(s: () -> Card) = PileConfig(s) { n -> 10 * (n - 1) }
        fun potion(s: () -> Card) = PileConfig(s) { 20 }
        fun platinum(s: () -> Card) = PileConfig(s) { 12 }
        fun event(s: () -> Card) = PileConfig(s) { 1 }
        fun mixed(s: () -> Card) = PileConfig(s) { n -> if (n <= 2) 1 else 2 }
        fun ruins(s: () -> Card) = PileConfig(s) { 5 }
        fun rats(s: () -> Card) = PileConfig(s) { 20 }
    }
}



object FactorySupplyPile {

private val cardToExpansion = TreeMap<Card, String>(compareBy<Card> { it.costValue }.thenBy { it.name })
private val nameToExpansion = HashMap<String, String>()
private val pileConfigs = HashMap<String, PileConfig>()
private val registry = HashMap<String, Card>()
private val mixedCards = EnumMap<CardType, MutableList<String>>(CardType::class.java)
private val templates = EnumMap<CardType, PileConfig>(CardType::class.java)
private val preSets = TreeMap<String, MutableMap<String, MutableList<String>>> { s1, s2 ->
    val ranks1 = s1.split(" & ").map { Order.getOrdinalOrMax(it) }.sorted()
    val ranks2 = s2.split(" & ").map { Order.getOrdinalOrMax(it) }.sorted()

    val size = minOf(ranks1.size, ranks2.size)
    for (i in 0 until size) {
        val cmp = ranks1[i].compareTo(ranks2[i])
        if (cmp != 0) return@TreeMap cmp
    }
    ranks1.size.compareTo(ranks2.size)
}

val allAvailableCards: List<String>
    get() = pileConfigs.keys.sorted()

val availableCardsByExtension: Map<String, List<String>>
    get() = cardToExpansion.entries
        .filter { !it.value.equals("Base", ignoreCase = true) && mixedCards.values.none { list -> list.contains(it.key.name) } }
        .groupBy({ it.value }, { it.key.name })
        .toSortedMap(compareBy { Order.getOrdinalOrMax(it) })

    val availableEvent: Map<String, List<String>>
        get() = mixedCards.filter { (k) -> k == CardType.EVENT || k == CardType.LANDMARK }.mapKeys { it.key.name }

fun getPreSets(): Map<String, Map<String, List<String>>> = preSets

fun loadAllCards() {
    println("--- DÉBUT DU SCAN DES CARTES ---")
    val ref = Reflections("fr.umontpellier.iut.dominion.cards.factories", Scanners.MethodsAnnotated)



    val bonusMethods = ref.getMethodsAnnotatedWith(Description::class.java)
    println("Nombre de méthodes de description détectées : ${bonusMethods.size}")

    for (method in bonusMethods) {
        try {
            val declaringClass = method.declaringClass
            val targetInstance = declaringClass.kotlin.objectInstance

            method.invoke(targetInstance)
        } catch (e: Exception) {
            throw RuntimeException("Erreur lors de l'exécution de la description bonus : ${method.name}", e)
        }
    }

    val methods = ref.getMethodsAnnotatedWith(Dominion_Card::class.java)
    println("Nombre de méthodes détectées : ${methods.size}")
    methods.forEach {
        println(it.name)
    }

    val tempSetCards = LinkedHashMap<String, MutableList<String>>()
    val tempSetExpansions = LinkedHashMap<String, TreeSet<String>>()
    val tempSetExtra = LinkedHashMap<String, String>()

    for (method in methods) {
        try {
            val card = method.getAnnotation(Dominion_Card::class.java) ?: continue
            val inSet = method.getAnnotation(InSet::class.java)
            val extraSet = method.getAnnotation(ExtraSet::class.java)

            val declaringClass = method.declaringClass
            val targetInstance = declaringClass.kotlin.objectInstance

            val supplier: () -> Card = { method.invoke(targetInstance) as Card }
            val sample = supplier()
            val name = sample.name
            val extensionName = card.extension

            val pileConfig = createPileConfig(card.pileType, supplier, card.cardsNumber)
            val special = sample.getSpecialType()

            if (special != null && !sample.hasType(CardType.TEMPLATE)) {
                mixedCards.computeIfAbsent(special) { ArrayList() }.add(name)
            }

            pileConfigs[name] = pileConfig
            registry[name] = sample


            if (special != null && sample.hasType(CardType.TEMPLATE)) {
                templates[special] = pileConfig
            }

            cardToExpansion[sample] = card.extension
            nameToExpansion[name] = card.extension

            inSet?.value?.forEach { setName ->
                tempSetCards.computeIfAbsent(setName) { ArrayList() }.add(name)
                tempSetExpansions.computeIfAbsent(setName) { TreeSet() }.add(extensionName)
            }

            extraSet?.value?.forEach { setName ->
                tempSetExtra[setName] = name
                tempSetExpansions.computeIfAbsent(setName) { TreeSet() }.add(card.extension)
            }
        } catch (e: Exception) {
            println("❌ [ERROR] Échec lors du chargement de la méthode '${method.name}' (Classe: ${method.declaringClass.simpleName})")
            e.printStackTrace()
            throw RuntimeException("Erreur lors de l'enregistrement de la méthode ${method.name}", e)
        }
    }

    tempSetCards.forEach { (setName, cards) ->
        val combinedKey = tempSetExpansions[setName]?.joinToString(" & ") ?: ""
        tempSetExtra[setName]?.let { cards.add(it) }
        preSets.computeIfAbsent(combinedKey) { LinkedHashMap() }[setName] = cards
    }
    println("--- FIN DU SCAN ---")
}

fun getExtensions(vararg extensions: String?): List<String> {
    return nameToExpansion.entries.filter {(_, extension) -> extension in extensions }.map { it.key }
}

fun createSupplyPile(cardName: String, numberOfPlayers: Int, scope : CoroutineScope): SupplyPile {
    val config = pileConfigs[cardName] ?: throw IllegalArgumentException("Carte inconnue: $cardName")
    return StandardSupplyPile( cardSupplier =  { config.cardSupplier() }, numberOfCopies =  config.countFunction(numberOfPlayers), scope)
}

    fun createMixedSupplyPile(pileName : String, cardNames: List<String>?, scope: CoroutineScope, nbPlayer: Int): SupplyPile {
        require(!cardNames.isNullOrEmpty()) { "La liste des cartes ne peut pas être vide" }

        val firstName = cardNames.first()
        val firstConfig = pileConfigs[firstName] ?: throw IllegalArgumentException("Configuration introuvable pour : $firstName")

        val suppliersWithCount: List<Pair<() -> Card, Int>> = cardNames.mapNotNull { name ->
            val config = pileConfigs[name] ?: return@mapNotNull null
            val count = config.countFunction(nbPlayer)
            config.cardSupplier to count
        }

        val targetType = suppliersWithCount.firstOrNull()?.first?.invoke()?.getSpecialType()
        val template = templates.getOrDefault(targetType, firstConfig).cardSupplier()

        return MixedSupplyPile(
            pileName = pileName,
            suppliersWithCount = suppliersWithCount,
            scope = scope,
            template = template
        )
    }

    fun createMixedSupplyPileWithTemplate(cardNames: List<String>?, template : Card, pileName: String = template.name ,scope : CoroutineScope, nbPlayer: Int ): SupplyPile {
        require(!cardNames.isNullOrEmpty()) { "La liste des cartes ne peut pas être vide" }

        val suppliersWithCount: List<Pair<() -> Card, Int>> = cardNames.mapNotNull { name ->
            val config = pileConfigs[name] ?: return@mapNotNull null
            val count = config.countFunction(nbPlayer)
            config.cardSupplier to count
        }

        return MixedSupplyPile(
            pileName = pileName,
            suppliersWithCount,
            scope,
            template
        )
    }



private fun createPileConfig(type: PileType, s: () -> Card, number : Int = 0): PileConfig = when (type) {
    PileType.COPPER -> PileConfig.copper(s)
    PileType.ESTATE -> PileConfig.estate(s)
    PileType.VICTORY -> PileConfig.victory(s)
    PileType.KINGDOM -> PileConfig.kingdom(s)
    PileType.SILVER -> PileConfig.silver(s)
    PileType.GOLD -> PileConfig.gold(s)
    PileType.POTION -> PileConfig.potion(s)
    PileType.PLATINUM -> PileConfig.platinum(s)
    PileType.CURSE -> PileConfig.curse(s)
    PileType.MIXED -> PileConfig.mixed(s)
    PileType.EVENT -> PileConfig.event(s)
    PileType.RUINS -> PileConfig.ruins(s)
    PileType.RATS -> PileConfig.rats(s)
    PileType.SIMPLE -> PileConfig(s){number}
    PileType.CUSTOM -> PileConfig(s){number}
    PileType.UNIQUE -> PileConfig(s){1}
}

fun isExpansionRequired(chosenNames: List<String>, expansionName: String): Boolean =
    chosenNames.mapNotNull { nameToExpansion[it] }.any { it == expansionName }

fun getFerrymanOptions(kingdom: List<String>): Map<String, List<String>> =
    getGroupedOptions(kingdom) { cost -> cost == 3 || cost == 4 }

fun getYoungWitchOptions(kingdom: List<String>): Map<String, List<String>> =
    getGroupedOptions(kingdom) { cost -> cost == 2 || cost == 3 }

private fun getGroupedOptions(kingdom: List<String>, costPredicate: (Int) -> Boolean): Map<String, List<String>> {
    val baseCards = cardToExpansion.entries
        .filter { it.value.equals("Base", ignoreCase = true) }
        .map { it.key.name }
        .toSet()

    return pileConfigs.entries
        .filter { (name, _) -> name !in kingdom && name !in baseCards && mixedCards.values.none { it.contains(name) } }
        .filter { (_, config) -> costPredicate(config.cardSupplier().costValue) }
        .groupBy(
            keySelector = { (name, _) ->
                cardToExpansion.entries.firstOrNull { it.key.name == name }?.value ?: "Unknown"
            },
            valueTransform = { it.key }
        )
        .toSortedMap(compareBy { Order.getOrdinalOrMax(it) })
}

fun getMixedCards(type: CardType): List<String>? = mixedCards[type]
    fun getSpecificMixedSupply(type: CardType, vararg cardNames : String) : List<String> {
        val mixedSupplyPile = getMixedCards(type) ?: throw IllegalArgumentException("$type mixed supply unknown")
        val list = mutableListOf<String>()
        for(name in cardNames) {
            if(mixedSupplyPile.contains(name)) {
                list.add(name)
            }
        }

        return list
    }

fun shouldEnableSpecialty(kingdom: List<String>, targetExtension: String, threshold: Int): Boolean {
    return kingdom.mapNotNull { nameToExpansion[it] }.countMax(threshold) { it.equals(targetExtension, ignoreCase = true) }
}

fun createCard(name: String): Card? = registry[name]
}

fun <T> List<T>.countMax(threshold: Int, predicate: (T) -> Boolean): Boolean {
    if (threshold <= 0) return true
    var n = 0
    return any { pile ->
        if (predicate(pile)) n++
        n >= threshold
    }
}

fun Game.createSupplyPile(nameCard : String): SupplyPile = factory.createSupplyPile(nameCard, nbPlayers, gameScope)
fun Game.createMixedSupplyPile(cardNames: List<String>?, template : Card ) = factory.createMixedSupplyPileWithTemplate(cardNames= cardNames, template = template, scope =  gameScope, nbPlayer = nbPlayers)
fun Game.createMixedSupplyPile(pileName: String, cardName: List<String>? ) = factory.createMixedSupplyPile(pileName, cardName, gameScope, nbPlayers)