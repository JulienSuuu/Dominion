package fr.umontpellier.iut.dominion.Annotation


/**
 * Annotation définissant l'extension, et le type de pile de la carte
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class Dominion_Card(
    val extension: String = "Base",
    val pileType: PileType = PileType.KINGDOM,
    val cardsNumber: Int = 0
)
