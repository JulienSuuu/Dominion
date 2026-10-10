package fr.umontpellier.iut.dominion.application.service

import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class UiStateService {
    private val activePrompts = ConcurrentHashMap<String, Boolean>()

    fun setPromptActive(gameId: String, active: Boolean) {
        if (active) {
            activePrompts[gameId] = true
        } else {
            activePrompts.remove(gameId)
        }
    }

    fun isPromptActive(gameId: String): Boolean {
        return activePrompts[gameId] ?: false
    }
}
