package fr.umontpellier.iut.dominion.gui.tables

import fr.umontpellier.iut.dominion.client.StatKey
import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = UnlockType.Free::class, name = "FREE"),
    JsonSubTypes.Type(value = UnlockType.Level::class, name = "LEVEL"),
    JsonSubTypes.Type(value = UnlockType.Purchase::class, name = "PURCHASE"),
    JsonSubTypes.Type(value = UnlockType.Achievement::class, name = "ACHIEVEMENT"),
    JsonSubTypes.Type(value = UnlockType.Stat::class, name = "STAT")
)
sealed class UnlockType {

    object Free : UnlockType()
    object Level : UnlockType()
    object Purchase : UnlockType()

    data class Achievement(val subject: String) : UnlockType()
    data class Stat(val key: StatKey, val detailName : String? = null) : UnlockType()
}

@Entity
@Table(name = "themes")
class Theme(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: UUID? = null,

    @Column(nullable = false, unique = true)
    var code: String = "",

    @Column(nullable = false)
    var name: String = "",

    @Convert(converter = UnlockTypeConverter::class)
    @Column(name = "unlock_type", nullable = false)
    var unlockType: UnlockType = UnlockType.Free,

    @Column(name = "unlock_value", nullable = false)
    var unlockValue: Int = 0
)

@Repository
interface ThemeRepository : JpaRepository<Theme, UUID> {
    fun findByCode(code: String): Theme?
}

@Converter(autoApply = true)
class UnlockTypeConverter : AttributeConverter<UnlockType, String> {

    private val mapper = jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: UnlockType?): String {
        if (attribute == null) return mapper.writeValueAsString(UnlockTypeDTO(type = "FREE"))

        val dto = when (attribute) {
            is UnlockType.Free -> UnlockTypeDTO(type = "FREE")
            is UnlockType.Level -> UnlockTypeDTO(type = "LEVEL")
            is UnlockType.Purchase -> UnlockTypeDTO(type = "PURCHASE")
            is UnlockType.Achievement -> UnlockTypeDTO(type = "ACHIEVEMENT", subject = attribute.subject)
            is UnlockType.Stat -> UnlockTypeDTO(
                type = "STAT",
                key = attribute.key.name,
                detailName = attribute.detailName
            )
        }

        return mapper.writeValueAsString(dto)
    }

    override fun convertToEntityAttribute(dbData: String?): UnlockType {
        if (dbData.isNullOrBlank()) return UnlockType.Free

        return try {
            when (dbData) {
                "FREE" -> return UnlockType.Free
                "LEVEL" -> return UnlockType.Level
                "PURCHASE" -> return UnlockType.Purchase
            }

            val dto: UnlockTypeDTO = mapper.readValue(dbData)
            when (dto.type) {
                "FREE" -> UnlockType.Free
                "LEVEL" -> UnlockType.Level
                "PURCHASE" -> UnlockType.Purchase
                "ACHIEVEMENT" -> UnlockType.Achievement(dto.subject ?: "")
                "STAT" -> {
                    val statKey = dto.key?.let { StatKey.valueOf(it) } ?: return UnlockType.Free
                    UnlockType.Stat(key = statKey, detailName = dto.detailName)
                }
                else -> UnlockType.Free
            }
        } catch (e: Exception) {
            UnlockType.Free
        }
    }

    // Structure JSON intermédiaire
    private data class UnlockTypeDTO(
        val type: String,
        val subject: String? = null,
        val key: String? = null,
        val detailName: String? = null
    )
}