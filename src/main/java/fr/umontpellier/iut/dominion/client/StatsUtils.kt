package fr.umontpellier.iut.dominion.client

import java.util.concurrent.ConcurrentHashMap

enum class StatKey(val displayName: String, val category: String, val type : String) {
    WINS("Victory", "General", "Info"),
    GAMES_PLAYED("Games Played", "General", "Info"),
    TOTAL_CARDS_PLAYED("Cards Played", "Actions", "Card"),
    TOTAL_VICTORY_POINTS("Victory Points", "Score", "Info"),
    RESOURCES_USED("Resources Used", "Resources", "Resources"),
    RESOURCES_OBTAINED("Resources Gained", "Resources", "Resources"),
    TOTAL_CARDS_TRASHED("Cards Trashed", "Actions", "Card"),
    TOTAL_CARDS_BOUGHT("Cards Bought", "Actions", "Card"),
    TOTAL_CARDS_GAINED("Cards Gained", "Actions", "Card"),
} 