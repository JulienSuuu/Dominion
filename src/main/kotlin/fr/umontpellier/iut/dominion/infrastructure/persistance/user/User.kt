package fr.umontpellier.iut.dominion.infrastructure.persistance.user

import fr.umontpellier.iut.dominion.infrastructure.persistance.theme.Theme
import fr.umontpellier.iut.dominion.infrastructure.network.websocket.client.StatKey
import jakarta.persistence.CollectionTable
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapKeyColumn
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class User(
    @Id
    var id: String = "",
    var email: String = "",
    var pseudo: String = "",
    var passwordHash: String = "",

    var level : Int = 0,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "current_theme_id")
    var currentTheme: Theme? = null,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_themes",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "theme_id")]
    )
    var unlockedThemes: MutableSet<Theme> = mutableSetOf(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_stats",
        joinColumns = [JoinColumn(name = "user_id")]
    )
    @MapKeyColumn(name = "stat_name")
    @Enumerated(EnumType.STRING)
    var stats: MutableMap<StatKey, StatValue> = mutableMapOf()
){
    fun getStatValue(key: StatKey, detailName: String? = null): Int {
        val stat = stats[key] ?: return 0
        return if (detailName != null) {
            stat.hoverDetailsMap[detailName] ?: 0
        } else {
            stat.value
        }
    }

    fun hasUnlockedTheme(theme: Theme): Boolean {
        return unlockedThemes.any { it.id == theme.id }
    }
}