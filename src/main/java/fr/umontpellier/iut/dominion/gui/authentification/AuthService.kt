package fr.umontpellier.iut.dominion.gui.authentification

import fr.umontpellier.iut.dominion.gui.tables.ThemeRepository
import jakarta.annotation.PostConstruct
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val themeRepository: ThemeRepository
) {

    private val passwordEncoder = BCryptPasswordEncoder()

    @PostConstruct
    fun initTestUser() {
        if (userRepository.findByEmailIgnoreCase("joueur1@gmail.com") == null) {
            val theme = themeRepository.findByCode("dominion") ?: throw IllegalArgumentException("Theme not found")

            val testUser = User(
                id = "player_test_1",
                email = "joueur1@gmail.com",
                pseudo = "Arthur",
                passwordHash = passwordEncoder.encode("password123"),
                currentTheme = theme,
                unlockedThemes = mutableSetOf(theme)

            )
            userRepository.save(testUser)
        }
    }

    fun getUserById(id: String): User? {
        return userRepository.findById(id).orElse(null)
    }

    fun authenticate(email: String, rawPassword: String): User? {
        val user = userRepository.findByEmailIgnoreCase(email.trim()) ?: return null

        return if (passwordEncoder.matches(rawPassword, user.passwordHash)) {
            user
        } else {
            null
        }
    }

    fun register(pseudo: String, email: String, rawPassword: String): User? {
        val cleanEmail = email.trim().lowercase()
        val cleanPseudo = pseudo.trim()

        if (userRepository.existsByEmailIgnoreCaseOrPseudoIgnoreCase(cleanEmail, cleanPseudo)) {
            return null
        }

        val theme = themeRepository.findByCode("dominion") ?: return null

        val hashedPassword = passwordEncoder.encode(rawPassword)

        val newUser = User(
            id = "player_${UUID.randomUUID()}",
            email = cleanEmail,
            pseudo = cleanPseudo,
            passwordHash = hashedPassword,
            currentTheme = theme,
            unlockedThemes = mutableSetOf(theme)
        )

        return userRepository.save(newUser)
    }
}


@Repository
interface UserRepository : JpaRepository<User, String> {
    fun findByEmailIgnoreCase(email: String): User?
    fun findByPseudoIgnoreCase(pseudo: String): User?
    fun existsByEmailIgnoreCaseOrPseudoIgnoreCase(email: String, pseudo: String): Boolean
}