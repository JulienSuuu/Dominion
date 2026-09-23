package fr.umontpellier.iut.dominion.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class UtilsService {
    companion object {
        private val mapper = jacksonObjectMapper()

        fun parseJson(message: String): JsonNode? {
            return try {
                mapper.readTree(message)
            } catch (e: Exception) {
                null
            }
        }

        fun JsonNode.toArray(fieldName: String): ArrayNode? = get(fieldName) as? ArrayNode
        fun JsonNode.toNameField(fieldName: String): String? = get(fieldName)?.asText()?.takeIf { it.isNotBlank() }

    }
}