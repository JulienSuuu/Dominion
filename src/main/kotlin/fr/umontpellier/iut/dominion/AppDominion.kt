package fr.umontpellier.iut.dominion

import fr.umontpellier.iut.dominion.domain.setup.card.factories.FactorySupplyPile
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy

@SpringBootApplication
@EnableAspectJAutoProxy
open class AppDominion : CommandLineRunner {

    override fun run(vararg args: String) {
        FactorySupplyPile.loadAllCards()
        println("--- Serveur Dominion (Engine + Spring Boot WS) Démarré ---")
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(AppDominion::class.java, *args)
}