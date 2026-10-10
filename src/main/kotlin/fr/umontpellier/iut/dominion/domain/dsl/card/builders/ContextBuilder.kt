package fr.umontpellier.iut.dominion.domain.dsl.card.builders

import fr.umontpellier.iut.dominion.domain.model.ui.Button
import fr.umontpellier.iut.dominion.domain.model.location.Destination
import fr.umontpellier.iut.dominion.domain.common.Logger
import fr.umontpellier.iut.dominion.domain.model.player.Player
import fr.umontpellier.iut.dominion.domain.model.player.PlayerMessage
import fr.umontpellier.iut.dominion.domain.model.player.skills.moveTo
import fr.umontpellier.iut.dominion.domain.model.card.Card
import fr.umontpellier.iut.dominion.domain.dsl.card.core.ListContainer
import fr.umontpellier.iut.dominion.domain.components.card.effect.BiEffect
import fr.umontpellier.iut.dominion.domain.dsl.card.core.InteractionRequest
import fr.umontpellier.iut.dominion.domain.components.card.effect.then
import fr.umontpellier.iut.dominion.domain.dsl.card.core.Context
import fr.umontpellier.iut.dominion.domain.dsl.card.core.ContextAction
import fr.umontpellier.iut.dominion.domain.dsl.card.core.LoopAction
import fr.umontpellier.iut.dominion.domain.dsl.card.core.LoopCounter
import fr.umontpellier.iut.dominion.domain.dsl.card.core.SuspendBiFunction
import fr.umontpellier.iut.dominion.domain.dsl.card.core.SuspendBiHandler
import fr.umontpellier.iut.dominion.domain.dsl.card.core.SuspendBiOperation
import fr.umontpellier.iut.dominion.domain.dsl.card.core.SuspendTriFunction
import fr.umontpellier.iut.dominion.domain.dsl.card.core.SuspendTriHandler
import fr.umontpellier.iut.dominion.domain.dsl.card.extensions.filterNotNull
import kotlin.collections.get


/**
 * Un constructeur de pipeline réactif et fluide permettant de transformer et manipuler
 * des données extraites à partir d'un événement de jeu.
 *
 * Le pipeline propage de manière paresseuse (*lazy*) un état encapsulé dans un [Context].
 * Si à une étape donnée la donnée [X] devient nulle (à cause d'un filtre ou d'une absence de choix),
 * la majorité des étapes suivantes sauteront leur exécution pour éviter les erreurs de type pointeur nul.
 *
 * @param T Le type du composant de carte réactif racine, devant hériter de [BiEffect].
 * @param U Le type de l'objet source initiateur (souvent un [Logger] ou un [Player]).
 * @param V Le type de l'objet déclencheur contextuel (souvent une [Card]).
 * @param X Le type de la donnée actuellement transportée et transformée dans le pipeline.
 * @property parent Le composant de carte parent vers lequel le pipeline sera compilé via [end].
 * @property function La fonction interne modélisant l'intégralité de la chaîne de traitement actuelle.
 */
