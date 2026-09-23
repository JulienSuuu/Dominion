package fr.umontpellier.iut.dominion.gui

import fr.umontpellier.iut.dominion.gui.authentification.JwtService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtCookieFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val jwtCookie = request.cookies?.find { it.name == "jwt" }
        val token = jwtCookie?.value
            ?: request.getHeader("Authorization")?.takeIf { it.startsWith("Bearer ") }?.substring(7)

        if (token != null) {
            try {
                if (jwtService.isTokenValid(token)) {
                    val playerId = jwtService.extractPlayerId(token)

                    val auth = UsernamePasswordAuthenticationToken(
                        playerId,
                        null,
                        listOf(SimpleGrantedAuthority("ROLE_USER"))
                    )
                    SecurityContextHolder.getContext().authentication = auth
                }
            } catch (e: Exception) {
                SecurityContextHolder.clearContext()
            }
        }

        filterChain.doFilter(request, response)
    }
}