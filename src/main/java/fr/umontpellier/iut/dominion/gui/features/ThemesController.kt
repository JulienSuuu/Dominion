package fr.umontpellier.iut.dominion.gui.features

import fr.umontpellier.iut.dominion.client.StatKey
import fr.umontpellier.iut.dominion.gui.authentification.ErrorResponse
import fr.umontpellier.iut.dominion.gui.authentification.StatValue
import fr.umontpellier.iut.dominion.gui.authentification.UserRepository
import fr.umontpellier.iut.dominion.gui.tables.Theme
import fr.umontpellier.iut.dominion.gui.tables.ThemeRepository
import fr.umontpellier.iut.dominion.gui.tables.UnlockType
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/themes")
open class ThemesController(
    private val userRepo: UserRepository,
    private val themeRepo: ThemeRepository
) {

    @GetMapping
    fun getAllThemes(): ResponseEntity<List<Theme>> {
        return ResponseEntity.ok(themeRepo.findAll())
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    open fun getMyThemes(authentication: Authentication): ResponseEntity<UserThemesResponse> {
        val userId = authentication.name

        val user = userRepo.findById(userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val response = UserThemesResponse(
            currentThemeCode = user.currentTheme?.code ?: "dominion",
            unlockedThemeCodes = user.unlockedThemes.map { it.code }
        )

        return ResponseEntity.ok(response)
    }

    @PostMapping("/unlock/{themeCode}")
    @Transactional
    open fun unlockTheme(
        authentication: Authentication,
        @PathVariable themeCode: String
    ): ResponseEntity<Any> {
        val userId = authentication.name

        val user = userRepo.findById(userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val themeToUnlock = themeRepo.findByCode(themeCode)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Thème introuvable"))

        if (user.unlockedThemes.any { it.code == themeCode }) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "Thème déjà débloqué"))
        }

        val errorMessage = when (val unlockType = themeToUnlock.unlockType) {
            UnlockType.Level ->
                if (user.level < themeToUnlock.unlockValue) "Niveau ${themeToUnlock.unlockValue} requis" else null

            is UnlockType.Stat -> {
                val actualValue = user.getStatValue(unlockType.key, unlockType.detailName)

                if (actualValue < themeToUnlock.unlockValue) {
                    val statLabel = unlockType.detailName?.let { "${unlockType.key.displayName} ($it)" }
                        ?: unlockType.key.displayName
                    "Statistique insuffisante ($statLabel: $actualValue/${themeToUnlock.unlockValue})"
                } else null
            }

            UnlockType.Purchase -> null
            is UnlockType.Achievement -> null
            UnlockType.Free -> null
        }

        if (errorMessage != null) {
            return ResponseEntity.badRequest().body(ErrorResponse(errorMessage))
        }

        if (user.unlockedThemes.none { it.code == themeToUnlock.code }) {
            user.unlockedThemes.add(themeToUnlock)
            userRepo.save(user)
        }

        val response = UserThemesResponse(
            currentThemeCode = user.currentTheme?.code ?: "dominion",
            unlockedThemeCodes = user.unlockedThemes.map { it.code }
        )

        return ResponseEntity.ok(response)
    }

    @PatchMapping("/equip/{themeCode}")
    @Transactional
    open fun equipTheme(
        authentication: Authentication,
        @PathVariable themeCode: String
    ): ResponseEntity<Map<String, String>> {
        val userId = authentication.name

        val user = userRepo.findById(userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val themeToEquip = themeRepo.findByCode(themeCode)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Thème introuvable"))

        val isUnlocked = user.unlockedThemes.any { it.code == themeCode }
        if (!isUnlocked) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "Vous n'avez pas encore débloqué ce thème"))
        }

        user.currentTheme = themeToEquip
        userRepo.save(user)

        return ResponseEntity.ok(
            mapOf(
                "message" to "Thème équipé avec succès",
                "currentTheme" to themeCode
            )
        )
    }
}

data class UserThemesResponse(
    val currentThemeCode: String,
    val unlockedThemeCodes: List<String>
)