class ContextBuilder<T : BiEffect<U, V>, U : Logger, V, X>(
    val parent: T?,
    val function: ContextAction<U, V, X>
) : ListContainer<X> {

    /**
     * Point d'entrée pour exécuter une action ou un effet de bord personnalisé.
     *
     * Cette méthode vérifie automatiquement si la donnée [X] est présente. Si elle est nulle,
     * l'[consumer] est ignoré et le contexte est propagé tel quel.
     *
     * @param consumer La lambda à exécuter, recevant le contexte de jeu ([U], [V]) et la donnée [X].
     * @return Un nouveau [ContextBuilder] incluant cet effet de bord.
     */
    infix fun thenDo(consumer: SuspendTriHandler<U, V, X>): ContextBuilder<T, U, V, X> {
        val pipe: ContextAction<U, V, X> = {
            val ctx = this.function()
            val data = ctx.data
            if (data != null) {
                consumer(ctx.right, ctx.left, data)
            }
            ctx
        }
        return ContextBuilder(parent, pipe)
    }

    fun <C> loop(
        number: Int,
        block: LoopAction<U, V, X, C>
    ): ContextBuilder<T, U, V, X> {
        return ContextBuilder(parent) {
            val ctx = this.function()
            val data = ctx.data
            if (data != null) {
                val loopCounter = LoopCounter()
                var currentData: Any? = Unit
                val context: ContextBuilder<BiEffect<U, V>, U, V, C> = BiEffect.empty<U, V>().block(data, loopCounter)
                for (iteration in 0 until number) {
                    val newCtx = context.function(Context(right, left, currentData))
                    if (newCtx.data == null) break
                    currentData = newCtx.data
                    loopCounter.current++
                }
            }

            ctx
        }
    }


    /**
     * Initie une phase de choix ciblant par défaut le joueur actif ([U]).
     *
     * @return Un [TargetedBuilder] configuré pour le joueur initiateur.
     */
    fun choose(): TargetedBuilder<T, U, V, X> {
        return TargetedBuilder(this) { u, _, _ -> u }
    }

    /**
     * Initie une phase de choix en ciblant explicitement un joueur défini par la fonction [target].
     *
     * @param target La fonction désignant le joueur devant effectuer le choix.
     * @return Un [TargetedBuilder] configuré pour la cible désignée.
     */
    fun choose(target: SuspendBiFunction<U, V, Logger>): TargetedBuilder<T, U, V, X> {
        return TargetedBuilder(this) { u, v, _ -> target(u, v) }
    }

    /** Déclenche une interface à choix multiples avec accès à la donnée courante [X] pour configurer la requête. */
    fun chooseWhatToDo(
        target: X.(U, V) -> Logger = { u, _ -> u },
        config: X.(U, V) -> InteractionRequest<X>
    ): ChooseBuilder<T, U, V, X, String, Unit> {
        return TargetedBuilder(this) { u, v, x -> x.target(u, v) }.chooseWhatToDo { u, v, x -> x.config(u, v) }
    }

    /** Déclenche un choix de carte en main avec accès à la donnée courante [X] pour configurer la requête. */
    fun chooseCardFromHand(
        target: SuspendBiOperation<U, V, X, Logger> = { u, _ -> u },
        config: X.(U, V) -> InteractionRequest<X>
    ): ChooseBuilder<T, U, V, X, Card, Unit> {
        return TargetedBuilder(this) { u, v, x -> x.target(u, v) }.chooseCardFromHand { u, v, x -> x.config(u, v) }
    }

    fun chooseCardFromSupply(
        target: SuspendBiOperation<U, V, X, Logger> = { u, _ -> u },
        config: X.(U, V) -> InteractionRequest<X>
    ): ChooseBuilder<T, U, V, X, Card, Unit> {
        return TargetedBuilder(this) { u, v, x -> x.target(u, v) }.chooseCardFromSupply { u, v, x -> x.config(u, v) }
    }

    /** Déclenche un choix de carte depuis une liste avec accès à la donnée courante [X] étant le receveur pour configurer la requête. */
    fun chooseCardFromList(
        target: SuspendBiOperation<U, V, X, Logger> = { u, _ -> u },
        config: X.(U, V) -> InteractionRequest<X>
    ): ChooseBuilder<T, U, V, X, Card, Unit> {
        return TargetedBuilder(this, { u, v, x -> x.target(u, v) }).chooseCardFromList { u, v, x -> x.config(u, v) }
    }

    /**
     * Compile le pipeline actuel et le convertit en sa forme exécutable finale [BiEffect].
     *
     * @return L'instance fonctionnelle de [BiEffect] prête à être stockée dans les composants de jeu.
     */
    fun end(): BiEffect<U, V> {
        return parent?.then { u, v -> function(Context(u, v, Unit)) } ?: BiEffect.empty<U, V>().then { u, v ->
            function(
                Context(u, v, Unit)
            )
        }
    }

    /**
     * Envoie un message système ou de chat au joueur réactif [U] sans altérer la donnée du pipeline.
     *
     * @param consumer Lambda générant la chaîne de caractères du message.
     * @param self La carte émettrice du message loggé.
     * @return Le même constructeur de contexte avec l'envoi de message encapsulé.
     */
    fun sendMessage(message: PlayerMessage, self: SuspendTriFunction<U, V, X, Card>): ContextBuilder<T, U, V, X> {
        return ContextBuilder(parent) {
            val ctx = function()
            if (ctx.data != null) {
                right.sendMessage(message, self(right, left, ctx.data))
            }
            Context(ctx.right, ctx.left, ctx.data)
        }
    }

    /** Exécute une action combinant le déclencheur [V] et la donnée [X]. */
    infix fun thenDo(consumer: SuspendBiHandler<X, V>): ContextBuilder<T, U, V, X> {
        return thenDo { _, v, x -> consumer(x, v) }
    }

    /** Exécute une action combinant le joueur/source [U] et la donnée [X]. */
    infix fun thenWith(consumer: SuspendBiHandler<U, X>): ContextBuilder<T, U, V, X> {
        return thenDo { u, _, x -> consumer(u, x) }
    }

    /** Exécute un effet de bord environnemental pur sur le couple [U] et [V], sans utiliser la donnée [X]. */
    infix fun so(consumer: SuspendBiHandler<U, V>): ContextBuilder<T, U, V, X> {
        return thenDo { u, v, _ -> consumer(u, v) }
    }

    /** Exécute un effet de bord pur concentré uniquement sur le joueur ou la source [U]. */
    infix fun so(consumer: suspend (U) -> Unit): ContextBuilder<T, U, V, X> {
        return thenDo { u, _, _ -> consumer(u) }
    }

    /** Exécute une action centrée exclusivement sur la donnée [X]. */
    infix fun thenDo(consumer: suspend (X) -> Unit): ContextBuilder<T, U, V, X> {
        return thenDo { _, _, x -> consumer(x) }
    }

    fun branch(vararg cases: Pair<X, SuspendBiHandler<U, V>>): ContextBuilder<T, U, V, X> {
        val casesMap = cases.toMap()
        return ContextBuilder(parent) {
            val ctx = function()
            val choice = ctx.data

            val associative = casesMap[choice]
            associative?.invoke(ctx.right, ctx.left)

            ctx
        }
    }

    /**
     * Spécifie une action alternative ou un bloc de repli qui s'exécutera uniquement
     * si la donnée transitant dans le pipeline est évaluée à `null`.
     *
     * @param alternative Effet de bord à exécuter en cas d'absence de donnée.
     * @return Le constructeur de contexte d'origine pour poursuivre la chaîne.
     */
    infix fun otherwise(alternative: SuspendBiHandler<U, V>): ContextBuilder<T, U, V, X> {
        return ContextBuilder(parent) {
            val ctx = function()
            if (ctx.data == null) {
                alternative(ctx.right, ctx.left)
            }
            ctx
        }
    }

    /**
     * Transforme la donnée courante [X] en une nouvelle donnée [R] via une fonction de mapping.
     * Si la donnée source est nulle, le mapping est ignoré et propage directement un contexte vide.
     *
     * @param R Le nouveau type de la donnée après transformation.
     * @param transformation Fonction de conversion prenant le joueur et la donnée [X].
     * @return Un nouveau [ContextBuilder] transportant le type transformé [R].
     */
    infix fun <R> map(transformation: SuspendBiFunction<U, X, R?>): ContextBuilder<T, U, V, R> {
        return ContextBuilder(parent) {
            val oldCtx = function()
            val oldData = oldCtx.data
            if (oldData == null) {
                Context(oldCtx.right, oldCtx.left, null)
            } else {
                val newData = transformation(oldCtx.right, oldData)
                Context(oldCtx.right, left, newData)
            }
        }
    }

    /**
     * Variante complète de transformation permettant d'exploiter à la fois la source [U],
     * le déclencheur [V] et la donnée actuelle [X] pour produire le résultat [R].
     */
    fun <R> map(transform: SuspendTriFunction<U, V, X, R>): ContextBuilder<T, U, V, R> {
        return ContextBuilder(this.parent) {
            val ctx = this.function()
            if (ctx.data != null) {
                val newData = transform(ctx.right, ctx.left, ctx.data)
                Context(right, left, newData)
            } else {
                Context(right, left, null)
            }
        }
    }

    /** Filtre la donnée du pipeline selon un prédicat simple. Si la condition échoue, la donnée devient nulle. */
    infix fun filter(condition: suspend (X) -> Boolean) = filter { _, _, x -> condition(x) }

    /** Filtre la donnée du pipeline en exploitant l'entité source [U]. */
    infix fun filter(condition: suspend (U, X) -> Boolean) = filter { u, _, x -> condition(u, x) }

    /**
     * Variante de filtrage exploitant l'intégralité du contexte environnemental ([U], [V])
     * et de la donnée courante [X] pour valider ou rejeter la continuité du flux.
     */
    infix fun filter(condition: suspend (U, V, X) -> Boolean): ContextBuilder<T, U, V, X> {
        return ContextBuilder(parent) {
            val ctx = function()
            val data = ctx.data
            if (data != null && condition(right, left, data)) ctx else Context(right, left, null)
        }
    }

    /**
     * Répète l'exécution séquentielle de toute la chaîne d'actions et de fonctions définie jusqu'ici.
     *
     * @param times Le nombre de répétitions à effectuer.
     * @return Le constructeur de contexte contenant le résultat de la dernière itération.
     */
    infix fun repeat(times: Int): ContextBuilder<T, U, V, X> {
        return ContextBuilder(parent) {
            var lastCtx: Context<U, V, X?>? = null
            repeat(times) {
                lastCtx = function()
            }
            lastCtx ?: Context(right, left, null)
        }
    }

    fun repeatContext(times: Int, block: suspend () -> ContextBuilder<T, U, V, X>): ContextBuilder<T, U, V, X> {
        return ContextBuilder(this.parent) {
            var lastCtx: Context<U, V, X?>? = null

            repeat(times) {
                val subBuilder = block()
                val res = subBuilder.function(this)
                lastCtx = res
            }

            lastCtx ?: Context(right, left, null)
        }
    }

    /**
     * Répète la chaîne d'effets tant qu'une condition dynamique est remplie,
     * avec une sécurité de limite de boucles fixée par [times].
     */
    fun repeatWhile(condition: suspend (U, V) -> Boolean, times: Int): ContextBuilder<T, U, V, X> {
        return ContextBuilder(parent) {
            var lastCtx: Context<U, V, X?>? = null
            for (i in 0 until times) {
                if (!condition(right, left)) break
                lastCtx = function()
            }
            lastCtx ?: Context(right, left, null)
        }
    }

    /**
     * Enregistre une entrée de log via le joueur source [U] à partir de l'état de la donnée [X].
     *
     * @param message Lambda générant le texte de log. Si le texte renvoyé est nul, aucun log n'est écrit.
     * @return Le constructeur de contexte d'origine.
     */
    infix fun log(message: suspend (U, X) -> String?): ContextBuilder<T, U, V, X> {
        return thenWith { u, x ->
            val msg = message(u, x)
            if (msg != null) u.log(msg)
        }
    }
}

