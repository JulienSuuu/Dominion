package fr.umontpellier.iut.dominion.Player.Skills

import fr.umontpellier.iut.dominion.Annotation.AfterAllyTrigger
import fr.umontpellier.iut.dominion.Button
import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Flags
import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.PlayerComponent.PlayerComponent
import fr.umontpellier.iut.dominion.Player.PlayerComponent.PlayerInTurnState
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.Events.Event
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent
import kotlinx.coroutines.flow.update
import org.hibernate.sql.ast.tree.expression.Over
import kotlin.math.min

class BuySkills : PlayerComponent {
}

fun Player.repayDebt(){
    val toRepay = min(getValueOf(Item.MONEY), getValueOf(Item.DEBT))
    decrement(Item.MONEY, toRepay)
    decrement(Item.DEBT, toRepay)
}


fun Player.useCoffer() = takeIf { getValueOf(Item.COFFER) > 0 }?.apply {
    decrement(Item.COFFER, 1)
    increment(Item.MONEY, 1)
}

fun Player.hasEnoughResourcesFor(card: Card): Boolean =
    getValueOf(Item.MONEY) >= card.costValue &&
            getValueOf(Item.POTION) >= card.potion &&
            getValueOf(Item.DEBT) == 0

fun Player.canBuy(card: Card): Boolean {
    if (!card.available(self)) return false
    if (!hasEnoughResourcesFor(card)) return false

    val isContraband = card.name in game.getNamedCardsThisTurn("contraband")
    val isDeludedAction = card.hasType(CardType.ACTION) && isFlagSet("Deluded")
    val isExpeditionRestricted = !card.hasType(CardType.EVENT) && getFlag(Flags.expedition).value

    return !isContraband && !isDeludedAction && !isExpeditionRestricted
}

private data class OverPaidResources(val cofferUsed: Int, val potionUsed : Int)

private suspend fun Player.overPaid(c: Card): OverPaidResources {
    var overpaidAmount = 0
    var overpaidPotion = 0

    val hasExtraMoney = getValueOf(Item.MONEY) > c.costValue
    val hasExtraPotion = getValueOf(Item.POTION) > c.potion

    if (c.hasType(CardType.OVERPAID) && (hasExtraMoney || hasExtraPotion)) {
        val choose = self.chooseWhatToDo("do you want to overpay?", listOf(c), buttons = Button.yesOrNo, canPass = true)

        if (choose == "y") {
            while (true) {
                val remainingMoney = getValueOf(Item.MONEY) - (c.costValue + overpaidAmount)
                val remainingPotion = getValueOf(Item.POTION) - (c.potion + overpaidPotion)

                val buttons = mutableListOf<Button>()
                if (remainingMoney > 0) { buttons.add(Button("Increment (+1 Coin)", "i")) }
                if (remainingPotion > 0) { buttons.add(Button("Use Potion (+1 Potion)", "p")) }

                if (buttons.isEmpty()) break

                val howMuch = self.chooseWhatToDo(
                    "overpaying: +$overpaidAmount Coins, +$overpaidPotion Potions. Choose what to add:",
                    listOf(c),
                    buttons = buttons,
                    canPass = true
                )

                when (howMuch) {
                    "i" -> overpaidAmount += 1
                    "p" -> overpaidPotion += 1
                    else -> break
                }
            }

            c.set("OverpaidNumber", overpaidAmount)
            c.set("Potion", overpaidPotion)
        }
    }

    c.getComponent<TriggerComponent.OverPaidCard>()?.let { it(self, c) }

    return OverPaidResources(overpaidAmount, overpaidPotion)
}

@AfterAllyTrigger
suspend fun Player.buyCard(c: Card?){
    if(c == null)return

    state.update { it.changeInTurnState(PlayerInTurnState.BuyingCard(c)) }

    suspend fun triggerBuy() {
        getCopyOf(Destination.PlayerZone.InPlay)?.forEach { card ->
            card.getComponent<TriggerComponent.OnBuy>()?.let { it(self, c) }
        }
    }


    suspend fun onCursePile() {
        if(game.hasToken(c.name))
            repeat(game.getToken(c.name)){
                gain(getCardFromSupply("Curse"), Destination.PlayerZone.Discard)
            }
    }

    val (overpaidAmount, overpaidPotion ) = overPaid(c)
    decrement(Item.MONEY, c.costValue + overpaidAmount)
    increment(Item.DEBT, c.debt)
    decrement(Item.POTION, c.potion + overpaidPotion)
    logBuy(c)

    val event = Event(c, Destination.PlayerZone.Discard, self, true)

    if(c.hasType(CardType.EVENT)){
        if (c.canExecute<TriggerComponent.CheckItSelfBuy>(event, self)) {
            c.getComponent<TriggerComponent.CheckItSelfBuy>()?.let { it(event, c) }
        }
        return
    }

    triggerBuy()
    gain(event)
    onCursePile()

}