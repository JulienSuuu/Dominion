package fr.umontpellier.iut.dominion.cards.component;

import fr.umontpellier.iut.dominion.Player.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Composant des cartes Durations
 */
public class DurationComponent implements CardComponent {
    /**
     * Durée de l'effet ( 0 ou 1 )
     *
     */
    private Predicate<Card> trigger = c -> true;
    private Predicate<Card> thingToDo = t -> false;
    private final IntegerProperty duration = new SimpleIntegerProperty() ;
    private int numberOfTurns=1;
    private boolean isInfinite = false;
    private Predicate<Card> stayInPlayCondition = c -> checkDuration().test(c);

    /**
     * Méthode qui lance l'effet au prochain de la carte
     */
    private final BiEffect<Player, Card, duration> nextTurnEffect;

    public interface duration extends TriggerBiEffect<Player, Card, duration> {
        @Override
        default duration self(){
            return this;
        };

        @Override
        default duration create(BiConsumer<Player, Card> effect) {
            return effect::accept;
        }
    }

    /**
     *
     * @param nextTurnEffect effet du prochain tour
     */
    public DurationComponent(duration nextTurnEffect) {
        this.nextTurnEffect =  nextTurnEffect;
    }

    public DurationComponent setTrigger(Predicate<Card> trigger) {
        this.trigger = trigger;
        return this;
    }

    public DurationComponent setInfinite(boolean infinite) {
        this.isInfinite = infinite;
        return this;
    }

    public DurationComponent stayInPlayCondition(Predicate<Card> stayInPlayCondition) {
        this.stayInPlayCondition = stayInPlayCondition;
        return this;
    }

    public DurationComponent thingToDo(Predicate<Card> thingToDo) {
        this.thingToDo = thingToDo;
        return this;
    }

    public DurationComponent setNumberOfTurns(int numberOfTurns) {
        this.numberOfTurns = numberOfTurns;
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
        if(isInfinite)return;
        duration.set(duration.get()-1);
    }

    /**
     *
     * @return si le joueur doit défausser la carte
     */
    public boolean isFinished(Card c) {
        if (isInfinite) return false;
        if (thingToDo.test(c)) return false;
        return stayInPlayCondition.test(c);
    }

    public void activeDuration(Card c){
        if(!trigger.test(c)|| duration.get() == numberOfTurns )return;
        duration.set(numberOfTurns);
    }


    public Predicate<Card> checkDuration(){
        return card -> duration.get() <= 0;
    }

}
