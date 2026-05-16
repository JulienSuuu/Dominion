package fr.umontpellier.iut.dominion.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation sur les méthode du Player qui nécessite l'ouverture d'un overlay de sélection
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Selection_Mode {
}
