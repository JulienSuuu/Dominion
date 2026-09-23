package fr.umontpellier.iut.dominion.Interface;

import fr.umontpellier.iut.dominion.AppDominion;
import fr.umontpellier.iut.dominion.Button;
import fr.umontpellier.iut.dominion.Game;
import fr.umontpellier.iut.dominion.cards.Card;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface IDominionObject {

    default Game getGame(){
        return AppDominion.getGame();
    }
}
