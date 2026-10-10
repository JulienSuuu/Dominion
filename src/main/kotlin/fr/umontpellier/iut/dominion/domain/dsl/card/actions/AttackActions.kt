package fr.umontpellier.iut.dominion.domain.dsl.card.actions

import fr.umontpellier.iut.dominion.domain.model.location.Destination
import fr.umontpellier.iut.dominion.domain.common.Logger
import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.card.Card
import fr.umontpellier.iut.dominion.domain.components.card.effect.BiEffect
import fr.umontpellier.iut.dominion.domain.components.card.effect.then
import fr.umontpellier.iut.dominion.domain.dsl.card.builders.ContextBuilder
import fr.umontpellier.iut.dominion.domain.dsl.card.builders.lookingAt

/**
 * Injecte un comportement d'attaque au sein du pipeline.
 *
 * Utilise la plomberie interne du jeu (`processAttack`) pour répertorier et cibler automatiquement
 * les adversaires valides (en gérant de manière transparente les cartes de réaction ou de protection).
 *
 * @param attackLogic La logique d'attaque appliquée à chaque adversaire valide détecté.
 */
fun <T : BiEffect<Player, Card>, X> ContextBuilder<T, Player, Card, X>.attack(
    attackLogic: suspend X.(attacker: Player, opponent: Player, card: Card) -> Unit
): ContextBuilder<T, Player, Card, X> {
    return ContextBuilder(this.parent) {
        val ctx = this.function()

        if(ctx.data != null) right.game.processAttack(right, left) { opponent -> ctx.data.attackLogic(right, opponent, left) }

        ctx
    }
}

fun <U : Logger, X> ContextBuilder<BiEffect<U, Card>, U, Card, X>.processHandDown(dest : Destination.PlayerZone = Destination.PlayerZone.Discard, toReach : Int = 1, mayDiscard : Boolean = false, extraAction : suspend (Player) -> Unit = {}) : ContextBuilder<BiEffect<U, Card>, U, Card, X> {
    return this.thenDo { u, v, _ -> u.toPlayer().game.processHandDown(u.toPlayer(), v, dest, toReach, mayDiscard, extraAction) }
}


/**
 * Injecte un comportement de bénéfice partagé du joueur actif.
 *
 * @param target Fonction permettant de déterminer le joueur initiateur du bénéfice.
 * @param benefitLogic La logique d'avantage appliquée à chaque joueur autre que le joueur actif.
 */
fun <T : BiEffect<U, V>, U : Logger, V, X> ContextBuilder<T, U, V, X>.benefitOthers(
    target: (U, V, X) -> Player = { logger, _, _ -> logger.toPlayer() },
    benefitLogic: suspend X.(attacker: Player, opponent: Player) -> Unit
): ContextBuilder<T, U, V, X> = thenDo { u, v, data ->
    val player = target(u, v, data)
    player.game.processBenefit(player) { ally ->
        data.benefitLogic(player, ally)
    }
}

fun BiEffect<Player, Card>.attack(playerLogic : suspend (attacker:Player, opponent:Player, card:Card) -> Unit) : ContextBuilder<BiEffect<Player, Card>, Player, Card, Boolean>{
    return this.lookingAt { _, _-> true }.attack{player, opponent, card -> playerLogic(player, opponent, card)}
}

fun <U : Logger> BiEffect<U, Card>.attackOthers(playerLogic: suspend (attacker: Player, opponent: Player, card : Card) -> Unit) : BiEffect<U, Card>{
    return this.then{u, card ->
        u.toPlayer().game.processAttack(u.toPlayer(), card){vi -> playerLogic(u.toPlayer(), vi, card)}
    }
}

fun <U : Logger, V> BiEffect<U, V>.benefit(playerLogic: suspend (Player) -> Unit) : BiEffect<U, V> =
    this.then { u, v -> u.toPlayer().game.processBenefit(u.toPlayer(), playerLogic ) }

fun <U : Logger, V> BiEffect<U, V>.globalEffect(playerLogic: suspend (Player, V, Player) -> Unit) : BiEffect<U, V> {
    return this.then { u, v -> u.toPlayer().game.processGlobalEffect(u.toPlayer(), {z -> playerLogic(u.toPlayer(), v, z) } ) }
}

fun <U : Logger> BiEffect<U, Card>.processHandDown(dest : Destination.PlayerZone = Destination.PlayerZone.Discard, toReach : Int = 1, mayDiscard : Boolean = false) : BiEffect<U, Card>{
    return this.then { u, v -> u.toPlayer().game.processHandDown(u.toPlayer(), v, dest, toReach, mayDiscard) }
}
