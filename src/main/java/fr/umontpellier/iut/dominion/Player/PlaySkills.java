package fr.umontpellier.iut.dominion.Player;

import fr.umontpellier.iut.dominion.Button;
import fr.umontpellier.iut.dominion.CardType;
import fr.umontpellier.iut.dominion.Destination;
import fr.umontpellier.iut.dominion.Item;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.Events.Discard_Type;
import fr.umontpellier.iut.dominion.cards.Events.Event;
import fr.umontpellier.iut.dominion.cards.component.DurationComponent;
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent;
import javafx.beans.property.BooleanProperty;

import java.util.*;

public class PlaySkills implements PlayerComponent{
    private final Player self;

    public PlaySkills(Player self) {
        this.self = self;
    }

    public void playCard(Card c) {
        playCard(c, 1);
    }

    public void playCard(Card c, int amount){
        c.moveTo(self.get(Destination.INPLAY), Destination.INPLAY);
        Event event = new Event(c, Destination.INPLAY, self);
        for(int i=0; i<amount; i++){
            triggerPlayEvent(event);
        }

    }

    public void triggerPlayEvent(Event event){
        if(event.getCard().hasType(CardType.ACTION))self.increment(Item.ACTION_PLAYED, 1);

        self.checkPlayToken(event);
        self.triggerEvent(TriggerComponent.beforeCardPlayed.class, event);
        event.getCard().play(event.getPlayer());
        if(!event.getCard().hasForLocation(Destination.INPLAY))event.getCard().set("unable", true);
        self.triggerEvent(TriggerComponent.OnCardPlayed.class, event);
        self.triggerPlayerTavern(TriggerComponent.afterCardPlayed.class, event);

        if(event.getCard().hasType(CardType.RESERVE) && !event.getCard().getFlag("unable")) event.getCard().moveTo(self.get(Destination.TAVERN), Destination.TAVERN);
    }

    /**
     * Exécute le tour d'un joueur
     * <p>
     * Cette méthode exécute successivement les phases du tour d'un joueur:
     * <p>
     * 1. (Préparation) initialise les compteurs d'actions, d'achats et d'argent du
     * joueur
     * <p>
     * 2. (Action, Trésor et Achat) Le joueur peut jouer des cartes Action et Trésor
     * de sa main, et acheter des cartes de la réserve. Cependant, dès qu'il joue
     * une carte Trésor, il ne peut plus jouer de carte Action pendant le reste de
     * son tour. De même, dès qu'il achète une carte, il ne peut plus jouer de carte
     * Action ni de carte Trésor pendant le reste de son tour.
     * <p>
     * Le joueur peut passer pour terminer son tour. Pour fluidifier le jeu, le tour
     * se termine également automatiquement lorsque le joueur n'a plus d'achat
     * disponible.
     */

    public void playTurn() {
        self.setUpTurn();

        self.getFlag("Action").set(true);
        self. getFlag("Treasure").set(true);
        BooleanProperty action = self.getFlag("Action");
        BooleanProperty treasure = self.getFlag("Treasure");

        self.triggerDurationCard();
        self.triggerStart(TriggerComponent.onStartTurn.class);
        self.getCopyOf(Destination.ASIDE_ACTIVE).forEach(self::playCard);
        while (true) {
            List<String> choices = new ArrayList<>();
            computeChoices(choices, action.get(), treasure.get());
            List<Button> buttons = computeButtons();
            String instruction = computeInstruction(action.get(), treasure.get());

            String playCard = self.choose(instruction, choices, new ArrayList<>(), buttons, true);

            if (playCard.isEmpty()) break;


            if(playCard.startsWith("BUTTON:")) {
                String choice =  playCard.split(":")[1];
                if(choice.equals("REMBOURSER")) {
                    self.repayDebt();
                    continue;
                }
                if(choice.equals("COFFRE")) {
                    self.useCoffer();
                    continue;
                }
            }

            if (playCard.startsWith("HAND:")) {
                Card play = self.get(Destination.HAND).stream()
                        .filter(c -> c.hasName(playCard.split(":")[1]))
                        .findFirst()
                        .orElse(null);

                if (play == null) continue;

                if (play.hasType(CardType.ACTION)) {
                    self.increment(Item.ACTION, -1);

                }
                if (play.hasType(CardType.TREASURE)) {
                    action.set(false);
                }

                self.playCard(play);

                if (self.getValueOf(Item.ACTION) == 0) {
                    action.set(false);
                }
            }

            if (playCard.startsWith("SUPPLY:") || playCard.startsWith("EVENT:")) {

                Card play = switch (playCard.split(":")[0]) {
                    case "SUPPLY" -> self.getCardFromSupply(playCard.split(":")[1]);
                    case "EVENT" ->  self.getGame().getEvent(playCard.split(":")[1]);
                    default -> null;
                };

                if(play == null) continue;
                self.increment(Item.BUY, -1);
                action.set(false);
                treasure.set(false);

                self.buyCard(play);

                if (self.getValueOf(Item.BUY) == 0) {
                    break;
                }
            }
        }
    }


