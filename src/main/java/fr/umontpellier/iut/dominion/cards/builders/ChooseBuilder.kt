package fr.umontpellier.iut.dominion.cards.builders

import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Interface.Logger
import fr.umontpellier.iut.dominion.Interface.chooseCardFromHand
import fr.umontpellier.iut.dominion.Interface.chooseCardFromList
import fr.umontpellier.iut.dominion.Interface.chooseCardFromSupply
import fr.umontpellier.iut.dominion.Interface.chooseWhatToDo
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.component.BiEffect
import fr.umontpellier.iut.dominion.cards.component.ChoiceMade
import fr.umontpellier.iut.dominion.cards.component.InteractionRequest
import fr.umontpellier.iut.dominion.cards.gainFromSupply

/**
 * Constructeur fluide dédié à l'enchaînement et à la gestion des requêtes d'interaction
 * et de choix des joueurs au sein d'un pipeline réactif.
 *
 * @param T Le type de l'effet réactif racine (ex: `BiEffect<Player, Card>`).
 * @param U Le type du Logger ou de l'événement principal agissant comme contexte (souvent le joueur actif).
 * @param V Le type de l'entité propriétaire ou déclencheuse de l'effet (souvent la carte jouée).
 * @param X Le type de la donnée initiale ou de la collection historique source du pipeline.
 * @param R Le type du résultat produit par le choix utilisateur à l'étape courante.
 * @property parent Le constructeur de contexte encapsulant la donnée d'origine [X].
 * @property result Le constructeur de contexte encapsulant le résultat actuel [R].
 * @property targetPicker Fonction permettant de cibler le joueur ([Logger]) recevant la requête UI.
 */
