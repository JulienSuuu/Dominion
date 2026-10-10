package fr.umontpellier.iut.dominion.domain.model.ui

data class Button(
    val label: String,
    val value: String
) {
    companion object {
        val Discard = Button("Discard", "d")
        val Trash = Button("Trash", "t")
        val Yes = Button("Yes", "y")
        val No = Button("No", "n")
        val Hand = Button("Hand", "h")
        val Money = Button("Money", "m")
        val Action = Button("Action", "a")
        val Buy = Button("Buy", "b")
        val Draw = Button("Draw", "d")

        val yesOrNo = listOf(Yes, No)
        val DiscardOrTrash = listOf(Discard, Trash)
        val DeckOrDiscard = listOf(Button("Deck", "deck"), Button("Discard", "discard"))
        val TrashOrDeck = listOf(Trash, Button("Deck", "d"))
    }
}