    private void computeChoices(List<String> choices, boolean canPlayAction, boolean canPlayTreasure) {
        for (Card c : self.get(Destination.HAND)) {
            if(canPlayAction && c.hasType(CardType.ACTION) && self.getValueOf(Item.ACTION) > 0){
                choices.add("HAND:" + c.getName());
            }
            if(canPlayTreasure && c.hasType(CardType.TREASURE)) {
                choices.add("HAND:" + c.getName());
            }
        }
        for(Card c : self.getGame().getAvailableSupplyCards()){
            if(!self.canBuy(c)) continue;
            choices.add("SUPPLY:" + c.getName());
        }

        for(Card c : self.getGame().getEventCard()){
            if(!self.canBuy(c)) continue;
            choices.add("EVENT:" + c.getName());
        }
    }

    private List<Button> computeButtons() {
        List<Button> buttons = new ArrayList<>();
        if(self.getValueOf(Item.DEBT) > 0){
            buttons.add(new Button("Rembourser la dette", "REMBOURSER"));
        }
        if(self.getValueOf(Item.COFFER) > 0){
            buttons.add(new Button("Coffre (" + self.getValueOf(Item.COFFER) + ")", "COFFRE"));
        }
        return buttons;
    }

    /**
     * l'instruction complète à donnée au joueur pour ces choix (Action, Treasure, Buy)
     *
     * @param canPlayAction si le joueur peut jouer une carte Action
     * @param canPlayTreasure si le joueur peut jouer une carte Treasure
     * @return l'instruction
     */
    private String computeInstruction(boolean canPlayAction, boolean canPlayTreasure) {
        StringJoiner instructions = new StringJoiner(" | ");
        instructions.add("CHOOSE AN EVENT: ");
        if(canPlayAction){
            instructions.add("ACTION");
        }
        if(canPlayTreasure){
            instructions.add("TREASURE");
        }
        instructions.add("BUY ");
        return instructions.toString();
    }



    public void cleanup() {
        self.getCopyOf(Destination.HAND).forEach(c -> {self.discard(c, Discard_Type.CLEANUP); c.clear();});

        self.triggerEvent(TriggerComponent.onEndBuy.class); // inPlay
        self.triggerActiveEffect(TriggerComponent.onEndBuy.class); //autre
        self.triggerPlayerAndCardTavern(TriggerComponent.onEndBuy.class, new Event(self)); //Tavern

        self.getCopyOf(Destination.INPLAY).stream().filter(c->
                        c.getComponent(DurationComponent.class)
                                .map(d -> d.isFinished(c))
                                .orElse(true))
                .toList()
                .forEach(c ->{
                    self.discard(c, Discard_Type.CLEANUP);
                    c.clear();
                });

        if (!self.getController().equals(self)) {
            self.setController(self);
            self.mustBeDiscarded().forEach(c -> {self.discard(c, Discard_Type.CLEANUP); c.clear();});
        }

        int numberOfDraw = Math.max(5 + self.getDrawBonus(),0) ;
        cleanUpDraw(numberOfDraw);
        self.setDrawBonus(0);
        self.clearList();
        self.resetItem();
        self.resetFlags();
        self.resetProperties();
    }

    private void cleanUpDraw(int todraw){
        for (int i = 0; i < todraw; i++) {
            Card c = self.getCardFromDeck();
             if (c == null) break;
             self.moveTo(c, Destination.HAND);
        }
    }
}
