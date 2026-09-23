package fr.umontpellier.iut.dominion.gui.game


import fr.umontpellier.iut.dominion.client.StatKey
import fr.umontpellier.iut.dominion.gui.authentification.StatValue
import fr.umontpellier.iut.dominion.service.ClientManager
import org.apache.tomcat.util.net.openssl.ciphers.Authentication
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/stats")
class StatsProviderService(
    val clientManager: ClientManager
) {

    @GetMapping("/{clientId}")
    fun getStatsFromClient(@PathVariable clientId: String): ResponseEntity<Any> {
        val client = clientManager.getClient(clientId)
            ?: return ResponseEntity.status(404).body(mapOf("error" to "Client non trouvé"))

        val dtoList: List<StatDTO> = client.stats.map { (key, statValue) ->
            StatDTO(
                key = key.name,
                displayName = key.displayName,
                category = key.category,
                type = key.type,
                value = statValue.value,
                details = statValue.hoverDetailsMap
            )
        }

        return ResponseEntity.ok(dtoList)
    }

    @GetMapping("/me")
    fun getMyStats(authentication: Authentication): ResponseEntity<Any> {
        return getStatsFromClient(authentication.name)
    }
}


data class StatDTO(
    val key: String,
    val displayName: String,
    val category: String,
    val type: String,
    val value: Int,
    val details: Map<String, Int>
)