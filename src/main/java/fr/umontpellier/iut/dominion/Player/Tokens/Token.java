package fr.umontpellier.iut.dominion.Player.Tokens;

public enum Token {
    ONE_MONEY_TOKEN("+1$", true),
    ONE_ACTION_TOKEN("+1 action", true),
    ONE_BUY_TOKEN("+1 buy", true),
    ONE_CARD_TOKEN("+1 Card", true),
    CARD_REDUCTION_TOKEN("-2$ cost", false),
    TAX_TOKEN("-1$", false),
    TRASHING_TOKEN("Trashing", false),
    ESTATE_TOKEN("Estate", false),
    JOURNEY_TOKEN("Journey", false),
    MINUS_ONE_CARD_TOKEN("-1 Card", false);


    private final String displayName;
    private final boolean playToken;
    Token(String displayName, boolean play) { this.displayName = displayName; playToken = play; }
    public boolean isPlayToken() { return playToken; }
}
