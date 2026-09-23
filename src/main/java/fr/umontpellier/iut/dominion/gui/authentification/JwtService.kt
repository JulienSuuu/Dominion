package fr.umontpellier.iut.dominion.gui.authentification

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret}") private val secretString: String
) {

    private val secretKey: SecretKey = Keys.hmacShaKeyFor(secretString.toByteArray())

    private val expirationMs = 7 * 24 * 60 * 60 * 1000L

    fun generateToken(userId: String, email: String, pseudo : String): String {
        val now = Date()
        val expiryDate = Date(now.time + expirationMs)

        return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .claim("pseudo", pseudo)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    fun isTokenValid(token: String): Boolean {
        return try {
            val claims = getClaims(token)
            !claims.expiration.before(Date())
        } catch (e: Exception) {
            false
        }
    }

    fun extractPlayerId(token: String): String {
        return getClaims(token).subject
    }

    fun extractEmail(token: String): String? {
        return getClaims(token).get("email", String::class.java)
    }

    fun extractPseudo(token: String): String? {
        return getClaims(token).get("pseudo", String::class.java)
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}