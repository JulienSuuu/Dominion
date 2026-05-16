package fr.umontpellier.iut.dominion.Annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * Annotation définissant l'extension, et le type de pile de la carte
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Dominion_Card {
    String extension() default "Base";
    PileType pileType() default PileType.KINGDOM;

}
