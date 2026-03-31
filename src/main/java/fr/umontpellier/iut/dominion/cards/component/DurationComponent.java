package fr.umontpellier.iut.dominion.cards.component;

import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;

import java.util.function.Consumer;

/**
 * Composant des cartes Durations
 */
public class DurationComponent implements CardComponent {
    /**
     * Durée de l'effet ( 0 ou 1 )
     *
     */
    private int duration;
    /**
     * Méthode qui lance l'effet au prochain de la carte
     */
    private final Consumer<Player> nextTurnEffect;
    /**
     * Carte qui contient ce composant
     */
    private final Card parent;

    /**
     *
     * @param duration durée
     * @param nextTurnEffect effet du prochain tour
     * @param parent la carte parent
     */
    public DurationComponent(int duration, Consumer<Player> nextTurnEffect, Card parent) {
        this.duration = duration;
        this.nextTurnEffect = nextTurnEffect;
        this.parent = parent;
    }

    /**
     * Lance l'effet du composant
     * @param p le joueur ( le lanceur ou le receveur )
     */
    public void execute(Player p){
        if(nextTurnEffect != null){
            nextTurnEffect.accept(p);
        }
    }

    /**
     * Décremente la durée
     */
    public void decrement(){
        duration--;
    }

    /**
     *
     * @return si le joueur doit défausser la carte
     */
    public boolean isFinished(){
        return duration == 0;
    }

    public Card getParent(){
        return parent;
    }

    public void setDuration(int duration){
        this.duration = duration;
    }


}
