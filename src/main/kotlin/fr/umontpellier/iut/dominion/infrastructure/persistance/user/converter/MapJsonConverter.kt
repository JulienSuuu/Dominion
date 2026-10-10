package fr.umontpellier.iut.dominion.infrastructure.persistance.user.converter

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.util.concurrent.ConcurrentHashMap

@Converter
class MapJsonConverter : AttributeConverter<MutableMap<String, Int>, String> {

    private val objectMapper = ObjectMapper()

    override fun convertToDatabaseColumn(attribute: MutableMap<String, Int>?): String {
        if (attribute.isNullOrEmpty()) return "{}"
        return objectMapper.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): MutableMap<String, Int> {
        if (dbData.isNullOrBlank()) return ConcurrentHashMap()
        val typeRef = object : TypeReference<ConcurrentHashMap<String, Int>>() {}
        return objectMapper.readValue(dbData, typeRef)
    }
}