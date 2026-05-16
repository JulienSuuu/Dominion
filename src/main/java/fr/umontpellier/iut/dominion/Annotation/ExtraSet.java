package fr.umontpellier.iut.dominion.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation définissant si une carte est une carte extra d'un set ( exemple : Farrier de Cornucopia )
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExtraSet {
    String [] value() default {};
}
