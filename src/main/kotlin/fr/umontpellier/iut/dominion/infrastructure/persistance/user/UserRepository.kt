package fr.umontpellier.iut.dominion.infrastructure.persistance.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, String> {
    fun findByEmailIgnoreCase(email: String): User?
    fun findByPseudoIgnoreCase(pseudo: String): User?
    fun existsByEmailIgnoreCaseOrPseudoIgnoreCase(email: String, pseudo: String): Boolean
}