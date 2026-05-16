package fr.umontpellier.iut.dominion.cards.component;

import java.util.Objects;
import java.util.function.Consumer;

public interface Effect<T, U extends Effect<T, U> & CardComponent> extends Consumer<T> {
    U create(Consumer<T> c);

    default U then(Consumer<? super T> effect) {
        Objects.requireNonNull(effect);
        return create(t -> {
                this.accept(t);
                effect.accept(t);}
        );
    }

    default U repeat(int times){
        return create(t -> {
            for(int i = 0; i < times; i++){
                this.accept(t);
            }
        });
    }

    default U compose(Consumer<? super T> before) {
        Objects.requireNonNull(before);
        return create(t -> {
            before.accept(t);
            this.accept(t);
        });
    }


}
