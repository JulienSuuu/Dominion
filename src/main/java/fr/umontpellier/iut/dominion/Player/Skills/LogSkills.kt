package fr.umontpellier.iut.dominion.Player.Skills

import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.cards.Card
import java.util.UUID

fun Player.reveals(c : Card?){
    if(c == null) return
    privateLog("revealed ${c.name}")
}

fun Player.logGain(c : Card?){
    if(c == null) return
    privateLog("gained ${c.name}")
}

fun Player.logTrash(c : Card?){
    if(c == null) return
    privateLog("trashed ${c.name}")
}

fun Player.logDiscard(c : Card?){
    if(c == null) return
    privateLog("discarded ${c.name}")
}

fun Player.logPlay(c : Card?){
    if(c == null) return
    privateLog("played ${c.name}")
}

fun Player.logEndTurn(){
    privateLog("ended the turn")
}

fun Player.logPass(){
    privateLog("passed his turn")
}

fun Player.logBuy(c: Card?){
    if(c == null) return
    privateLog("bought ${c.name}")
}

fun Player.privateLog(message: String, type : String = "INFO") {
    val playerId = client.id
    val id = UUID.randomUUID().toString()
    val timestamp = System.currentTimeMillis()

    val escapedMessage = message
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")

    game.sendToUI(
        """
        {
          "logChange": {
            "id": "$id",
            "type": "$type",
            "playerId": "$playerId",
            "playerName": "$name",
            "text": "$escapedMessage",
            "timestamp": "$timestamp"
          }
        }
        """.trimIndent()
    )
}


fun Player.reveals(list : List<Card>) = privateLog("reveals $list")
fun Player.revealsIf(list : List<Card>, predicate : Player.(List<Card>) -> Boolean) {
    if(self.predicate(list)) privateLog("reveals $list")
}