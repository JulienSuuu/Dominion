package fr.umontpellier.iut.dominion.cards.component;

import fr.umontpellier.iut.dominion.Player;
import fr.umontpellier.iut.dominion.cards.Card;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Composant des cartes Durations
 */
public class DurationComponent implements CardComponent {
    /**
     * Durée de l'effet ( 0 ou 1 )
     *
     */
    private Predicate<Card> trigger = c -> false;
    private boolean duration = false;
    /**
     * Méthode qui lance l'effet au prochain de la carte
     */
    private final BiConsumer<Player, Card> nextTurnEffect;

    /**
     *
     * @param nextTurnEffect effet du prochain tour
     */
    public DurationComponent(BiConsumer<Player, Card> nextTurnEffect) {
        this.nextTurnEffect = nextTurnEffect;
    }

    public DurationComponent setTrigger(Predicate<Card> trigger) {
        this.trigger = trigger;
        return this;
    }

    /**
     * Lance l'effet du composant
     * @param p le joueur ( le lanceur ou le receveur )
     */
    public void execute(Player p, Card c) {
        if(nextTurnEffect != null){
            nextTurnEffect.accept(p, c);
        }
    }

    /**
     * Décremente la durée
     */
    public void consume(){
        duration = false;
    }

    /**
     *
     * @return si le joueur doit défausser la carte
     */
    public boolean isFinished(){return !duration;}

    public void activeDuration(Card c){
        if(trigger.test(c))return;
        duration = true;
    }

}
