package fr.umontpellier.iut.dominion.domain.model.card

class FaceDown(var value: Boolean) {
    operator fun invoke(): Boolean = value
    operator fun not(): Boolean = !value
    fun hide() {
        value = true
    }
    fun reveal() {
        value = false
    }
    fun toggle() {
        value = !value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceDown) return false
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = if (value) "Face Down (Hidden)" else "Face Up (Visible)"
    fun toJson() : String = """
        "faceDown": $value
    """.trimIndent()
}