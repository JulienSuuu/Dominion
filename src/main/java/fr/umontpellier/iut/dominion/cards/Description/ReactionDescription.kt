package fr.umontpellier.iut.dominion.cards.Description


enum class TargetPlayer {SELF, OTHER_PLAYERS, ALL}


class ReactionDescription : DominionDescription {
    override var text: String = ""
        private set
    var triggerCondition: TriggerCondition? = null
    var reward: Reward? = null

    fun text(text: String) = apply { this.text = text }
    fun trigger(condition: TriggerCondition) = apply { this.triggerCondition = condition }
    fun effect(reward: Reward) = apply { this.reward = reward }

    override fun toJson(): String {
        val triggerJson = triggerCondition?.toJson() ?: "null"
        val rewardJson = reward?.toJson() ?: "null"

        return """
        {
          "text": "$text",
          "trigger": $triggerJson,
          "effect": $rewardJson
        }
        """.trimIndent()
    }
}

sealed interface TriggerCondition {
    fun toJson(): String

    data class FirstCardPlayed(val cardName: String) : TriggerCondition {
        override fun toJson(): String = """{"type": "FIRST_CARD_PLAYED", "card": "$cardName"}"""
    }

    data class OnAttackPlayed(val fromPlayer: TargetPlayer = TargetPlayer.OTHER_PLAYERS) : TriggerCondition {
        override fun toJson(): String = """{"type": "ON_ATTACK_PLAYED"}"""
    }
}

sealed interface Reward : SecondaryEffect {
    data class BonusCoins(val amount: Int) : Reward {
        override fun toJson(): String = """{"type": "BONUS_COINS", "amount": $amount}"""
    }
}