package fr.umontpellier.iut.dominion.cards.builders

import fr.umontpellier.iut.dominion.Enums.Locations.Destination
import fr.umontpellier.iut.dominion.Interface.Logger
import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.Player.Player
import fr.umontpellier.iut.dominion.Player.PlayerMessage
import fr.umontpellier.iut.dominion.Player.Skills.draw
import fr.umontpellier.iut.dominion.Player.Skills.reveals
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.Description.GainDescription
import fr.umontpellier.iut.dominion.cards.Description.InstructionDescription
import fr.umontpellier.iut.dominion.cards.component.BiEffect
import fr.umontpellier.iut.dominion.cards.component.ChoiceMade
import fr.umontpellier.iut.dominion.cards.component.InteractionRequest
import fr.umontpellier.iut.dominion.cards.component.LoopCounter
import fr.umontpellier.iut.dominion.cards.component.NonNullChoiceMade
import fr.umontpellier.iut.dominion.cards.component.NonNullPair
import fr.umontpellier.iut.dominion.cards.component.Tuple
import fr.umontpellier.iut.dominion.cards.component.lookingAt
import fr.umontpellier.iut.dominion.cards.component.then
import fr.umontpellier.iut.dominion.cards.gainFromSupply



typealias ContextAction<U, V, X> = suspend Context<U, V, *>.() -> Context<U, V, X?>
typealias LoopAction<U, V, X, C> = suspend BiEffect<U, V>.(X, LoopCounter) -> ContextBuilder<BiEffect<U, V>, U, V, C>
typealias SuspendTriHandler<U, V, X> = suspend (U, V, X) -> Unit
typealias SuspendBiHandler<U, V> = suspend (U, V) -> Unit
typealias SuspendBiFunction<U, V, R> = suspend (U, V) -> R
typealias SuspendTriFunction<U, V, X, R> = suspend (U, V, X) -> R
typealias SuspendBiOperation<U, V, X, R> = suspend X.(U, V) -> R



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
        block : LoopAction<U, V, X, C>
    ) : ContextBuilder<T, U, V, X> {
        return ContextBuilder(parent) {
            val ctx = this.function()
            val data = ctx.data
            if (data != null) {
                val loopCounter = LoopCounter()
                var currentData : Any? = Unit
                val context : ContextBuilder<BiEffect<U, V>, U, V, C> = BiEffect.empty<U, V>().block(data, loopCounter)
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
        return TargetedBuilder(this){u, v, _ -> target(u, v)}
    }

    /** Déclenche une interface à choix multiples avec accès à la donnée courante [X] pour configurer la requête. */
    fun chooseWhatToDo(target : X.(U,V) -> Logger = {u, _->u},config: X.(U, V) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, String, Unit> {
        return TargetedBuilder(this) { u, v, x -> x.target(u, v) }.chooseWhatToDo{u, v, x -> x.config(u, v) }
    }

    /** Déclenche un choix de carte en main avec accès à la donnée courante [X] pour configurer la requête. */
    fun chooseCardFromHand(target : SuspendBiOperation<U, V, X, Logger> = {u, _->u}, config: X.(U, V) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, Card, Unit> {
        return TargetedBuilder(this) { u, v, x -> x.target(u, v) }.chooseCardFromHand{u, v, x -> x.config(u, v) }
    }

    fun chooseCardFromSupply(target: SuspendBiOperation<U, V, X, Logger> = {u, _ -> u}, config: X.(U, V) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, Card, Unit> {
        return TargetedBuilder(this){u, v, x -> x.target(u, v)}.chooseCardFromSupply{u, v, x -> x.config(u, v)}
    }

    /** Déclenche un choix de carte depuis une liste avec accès à la donnée courante [X] étant le receveur pour configurer la requête. */
    fun chooseCardFromList(target : SuspendBiOperation<U, V, X, Logger> = {u, _ ->u}, config: X.(U, V) -> InteractionRequest<X>): ChooseBuilder<T, U, V, X, Card, Unit> {
        return TargetedBuilder(this, {u, v, x-> x.target(u, v)}).chooseCardFromList{u, v, x -> x.config(u, v) }
    }

    /**
     * Compile le pipeline actuel et le convertit en sa forme exécutable finale [BiEffect].
     *
     * @return L'instance fonctionnelle de [BiEffect] prête à être stockée dans les composants de jeu.
     */
    fun end(): BiEffect<U, V> {
        return parent?.then { u, v -> function(Context(u, v, Unit)) } ?: BiEffect.empty<U, V>().then { u, v -> function(Context(u, v, Unit)) }
    }

    /**
     * Envoie un message système ou de chat au joueur réactif [U] sans altérer la donnée du pipeline.
     *
     * @param consumer Lambda générant la chaîne de caractères du message.
     * @param self La carte émettrice du message loggé.
     * @return Le même constructeur de contexte avec l'envoi de message encapsulé.
     */
    fun sendMessage(message : PlayerMessage, self : SuspendTriFunction<U, V, X, Card>): ContextBuilder<T, U, V, X>{
        return ContextBuilder(parent){
            val ctx = function()
            if(ctx.data != null){
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

    fun branch(vararg cases : Pair<X, SuspendBiHandler<U, V>>) : ContextBuilder<T, U, V, X> {
        val casesMap = cases.toMap()
        return ContextBuilder(parent){
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
    infix fun filter(condition: suspend (U, X) -> Boolean) = filter { u, _, x -> condition(u, x)  }

    /**
     * Variante de filtrage exploitant l'intégralité du contexte environnemental ([U], [V])
     * et de la donnée courante [X] pour valider ou rejeter la continuité du flux.
     */
    infix fun filter(condition : suspend (U, V, X) -> Boolean): ContextBuilder<T, U, V, X> {
        return ContextBuilder(parent){
            val ctx = function()
            val data = ctx.data
            if(data!=null && condition(right, left, data)) ctx else Context(right, left, null)
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
        return ContextBuilder(parent){
            var lastCtx: Context<U, V, X?>? = null
            for(i in 0 until times) {
                if(!condition(right, left)) break
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


/**
 * Filtre les éléments d'une collection contenue dans le pipeline.
 * Modifie le type de transport d'un [Iterable] vers une [MutableList].
 *
 * @param E Le type des éléments contenus dans l'itérable.
 * @param predicate Le critère de sélection à appliquer sur chaque élément.
 */
fun <T : BiEffect<U, V>, U : Logger, V, E : Any> ContextBuilder<T, U, V, out Iterable<E>>.filterList(
    predicate: (E) -> Boolean
): ContextBuilder<T, U, V, MutableList<E>> {
    return ContextBuilder(this.parent) {
        val ctx = this.function()
        val list = ctx.data
        if (list != null) {
            val destination = ArrayList<E>()
            list.filterTo(destination, predicate)
            Context(right, left, destination)
        } else {
            Context(right, left, null)
        }
    }
}

/**
 * Filtre les éléments d'une collection et s'assure que la liste résultante n'est pas vide.
 * Si aucun élément ne valide le filtre ou si la source est vide, le contexte du pipeline devient immédiatement nul.
 */
fun <T : BiEffect<U, V>, U : Logger, V, E : Any> ContextBuilder<T, U, V, out Iterable<E>>.filterListAndNotEmpty(
    predicate: (E) -> Boolean = {true}
): ContextBuilder<T, U, V, MutableList<E>> {
    return ContextBuilder(this.parent) {
        val ctx = this.function()
        val list = ctx.data
        if (list != null) {
            val destination = ArrayList<E>()
            list.filterTo(destination, predicate)

            if (destination.isNotEmpty()) {
                Context(right, left, destination)
            } else {
                Context(right, left, null)
            }
        } else {
            Context(right, left, null)
        }
    }
}


fun <T : BiEffect<U, V>, U : Logger, V, E : Any, C : Collection<E>> ContextBuilder<T, U, V, C>.listIsNotEmpty(): ContextBuilder<T, U, V, C> {
    return ContextBuilder(this.parent) {
        val ctx = this.function()
        val list = ctx.data

        if (!list.isNullOrEmpty()) {
            Context(right, left, list)
        } else {
            Context(right, left, null)
        }
    }
}

fun <T : BiEffect<U, V>, U : Logger, V, E : Any, K> ContextBuilder<T, U, V, out Iterable<E>>.distinctListAndNotEmpty(
    selector: (E) -> K
): ContextBuilder<T, U, V, PipelineState<MutableList<E>, List<E>, Unit>> {

    return ContextBuilder(parent) {
        val ctx = this.function()
        val currentCollection = ctx.data
        if (currentCollection != null && currentCollection.any()) {
            val uniqueList = currentCollection.distinctBy(selector)
            Context(ctx.right, ctx.left, PipelineState(currentCollection.toMutableList(), uniqueList, Unit))
        } else {
            Context(ctx.right, ctx.left, null)
        }
    }
}

fun <T : BiEffect<U, V>, U : Logger, V, E : Any, K> ContextBuilder<T, U, V, out Iterable<E>>.distinctList(
    selector: (E) -> K
): ContextBuilder<T, U, V, PipelineState<MutableList<E>, List<E>, Unit>> {
    return ContextBuilder(parent) {
        val ctx = this.function()
        val currentCollection = ctx.data
        if (currentCollection != null) {
            val uniqueList = currentCollection.distinctBy(selector)
            Context(ctx.right, ctx.left, PipelineState(currentCollection.toMutableList(), uniqueList, Unit))
        } else {
            Context(ctx.right, ctx.left, null)
        }
    }
}



/**
 * Transforme chaque élément d'une collection via une fonction de mapping.
 * Convertit le pipeline d'un [Iterable] vers une [MutableList] contenant les éléments transformés.
 */
fun <T : BiEffect<U, V>, U : Logger, V, E : Any, R> ContextBuilder<T, U, V, out Iterable<E>>.mapList(
    transform: (E) -> R
): ContextBuilder<T, U, V, MutableList<R>> {
    return ContextBuilder(this.parent) {
        val ctx = this.function()
        val list = ctx.data
        if (list != null) {
            val destination = ArrayList<R>()
            for (element in list) {
                destination.add(transform(element))
            }
            Context(right, left, destination)
        } else {
            Context(right, left, null)
        }
    }
}

/**
 * Compte combien d'éléments de la liste valident le critère spécifié.
 * Transforme la donnée du pipeline d'un [Iterable] vers un entier ([Int]).
 */
fun <T : BiEffect<U, V>, U : Logger, V, E : Any> ContextBuilder<T, U, V, out Iterable<E>>.countList(
    predicate: (E) -> Boolean
): ContextBuilder<T, U, V, Int> {
    return ContextBuilder(this.parent) {
        val ctx = this.function()
        val list = ctx.data
        if (list != null) {
            val count = list.count (predicate)
            Context(right, left, count)
        } else {
            Context(right, left, 0)
        }
    }
}

/**
 * Extrait le tout premier élément de la collection validant le prédicat (ou le premier élément par défaut).
 * Si aucun élément ne correspond, retourne un contexte contenant une valeur nulle.
 */
fun <T : BiEffect<U, V>, U : Logger, V, E : Any> ContextBuilder<T, U, V, out Iterable<E>>.firstOrNull(
    predicate: (E) -> Boolean = {true}
): ContextBuilder<T, U, V, E?> {
    return ContextBuilder(this.parent) {
        val ctx = this.function()
        val list = ctx.data
        if (list != null) {
            var result: E? = null
            for (element in list) {
                if (predicate(element)) {
                    result = element
                    break
                }
            }
            Context(right, left, result)
        } else {
            Context(right, left, null)
        }
    }
}

/**
 * Applique une boucle d'effets de bord sur chacun des éléments de la collection,
 * puis retourne la liste originale totalement intacte pour la suite du pipeline.
 */
fun <T : BiEffect<U, V>, U : Logger, V, E : Any> ContextBuilder<T, U, V, out Iterable<E>>.flatWith(
    action : SuspendTriHandler<U, V, E>
): ContextBuilder<T, U, V, out Iterable<E>> {
    return ContextBuilder(this.parent) {
        val ctx = this.function()
        val list = ctx.data
        if (list != null) {
            for (element in list) {
                action(right, left, element)
            }
        }
        Context(right, left, list)
    }
}

/**
 * Filtre les données optionnelles et garantit que la donnée [X] extraite n'est pas nulle.
 * Si le critère ou la présence de donnée échoue, le contexte bascule sur une valeur nulle.
 */
fun <T : BiEffect<U, V>, U : Logger, V, X : Any> ContextBuilder<T, U, V, X?>.filterNotNull(predicate :suspend (X) -> Boolean = {true}): ContextBuilder<T, U, V, X> {
    return filterNotNullWithContext { _, _, x -> predicate(x)  }
}

fun <T : BiEffect<U, V>, U : Logger, V, X : Any> ContextBuilder<T, U, V, X?>.filterNotNullWithContext(predicate : suspend (U, V, X) -> Boolean = {_, _, _ ->true}): ContextBuilder<T, U, V, X> {
    return ContextBuilder(parent){
        val ctx = this.function()
        val data = ctx.data

        if(data != null && predicate(right, left, data)){
            Context(right, left, data)
        }else Context(right, left, null)
    }
}


/**
 * Spécifique aux objets composites de type [Tuple] : s'assure que le conteneur et que
 * son contenu sous-jacent ne contiennent aucune valeur nulle avant de poursuivre.
 */

fun <T : BiEffect<U, V>, U : Logger, V, X : Tuple<OUT>, OUT : Any> ContextBuilder<T, U, V, X>.filterTupleNotNull(
): ContextBuilder<T, U, V, OUT> {
    return ContextBuilder(this.parent) {
        val ctx = this.function()
        val tuple = ctx.data

        if (tuple != null && tuple.contentIsNotNull()) {
            Context(ctx.right, ctx.left, tuple.toNotNull())
        } else {
            Context(ctx.right, ctx.left, null)
        }
    }
}



fun <T : BiEffect<U, V>, U : Logger, V, X, Y> ContextBuilder<T, U, V, ChoiceMade<X, Y>>.filterChoiceNotNull(
): ContextBuilder<T, U, V, NonNullChoiceMade<X & Any, Y & Any>> {
    return ContextBuilder(this.parent) {
        val ctx = this.function()
        val choiceMade = ctx.data

        if(choiceMade != null && choiceMade.contentIsNotNull()){
            Context(right, left, choiceMade.toNotNull())
        } else Context(right, left, null)
    }
}

fun <T : BiEffect<U, V>, U : Logger, V, X, Y> ContextBuilder<T, U, V, fr.umontpellier.iut.dominion.cards.component.Pair<X, Y>>.filterPairNotNull(
): ContextBuilder<T, U, V, NonNullPair<X & Any, Y & Any>> {
    return ContextBuilder(this.parent) {
        val ctx = this.function()
        val choiceMade = ctx.data

        if(choiceMade != null && choiceMade.contentIsNotNull()){
            Context(right, left, choiceMade.toNotNull())
        } else Context(right, left, null)
    }
}



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
 * @param benefitLogic La logique d'avantage appliquée à chauqe joueur autre que le joueur actif de la table.
 */
fun <T : BiEffect<U, V>, U: Logger, V,  X> ContextBuilder<T, U, V, X>.benefitOthers(
    target : (U, V, X ) -> Player = {l, v, x -> l.toPlayer()},
    benefitLogic: suspend X.(attacker : Player, opponent : Player) -> Unit
): ContextBuilder<T, U, V, X> {
    return ContextBuilder(this.parent) {
        val ctx = this.function()
        if(ctx.data != null) {
            val p = target(right, left, ctx.data)
            p.game.processBenefit(p) { ally -> ctx.data.benefitLogic(p, ally) }
        }
        ctx
    }
}


fun <T : BiEffect<U, V>, U : Logger, V , X> ContextBuilder<T, U, V, X>.draw(number : Int = 1): ContextBuilder<T, U, V, X> {
    return ContextBuilder(this.parent){
        val ctx = this.function()
        val player = right.toPlayer()

        if(ctx.data != null){
            player.draw(number)
        }
        ctx
    }
}

fun <T : BiEffect<U, V>, U : Logger, V , X> ContextBuilder<T, U, V, X>.increment(item : Item, number : Int = 1): ContextBuilder<T, U, V, X> {
    return ContextBuilder(this.parent){
        val ctx = this.function()
        val player = right.toPlayer()
        if(ctx.data != null)player.increment(item, number)
        ctx
    }
}


fun <T : BiEffect<U, V>, U : Logger, V> ContextBuilder<T, U, V, Card>.revealCard() : ContextBuilder<T, U, V, Card> {
    return ContextBuilder(parent){
        val ctx = this.function()
        if(ctx.data != null) right.toPlayer().reveals(ctx.data)
        ctx
    }
}

fun <T : BiEffect<U, V>, U : Logger, V, X : List<Card>> ContextBuilder<T, U, V, X>.revealList() : ContextBuilder<T, U, V, X> {
    return ContextBuilder(parent){
        val ctx = this.function()
        if(ctx.data != null) {
            right.toPlayer().reveals(ctx.data)
        }
        ctx
    }
}

fun <T : BiEffect<U, V>, U : Logger, V, X : List<Card>> ContextBuilder<T, U, V, X>.partition(
    predicate: (Card) -> Boolean
): ContextBuilder<T, U, V, Pair<List<Card>, List<Card>>> {
    return ContextBuilder(parent) {
        val ctx = this.function()
        val cardList = ctx.data

        if (cardList == null) {
            Context(right, left, null)
        } else {
            val (matching, remaining) = cardList.partition(predicate)
            Context(right, left, Pair(matching, remaining))
        }
    }
}



fun <T : BiEffect<U, V>, U : Logger, V, X : List<Card>> ContextBuilder<T, U, V, X>.partitionWithContext(
    predicate: (U, Card) -> Boolean
): ContextBuilder<T, U, V, Pair<List<Card>, List<Card>>> {
    return ContextBuilder(parent) {
        val ctx = this.function()
        val cardList = ctx.data

        if (cardList == null) {
            Context(right, left, Pair(emptyList(), emptyList()))
        } else {
            val matching = mutableListOf<Card>()
            val remaining = mutableListOf<Card>()

            for (card in cardList) {
                if (predicate(right, card)) matching.add(card) else remaining.add(card)
            }

            Context(right, left, Pair(matching, remaining))
        }
    }
}

fun <T : BiEffect<U, V>, U : Logger, V, X : Any> ContextBuilder<T, U, V, X>.branchDecision(
    block: BranchBuilder<U, V, X>.() -> Unit
): ContextBuilder<T, U, V, X> {
    return ContextBuilder(this.parent) {
        val ctx = this.function()
        val data = ctx.data

        if (data != null) {
            val decisionContext = DecisionContext(right, left, data)
            val builder = BranchBuilder(decisionContext)
            builder.block()

            val matchingCase = builder.cases.firstOrNull { (condition, _) -> decisionContext.condition() }
            matchingCase?.second?.invoke(right, left, data)
        }
        ctx
    }
}

fun <T : BiEffect<U, V>, U : Logger, V, X : Any> ContextBuilder<T, U, V, X>.match(
    block: BranchBuilder<U, V, X>.() -> Unit)
: ContextBuilder<T, U, V, X> {
    return ContextBuilder(parent) {
        val ctx = this.function()
        val data = ctx.data

        if (data != null) {
            val decisionContext = DecisionContext(right, left, data)
            val builder = BranchBuilder(decisionContext)
            builder.block()

            builder.cases.forEach { (condition, action) ->
                if (decisionContext.condition()) {
                    action(right, left, ctx.data)
                }
            }
        }

        ctx
    }
}

inline fun <T : BiEffect<U, V>, U : Logger, V, X> ContextBuilder<T, U, V, X>.forEachCard(
    crossinline elements: (U, X) -> Iterable<Card>,
    crossinline block: suspend ContextBuilder<BiEffect<U, V>, U, V, X>.(X, Card) -> ContextBuilder<BiEffect<U, V>, U, V, X>
): ContextBuilder<BiEffect<U, V>, U, V, X> {
    return ContextBuilder(this.parent) {
        val parentCtx = this.function()
        val currentX = parentCtx.data
        var lastCtx: Context<U, V, X?> = parentCtx

        if (currentX != null) {
            elements(right, currentX).forEach { card ->
                val emptyBuilder : ContextBuilder<BiEffect<U, V>, U, V, X> = BiEffect.empty<U, V>().lookingAt { u, v -> currentX }
                val subBuilder = emptyBuilder.block(currentX, card)
                val res = subBuilder.function(this)
                lastCtx = res
            }
        }

        lastCtx
    }
}

fun <T : BiEffect<U, V>, U : Logger, V, X, R, D> ContextBuilder<T, U, V, PipelineState<X, R, D>>.filterPipe(condition : suspend (R) -> Boolean) : ContextBuilder<T, U, V, PipelineState<X, R, D>> {
    return this.filterPipe { _, _ -> condition(current) }
}

fun <T : BiEffect<U, V>, U : Logger, V, X, D, R> ContextBuilder<T, U, V,PipelineState<X, R, D>>.filterPipe(condition : suspend (U, V, X, R) -> Boolean) : ContextBuilder<T, U, V, PipelineState<X, R, D>> {
   return this.filterPipe { u, v -> condition(u, v, source, current) }
}

fun <T : BiEffect<U, V>, U : Logger, V, X, D, R> ContextBuilder<T, U, V,PipelineState<X, R, D>>.filterPipe(condition : suspend PipelineState<X, R, D>.(U, V) -> Boolean) : ContextBuilder<T, U, V, PipelineState<X, R, D>> {
    return ContextBuilder(parent) {
        val ctx = function()
        val data = ctx.data
        val x = ctx.data?.source
        val r = ctx.data?.current
        val d = ctx.data?.extraData
        if (x != null && r!= null && d!=null && data.condition(right, left)) ctx else Context(right, left, null)
    }
}


fun <T : BiEffect<U, V>, U : Logger, V, X, R, next, D> ContextBuilder<T, U, V, PipelineState<X, R, D>>.internalChooseWithData(
    config: (U, V, X, R) -> InteractionRequest<X>,
    target: suspend (U, V, X) -> Logger,
    playerMethod: suspend (Logger, InteractionRequest<X>) -> next?
) : ContextBuilder<T, U, V, PipelineState<X, next, D>> {
    return ContextBuilder(parent) {
        val ctx = this.function()
        val state = ctx.data

        if (state != null) {
            val req = config(ctx.right, ctx.left, state.source, state.current)
            val logger = target(ctx.right, ctx.left, state.source)
            val nextResult = playerMethod(logger, req)

            if (nextResult != null) {
                Context(ctx.right, ctx.left, PipelineState(state.source, nextResult, state.extraData))
            } else { Context(ctx.right, ctx.left, null) }
        } else { Context(ctx.right, ctx.left, null) }
    }
}

fun <T : BiEffect<U, V>, U : Logger, V, X, R, next, D> ContextBuilder<T, U, V, PipelineState<X, R, D>>.internalChooseWithPipe(
    config: (U, V, X, R) -> InteractionRequest<X>,
    target: suspend (U, V, X) -> Logger,
    playerMethod: suspend (Logger, InteractionRequest<X>) -> next?
) : ContextBuilder<T, U, V, PipelineState<X, R, next>> {
    return ContextBuilder(parent) {
        val ctx = this.function()
        val state = ctx.data

        if (state != null) {
            val req = config(ctx.right, ctx.left, state.source, state.current)
            val logger = target(ctx.right, ctx.left, state.source)
            val nextResult = playerMethod(logger, req)

            if (nextResult != null) {
                Context(ctx.right, ctx.left, PipelineState(state.source, state.current, nextResult))
            } else { Context(ctx.right, ctx.left, null) }
        } else { Context(ctx.right, ctx.left, null) }
    }
}


fun <T : BiEffect<U, V>, U : Logger, V, X, R, next, D> ContextBuilder<T, U, V, PipelineState<X, R, D>>.mapPipe(
    transformation: suspend PipelineState<X, R, D>.(U, V) -> next
): ContextBuilder<T, U, V, PipelineState<X, next, D>> {
    return ContextBuilder<T, U, V, PipelineState<X, next, D>>(parent) {
        val oldCtx = this.function()
        val state = oldCtx.data

        if (state == null) {
            Context(oldCtx.right, oldCtx.left, null)
        } else {
            val newData = state.transformation(oldCtx.right, oldCtx.left)
            Context(oldCtx.right, oldCtx.left, PipelineState(state.source, newData, state.extraData))
        }
    }
}

fun <T : BiEffect<U, V>, U : Logger, V, X, R, D> ContextBuilder<T, U, V, PipelineState<X, R, D>>.filterPipeNotNull(
    predicate: PipelineState<X, R, D>.(U, V) -> Boolean = {_, _ -> true}
): ContextBuilder<T, U, V, PipelineState<X, R, D>> {

    return ContextBuilder(parent) {
        val ctx = this.function()
        val state = ctx.data

        if (state != null && state.predicate(ctx.right, ctx.left)) {
            Context(ctx.right, ctx.left, state)
        } else {
            Context(ctx.right, ctx.left, null)
        }
    }
}


fun <T : BiEffect<U, V>, U : Logger, V, X, R, D> ContextBuilder<T, U, V, PipelineState<X, R, D>>.thenWithPipe(
    nextAction : suspend PipelineState<X, R, D>.(U, V) -> Unit
) : ContextBuilder<T, U, V, PipelineState<X, R, D>>{
    return this.thenDoPipe (nextAction)
}

fun <T : BiEffect<U, V>, U : Logger, V, X, R, D> ContextBuilder<T, U, V, PipelineState<X, R, D>>.thenIfFail(
    nextAction : suspend (U, V) -> Unit
) : ContextBuilder<T, U, V, PipelineState<X, R, D>>{
    return ContextBuilder(parent){
        val ctx = this.function()
        val stat = ctx.data

        if(stat == null){
            nextAction(right, left)
        }

        ctx
    }
}

fun <T : BiEffect<U, V>, U : Logger, V,  X, D> ContextBuilder<T, U, V, PipelineState<X, Card, D>>.gainFromSupplyWithPipe(
    target : PipelineState<X, Card, D>.(U, V) -> Player = {u, _ -> u.toPlayer()},
    pipeLineCard : PipelineState<X, Card, D>.(U, V) -> Card = {_, _-> current},
    instruction : GainChoice.() -> String,
    filter : GainChoice.(Card) -> Boolean = { true },
    destination: Destination.PlayerZone = Destination.PlayerZone.Discard,
    canPass : Boolean = false,
) : ContextBuilder<T, U, V, PipelineState<X, Card, D>> {
    return ContextBuilder(parent){
        val ctx = function()
        val stat = ctx.data

        if(stat != null){
            val player = stat.target(right, left)
            val card = stat.pipeLineCard(right, left)
            val choice = GainChoice(player, card)

            player.gainFromSupply(choice.instruction(), {choice.filter(it)}, destination, canPass)
        }

        ctx
    }
}

fun <T : BiEffect<U, Card>, U : Logger, X> ContextBuilder<T, U, Card, X>.gainFromSupply(
    target : X.(U, Card) -> Player = {u, _ -> u.toPlayer()},
    pipeLineCard : X.(U, Card) -> Card = {_, v-> v},
    instruction : GainChoice.() -> String,
    filter : GainChoice.(Card) -> Boolean = { true },
    destination: Destination.PlayerZone = Destination.PlayerZone.Discard,
    canPass : Boolean = false,
) : ContextBuilder<T, U, Card, X> {
    return ContextBuilder(parent){
        val ctx = function()
        val stat = ctx.data

        if(stat != null){
            val player = stat.target(right, left)
            val card = stat.pipeLineCard(right, left)
            val choice = GainChoice(player, card)

            player.gainFromSupply(choice.instruction(), {choice.filter(it)}, destination, canPass)
        }

        ctx
    }
}


fun <T : BiEffect<U, Card>, U : Logger, X> ContextBuilder<T, U, Card, X>.gainFromSupply(
    target: X.(U, Card) -> Player = { u, _ -> u.toPlayer() },
    pipeLineCard: X.(U, Card) -> Card = { _, v -> v },
    gainChoice: (Card) -> GainDescription? = { it.getDescription<InstructionDescription>()?.getTop<GainDescription>() },
    filter: Card.(Card, Int) -> Boolean = { card, cost -> card.isAtMost(cost) },
): ContextBuilder<T, U, Card, X> {
    return ContextBuilder(parent) {
        val ctx = function()
        val stat = ctx.data

        if (stat != null) {
            val player = stat.target(right, left)
            val referenceCard = stat.pipeLineCard(right, left)

            val gainDesc = gainChoice(left)
            val instruction = gainDesc?.let { "$player, ${it.text}" } ?: ""
            val dest = gainDesc?.destination ?: Destination.PlayerZone.Discard
            val optional = gainDesc?.optional ?: false

            val cardPredicate: (Card) -> Boolean = gainDesc?.applyFilter { cardToGain, maxCost ->
                referenceCard.filter(cardToGain, maxCost)
            } ?: { true }

            player.gainFromSupply(instruction, cardPredicate, dest, optional)
        }

        ctx
    }
}


fun <T : BiEffect<U, V>, U : Logger, V, X, R, D> ContextBuilder<T, U, V, PipelineState<X, R, D>>.thenDoPipe(
    nextAction : suspend PipelineState<X, R, D>.(U, V) -> Unit
) : ContextBuilder<T, U, V, PipelineState<X, R, D>>{
    return ContextBuilder(parent){
        val ctx = function()
        val stat = ctx.data

        if(stat == null) ctx
        else {
            stat.nextAction(right, left)
            Context(right, left, stat)
        }
    }
}











