package fr.umontpellier.iut.dominion

import fr.umontpellier.iut.dominion.cards.factories.*
import kotlinx.coroutines.CoroutineScope
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking

@SpringBootApplication
@EnableAspectJAutoProxy
open class AppDominion: CommandLineRunner {

    override fun run(vararg args: String): Unit = runBlocking {
        FactorySupplyPile.loadAllCards()

        println("--- Serveur Dominion (Engine + Spring Boot REST/WS) Démarré ---")
        awaitCancellation()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(AppDominion::class.java, *args)
        }
    }
}