package fr.umontpellier.iut.dominion;

/**
 * Type énuméré des différents types de cartes possibles
 * <p>
 * Une carte peut éventuellement avoir plusieurs types, par exemple
 * Action/Attaque ou Action/Réaction
 */
public enum CardType {
    TREASURE,
    ACTION,
    VICTORY,
    CURSE,
    REACTION,
    ATTACK,
    DURATION,
    OVERPAID,
    COMMAND,
    LOOTER,
    TRAVELLER,
    REWARDS{
        @Override
        public boolean isSpecial() {
            return true;
        }
    },
    KNIGHT{
        @Override
        public boolean isSpecial() {
            return true;
        }
    },
    RUINS{
        @Override
        public boolean isSpecial() {
            return true;
        }
    },
    SHELTER{
        @Override
        public boolean isSpecial() {
            return true;
        }
    },
    TEMPLATE,
    EVENT{
        @Override
        public boolean isSpecial() {
            return true;
        }
    },
    LANDMARK{
        @Override
        public boolean isSpecial() {
            return true;
        }
    },
    RESERVE,
    ASIDE{
        @Override
        public boolean isSpecial() {
            return true;
        }
    },
    NIGHT,
    GATHERING,
    CASTLE{
        @Override
        public boolean isSpecial() {
            return true;
        }
    },
    Split{
        @Override
        public boolean isSpecial() {
            return true;
        }
    },
    HEIRLOOM{
        @Override
        public boolean isSpecial() {
            return true;
        }
    },
    FATE,
    ZOMBIE{
        @Override
        public boolean isSpecial() {
            return true;
        }
    },
    DOOM,
    SPIRIT{
        @Override
        public boolean isSpecial() {
            return true;
        }
    },
    BOON,
    HEX;


    public boolean isSpecial(){
        return false;
    }

    public static final CardType[] ActionAndAttack = new CardType[]{ACTION, ATTACK};

}