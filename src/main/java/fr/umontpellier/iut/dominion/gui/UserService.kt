package fr.umontpellier.iut.dominion.gui

import fr.umontpellier.iut.dominion.client.Client
import fr.umontpellier.iut.dominion.client.StatKey
import fr.umontpellier.iut.dominion.gui.authentification.StatValue
import fr.umontpellier.iut.dominion.gui.authentification.User
import fr.umontpellier.iut.dominion.gui.authentification.UserRepository
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class UserService(
    private val userRepository: UserRepository
) {

    @Transactional(readOnly = true)
    open fun loadStatsForClient(client: Client) {
        val user = userRepository.findById(client.id).orElse(null)
        val savedStats = user?.stats ?: mutableMapOf()
        client.updateStats(savedStats)
    }

    @Async
    @Transactional
    open fun saveStatsAsync(clientId: String, statsSnapshot: Map<StatKey, StatValue>) {
        val user = userRepository.findById(clientId).orElse(null) ?: return
        user.stats.putAll(statsSnapshot)
        userRepository.save(user)
    }

    @Transactional
    open fun updateStatsAfterGame(client: Client, isWinner: Boolean, score: Int, gameId: String) {

        if (isWinner) {
            client.recordHoverDetail(StatKey.GAMES_PLAYED, "Win")
        }else client.recordHoverDetail(StatKey.GAMES_PLAYED, "Loose")

        client.recordHoverDetail(StatKey.TOTAL_VICTORY_POINTS, gameId, score)

        val user = userRepository.findById(client.id).orElse(null) ?: return
        user.stats.putAll(client.stats.toMap())
        userRepository.save(user)
    }
}