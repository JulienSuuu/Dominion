package fr.umontpellier.iut.dominion.cards.component;

import fr.umontpellier.iut.dominion.Interface.Logger;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.*;
/**
 * Interface fonctionnelle représentant un effet de jeu agissant sur deux entités (U et V).
 * <p>
 * Un {@code BiEffect} est à la fois un {@link BiConsumer} et un {@link CardComponent}.
 * Il permet de construire des chaînes d'actions complexes via une approche fluide.
 * </p>
 *
 * @param <U> Le type du premier argument (souvent l'entité source comme le {@code Player} ou l' {@code Event}).
 * @param <V> Le type du second argument (souvent l'objet du contexte comme la {@code Card}).
 * @param <T> Le type auto-référencé de l'effet, garantissant que les méthodes retournent le bon type de composant.
 * @see ContextBuilder
 * @see CardComponent
 */
public interface BiEffect<U extends Logger, V, T extends BiEffect<U, V, T> & CardComponent> extends BiConsumer<U, V> {
    /**
     * Crée une nouvelle instance du composant à partir d'une fonction de consommation.
     * @param effect La logique brute à encapsuler.
     * @return Une nouvelle instance de {@link T}.
     */
    T create(BiConsumer<U, V> effect);

    /**
     * Retourne l'instance actuelle.
     * @return L'instance {@link T}.
     */
    T self();


    /**
     * Alias pour {@link #then(BiConsumer)}.
     * @param current L'action à exécuter.
     * @return Un nouvel effet composé.
     */
    default T first(@Nonnull BiConsumer<? super U, ? super V> current) {
        return then(current);
    }

    /**
     * Enchaîne une action après l'effet actuel.
     * @param after L'action à exécuter après {@code this.accept(u, v)}.
     * @return Un nouveau composant {@link T} représentant la séquence.
     * @see Objects#requireNonNull(Object)
     */
    default T then(@Nonnull BiConsumer<? super U, ? super V> after) {
        Objects.requireNonNull(after);
        return create((u, v) ->{
            this.accept(u, v);
            after.accept(u, v);
        });
    }

    /**
     * Répète l'effet actuel un nombre fixe de fois.
     * @param times Le nombre de répétitions.
     * @return Un nouvel effet répété.
     */
    default T repeat(int times) {
        return create((U u, V v) -> {
            for (int i = 0; i < times; i++) {
                this.accept(u, v);
            }
        });
    }


    /**
     * Conditionne l'exécution de l'effet actuel par un prédicat.
     * @param check Le test à effectuer avant de déclencher l'effet.
     * @return Un effet qui ne s'exécute que si {@code check} est vrai.
     */
    default T when(BiPredicate<U, V> check){
        return create((U u, V v) -> {
            if(check.test(u, v)){
                this.accept(u, v);
            }
        });
    }

    /**
     * Insère une action avant l'effet actuel.
     * @param before L'action à exécuter avant {@code this.accept(u, v)}.
     * @return Un nouvel effet composé.
     */
    default T compose(@Nonnull BiConsumer<? super U, ? super V> before) {
        return create((U u, V v) -> {
            before.accept(u, v);
            this.accept(u, v);
        });
    }

    /**
     * Répète l'effet tant qu'une condition est remplie ou que le nombre maximum est atteint.
     * @param check La condition de continuation.
     * @param times Le nombre maximum d'itérations.
     * @return Un effet itératif conditionnel.
     */
    default T repeatWhile(@Nonnull BiPredicate<U, V> check, int times){
        return create((U u, V v) -> {
            for(int i = 0; i < times; i++){
                if(!check.test(u, v))break;
                this.accept(u, v);
            }
        });
    }

    /**
     * Bascule vers un {@link ContextBuilder} en extrayant une donnée spécifique.
     * <p>
     * Cette méthode permet d'isoler une donnée (X) pour lui appliquer des transformations
     * via {@link ContextBuilder#map} ou des actions via {@link ContextBuilder#thenWith}.
     * </p>
     * @param <X> Le type de la donnée à extraire.
     * @param extractor La fonction d'extraction à partir de U et V.
     * @return Un nouveau {@link ContextBuilder} transportant la donnée X.
     * @see ContextBuilder
     */
    default <X> ContextBuilder<T, U, V, X> lookingAt(BiFunction<U, V, X> extractor) {
        return new ContextBuilder<>(self(), (u, v) -> {
            X value = extractor.apply(u, v);
            return new Context<>(u, v, value);
        });
    }

    default TargetedBuilder<T, U, V, Boolean> choose(){
        return lookingAt((u, v) -> true).choose();
    }

    default TargetedBuilder<T, U, V, Boolean > choose(BiFunction<U, V, Logger> target){
        return lookingAt((u, v) -> true).choose(target);
    }

    /**
     * Variante de {@link #lookingAt(BiFunction)} extrayant une {@link Pair} de données.
     * @param <X> Le premier type de la paire.
     * @param <Y> Le second type de la paire.
     * @param extractor La fonction d'extraction produisant une paire.
     * @return Un {@link ContextBuilder} transportant une {@link Pair}.
     * @see Pair
     */
    default <X, Y> ContextBuilder<T, U, V, Pair<X, Y>> lookingAtPair(BiFunction<U, V, Pair<X, Y>> extractor) {
        return new ContextBuilder<>(self(), (u, v) -> {
            Pair<X, Y> pair = extractor.apply(u, v);
            return new Context<>(u, v, pair);
        });
    }
}
