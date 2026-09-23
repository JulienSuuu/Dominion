package fr.umontpellier.iut.dominion.Enums.Locations


sealed class Destination(val name: String) {
    object Trash : Destination("TRASH")
    object Supply : Destination("SUPPLY")

    sealed class NocturneZone(name : String) : Destination(name) {
        object Hex : NocturneZone("Hex")
        object Boons : NocturneZone("Boons")
        object Druid : NocturneZone("Druid")
    }

    override fun toString(): String = name.lowercase()

    sealed class PlayerZone( name : String) : Destination(name) {
        object Hand : PlayerZone("HAND")
        object InPlay : PlayerZone("INPLAY")
        object Discard : PlayerZone("DISCARD")
        object Draw : PlayerZone("DRAW")
        object Aside : PlayerZone("ASIDE")

        sealed class NocturneZone(name : String) : PlayerZone(name) {
            object Boons : NocturneZone("BOONS")
            object Hex : NocturneZone("HEX")
        }

        companion object {
            private val baseZones = setOf(Hand, InPlay, Discard, Draw, Aside)


            /**
             * Tente de récupérer la PlayerZone correspondante à partir d'une Destination standard.
             * @return La PlayerZone trouvée, ou `null` si la destination est TRASH, SUPPLY, etc.
             */
            fun valueOf(destination: Destination?): PlayerZone? {
                if (destination == null) return null
                if (destination is PlayerZone) return destination
                return baseZones.find { it.name.equals(destination.name, ignoreCase = true) }
            }
        }
    }


    sealed class OtherZone(name: String) : PlayerZone(name) {
        object Native : OtherZone("NATIVE")
        object Island : OtherZone("Island")
        object Tavern : OtherZone("TAVERN")

    }

    sealed class TempZone(name: String) : PlayerZone(name) {
        object Temp : TempZone("TEMP")
    }

}