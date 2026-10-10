package fr.umontpellier.iut.dominion.domain.components.card.trigger

import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.setup.card.factories.futaba.EvaluatedPokerHand
import fr.umontpellier.iut.dominion.domain.components.card.CardComponent
import fr.umontpellier.iut.dominion.domain.components.card.effect.BiEffect

fun interface PokerHandReactionComponent : BiEffect<Player, EvaluatedPokerHand>, CardComponent