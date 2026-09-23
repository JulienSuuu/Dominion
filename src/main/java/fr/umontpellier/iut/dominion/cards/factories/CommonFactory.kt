package fr.umontpellier.iut.dominion.cards.factories

import fr.umontpellier.iut.dominion.Annotation.Description
import fr.umontpellier.iut.dominion.Annotation.Dominion_Card
import fr.umontpellier.iut.dominion.Annotation.PileType
import fr.umontpellier.iut.dominion.CardType
import fr.umontpellier.iut.dominion.Item
import fr.umontpellier.iut.dominion.cards.Bonus.Bonus
import fr.umontpellier.iut.dominion.cards.Bonus.MutableBonus
import fr.umontpellier.iut.dominion.cards.Card
import fr.umontpellier.iut.dominion.cards.CardConfigurator
import fr.umontpellier.iut.dominion.cards.Description.BasicScoreDescription
import fr.umontpellier.iut.dominion.cards.Description.BonusDescription
import fr.umontpellier.iut.dominion.cards.Description.CardCatalog
import fr.umontpellier.iut.dominion.cards.component.Price

object CommonFactory {
    fun createTreasure(name : String, cost :Int, value:Int) : Card {
        val money = Bonus.empty().with(Item.MONEY, value)
        return Card.treasure(name, Price.dominion(cost))
            .setup { onPlay(money.onPlay()) }
    }

    fun createTreasureEnvious(name : String, cost :Int, currentValue:Int) : Card {
        val bonus = MutableBonus().with(Item.MONEY){player, _ ->
            if(player.isFlagSet("Envious")) 1 else currentValue
        }
        return Card.treasure(name, Price.dominion(cost))
            .setup { simpleAction(bonus) }
    }

    fun createVictoryCard(name : String, cost :Int, value:Int) : Card {
        return Card.victory(name,Price.dominion(cost))
            .setup { score{ value } }
    }

    fun createCurseCard(name: String, cost :Int, value:Int) : Card {
        return Card(name, Price.dominion(cost), CardType.CURSE)
            .setup {
                score { value }
                onPlay {player, self -> player.increment(Item.MONEY, if (self.hasType(CardType.TREASURE)) 1 else 0)}
            }
    }

    fun createPotion(name : String, cost :Int, value:Int) : Card {
        val potion = Bonus.empty().with(Item.POTION, value)

        return Card.treasure(name, Price.dominion(cost))
            .setup { onPlay(CardConfigurator.bonus(potion)) }
    }




    @Dominion_Card(pileType = PileType.COPPER)
    @JvmStatic fun Copper(): Card = createTreasure("Copper", 0, 1)

    @Dominion_Card(pileType = PileType.SILVER)
    @JvmStatic fun Silver(): Card = createTreasureEnvious("Silver",3 , 2)

    @Dominion_Card(pileType = PileType.GOLD)
    @JvmStatic fun Gold(): Card = createTreasureEnvious("Gold", 6, 3)

    @Dominion_Card(pileType = PileType.PLATINUM)
    @JvmStatic fun Platinum(): Card = createTreasure("Platinum", 9, 5)

    @Dominion_Card(pileType = PileType.ESTATE)
    @JvmStatic fun Estate(): Card = createVictoryCard("Estate", 2, 1)

    @Dominion_Card(pileType = PileType.VICTORY)
    @JvmStatic fun Duchy(): Card = createVictoryCard("Duchy", 5, 3)

    @Dominion_Card(pileType = PileType.VICTORY)
    @JvmStatic fun Province(): Card = createVictoryCard("Province", 8, 6)

    @Dominion_Card(pileType = PileType.VICTORY)
    @JvmStatic fun Colony(): Card = createVictoryCard("Colony", 11, 10)

    @Dominion_Card(pileType = PileType.CURSE)
    @JvmStatic fun Curse(): Card = createCurseCard("Curse", 0, -1)

    @Dominion_Card(pileType = PileType.POTION)
    @JvmStatic fun Potion(): Card = createPotion("Potion", 4, 1)

    @Description
    fun commonBonusDescription(){
        CardCatalog.apply {
            registerDescription("Copper", BonusDescription().setManualAttribute("money", 1))
            registerDescription("Silver", BonusDescription().setManualAttribute("money", 2))
            registerDescription("Gold", BonusDescription().setManualAttribute("money", 3))
            registerDescription("Platinum", BonusDescription().setManualAttribute("money", 5))
            registerDescription("Potion", BonusDescription().setManualAttribute("potion", 1))
            registerDescription("Estate", BasicScoreDescription(1))
            registerDescription("Duchy", BasicScoreDescription(3))
            registerDescription("Province", BasicScoreDescription(6))
            registerDescription("Colony", BasicScoreDescription(10))
            registerDescription("Curse", BasicScoreDescription(-1))
        }
    }
}