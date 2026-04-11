package fr.umontpellier.iut.dominion.cards;

import fr.umontpellier.iut.dominion.cards.component.Price;

public class RegistryPrice {
    public static Price SeasidePrice(int coins){
        return new Price(coins, 0, 0);
    }
    public static Price DominionPrice(int coins){
        return new Price(coins, 0, 0);
    }
    public static Price IntriguePrice(int coins){return new Price(coins, 0, 0);}

    public static Price AlchimyPrice(int coins, int potions){return new Price(coins, potions, 0);}
}
