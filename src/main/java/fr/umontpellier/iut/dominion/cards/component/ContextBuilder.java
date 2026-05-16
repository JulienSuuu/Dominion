package fr.umontpellier.iut.dominion.cards.component;

import fr.umontpellier.iut.dominion.Interface.Logger;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.Optional;
import java.util.function.*;

/**
 * Un constructeur de pipeline fluide pour manipuler des données extraites d'un événement de jeu.
 * <p>
 * Le {@code ContextBuilder} permet d'enchaîner des transformations ({@link #map}) et
 * des actions ({@link #thenDo}) tout en conservant une référence au composant parent.
 * </p>
 *
 * @param <T> Le type du composant de carte, doit hériter de {@link BiEffect} et {@link CardComponent}.
 * @param <U> Le type de l'objet source (ex: {@code Player}).
 * @param <V> Le type de l'objet déclencheur (ex: {@code Card}).
 * @param <X> Le type de la donnée actuellement transportée dans le pipeline.
 * @see Context
 * @see BiEffect
 */
public class ContextBuilder<T extends BiEffect<U, V, T> & CardComponent, U extends Logger, V, X> {

    /** Le composant de la carte vers lequel le pipeline sera compilé via {@link #end()}. */
    private final T parent;

    /** La fonction interne représentant la chaîne de traitement actuelle. */
    private final BiFunction<U, V, Context<U, V, X>> function;

    public ContextBuilder(T parent, BiFunction<U, V, Context<U, V, X>> function) {
        this.function = function;
        this.parent = parent;
    }

    /**
     * Point d'entrée principal pour exécuter une action personnalisée.
     * <p>Cette méthode vérifie automatiquement que le contexte et la donnée {@code X} ne sont pas nulls.</p>
     *
     * @param consumer La logique à exécuter utilisant {@code U, V, X}.
     * @return Un nouveau {@link ContextBuilder} pour continuer le chaînage.
     */
    public ContextBuilder<T, U, V, X> thenDo(TriConsumer<U, V, X> consumer) {
        BiFunction<U, V, Context<U, V, X>> pipe = (u, v) -> {
            Context<U, V, X> ctx = function.apply(u, v);
            if(ctx != null && ctx.data() != null){
                consumer.accept(u, v, ctx.data());
            }
            return ctx;
        };

        return new ContextBuilder<>(parent, pipe);
    }


    protected <R> ContextBuilder<T, U, V, R> internalChoose(BiFunction<U, V, InteractionRequest<X>> config, BiFunction<U, V ,Logger> target, BiFunction<Logger, InteractionRequest<X>, Optional<R>> playerMethod) {
        return new ContextBuilder<>(parent, (event, owner) -> {
            Context<U, V, X> ctx = function.apply(event, owner);

            if(ctx.data() == null){
                return new Context<>(event, owner, null);
            }

            InteractionRequest<X> req = config.apply(event, owner);

            Logger logger = target.apply(event, owner);

            R result = playerMethod.apply(logger, req).orElse(null);

            return new Context<>(event, owner, result);
        });
    }


    public TargetedBuilder<T, U, V, X>  choose(){
        return new TargetedBuilder<>(this, (u, v) -> u );
    }

    public TargetedBuilder<T, U, V, X> choose(BiFunction<U, V, Logger> target){
        return new TargetedBuilder<>(this, target);
    }

    /**
     * Compile le pipeline et retourne au composant parent.
     * @return Une instance de {@link T} contenant toute la logique définie.
     * @see BiEffect#create(BiConsumer)
     */
    public T end() {
        return parent.create(function::apply);
    }

    /**
     * Exécute une action combinant le déclencheur {@link U} et la donnée {@link X}.
     * @param consumer La logique à exécuter
     * @return Ce builder pour continuer la chaîne.
     */
    public ContextBuilder<T, U, V, X> thenDo(BiConsumer<X, V> consumer) {
        return thenDo((u, v, x) -> consumer.accept(x, v));
    }

    /**
     * Exécute une action combinant la source {@code U} et la donnée {@code X}.
     * @param consumer La logique à exécuter (ex: un joueur qui agit sur une carte piochée).
     * @return Ce builder pour continuer la chaîne.
     */
    public ContextBuilder<T, U, V, X> thenWith(BiConsumer<U, X> consumer) {
        return thenDo((u, v, x) -> consumer.accept(u, x));
    }

    /**
     * Exécute un effet de bord sur l'événement d'origine sans utiliser la donnée {@code X}.
     * @param consumer Action sur {@code U} et {@code V}.
     * @return Ce builder.
     */
    public ContextBuilder<T, U, V, X> also(BiConsumer<U, V> consumer) {
        return  thenDo((u, v, x) -> consumer.accept(u, v));
    }

    /**
     * @see #thenDo(TriConsumer)
     */
    public ContextBuilder<T, U, V, X> thenDo(Consumer<X> consumer) {
        return thenDo((x, v) -> consumer.accept(x));
    }

    public ContextBuilder<T, U, V, X> otherwise(BiConsumer<U, V> alternative) {
        return new ContextBuilder<>(parent, (u, v) -> {
            Context<U, V, X> ctx = function.apply(u, v);
            if (ctx == null || ctx.data() == null) {
                alternative.accept(u, v);
            }
            return ctx;
        });
    }

    /**
     * Transforme la donnée {@code X} en une donnée {@code R} via une fonction.
     * <p>Si le contexte précédent est vide, la transformation est ignorée.</p>
     *
     * @param <R> Le nouveau type de donnée.
     * @param transformation Fonction de mapping utilisant la source et la donnée actuelle.
     * @return Un {@link ContextBuilder} transportant le nouveau type {@code R}.
     */
    public <R> ContextBuilder<T, U, V, R> map(BiFunction<U, X, R> transformation) {
        return new ContextBuilder<>(parent, (u, v) -> {
            Context<U, V, X> oldCtx = function.apply(u, v);
            if (oldCtx == null || oldCtx.data() == null) {
                return new Context<>(u, v, null);
            }
            R newData = transformation.apply(u, oldCtx.data());
            return new Context<>(u, v, newData);
        });
    }

    public ContextBuilder<T, U, V, X> filter(Predicate<X> condition) {
        return new ContextBuilder<>(parent, (u, v) -> {
            Context<U, V, X> ctx = function.apply(u, v);
            if (ctx != null && ctx.data() != null && condition.test(ctx.data())) {
                return ctx;
            }
            return new Context<>(u, v, null);
        });
    }

    /**
     * Répète l'exécution de toute la chaîne de fonctions définie jusqu'ici.
     *  @param times Nombre de répétitions.
     * @return Un builder répétant l'application de {@link #function}.
     */
    public ContextBuilder<T, U, V, X> repeat(int times) {
        return new ContextBuilder<>(parent, (u, v) -> {
            Context<U, V, X> lastCtx = null;
            for (int i = 0; i < times; i++) {
                lastCtx = function.apply(u, v);
            }
            return lastCtx;
        });
    }

    public ContextBuilder<T, U, V, X> log(String message) {
        return thenWith((u, x) -> u.log(message));
    }

}
