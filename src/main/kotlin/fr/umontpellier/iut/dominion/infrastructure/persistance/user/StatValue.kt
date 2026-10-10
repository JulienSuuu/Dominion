package fr.umontpellier.iut.dominion.infrastructure.persistance.user

import fr.umontpellier.iut.dominion.infrastructure.persistance.user.converter.MapJsonConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Embeddable
import java.util.concurrent.ConcurrentHashMap

@Embeddable
class StatValue(
    @Column(name = "stat_value")
    var value: Int = 0,

    @Convert(converter = MapJsonConverter::class)
    @Column(name = "hover_details", columnDefinition = "TEXT")
    var hoverDetailsMap: MutableMap<String, Int> = ConcurrentHashMap()
)