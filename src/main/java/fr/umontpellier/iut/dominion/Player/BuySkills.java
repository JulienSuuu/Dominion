package fr.umontpellier.iut.dominion.Player;

import fr.umontpellier.iut.dominion.*;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.Events.Event;
import fr.umontpellier.iut.dominion.cards.component.TriggerComponent;

import java.util.ArrayList;
import java.util.List;

public class BuySkills implements PlayerComponent{
    private final Player self;
    public BuySkills(Player self) {
        this.self = self;
    }


    public void repayDebt(){
        int toRepay = Math.min(self.getValueOf(Item.MONEY), self.getValueOf(Item.DEBT));
        self.decrement(Item.DEBT, toRepay);
        self.decrement(Item.MONEY, toRepay);
    }

    public void useCoffre(){
        if(self.getValueOf(Item.COFFER) > 0){
            self.decrement(Item.COFFER, 1);
            self.increment(Item.MONEY, 1);
        }
    }

    public boolean canBuy(Card c) {
        boolean enoughMoney = self.getValueOf(Item.MONEY) >= c.getCost();
        boolean enoughPotion = self.getValueOf(Item.POTION) >= c.getPotion();
        boolean isNotDebted = self.getValueOf(Item.DEBT) == 0;
        boolean available = c.getAvailable().test(self);
        boolean isNotContraband = !self.getGame().getNamedCardsThisTurn("contraband").contains(c.getName());
        boolean isNotExpedition = (!c.hasType(CardType.EVENT) && !self.isFlagSet(Flags.expedition)) || c.hasType(CardType.EVENT);
        return enoughMoney && enoughPotion && isNotDebted && available && isNotContraband && isNotExpedition;
    }

    public int overPaid(Card c, int potion){
        int i = 0;
        if(c.hasType(CardType.OVERPAID) && self.getMoney() > c.getCost()){
            String choose = self.chooseWhatToDo("Do you want to overpaid ? ", List.of(c), Button.yesOrNo, true);
            if("y".equals(choose)) {
                int start = c.getCost();
                while(start < self.getMoney()){
                    List<Button> buttons = new ArrayList<>();
                    buttons.add(new Button("Increment", "i"));
                    buttons.add(new Button(" use Potion", "p"));

                    String howMuch = self.chooseWhatToDo("How much ? Max : " + self.getValueOf(Item.MONEY),  List.of(c), buttons , true);
                    if("i".equals(howMuch)) {
                        start += 1;
                    }else if("p".equals(howMuch)) {
                        potion += 1;
                    }else break;
                }
                i = start - c.getCost();
                c.set("OverpaidNumber", i);
                c.set("Potion", potion);
            }
        }
        c.getComponent(TriggerComponent.overPaidCard.class).ifPresent(t -> t.accept(self, c));
        return i;
    }

    public void buyCard(Card c) {
        if(c==null)return;
        int potion = 0;
        int overpaid = overPaid(c, potion);
        self.decrement(Item.MONEY, c.getCost() + overpaid);
        self.decrement(Item.POTION, c.getPotion() + potion);
        self.decrement(Item.DEBT, c.getDebt());
        self.log(self.toLog() + " bought " + c.toLog());
        Event event = new Event(c, Destination.DISCARD, self, true);
        if(c.hasType(CardType.EVENT)){
            if(c.canExecute(event, self, TriggerComponent.checkItSelfBuy.class))
                c.getComponent(TriggerComponent.checkItSelfBuy.class).ifPresent(t -> t.accept(event, c));
            return;
        }
        triggerBuy(event.getCard());
        self.gain(event);
        onCursePile(c);
    }

    public void triggerBuy(Card c){
        self.getCopyOf(Destination.INPLAY)
                .forEach(card -> card.getComponent(TriggerComponent.onBuy.class).ifPresent(d -> d.accept(self, c)));

    }

    public void onCursePile(Card c){
        if(self.getGame().hasToken(c.getName())){
            for (int i = 0; i <  self.getGame().getToken(c.getName()); i++ ){
                self.gain(self.getCardFromSupply("Curse"),Destination.DISCARD);
            }
        }
    }


}