class ChooseBuilder<T : BiEffect<U, V>, U : Logger, V, X, R, D>(
    val parent: ContextBuilder<T, U, V, X>,
    val result: ContextBuilder<T, U, V, PipelineState<X, R, D>>,
    val targetPicker: suspend (U, V, X) -> Logger
) {

    /**
     * Termine la phase de choix en appliquant une action finale sur le résultat courant [R]
     * avec un accès complet à l'environnement de l'effet ([U], [V]).
     */
    fun thenWith(action: suspend (U, V, R) -> Unit): ContextBuilder<T, U, V, PipelineState<X, R, D>> {
        return result.thenDo { u, v, r -> action(u, v, r.current) }
    }

    /**
     * Termine la phase de choix en appliquant une action finale simplifiée sur l'entité
     * de contexte [U] et le résultat courant [R].
     */
    fun thenWith(action : suspend (U, R) -> Unit): ContextBuilder<T, U, V, PipelineState<X, R, D>> {
        return result.thenWith { u, r -> action(u, r.current)  }
    }

    /**
     * Oriente le flux selon le choix utilisateur [R] tout en transmettant la donnée historique [X].
     */
    fun branch(vararg cases: Pair<R, suspend (U, V, X) -> Unit>): ContextBuilder<T, U, V, R> {
        val choiceBuilder = this.result
        val casesMap = cases.toMap()

        return ContextBuilder(choiceBuilder.parent) {

            val choiceCtx = choiceBuilder.function(this)
            val choiceR = choiceCtx.data?.current
            val dataX = choiceCtx.data?.source

            if (choiceR != null && dataX != null) {
                val associative = casesMap[choiceR]
                associative?.invoke(choiceCtx.right, choiceCtx.left, dataX)
            }

            Context(right, left, choiceR)
        }
    }

    /**
     * Étape de clôture avancée : résout et consomme simultanément la donnée historique d'origine [X]
     * ET le résultat final du choix [R] sous le contexte environnemental ([U], [V]).
     */
    fun thenDo(action: suspend (U, V, X, R) -> Unit): ContextBuilder<T, U, V, R> {
        val choiceBuilder = this.result
        return ContextBuilder(choiceBuilder.parent) {

            val ctxR = choiceBuilder.function(this)
            val dataR = ctxR.data?.current
            val dataX = ctxR.data?.source

            if (dataX != null && dataR != null) {
                action(ctxR.right, ctxR.left, dataX, dataR)
            }

            Context(right, left, dataR)
        }
    }

    /**
     * Termine la phase de choix en ignorant le résultat courant [R] pour se focaliser
     * uniquement sur la donnée source originelle [X].
     */
    fun thenDo(action: suspend (U, V, X) -> Unit): ContextBuilder<T, U, V, R> {
        return thenDo { u, v, x, _ -> action(u, v, x) }
    }

    /**
     * Termine la phase de choix en exécutant un effet de bord pur basé uniquement sur le contexte
     * environnemental ([U], [V]), sans exploiter les données stockées dans le pipeline.
     */
    fun thenDo(action: suspend (U, V) -> Unit): ContextBuilder<T, U, V, R> {
        return thenDo { u, v, _, _ -> action(u, v) }
    }

    /**
     * Exécute une action rapide d'effet de bord sur le [Logger] / Joueur [U] en fin de parcours.
     */
    fun so(action :suspend (U) -> Unit): ContextBuilder<T, U, V, PipelineState<X, R, D>> {
        return result.so(action)
    }

    /**
     * Extrait directement le constructeur de contexte du résultat courant [R].
     */
    fun result(): ContextBuilder<T, U, V, PipelineState<X, R, D>> = result

    /**
     * Permet de revenir en arrière dans l'arbre syntaxique pour récupérer le constructeur
     * lié à la donnée d'origine [X].
     */
    fun back(): ContextBuilder<T, U, V, X> = parent

    /**
     * Clôture définitivement le constructeur de choix en remontant au parent pour compiler
     * l'instance de l'effet réactif racine [T].
     */
    fun endParent() : BiEffect<U, V> = result.end()

    /**
     * Applique un effet de bord intermédiaire sur le résultat actuel [R] (avec accès à [U] et [V])
     * tout en conservant le contexte fluide du [ChooseBuilder].
     */
    fun also(action:suspend (U, V, R) -> Unit): ChooseBuilder<T, U, V, X, R, D> {
        val nextResult = result.thenDo { u, v, r -> action(u, v, r.current) }
        return ChooseBuilder(parent, nextResult, targetPicker)
    }

    /**
     * Applique un effet de bord intermédiaire exploitant simultanément [X] et [R].
     */
    fun alsoDo(action: suspend (U, V, X, R) -> Unit): ChooseBuilder<T, U, V, X, R, D> {
        val nextResult = ContextBuilder(result.parent) {
            val ctxR = result.function(this)
            val dataR = ctxR.data?.current
            val dataX = ctxR.data?.source

            if (dataX != null && dataR != null) {
                action(ctxR.right, ctxR.left, dataX, dataR)
            }
            ctxR
        }
        return ChooseBuilder(parent, nextResult, targetPicker)
    }

    /**
     * Déclenche une interface utilisateur forçant le choix d'une carte depuis la main du joueur ciblé.
     */
    fun chooseCardFromHand(config: (U, V) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, Card, D> {
        val next = result.internalChooseWithData({u, v, _, _ -> config(u, v)}, targetPicker) { logger, request -> logger.chooseCardFromHand(request) }
        return ChooseBuilder(parent, next, targetPicker)
    }

    /**
     * Déclenche une interface utilisateur forçant le choix d'une carte depuis une liste spécifique.
     */
    fun chooseCardFromList(config: (U, V) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, Card, D> {
        val next = result.internalChooseWithData({u, v, _, _-> config(u, v)}, targetPicker) { logger, request -> logger.chooseCardFromList(request) }
        return ChooseBuilder(parent, next, targetPicker)
    }

    /**
     * Déclenche une requête d'interaction par choix multiples ou boutons textuels (ex: Oui/Non).
     */
    fun chooseWhatToDo(config: (U, V) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, String, D> {
        val next = result.internalChooseWithData({u, v, _, _ -> config(u, v)}, targetPicker) { logger, request -> logger.chooseWhatToDo(request) }
        return ChooseBuilder(parent, next, targetPicker)
    }

    /**
     * Déclenche un choix de carte en main en injectant la valeur actuelle de [R] dans la requête.
     */
    fun chooseCardFromHand(config: (U, V, R) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, Card, D> {
        val next = result.internalChooseWithData({u, v, _, r -> config(u, v, r)}, targetPicker) { logger, request -> logger.chooseCardFromHand(request) }
        return ChooseBuilder(parent, next, targetPicker)
    }

    fun chooseCardFromSupply(config: (U, V) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, Card, D> {
        val next = result.internalChooseWithData({u, v, _, _ -> config(u, v)}, targetPicker){logger, request -> logger.chooseCardFromSupply(request) }
        return ChooseBuilder(parent, next, targetPicker)
    }

    fun chooseCardFromSupply(config: (U, V, R) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, Card, D> {
        val next = result.internalChooseWithData({u, v, _, r -> config(u, v, r)}, targetPicker){logger, request -> logger.chooseCardFromSupply(request) }
        return ChooseBuilder(parent, next, targetPicker)
    }

    /**
     * Déclenche un choix de carte depuis une liste en injectant la valeur actuelle de [R] dans la requête.
     */
    fun chooseCardFromList(config: (U, V, R) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, Card, D> {
        val next = result.internalChooseWithData({u, v, _, r -> config(u, v, r)}, targetPicker) { logger, request -> logger.chooseCardFromList(request) }
        return ChooseBuilder(parent, next, targetPicker)
    }

    /**
     * Déclenche un choix textuel en injectant la valeur actuelle de [R] dans la configuration de la requête.
     */
    fun chooseWhatToDo(config: (U, V, R) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, String, D> {
        val next = result.internalChooseWithData({u, v, _, r -> config(u, v, r)}, targetPicker) { logger, request -> logger.chooseWhatToDo(request) }
        return ChooseBuilder(parent, next, targetPicker)
    }

    /**
     * Ouvre un choix de carte en main en fournissant l'ensemble des données cumulées sans re-déclenchement.
     */
    fun chooseCardFromHand(config: (U, V, X, R) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, Card,D> {
        val next = result.internalChooseWithData(
            config = { u, v, x, r -> config(u, v, x, r)
            },
            target = targetPicker
        ) { logger, request -> logger.chooseCardFromHand(request) }
        return ChooseBuilder(parent, next, targetPicker)
    }

    /**
     * Ouvre un choix de carte depuis une liste en fournissant l'ensemble des données à la configuration.
     */
    fun chooseCardFromList(config: (U, V, X, R) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, Card, D> {
        val next = result.internalChooseWithData(
            config = { u, v, x, r -> config(u, v, x, r)
            },
            target = targetPicker
        ) { logger, request -> logger.chooseCardFromList(request) }
        return ChooseBuilder(parent, next, targetPicker)
    }

    /**
     * Ouvre un choix textuel/boutons en fournissant l'ensemble des données à la configuration.
     */
    fun chooseWhatToDo(config: (U, V, X, R) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, String, D> {
        val next = result.internalChooseWithData(
            config = { u, v, x, r -> config(u, v, x, r) },
            target = targetPicker
        ) { logger, request -> logger.chooseWhatToDo(request) }
        return ChooseBuilder(parent, next, targetPicker)
    }

    fun chooseCardFromSupply(config: (U, V, X, R) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, Card, D> {
        val next = result.internalChooseWithData({u, v, x, r -> config(u, v, x, r)}, targetPicker){logger, request -> logger.chooseCardFromSupply(request) }
        return ChooseBuilder(parent, next, targetPicker)
    }

    /**
     * Enchaîne un choix textuel ou par boutons sans détruire la donnée [R] issue de l'étape précédente.
     */
    fun thenChooseWhatToDo(config: (U, V, X, R) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, R, String> {
        val next = result.internalChooseWithPipe(
            config = { u, v, x, r -> config(u, v, x, r)
            },
            target = targetPicker
        ) { logger, request ->
            logger.chooseWhatToDo(request)
        }

        return ChooseBuilder(parent, next, targetPicker)
    }


    fun thenChooseWhatToDo(target: suspend (U, V, X) -> Logger = targetPicker, config: (U, V, X, R) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, R, String> {
        val next = result.internalChooseWithPipe(
            config = { u, v, x, r -> config(u, v, x, r)
            },
            target = target
        ) { logger, request ->
            logger.chooseWhatToDo(request)
        }

        return ChooseBuilder(parent, next, targetPicker)
    }

    /**
     * Enchaîne un choix de carte depuis une liste en préservant le résultat précédent [R] au sein d'un [ChoiceMade].
     */
    fun thenChooseCardFromList(target: suspend (U, V, X) -> Logger = targetPicker, config: (U, V, X, R) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, R, Card> {
        val next = result.internalChooseWithPipe(
            config = { u, v, x, r -> config(u, v, x, r) },
            target = target
        ) { logger, request ->
            logger.chooseCardFromList(request)
        }

        return ChooseBuilder(parent, next, targetPicker)
    }


    fun thenChooseCardFromList(config: (U, V, X, R) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, R, Card> {
        val next = result.internalChooseWithPipe(
            config = { u, v, x, r -> config(u, v, x, r) },
            target = targetPicker
        ) { logger, request ->
            logger.chooseCardFromList(request)
        }

        return ChooseBuilder(parent, next, targetPicker)
    }

    /**
     * Enchaîne un choix de carte depuis la main en préservant le résultat précédent [R] au sein d'un [ChoiceMade].
     */
    fun thenChooseCardFromHand(config: (U, V, X, R) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, R, Card> {
        val next = result.internalChooseWithPipe(
            config = { u, v, x, r -> config(u, v, x, r) },
            target = targetPicker
        ) { logger, request ->
            logger.chooseCardFromList(request)
        }

        return ChooseBuilder(parent, next, targetPicker)
    }

    fun filterExtraData(predicate: suspend (D) -> Boolean): ChooseBuilder<T, U, V, X, R, D> {
        val next = result.filterPipe { _, _ -> predicate(extraData) }
        return ChooseBuilder(parent, next, targetPicker)
    }

    fun filter(predicate: suspend (R) -> Boolean): ChooseBuilder<T, U, V, X, R, D> {
        val nextResult = result.filterPipe(predicate)
        return ChooseBuilder(parent, nextResult, targetPicker)
    }

    fun filter(predicate: suspend (U, R) -> Boolean): ChooseBuilder<T, U, V, X, R, D> {
        val nextResult = result.filterPipe{u, v -> predicate(u, current)}
        return ChooseBuilder(parent, nextResult, targetPicker)
    }

    fun filterWith(predicate: suspend (U, V, R) -> Boolean): ChooseBuilder<T, U, V, X, R, D> {
        val nextResult = result.filterPipe {u, v -> predicate(u, v, current) }
        return ChooseBuilder(parent, nextResult, targetPicker)
    }

    /**
     * Filtre absolu : évalue une condition croisant l'intégralité du pipeline historique à cet instant.
     */
    fun filterDo(predicate: suspend (U, V, X, R) -> Boolean): ChooseBuilder<T, U, V, X, R, D> {
        val next = result.filterPipe(predicate)
        return ChooseBuilder(parent, next, targetPicker)
    }

    fun filterOrOtherwise(predicate: suspend (U, V, X, R) -> Boolean): ConditionalChooseBuilder<T, U, V, X, R, D> {
        return ConditionalChooseBuilder(parent, result, targetPicker, predicate)
    }
}

// --- Fonctions globales d'extension corrigées sans double-évaluation ---

fun <T : BiEffect<U, V>, U : Logger, V, X : Any, R : Any, D : Any> ChooseBuilder<T, U, V, X, R, D>.branchDecision(
    block: ChooseBranchBuilder<U, V, X, R, D>.() -> Unit
): ChooseBuilder<T, U, V, X, R, D> {
    val choiceBuilder = this.result

    val next = ContextBuilder(choiceBuilder.parent) {
        val choiceCtx = choiceBuilder.function(this)
        val dataR = choiceCtx.data?.current
        val dataX = choiceCtx.data?.source
        val dataD = choiceCtx.data?.extraData

        if (dataR != null && dataX != null && dataD != null) {
            val combinedContext = ChooseDecisionContext(right, left, dataX, dataR, dataD)
            val builder = ChooseBranchBuilder(combinedContext)
            builder.block()

            val matchingCase = builder.cases.firstOrNull { (condition, _) ->
                combinedContext.condition()
            }
            matchingCase?.second?.invoke(dataR, choiceCtx.right, choiceCtx.left, dataX)
        }
        choiceCtx
    }
    return ChooseBuilder(parent, next, targetPicker)
}

fun <T : BiEffect<U, V>, U : Logger, V, X : Any, R : Any, D : Any> ChooseBuilder<T, U, V, X, R, D>.matchAll(
    block: ChooseBranchBuilder<U, V, X, R, D>.() -> Unit
): ChooseBuilder<T, U, V, X, R, D> {
    val choiceBuilder = this.result

    val next = ContextBuilder(choiceBuilder.parent) {
        val choiceCtx = choiceBuilder.function(this)
        val dataR = choiceCtx.data?.current
        val dataX = choiceCtx.data?.source
        val dataD = choiceCtx.data?.extraData


        if (dataX != null && dataR != null && dataD != null) {
            val combinedContext = ChooseDecisionContext(right, left, dataX, dataR, dataD)
            val builder = ChooseBranchBuilder(combinedContext)
            builder.block()

            builder.cases.forEach { (condition, action) ->
                if (combinedContext.condition()) {
                    dataR.action(choiceCtx.right, choiceCtx.left, dataX)
                }
            }
        }
        choiceCtx
    }
    return ChooseBuilder(parent, next, targetPicker)
}

data class GainChoice(val player: Player, val pipeLineCard: Card)

fun <T : BiEffect<U, V>, U : Logger, V, X : Any, D : Any> ChooseBuilder<T, U, V, X, Card, D>.gainFromSupply(
    player : (U, V) -> Player = {u, _ -> u.toPlayer()},
    instruction: GainChoice.() -> String,
    filter: GainChoice.(Card) -> Boolean = { true },
    destination: Destination.PlayerZone = Destination.PlayerZone.Discard,
    canPass : Boolean = false
): ChooseBuilder<T, U, V, X, Card, D> {
    val next = ContextBuilder(result.parent){
        val ctx = result.function(this)
        val dataR = ctx.data?.current
        val dataX = ctx.data?.source

        if (dataX != null && dataR != null) {
            val player = player(right, left)
            val choice = GainChoice(player, dataR)
            player.gainFromSupply(choice.instruction(), {choice.filter(it)}, destination, canPass)
        }

        ctx
    }

    return ChooseBuilder(parent, next, targetPicker)
}
