package fr.umontpellier.iut.dominion.cards.component;


import fr.umontpellier.iut.dominion.Player.Player;
import fr.umontpellier.iut.dominion.cards.Card;
import fr.umontpellier.iut.dominion.cards.Events.Event;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface TriggerComponent extends CardComponent {

    /**
     * Triggers se déclenchant lors du gain d'une carte
     */
    interface DuringPlayerGain extends TriggerBiEffect<Event, Player, DuringPlayerGain> {
        @Override
        default DuringPlayerGain create(BiConsumer<Event, Player> effect){return effect::accept;};

        @Override
        default DuringPlayerGain self() {
            return this;
        }
    }
    interface AfterPlayerGain extends TriggerBiEffect<Event, Player, AfterPlayerGain> {
        @Override
        default AfterPlayerGain create(BiConsumer<Event, Player> effect){return effect::accept;};
        default AfterPlayerGain self() {
            return this;
        }
    }

    /**
     * Trigger se déclenchant lorsqu'un joueur joue une carte
     */
    interface OnCardPlayed extends TriggerBiEffect<Event, Player, OnCardPlayed> {
        @Override
        default OnCardPlayed create(BiConsumer<Event, Player> effect) {return effect::accept;}
        default OnCardPlayed self() {
            return this;
        }
    }
    interface beforeCardPlayed extends TriggerBiEffect<Event, Player, beforeCardPlayed> {
        @Override
        default beforeCardPlayed create(BiConsumer<Event, Player> effect) {return effect::accept;}
        default beforeCardPlayed self() {
            return this;
        }
    }
    interface afterCardPlayed extends TriggerBiEffect<Event, Player, afterCardPlayed> {
        @Override
        default afterCardPlayed create(BiConsumer<Event, Player> effect){return effect::accept;}
        default afterCardPlayed self() {
            return this;
        }
    }

    /**
     * Trigger se déclenchant en début de tour
     */
    interface onStartTurn extends TriggerEffect<Player, onStartTurn> {
        default onStartTurn create(Consumer<Player> effect){return effect::accept;}
        default onStartTurn self() {
            return this;
        }
    }

    /**
     * Trigger se déclenchant au moment où le joueur peut être immunisé
     */
    interface Immunity extends TriggerComponent {
        default boolean revealed(Player player, Card self) {return false;}
        default boolean immune(Card self) {return false;}
        default boolean isImmuneAgainst(Card self, Card attack){return false;}
    }

    /**
     * Trigger se déclenchant en fin de la phase d'achat ( début clean Up )
     */
    interface onEndBuy extends TriggerBiEffect<Player, Card, onEndBuy> {
        @Override
        default onEndBuy create(BiConsumer<Player, Card> effect) {return  effect::accept;}
        default onEndBuy self() {return this;}
    }

    /**
     * Trigger se déclenchant sur une carte défaussé ( elle-même )
     */
    interface checkItselfDiscarded extends TriggerBiEffect<Event, Card, checkItselfDiscarded> {
        @Override
        default checkItselfDiscarded create(BiConsumer<Event, Card> effect){
            return effect::accept;
        };
        default  checkItselfDiscarded self() {return this;}
    }

    /**
     * Trigger se déclenchant sur une carte écarte ( elle-même )
     */
    interface checkItselfTrashed extends TriggerBiEffect<Event, Card, checkItselfTrashed> {
        default checkItselfTrashed create(BiConsumer<Event, Card> effect){
            return effect::accept;
        }

        @Override
        default checkItselfTrashed self() {
            return this;
        }
    }
    interface onCardTrashed extends TriggerBiEffect<Event, Card, onCardTrashed> {
        @Override
        default onCardTrashed create(BiConsumer<Event, Card> effect) {return effect::accept;}
        default  onCardTrashed self() {return this;}
    }

    /**
     * Trigger se déclenchant au moment de l'achat d'une carte ( avant son déplacement physique )
     */
    interface onBuy extends TriggerBiEffect<Player, Card, onBuy> {
        @Override
        default onBuy create(BiConsumer<Player, Card> effect){return effect::accept;};
        default  onBuy self() {return this;}
    }

    interface checkItSelfBuy extends TriggerBiEffect<Event, Card, checkItSelfBuy> {
        @Override
        default checkItSelfBuy create(BiConsumer<Event, Card> effect) {return effect::accept;}
        default  checkItSelfBuy self() {return this;}
        default checkItSelfBuy setFlag (String name) { return then((event, card) -> event.getPlayer().getFlag(name).set(true));}
    }


    /**
     * Trigger se déclenchant sur la carte gagné ( elle-même)
     */
    interface checkItselfGain extends TriggerBiEffect<Event, Card, checkItselfGain> {
        @Override
        default checkItselfGain create(BiConsumer<Event, Card> effect) {return effect::accept;}
        default  checkItselfGain self() {return this;}
    }

    /**
     * Trigger se déclenchant sur une carte qui peut être payer plus chère
     */
    interface overPaidCard extends TriggerBiEffect<Player, Card, overPaidCard> {
        @Override
        default overPaidCard create(BiConsumer<Player, Card> effect) {return effect::accept;}
        default  overPaidCard self() {return this;}
    }

    /**
     * Trigger personnel du joueur sur les carte défaussé
     */
    interface discardHook extends TriggerEffect<Event, discardHook> {
        default discardHook create(Consumer<Event> effect){return effect::accept;}
    }
}
