package fr.umontpellier.iut.dominion.domain.dsl.card.core

import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.card.Card

data class GainChoice(val player: Player, val pipeLineCard: Card)
