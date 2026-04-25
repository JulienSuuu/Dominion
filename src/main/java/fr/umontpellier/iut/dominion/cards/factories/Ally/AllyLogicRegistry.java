package fr.umontpellier.iut.dominion.cards.factories.Ally;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AllyLogicRegistry {
    private final Map<String, AllyLogic> logiques = new HashMap<>();

    public AllyLogicRegistry(){}


    public AllyLogic getLogicFor(String name){
        return logiques.getOrDefault(name, new AllyLogic());
    }
}
