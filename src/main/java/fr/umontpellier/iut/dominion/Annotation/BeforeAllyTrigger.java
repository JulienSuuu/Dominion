package fr.umontpellier.iut.dominion.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * Annotation regardant les méthodes du joueur qui nécessite une activation d'un trigger d'allié avant son exécution
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BeforeAllyTrigger {
}
