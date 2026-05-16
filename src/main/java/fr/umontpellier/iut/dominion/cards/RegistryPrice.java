package fr.umontpellier.iut.dominion.cards;

import fr.umontpellier.iut.dominion.cards.component.Price;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class RegistryPrice {
    public static Price SeasidePrice(int coins){
        return new Price(create(coins), 0, createDebt(0));
    }
    public static Price DominionPrice(int coins){
        return new Price(create(coins), 0, createDebt(0));
    }
    public static Price IntriguePrice(int coins){return new Price(create(coins), 0, createDebt(0));}

    public static Price AlchimyPrice(int coins, int potions){return new Price(create(coins), potions, createDebt(0));}

    public static Price ProsperityPrice(int coins){return new Price(create(coins), 0, createDebt(0));}

    public static Price Cornucopia(int coins){return new Price(create(coins), 0, createDebt(0));}
    public static Price Hinterlands(int coins){return new Price(create(coins), 0, createDebt(0));}

    public static Price DarkAges(int coins){return new Price(create(coins), 0, createDebt(0));}

    public static Price Adventure(int coins){return new Price(create(coins), 0, createDebt(0));}


    public static Price Empires(int coins, int debt){return new Price(create(coins), 0, createDebt(debt));}


    private static IntegerProperty create(int i){
        return new SimpleIntegerProperty(i);
    }

    private static IntegerProperty createDebt(int i){
        return  new SimpleIntegerProperty(i);
    }
}
