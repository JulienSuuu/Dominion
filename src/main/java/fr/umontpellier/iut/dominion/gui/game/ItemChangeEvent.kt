package fr.umontpellier.iut.dominion.gui.game

data class ItemChangeEvent(
    val playerId: String,
    val item: String,
    val old: Int,
    val new: Int,
    val delta: Int
) {
    fun toJson(): String {
        val safePlayerId = playerId.replace("\"", "\\\"")
        val safeItem = item.replace("\"", "\\\"")

        return """{"itemChange":{"id":"$safePlayerId","item":"$safeItem","old":$old,"new":$new,"delta":$delta}}"""
    }
}