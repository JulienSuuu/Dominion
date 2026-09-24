package fr.umontpellier.iut.dominion.cards.component

import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.cards.factories.Futaba.EvaluatedPokerHand

fun interface PokerHandReactionComponent : BiEffect<Player, EvaluatedPokerHand>, CardComponent