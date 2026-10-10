package fr.umontpellier.iut.dominion.infrastructure.web.auth

import fr.umontpellier.iut.dominion.application.service.AuthService
import fr.umontpellier.iut.dominion.application.service.JwtService
import fr.umontpellier.iut.dominion.infrastructure.persistance.theme.Theme
import fr.umontpellier.iut.dominion.application.service.ClientManager
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
            SessionResponse(
                message = "Account created !",
                id = user.id,
                pseudo = user.pseudo,
                false,
                theme = user.currentTheme
            )
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
data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val pseudo: String, val email: String, val password: String)