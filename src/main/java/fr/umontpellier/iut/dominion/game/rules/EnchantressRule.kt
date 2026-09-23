package fr.umontpellier.iut.dominion.game.rules

import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.factories.Empires.EmpiresRules
import fr.umontpellier.iut.dominion.game.Game
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class EnchantressRule(val game : Game) : IEmpireRules {
    private val activeEnchantressMap = mutableMapOf<Card, Player>()
    private val playersAttackedThisTurn = mutableSetOf<Player>()

    fun registerEnchantress(card: Card, attacker: Player) {
        activeEnchantressMap[card] = attacker
    }

    fun unregisterEnchantress(card: Card) {
        activeEnchantressMap.remove(card)
    }

    fun shouldEnchant(currentPlayer: Player, cardPlayed: Card): Boolean {
        if (activeEnchantressMap.isEmpty()) return false
        if (!currentPlayer.isActive) return false
        if (!cardPlayed.hasType(CardType.ACTION)) return false
        if (playersAttackedThisTurn.contains(currentPlayer)) return false

        return activeEnchantressMap.entries.any { (enchantressCard, attacker) ->
            val isNotSelf = attacker != currentPlayer
            val isNotProtected = !enchantressCard.getCollection<Player>("Players").contains(currentPlayer)

            isNotSelf && isNotProtected
        }
    }

    fun markPlayerAsAttacked(player: Player) {
        playersAttackedThisTurn.add(player)
    }

    fun resetTurnTracker() {
        playersAttackedThisTurn.clear()
    }

    fun toJson(): String {
        return buildJsonObject {
            putJsonArray("enchantress") {
                activeEnchantressMap.forEach { (card, player) ->
                    add(buildJsonObject {
                        put("cardId", card.id.id)
                        put("clientId", player.client.id)
                    })
                }
            }
        }.toString()
    }
}

val EmpiresRules.enchantressRule get() = getRule<EnchantressRule>()

fun EmpiresRules.registerEnchantress(card : Card, attacker: Player) = enchantressRule?.registerEnchantress(card, attacker)
fun EmpiresRules.unregisterEnchantress(card : Card) = enchantressRule?.unregisterEnchantress(card)
fun EmpiresRules.shouldEnchant(currentPlayer: Player, cardPlayed: Card) = enchantressRule?.shouldEnchant(currentPlayer, cardPlayed) ?: false
fun EmpiresRules.markPlayerAsAttacked(player: Player) = enchantressRule?.markPlayerAsAttacked(player)
fun EmpiresRules.resetTurnTracker() = enchantressRule?.resetTurnTracker()
