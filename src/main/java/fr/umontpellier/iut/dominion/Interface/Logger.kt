package fr.umontpellier.iut.dominion.Interface;

import fr.umontpellier.iut.dominion.Button;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.component.InteractionRequest;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface Logger extends  IDominionObject{

    default void log(String message){
        getGame().log(message);
    }

    String chooseWhatToDo(String instruction, List<Card> list, List<Button> buttons, boolean canPass);

    Optional<Card> chooseCardFromHand(String instruction, Predicate<? super Card> predicate, boolean canPass);
    Optional<Card> chooseCardFromList(String instruction, Predicate<? super Card> predicate, List<Card> cards ,boolean canPass);



    default <X> Optional<String> chooseWhatToDo(InteractionRequest<X> request) {
        String result = chooseWhatToDo(
                request.instruction(),
                request.cards(),
                request.buttons(),
                request.canPass()
        );

        return Optional.ofNullable(result);
    }

    default <X> Optional<Card> chooseCardFromHand(InteractionRequest<X> request) {
        return chooseCardFromHand(request.instruction(), request.filter(), request.canPass());
    }

    default <X> Optional<Card> chooseCardFromList(InteractionRequest<X> request) {
        return chooseCardFromList(request.instruction(), request.filter(), request.cards(), request.canPass());
    }






}
