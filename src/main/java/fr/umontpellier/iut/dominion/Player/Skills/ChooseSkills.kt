package fr.umontpellier.iut.dominion.Player.Skills

import fr.umontpellier.iut.dominion.Annotation.Selection_Mode
import fr.umontpellier.iut.dominion.Button
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.Tokens.Token
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.Player.PlayerComponent.TokenComponent
import fr.umontpellier.iut.dominion.Player.PlayerComponent.controller
import fr.umontpellier.iut.dominion.Player.PlayerComponent.underPossession

suspend fun Player.choose(instruction: String, choices: MutableList<String> = mutableListOf(), allCards: List<String> = listOf(), buttons : List<Button> = listOf(), canPass : Boolean = false ) : String {
    buttons.forEach { choices.add("BUTTON:${it.value}") }
    if(choices.isEmpty() || canPass) choices.add("")
    game.lastPlayerIdChoice = client
    while (true){
        controller.game.prompt("${if (underPossession) "$controller, choose for : $this," else "$this,"} $instruction", choices, allCards, buttons, getIndex())
        val input = controller.game.readLine()?.get("message")?.asText() ?: ""
        if(choices.contains(input)){
            return input
        }
    }
}

suspend fun Player.choose(instruction: String, canPass: Boolean = false) : String {
    val choices = mutableListOf<String>()
    if(canPass){
        choices.add("")
    }

    controller.game.prompt("${if (underPossession) "$controller, choose for : $this," else "$this,"} $instruction", choices, listOf(), listOf(), getIndex())
    return controller.game.readLine()?.get("message")?.asText() ?: ""
}

suspend fun Player.chooseToken(instruction: String, filter: (Token.OnPile) -> Boolean = {true}, canPass: Boolean = false )
= getComponent<TokenComponent>()?.chooseToken(instruction, filter, canPass)



fun computeChoices(filter: (Card) -> Boolean = { true }, list: List<Card>) : MutableList<String> =
    list.filter(filter).map { "SELECT_CARD:${list.indexOf(it)}:${it.name}" }.toMutableList()

@Selection_Mode
internal suspend fun Player.privateChooseWhatToDo(instruction: String, filter:(Card) -> Boolean = {false}, list: List<Card>, buttons: List<Button>, canPass: Boolean = true) : String {
    val all = computeChoices({true}, list).toList()
    val choices = computeChoices(filter, list)
    val choice = choose(instruction, choices, all, buttons, canPass)
    return when {
        choice.startsWith("SELECT_CARD:") -> getCardFromIndex(list, choice).name
        choice.isEmpty() -> { logPass(); ""}
        else -> choice.substringAfterLast(":")
    }
}

suspend fun Player.chooseStringFromButton(instruction: String, buttons: List<Button>, canPass: Boolean = false ) : String {
    val choice = choose(instruction, buttons = buttons, canPass = canPass)
    return choice.substringAfterLast(":")
}


