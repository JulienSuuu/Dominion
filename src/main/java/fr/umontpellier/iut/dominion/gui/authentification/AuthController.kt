package fr.umontpellier.iut.dominion.gui.authentification

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import fr.umontpellier.iut.dominion.client.StatKey
import fr.umontpellier.iut.dominion.gui.tables.Theme
import fr.umontpellier.iut.dominion.service.ClientManager
import jakarta.persistence.AttributeConverter
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Converter
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.Lob
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapKeyColumn
import jakarta.persistence.PostLoad
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.*
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val jwtService: JwtService,
    private val clientManager: ClientManager,
    @Value("\${server.ssl.enabled:false}") private val isSslEnabled: Boolean
) {

    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
        response: HttpServletResponse, errors: Errors
    ): ResponseEntity<Any> {

        val user = authService.authenticate(request.email, request.password)
            ?: return ResponseEntity.status(401).body(ErrorResponse("Invalid Id"))

        val jwtToken = jwtService.generateToken(user.id, user.email, user.pseudo)

        val cookie = ResponseCookie.from("jwt", jwtToken)
            .httpOnly(true)
            .secure(isSslEnabled)
            .path("/")
            .maxAge(7 * 24 * 3600)
            .sameSite(if (isSslEnabled) "Strict" else "Lax")
            .build()

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(SessionResponse("Successful Connexion", user.id, user.pseudo, false, theme = user.currentTheme))
    }

    @PostMapping("/register")
    fun register(
        @RequestBody request: RegisterRequest
    ): ResponseEntity<Any> {

        val user = authService.register(request.pseudo, request.email, request.password)
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse("email or pseudo already used"))

        return ResponseEntity.ok(
            SessionResponse(message = "Account created !", id = user.id, pseudo = user.pseudo, false, theme = user.currentTheme)
        )
    }

    @PostMapping("/logout")
    fun logout(
        @CookieValue(name = "jwt", required = false) token: String?
    ): ResponseEntity<Any> {
        if (token == null || !jwtService.isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Not Authentified"))
        }
        val cookie = ResponseCookie.from("jwt", "")
            .httpOnly(true)
            .secure(isSslEnabled)
            .path("/")
            .maxAge(0)
            .sameSite(if (isSslEnabled) "Strict" else "Lax")
            .build()

        clientManager.clientDisconnect(jwtService.extractPlayerId(token))

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(mapOf("message" to "Déconnexion réussie"))
    }

    @GetMapping("/me")
    fun checkSession(@CookieValue("jwt", required = false) jwtCookie: String?): ResponseEntity<Any> {
        if (jwtCookie == null || !jwtService.isTokenValid(jwtCookie)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Not Authentified"))
        }

        val userId = jwtService.extractPlayerId(jwtCookie)

        val user = authService.getUserById(userId)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("User not found"))

        return ResponseEntity.ok(
            SessionResponse(id = user.id, pseudo = user.pseudo, authenticated = true, theme = user.currentTheme)
        )
    }
}

data class ErrorResponse(val error: String)
data class SessionResponse(val message : String = "", val id: String, val pseudo: String, val authenticated: Boolean, val theme : Theme?)

class JwtWebSocketInterceptor(
    private val jwtService: JwtService
) : HandshakeInterceptor {

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        if (request is ServletServerHttpRequest) {
            val servletRequest = request.servletRequest
            val jwtCookie = servletRequest.cookies?.find { it.name == "jwt" }

            if (jwtCookie != null) {
                val token = jwtCookie.value

                if (jwtService.isTokenValid(token)) {
                    val playerId = jwtService.extractPlayerId(token)

                    attributes["PLAYER_ID"] = playerId
                    attributes["playerId"] = playerId

                    val pseudo = jwtService.extractPseudo(token)
                    attributes["playerName"] = pseudo ?: playerId

                    return true
                }
            }
        }
        return false
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {}
}

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val pseudo: String,
    val email: String,
    val password: String
)

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

@Embeddable
class StatValue(
    @Column(name = "stat_value")
    var value: Int = 0,

    @Convert(converter = MapJsonConverter::class)
    @Column(name = "hover_details", columnDefinition = "TEXT")
    var hoverDetailsMap: MutableMap<String, Int> = ConcurrentHashMap()
)

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