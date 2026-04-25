package fr.umontpellier.iut.dominion;

import java.util.List;

public record Button(String label, String value) {
    public static List<Button> yesOrNo = List.of(new Button("Yes", "y"), new Button("No", "n"));
    public static List<Button> DiscardOrTrash = List.of(new Button("Discard", "d"), new Button("Trash", "t"));
    public static List<Button> DeckOrDiscard = List.of(new Button("Deck", "deck"), new Button("Discard", "discard"));
    public static Button Discard  = new Button("Discard", "d");
    public static Button Trash = new Button("Trash", "t");
    public static Button Yes = new Button("Yes", "y");
    public static Button No = new Button("No", "n");
    public static List<Button> TrashOrDeck = List.of(new Button("Trash", "t"), new Button("Deck", "d"));
    public static Button Money =  new Button("Money", "m");
    public static Button Action = new Button("Action", "a");
    public static Button Buy = new Button("Buy", "b");
    public static Button Draw = new Button("Draw", "d");
}