fun <U : Logger, V, T : BiEffect<U, V>> T.filter(predicate: suspend (U, V) -> Boolean): ContextBuilder<T, U, V, Boolean> {
    return  this.lookingAt { u, v -> true }.filter { u, v, _ -> predicate(u, v) }
}

/**
 * Bascule vers un ContextBuilder en extrayant une donnée spécifique.
 */
infix fun <U : Logger, V, T, X> T.lookingAt(extractor: suspend (U, V) -> X): ContextBuilder<T, U, V, X> where T : BiEffect<U, V>{
    return ContextBuilder(this) {
        val value = extractor(right, left)
        Context(right, left, value)
    }
}

fun <U : Logger, V, T : BiEffect<U, V>, R> T.map(
    transformation: (U, V) -> R
): ContextBuilder<T, U, V, R> {
    return this.lookingAt { u, v -> transformation(u, v) }
}

fun BiEffect<Player, Card>.spyEffect(
    target: (Player, Card) -> Player
): ContextBuilder<BiEffect<Player, Card>, Player, Card, String> {
    return this
        .lookingAt { player, card -> target(player, card).getCardFromDeck() }
        .filterNotNull()
        .chooseWhatToDo { _, _ ->
            InteractionRequest(
                instruction = "Do you want to discard ?",
                cards = listOf(this),
                buttons = Button.yesOrNo,
                canPass = true
            )
        }.filter { "y" == it }
        .thenDo { player, card, chosen ->
            target(player, card).moveTo(chosen, Destination.PlayerZone.Discard)

        }
}

infix fun <U : Logger, V, T : BiEffect<U, V>, X, Y> T.lookingAtPair(extractor: suspend (U, V) -> fr.umontpellier.iut.dominion.domain.dsl.card.core.Pair<X, Y>): ContextBuilder<T, U, V, fr.umontpellier.iut.dominion.domain.dsl.card.core.Pair<X, Y>> {
    return ContextBuilder(this) {
        val pair = extractor(right, left)
        Context(right, left, pair)
    }
}
